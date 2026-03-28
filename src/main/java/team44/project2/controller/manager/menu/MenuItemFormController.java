package team44.project2.controller.manager.menu;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import team44.project2.model.menu.MenuItem;
import team44.project2.model.menu.MenuItemContent;
import team44.project2.service.MenuService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the menu-item form dialog used by managers to add new menu items or
 * edit existing ones, including name, category, size, price, availability, and the
 * associated inventory ingredient requirements.
 */
@Dependent
public class MenuItemFormController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> categoryBox;

    @FXML
    private TextField sizeField;

    @FXML
    private TextField priceField;

    @FXML
    private CheckBox availableCheck;

    @FXML
    private VBox inventoryLinksContainer;

    private MenuItem editingItem;
    private final List<InventoryRequirementRow> requirementRows = new ArrayList<>();

    @Inject
    private MenuService menuService;

    /**
     * Populates the category combo box with existing categories and enables free-text
     * entry for new ones.
     */
    @FXML
    private void initialize() {
        categoryBox.getItems().setAll(menuService.getCategories());
        categoryBox.setEditable(true);
    }

    /**
     * Handles the Add Ingredient Row button action by appending a new empty
     * {@link InventoryRequirementRow} to the ingredient list section of the form.
     */
    @FXML
    private void handleAddIngredientRow() {
        InventoryRequirementRow row = new InventoryRequirementRow();
        requirementRows.add(row);

        int buttonIndex = inventoryLinksContainer.getChildren().size() - 1;
        inventoryLinksContainer.getChildren().add(buttonIndex, row.getView());
    }

    /**
     * Pre-populates the form fields with an existing menu item's data for editing.
     * When {@code item} is {@code null} the form is treated as an add-new form.
     *
     * @param item The item to edit, or {@code null} to create a new item.
     */
    public void setEditingItem(MenuItem item) {
        this.editingItem = item;
        if (item != null) {
            nameField.setText(item.name());
            categoryBox.setValue(item.category());
            sizeField.setText(item.size());
            priceField.setText(item.basePrice().toString());
            availableCheck.setSelected(item.isAvailable());
        }
    }

    /**
     * Handles the Save button action. Validates all required fields, creates or updates
     * the menu item through the service, saves any ingredient relationship rows, and
     * closes the dialog window.
     */
    @FXML
    private void handleSave() {
        String name = nameField.getText();
        String category = categoryBox.getEditor().getText();
        String size = sizeField.getText();
        String priceText = priceField.getText();

        if (name == null || name.isBlank() || category == null || category.isBlank() || priceText == null || priceText.isBlank()) {
            showAlert("Validation Error", "Please fill in all required fields.");
            return;
        }

        try {
            BigDecimal price = new BigDecimal(priceText);

            if (editingItem == null) {
                MenuItem newItem = new MenuItem(0, name, category, size, price, availableCheck.isSelected());
                int newId = menuService.addMenuItemAndGetId(newItem);

                for (InventoryRequirementRow row : requirementRows) {
                    if (row.isValid()) {
                        row.saveRelationship(newId);
                    }
                }
            } else {
                MenuItem updatedItem = new MenuItem(editingItem.menuItemId(), name, category, size, price, availableCheck.isSelected());
                menuService.updateMenuItem(updatedItem);
            }

            closeWindow();

        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Price and Quantities must be valid numbers.");
        }
    }

    /**
     * Displays a warning alert dialog with the given title and message.
     *
     * @param title   The dialog title.
     * @param message The warning message to display.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Closes the menu-item form window.
     */
    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    /**
     * Handles the Cancel button action by closing the window without saving.
     */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /**
     * Represents a single ingredient requirement row in the menu-item form, allowing
     * the manager to link an inventory item and specify the recipe quantity needed.
     */
    private class InventoryRequirementRow {
        private final ComboBox<String> nameCombo = new ComboBox<>();
        private final TextField categoryField = new TextField();
        private final TextField currentQtyField = new TextField();
        private final TextField unitField = new TextField();
        private final TextField reorderField = new TextField();
        private final TextField costField = new TextField();
        private final TextField qtyField = new TextField();
        private final HBox layout;

        // Cache this to avoid hitting the database on every keystroke
        private final List<String> knownInventoryNames;

        public InventoryRequirementRow() {
            knownInventoryNames = menuService.getAllInventoryNames();

            nameCombo.setPromptText("Ingredient Name");
            nameCombo.setEditable(true);
            nameCombo.setPrefWidth(140);
            nameCombo.getItems().setAll(knownInventoryNames);

            categoryField.setPromptText("Category"); categoryField.setPrefWidth(80);
            currentQtyField.setPromptText("Curr Qty"); currentQtyField.setPrefWidth(70);
            unitField.setPromptText("Unit"); unitField.setPrefWidth(60);
            reorderField.setPromptText("Reorder"); reorderField.setPrefWidth(70);
            costField.setPromptText("Cost"); costField.setPrefWidth(60);
            qtyField.setPromptText("Recipe Qty"); qtyField.setPrefWidth(80);

            // Dynamically disable/enable fields based on whether the item exists
            nameCombo.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
                boolean exists = knownInventoryNames.contains(newValue);
                categoryField.setDisable(exists);
                currentQtyField.setDisable(exists);
                unitField.setDisable(exists);
                reorderField.setDisable(exists);
                costField.setDisable(exists);
            });

            layout = new HBox(10, nameCombo, categoryField, currentQtyField, unitField, reorderField, costField, qtyField);
            layout.setStyle("-fx-padding: 5; -fx-alignment: center-left;");
        }

        public HBox getView() {
            return layout;
        }

        public boolean isValid() {
            return !nameCombo.getEditor().getText().isBlank() && !qtyField.getText().isBlank();
        }

        public void saveRelationship(int menuItemId) {
            String name = nameCombo.getEditor().getText();
            boolean exists = knownInventoryNames.contains(name);

            // If it exists, we pass empty/zero values because getOrAddInventoryItem will just pull the existing ID
            String cat = exists ? "" : categoryField.getText();
            String unit = exists ? "" : unitField.getText();
            BigDecimal currentQty = exists || currentQtyField.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(currentQtyField.getText());
            BigDecimal reorder = exists || reorderField.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(reorderField.getText());
            BigDecimal cost = exists || costField.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(costField.getText());

            BigDecimal recipeQty = new BigDecimal(qtyField.getText());

            int inventoryId = menuService.getOrAddInventoryItem(name, cat, currentQty, unit, reorder, cost);
            if (inventoryId != -1) {
                menuService.addMenuItemContent(new MenuItemContent(menuItemId, inventoryId, recipeQty));
            }
        }
    }
}