package team44.project2.controller.manager.reports;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.Stage;
import team44.project2.model.report.ProductSalesData;
import team44.project2.service.ReportService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the sales-report screen in the manager dashboard, showing aggregated
 * sales data (quantity sold and revenue) per menu item over a date range selected by
 * the manager.
 */
@Dependent
public class SalesReportController implements Initializable {

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TableView<ProductSalesData> salesTable;

    @FXML
    private TableColumn<ProductSalesData, String> itemNameColumn;

    @FXML
    private TableColumn<ProductSalesData, Integer> quantityColumn;

    @FXML
    private TableColumn<ProductSalesData, BigDecimal> revenueColumn;

    @FXML
    private MenuItem xReportLinkButton;

    @FXML
    private MenuItem zReportLinkButton;

    @FXML
    private MenuItem orderingTrendsLinkButton;

    @Inject
    ReportService reportService;

    @Inject
    Instance<Object> cdiInstance;

    private LocalDate selectedDate;
    private LocalDate endDate;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();

        LocalDate now = LocalDate.now();
        selectedDate = now.withDayOfMonth(1);
        endDate = now;

        setupDatePickers();
        startDatePicker.setValue(selectedDate);
        endDatePicker.setValue(endDate);

        setupNavigationMenus();
        Log.info("SalesReportController initialized with date range: " + selectedDate + " to " + endDate);
        loadSalesData();
    }

    /**
     * Configures table column value factories to extract and display the appropriate
     * fields from {@link team44.project2.model.report.ProductSalesData} records.
     */
    private void setupTableColumns() {
        itemNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().productName()));
        quantityColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().quantity()));
        revenueColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().totalPrice()));
    }

    /**
     * Registers change listeners on the start and end date pickers so that the sales
     * table is reloaded whenever the date range is modified. Enforces that the start
     * date never exceeds the end date.
     */
    private void setupDatePickers() {
        startDatePicker.setOnAction(e -> {
            if (startDatePicker.getValue() != null) {
                selectedDate = startDatePicker.getValue();
                if (endDate.isBefore(selectedDate)) {
                    endDate = selectedDate;
                    endDatePicker.setValue(endDate);
                }
                loadSalesData();
            }
        });

        endDatePicker.setOnAction(e -> {
            if (endDatePicker.getValue() != null) {
                endDate = endDatePicker.getValue();
                if (selectedDate.isAfter(endDate)) {
                    selectedDate = endDate;
                    startDatePicker.setValue(selectedDate);
                }
                loadSalesData();
            }
        });

        startDatePicker.setDisable(false);
        endDatePicker.setDisable(false);
    }

    /**
     * Wires the report-navigation menu items to their respective FXML destination paths.
     */
    private void setupNavigationMenus() {
        xReportLinkButton.setOnAction(e -> navigateToReport("/fxml/manager/reports/XReport.fxml"));
        zReportLinkButton.setOnAction(e -> {
            // TODO: implement when z-report screen exists
        });
        orderingTrendsLinkButton.setOnAction(e -> navigateToReport("/fxml/manager/reports/OrderingTrends.fxml"));
    }

    /**
     * Fetches raw product-sales data for the current date range, aggregates quantities
     * and revenues by item name, sorts alphabetically, and populates the table.
     */
    private void loadSalesData() {
        try {
            Log.info("loadSalesData called with dates: " + selectedDate + " to " + endDate);
            
            // Use the proven working method and aggregate locally
            List<ProductSalesData> allSalesData = reportService.getProductSales(selectedDate, endDate);
            Log.info("Received " + allSalesData.size() + " line items from service");
            
            // Aggregate by item name
            Map<String, Integer> totalQuantityByItem = new HashMap<>();
            Map<String, BigDecimal> totalRevenueByItem = new HashMap<>();
            
            for (ProductSalesData data : allSalesData) {
                String itemName = data.productName();
                totalQuantityByItem.merge(itemName, data.quantity(), Integer::sum);
                totalRevenueByItem.merge(itemName, data.totalPrice(), BigDecimal::add);
            }
            
            // Convert aggregated data to ProductSalesData list
            List<ProductSalesData> aggregatedSales = new ArrayList<>();
            for (String itemName : totalQuantityByItem.keySet()) {
                aggregatedSales.add(new ProductSalesData(
                    null,  // saleDate not applicable for aggregated view
                    itemName,
                    totalQuantityByItem.get(itemName),
                    totalRevenueByItem.get(itemName)
                ));
            }
            
            // Sort by item name
            aggregatedSales.sort((a, b) -> a.productName().compareTo(b.productName()));
            
            Log.info("Aggregated to " + aggregatedSales.size() + " unique items");
            
            salesTable.getItems().clear();
            salesTable.getItems().addAll(aggregatedSales);
            Log.info("Table updated with " + salesTable.getItems().size() + " items");
        } catch (Exception e) {
            Log.error("Failed to load sales data", e);
            e.printStackTrace();
            showError("Failed to load sales data: " + e.getMessage());
        }
    }

    /**
     * Utility method to load another FXML report screen. Used by the menu link buttons.
     */
    private void navigateToReport(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());

            Stage stage = (Stage) salesTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            Log.error("Failed to navigate to report page: " + fxmlPath, e);
        }
    }

    /**
     * Handles the Back button action by navigating to the manager dashboard.
     */
    @FXML
    private void handleBack() {
        navigateToReport("/fxml/manager/ManagerDashboard.fxml");
    }

    /**
     * Displays an error alert with the given message.
     *
     * @param message The error description to show in the alert body.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Data Load Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
