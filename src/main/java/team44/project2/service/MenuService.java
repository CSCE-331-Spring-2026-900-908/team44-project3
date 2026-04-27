package team44.project2.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;
import team44.project2.model.menu.MenuItem;
import team44.project2.model.menu.MenuItemContent;
import team44.project2.model.menu.MenuItemContentWithName;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class responsible for all menu-item database operations, including fetching
 * categories, items by category, available sizes, add-ons, and performing create, update,
 * and delete operations on menu items and their inventory ingredient links.
 */
@ApplicationScoped
public class MenuService {
    //sql queries for menu operations
    private static final String GET_CATEGORIES = """
            SELECT DISTINCT category
            FROM menu_items
            WHERE is_available = true
            AND category != 'topping'
            ORDER BY category
            """;

    private static final String MENU_COLS = """
            menu_item_id, name, category, size, base_price, is_available, is_hot, EXISTS (SELECT 1 FROM menu_item_images WHERE menu_item_images.name = menu_items.name) AS has_image""";

    private static final String GET_ITEMS_BY_CATEGORY = """
            SELECT %s
            FROM menu_items
            WHERE category = ?
            AND is_available = true
            ORDER BY name
            """.formatted(MENU_COLS);

    private static final String GET_SIZES_FOR_ITEM = """
            SELECT DISTINCT size
            FROM menu_items
            WHERE name = (
                SELECT name
                FROM menu_items
                WHERE menu_item_id = ?
            )
            AND is_available = true
            AND size IS NOT NULL
            ORDER BY size
            """;

    private static final String GET_ADD_ONS = """
            SELECT %s
            FROM menu_items
            WHERE category = 'topping'
            AND is_available = true
            ORDER BY name
            """.formatted(MENU_COLS);

    private static final String GET_ALL_MENU_ITEMS = """
            SELECT %s
            FROM menu_items
            WHERE is_available = true
            AND category != 'topping'
            ORDER BY category, name
            """.formatted(MENU_COLS);

    private static final String GET_TOP_ITEMS = """
            SELECT %s
            FROM menu_items
            WHERE is_available = true
              AND menu_items.name IN (
                SELECT m.name
                FROM order_items oi
                JOIN orders o ON oi.order_id = o.order_id
                JOIN menu_items m ON oi.menu_item_id = m.menu_item_id
                WHERE o.timestamp >= NOW() - (? * INTERVAL '1 day')
                  AND oi.parent_item_id IS NULL
                  AND m.category != 'topping'
                GROUP BY m.name
                ORDER BY SUM(oi.quantity) DESC
                LIMIT ?
              )
            ORDER BY menu_items.name, menu_items.size
            """.formatted(MENU_COLS);

    private static final String INSERT_CONTENT = """
            INSERT INTO menu_item_contents (menu_item_id, inventory_id, quantity)
            VALUES (?, ?, ?)
            """;
    private static final String INSERT_ITEM = """
            INSERT INTO menu_items (name, category, size, base_price, is_available, is_hot)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String GET_INVENTORY_ID_BY_NAME = "SELECT inventory_id FROM inventory WHERE item_name = ?";

    private static final String INSERT_INVENTORY_ITEM = """
            INSERT INTO inventory (item_name, category, current_quantity, unit, reorder_threshold, supplier_cost)
            VALUES (?, ?, ?, ?, ?, ?)
            """;


    @Inject
    DataSource dataSource;

    /**
     * Returns all distinct non-topping menu categories available in the database.
     *
     * @return An ordered list of category name strings; empty if none exist.
     */
    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_CATEGORIES)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (Exception e) {
            Log.error("Failed to load categories", e);
        }

        return categories;
    }

    /**
     * Returns every available (non-topping) menu item regardless of category.
     *
     * @return An ordered list of {@link team44.project2.model.menu.MenuItem} records.
     */
    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> items = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_ALL_MENU_ITEMS)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(mapMenuItem(rs));
            }
        } catch (Exception e) {
            Log.error("Failed to load all menu items", e);
        }

        return items;
    }

    /**
     * Returns all variants of the top-N most-purchased menu items in the past
     * {@code days} days, ranked by total quantity sold. Add-ons and toppings are
     * excluded from the ranking.
     *
     * @param days  Lookback window in days.
     * @param limit Number of distinct item names to return variants for.
     * @return Menu item variants for the top-N names, ordered by name then size.
     */
    public List<MenuItem> getTopItems(int days, int limit) {
        List<MenuItem> items = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_TOP_ITEMS)) {
            stmt.setInt(1, days);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(mapMenuItem(rs));
            }
        } catch (Exception e) {
            Log.error("Failed to load top items", e);
        }

        return items;
    }

    /**
     * Returns all available menu items belonging to the given category.
     *
     * @param category The category name to filter by.
     * @return An ordered list of matching {@link team44.project2.model.menu.MenuItem}
     *         records; empty if the category has no items.
     */
    public List<MenuItem> getItemsByCategory(String category) {
        List<MenuItem> items = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_ITEMS_BY_CATEGORY)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                items.add(mapMenuItem(rs));
            }
        } catch (Exception e) {
            Log.error("Failed to load items for category: " + category, e);
        }

        return items;
    }

    /**
     * Returns the distinct size options for the menu item identified by
     * {@code menuItemId}. Sizes are shared across all variants of the same name.
     *
     * @param menuItemId The ID of the base menu item.
     * @return An ordered list of size strings (e.g. {@code "small"}, {@code "large"}).
     */
    public List<String> getSizesForItem(int menuItemId) {
        List<String> sizes = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_SIZES_FOR_ITEM)) {
            stmt.setInt(1, menuItemId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sizes.add(rs.getString("size"));
            }
        } catch (Exception e) {
            Log.error("Failed to load sizes", e);
        }

        return sizes;
    }

    /**
     * Returns all available add-on items (items with category {@code 'topping'}).
     *
     * @return An ordered list of add-on {@link team44.project2.model.menu.MenuItem}
     *         records.
     */
    public List<MenuItem> getAddOns() {
        List<MenuItem> addOns = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_ADD_ONS)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                addOns.add(mapMenuItem(rs));
            }
        } catch (Exception e) {
            Log.error("Failed to load add-ons", e);
        }

        return addOns;
    }

    private MenuItem mapMenuItem(ResultSet rs) throws Exception {
        return new MenuItem(
                rs.getInt("menu_item_id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("size"),
                rs.getBigDecimal("base_price"),
                rs.getBoolean("is_available"),
                rs.getBoolean("is_hot"),
                rs.getBoolean("has_image")
        );
    }

    /**
     * Persists changes to an existing menu item's name, category, size, price, and
     * availability.
     *
     * @param item The item to update; must have a valid {@code menuItemId}.
     */
    public void updateMenuItem(MenuItem item) {
        String UPDATE_ITEM = """
                UPDATE menu_items
                SET name = ?, category = ?, size = ?, base_price = ?, is_available = ?, is_hot = ?
                WHERE menu_item_id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_ITEM)) {
            ps.setString(1, item.name().toLowerCase());
            ps.setString(2, item.category().toLowerCase());
            ps.setString(3, item.size());
            ps.setBigDecimal(4, item.basePrice());
            ps.setBoolean(5, item.isAvailable());
            ps.setBoolean(6, item.isHot());
            ps.setInt(7, item.menuItemId());
            ps.executeUpdate();
        } catch (Exception e) {
            Log.error("Failed to update menu item", e);
        }
    }

    /**
     * Permanently removes the menu item with the given ID.
     *
     * @param menuItemId The primary key of the item to delete.
     */
    public void deleteMenuItem(int menuItemId) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM menu_item_contents WHERE menu_item_id = ?")) {
                ps.setInt(1, menuItemId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM menu_items WHERE menu_item_id = ?")) {
                ps.setInt(1, menuItemId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            Log.error("Failed to delete menu item", e);
        }
    }

    /**
     * Inserts a new menu item and returns the generated primary key.
     *
     * @param item The item data to insert (ID field is ignored).
     * @return The generated {@code menu_item_id}, or {@code -1} if the insert failed.
     */
    public int addMenuItemAndGetId(MenuItem item) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_ITEM, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.name().toLowerCase());
            ps.setString(2, item.category().toLowerCase());
            ps.setString(3, item.size());
            ps.setBigDecimal(4, item.basePrice());
            ps.setBoolean(5, item.isAvailable());
            ps.setBoolean(6, item.isHot());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : -1;
        } catch (Exception e) {
            Log.error("Failed to add menu item", e);
            return -1;
        }
    }

    /**
     * Links an inventory ingredient to a menu item by inserting a
     * {@link team44.project2.model.menu.MenuItemContent} record.
     *
     * @param content The join-record describing which ingredient and how much.
     */
    public void addMenuItemContent(MenuItemContent content) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_CONTENT)) {
            ps.setInt(1, content.menuItemId());
            ps.setInt(2, content.inventoryId());
            ps.setBigDecimal(3, content.quantity());
            ps.executeUpdate();
        } catch (Exception e) {
            Log.error("Failed to link inventory", e);
        }
    }

    public List<MenuItemContentWithName> getContentsForItem(int menuItemId) {
        List<MenuItemContentWithName> contents = new ArrayList<>();
        String query = "SELECT mic.menu_item_id, mic.inventory_id, mic.quantity, i.item_name FROM menu_item_contents mic JOIN inventory i ON i.inventory_id = mic.inventory_id WHERE mic.menu_item_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, menuItemId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                contents.add(new MenuItemContentWithName(
                        rs.getInt("menu_item_id"),
                        rs.getInt("inventory_id"),
                        rs.getBigDecimal("quantity"),
                        rs.getString("item_name")
                ));
            }
        } catch (Exception e) {
            Log.error("Failed to load contents for item " + menuItemId, e);
        }
        return contents;
    }

    public void replaceContents(int menuItemId, List<MenuItemContent> contents) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM menu_item_contents WHERE menu_item_id = ?")) {
                    ps.setInt(1, menuItemId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(INSERT_CONTENT)) {
                    for (MenuItemContent c : contents) {
                        ps.setInt(1, menuItemId);
                        ps.setInt(2, c.inventoryId());
                        ps.setBigDecimal(3, c.quantity());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            Log.error("Failed to replace contents for item " + menuItemId, e);
        }
    }

    /**
     * Returns the ID of an existing inventory item with the given name, or inserts a new
     * one if no match is found.
     *
     * @param name             The item name to look up or create.
     * @param category         The inventory category.
     * @param currentQty       The initial stock quantity.
     * @param unit             The unit of measurement.
     * @param reorderThreshold The quantity at which a restock should be triggered.
     * @param cost             The supplier cost per unit.
     * @return The existing or newly generated {@code inventory_id}, or {@code -1} on
     *         error.
     */
    public int getOrAddInventoryItem(String name, String category, BigDecimal currentQty, String unit, BigDecimal reorderThreshold, BigDecimal cost) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(GET_INVENTORY_ID_BY_NAME)) {
                ps.setString(1, name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("inventory_id");
            }
            try (PreparedStatement ps = conn.prepareStatement(INSERT_INVENTORY_ITEM, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, category);
                ps.setBigDecimal(3, currentQty);
                ps.setString(4, unit);
                ps.setBigDecimal(5, reorderThreshold);
                ps.setBigDecimal(6, cost);
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            Log.error("Error adding inventory: " + name, e);
            return -1;
        }
    }

    /**
     * Returns the names of all inventory items, ordered alphabetically, for use in
     * ingredient-row combo boxes.
     *
     * @return An ordered list of inventory item name strings.
     */
    public List<String> getAllInventoryNames() {
        List<String> names = new ArrayList<>();
        String query = "SELECT item_name FROM inventory ORDER BY item_name";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) names.add(rs.getString("item_name"));
        } catch (Exception e) {
            Log.error("Failed to load inventory names", e);
        }
        return names;
    }

    public List<MenuItem> getAllMenuItemsForManager() {
        List<MenuItem> items = new ArrayList<>();
        String query = "SELECT %s FROM menu_items WHERE category != 'topping' ORDER BY name, size".formatted(MENU_COLS);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) items.add(mapMenuItem(rs));
        } catch (Exception e) {
            Log.error("Failed to load all menu items for manager", e);
        }
        return items;
    }

    public void updateSharedFields(String originalName, String newName, String category, boolean isHot) {
        String query = "UPDATE menu_items SET name = ?, category = ?, is_hot = ? WHERE name = ? AND category != 'topping'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, newName.toLowerCase());
            ps.setString(2, category.toLowerCase());
            ps.setBoolean(3, isHot);
            ps.setString(4, originalName);
            ps.executeUpdate();
        } catch (Exception e) {
            Log.error("Failed to batch update shared fields", e);
        }
        if (!originalName.equalsIgnoreCase(newName)) {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE menu_item_images SET name = ? WHERE name = ?")) {
                ps.setString(1, newName.toLowerCase());
                ps.setString(2, originalName);
                ps.executeUpdate();
            } catch (Exception e) {
                Log.error("Failed to rename image entry", e);
            }
        }
    }

    public void saveImage(String itemName, byte[] imageData) {
        String query = "INSERT INTO menu_item_images (name, image) VALUES (?, ?) ON CONFLICT (name) DO UPDATE SET image = EXCLUDED.image";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, itemName.toLowerCase());
            ps.setBytes(2, imageData);
            ps.executeUpdate();
        } catch (Exception e) {
            Log.error("Failed to save image for " + itemName, e);
        }
    }

    public void deleteImage(String itemName) {
        String query = "DELETE FROM menu_item_images WHERE name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, itemName);
            ps.executeUpdate();
        } catch (Exception e) {
            Log.error("Failed to delete image for " + itemName, e);
        }
    }

    public byte[] getImageByName(String itemName) {
        String query = "SELECT image FROM menu_item_images WHERE name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, itemName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBytes("image");
        } catch (Exception e) {
            Log.error("Failed to load image for " + itemName, e);
        }
        return null;
    }
}