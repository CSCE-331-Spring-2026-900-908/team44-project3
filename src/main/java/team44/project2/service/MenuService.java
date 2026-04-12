package team44.project2.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;
import team44.project2.model.menu.MenuItem;
import team44.project2.model.menu.MenuItemContent;

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

    private static final String GET_ITEMS_BY_CATEGORY = """
            SELECT *
            FROM menu_items
            WHERE category = ?
            AND is_available = true
            ORDER BY name
            """;

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
            SELECT *
            FROM menu_items
            WHERE category = 'topping'
            AND is_available = true
            ORDER BY name
            """;

    private static final String GET_ALL_MENU_ITEMS = """
            SELECT *
            FROM menu_items
            WHERE is_available = true
            AND category != 'topping'
            ORDER BY category, name
            """;

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
                rs.getBoolean("is_hot")
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
            ps.setString(1, item.name());
            ps.setString(2, item.category());
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
        String DELETE_ITEM = "DELETE FROM menu_items WHERE menu_item_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_ITEM)) {
            ps.setInt(1, menuItemId);
            ps.executeUpdate();
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
            ps.setString(1, item.name());
            ps.setString(2, item.category());
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
}