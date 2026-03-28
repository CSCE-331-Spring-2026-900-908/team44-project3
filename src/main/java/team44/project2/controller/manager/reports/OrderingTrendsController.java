package team44.project2.controller.manager.reports;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import team44.project2.model.report.DailySalesSummary;
import team44.project2.model.report.ProductSalesData;
import team44.project2.service.ReportService;
import team44.project2.model.report.InventoryUsageData;

import java.net.URL;
import java.time.LocalDate;
import java.util.*;

/**
 * Controller for the ordering-trends report screen in the manager dashboard, displaying
 * line charts for product sales and dollar sales over a manager-selected date range,
 * with optional log-scale Y-axis and navigation to other report screens.
 */
@Dependent
public class OrderingTrendsController implements Initializable {

    @FXML
    private LineChart<String, Number> orderingTrendsProductSalesChart;

    @FXML
    private LineChart<String, Number> orderingTrendsDollarSalesChart;

    @FXML
    private DatePicker orderingTrendsStartDatePicker;

    @FXML
    private DatePicker orderingTrendsEndDatePicker;

    @FXML
    private Button productLogButton;

    @FXML
    private Button dollarLogButton;

    // menu items added to support navigation between report screens
    @FXML
    private MenuItem xReportLinkButton;

    @FXML
    private MenuItem zReportLinkButton;

    @FXML
    private MenuItem salesReportLinkButton;

    @FXML
    private Label orderingTrendsProductLabel;

    @FXML
    private Label orderingTrendsDollarLabel;

    @Inject
    ReportService reportService;

    @Inject
    Instance<Object> cdiInstance;

    private LocalDate selectedDate;
    private LocalDate endDate;

    private Map<LocalDate, Map<String, Integer>> lastProductSalesData = new TreeMap<>();
    private Map<LocalDate, Double> lastDollarSalesData = new TreeMap<>();

    private boolean productLogScale = false;
    private boolean dollarLogScale = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCharts();
        setupDatePicker();

        LocalDate now = LocalDate.now();
        selectedDate = now.withDayOfMonth(1);
        endDate = now;

        orderingTrendsStartDatePicker.setValue(selectedDate);
        orderingTrendsEndDatePicker.setValue(endDate);

        productLogButton.setOnAction(e -> {
            productLogScale = !productLogScale;
            productLogButton.setText(productLogScale ? "Linear Y" : "Log Y");
            populateProductSalesChart(lastProductSalesData);
        });

        dollarLogButton.setOnAction(e -> {
            dollarLogScale = !dollarLogScale;
            dollarLogButton.setText(dollarLogScale ? "Linear Y" : "Log Y");
            populateDollarSalesChart(lastDollarSalesData);
        });

        
        xReportLinkButton.setOnAction(e -> navigateToReport("/fxml/manager/reports/XReport.fxml"));
        zReportLinkButton.setOnAction(e -> navigateToReport("/fxml/manager/reports/ZReport.fxml"));
        salesReportLinkButton.setOnAction(e -> navigateToReport("/fxml/manager/reports/SalesReport.fxml"));

        loadOrderingTrendsAsync();
    }

    /**
     * Sets axis labels and titles for each of the three line charts (product sales, dollar
     * sales, inventory usage) and disables chart animations for smoother updates.
     */
    private void setupCharts() {
        // Configure product sales chart
        CategoryAxis productXAxis = (CategoryAxis) orderingTrendsProductSalesChart.getXAxis();
        productXAxis.setLabel("Date");
        NumberAxis productYAxis = (NumberAxis) orderingTrendsProductSalesChart.getYAxis();
        productYAxis.setLabel("Quantity Sold");
        orderingTrendsProductSalesChart.setTitle("Product Sales Over Time");
        orderingTrendsProductSalesChart.setAnimated(false);

        // Configure dollar sales chart
        CategoryAxis dollarXAxis = (CategoryAxis) orderingTrendsDollarSalesChart.getXAxis();
        dollarXAxis.setLabel("Date");
        NumberAxis dollarYAxis = (NumberAxis) orderingTrendsDollarSalesChart.getYAxis();
        dollarYAxis.setLabel("Total Sales ($)");
        orderingTrendsDollarSalesChart.setTitle("Dollar Sales Over Time");
        orderingTrendsDollarSalesChart.setAnimated(false);

        // Configure inventory usage chart
        CategoryAxis inventoryXAxis = (CategoryAxis) inventoryUsageChart.getXAxis();
        inventoryXAxis.setLabel("Date");
        NumberAxis inventoryYAxis = (NumberAxis) inventoryUsageChart.getYAxis();
        inventoryYAxis.setLabel("Inventory Used");
        inventoryUsageChart.setTitle("Inventory Usage Over Time");
        inventoryUsageChart.setAnimated(false);
    }

    /**
     * Wires date-picker change listeners to reload data whenever the selected range is
     * modified. Enforces that the start date never exceeds the end date and vice versa.
     */
    private void setupDatePicker() {
        orderingTrendsStartDatePicker.setOnAction(e -> {
            if (orderingTrendsStartDatePicker.getValue() != null) {
                selectedDate = orderingTrendsStartDatePicker.getValue();
                if (endDate.isBefore(selectedDate)) {
                    endDate = selectedDate;
                    orderingTrendsEndDatePicker.setValue(endDate);
                }
                loadOrderingTrendsAsync();
            }
        });

        orderingTrendsEndDatePicker.setOnAction(e -> {
            if (orderingTrendsEndDatePicker.getValue() != null) {
                endDate = orderingTrendsEndDatePicker.getValue();
                if (selectedDate.isAfter(endDate)) {
                    selectedDate = endDate;
                    orderingTrendsStartDatePicker.setValue(selectedDate);
                }
                loadOrderingTrends();
            }
        });

        orderingTrendsStartDatePicker.setDisable(false);
        orderingTrendsEndDatePicker.setDisable(false);
    }

    /**
     * Loads ordering-trends data (product sales, daily dollar totals, and inventory usage)
     * synchronously and refreshes all three charts.
     */
    private void loadOrderingTrends() {
        try {
            List<ProductSalesData> productSales = reportService.getProductSales(selectedDate, endDate);
            List<DailySalesSummary> dailySummaries = reportService.getDailySalesSummary(selectedDate, endDate);

            Map<LocalDate, Map<String, Integer>> productSalesData = new TreeMap<>();
            Map<LocalDate, Double> dollarSalesData = new TreeMap<>();

            for (ProductSalesData data : productSales) {
                productSalesData
                        .computeIfAbsent(data.saleDate(), k -> new HashMap<>())
                        .put(data.productName(), data.quantity());
            }

            for (DailySalesSummary summary : dailySummaries) {
                dollarSalesData.put(summary.saleDate(), summary.totalSales().doubleValue());
            }

            lastProductSalesData = productSalesData;
            lastDollarSalesData = dollarSalesData;

            populateProductSalesChart(lastProductSalesData);
            populateDollarSalesChart(lastDollarSalesData);
        } catch (Exception e) {
            Log.error("Failed to load ordering trends", e);
            showError("Failed to load ordering trends: " + e.getMessage());
        }

        List<InventoryUsageData> inventoryUsage =
        reportService.getInventoryUsage(selectedDate, endDate);

        Map<LocalDate, Double> inventoryUsageData = new TreeMap<>();

        for (InventoryUsageData data : inventoryUsage) {
            inventoryUsageData.merge(
                    data.usageDate(),
                    data.quantityUsed().doubleValue(),
                    Double::sum
            );
        }

        lastInventoryUsageData = inventoryUsageData;
        populateInventoryUsageChart(lastInventoryUsageData);
    }

    /**
     * Rebuilds the product-sales line chart from the given aggregated data. Applies a
     * log10 transform to Y values when log scale is active.
     *
     * @param productSalesData A date-keyed map of product-name-to-quantity entries.
     */
    private void populateProductSalesChart(Map<LocalDate, Map<String, Integer>> productSalesData) {
        orderingTrendsProductSalesChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(productLogScale ? "Total Products Sold (log10)" : "Total Products Sold");

        for (Map.Entry<LocalDate, Map<String, Integer>> entry : productSalesData.entrySet()) {
            String dateStr = entry.getKey().toString();
            int totalQuantity = entry.getValue().values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            double value;
            if (productLogScale) {
                value = totalQuantity > 0 ? Math.log10(totalQuantity) : 0.0;
            } else {
                value = totalQuantity;
            }

            series.getData().add(new XYChart.Data<>(dateStr, value));
        }

        NumberAxis yAxis = (NumberAxis) orderingTrendsProductSalesChart.getYAxis();
        yAxis.setLabel(productLogScale ? "Log10(Quantity)" : "Quantity Sold");

        orderingTrendsProductSalesChart.getData().add(series);
    }

    /**
     * Rebuilds the dollar-sales line chart from the given aggregated data. Applies a
     * log10 transform to Y values when log scale is active.
     *
     * @param dollarSalesData A date-keyed map of total daily revenue values.
     */
    private void populateDollarSalesChart(Map<LocalDate, Double> dollarSalesData) {
        orderingTrendsDollarSalesChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(dollarLogScale ? "Total Sales (log10)" : "Total Sales");

        for (Map.Entry<LocalDate, Double> entry : dollarSalesData.entrySet()) {
            String dateStr = entry.getKey().toString();
            Double totalSales = entry.getValue();

            double value;
            if (dollarLogScale) {
                value = totalSales != null && totalSales > 0.0 ? Math.log10(totalSales) : 0.0;
            } else {
                value = totalSales != null ? totalSales : 0.0;
            }

            series.getData().add(new XYChart.Data<>(dateStr, value));
        }

        NumberAxis yAxis = (NumberAxis) orderingTrendsDollarSalesChart.getYAxis();
        yAxis.setLabel(dollarLogScale ? "Log10(Total $)" : "Total Sales ($)");

        orderingTrendsDollarSalesChart.getData().add(series);
    }


    /**
     * Displays an error alert with the provided message.
     *
     * @param message The error message to show in the alert body.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Error Loading Data");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Handles the Back button action by navigating to the manager dashboard.
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/manager/ManagerDashboard.fxml"));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());
            
            Stage stage = (Stage) orderingTrendsProductSalesChart.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Utility method to load another FXML report screen.  Used by the menu link buttons.
     */
    private void navigateToReport(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());

            Stage stage = (Stage) orderingTrendsProductSalesChart.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            Log.error("Failed to navigate to report page: " + fxmlPath, e);
        }
    }

    @FXML
    private LineChart<String, Number> inventoryUsageChart;

    @FXML
    private Label inventoryUsageLabel;

    private Map<LocalDate, Double> lastInventoryUsageData = new TreeMap<>();

    /**
     * Rebuilds the inventory-usage chart from the given aggregated data.
     *
     * @param usageData A date-keyed map of total inventory quantity used.
     */
    private void populateInventoryUsageChart(Map<LocalDate, Double> usageData) {

        inventoryUsageChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Inventory Used");

        for (Map.Entry<LocalDate, Double> entry : usageData.entrySet()) {

            String dateStr = entry.getKey().toString();
            Double value = entry.getValue() != null ? entry.getValue() : 0.0;

            series.getData().add(new XYChart.Data<>(dateStr, value));
        }

        inventoryUsageChart.getData().add(series);
    }

    /**
     * Loads ordering-trends data asynchronously on a background thread to prevent UI
     * blocking, then updates all three charts on the JavaFX Application Thread when the
     * data is ready.
     */
    private void loadOrderingTrendsAsync() {

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {

                List<ProductSalesData> productSales =
                        reportService.getProductSales(selectedDate, endDate);

                List<DailySalesSummary> dailySummaries =
                        reportService.getDailySalesSummary(selectedDate, endDate);

                List<InventoryUsageData> inventoryUsage =
                        reportService.getInventoryUsage(selectedDate, endDate);

                Map<LocalDate, Map<String, Integer>> productSalesData = new TreeMap<>();
                Map<LocalDate, Double> dollarSalesData = new TreeMap<>();
                Map<LocalDate, Double> inventoryUsageData = new TreeMap<>();

                for (ProductSalesData data : productSales) {
                    productSalesData
                            .computeIfAbsent(data.saleDate(), k -> new HashMap<>())
                            .put(data.productName(), data.quantity());
                }

                for (DailySalesSummary summary : dailySummaries) {
                    dollarSalesData.put(
                            summary.saleDate(),
                            summary.totalSales().doubleValue());
                }

                for (InventoryUsageData data : inventoryUsage) {
                    inventoryUsageData.merge(
                            data.usageDate(),
                            data.quantityUsed().doubleValue(),
                            Double::sum
                    );
                }

                lastProductSalesData = productSalesData;
                lastDollarSalesData = dollarSalesData;
                lastInventoryUsageData = inventoryUsageData;

                return null;
            }
        };

        task.setOnSucceeded(e -> {
            populateProductSalesChart(lastProductSalesData);
            populateDollarSalesChart(lastDollarSalesData);
            populateInventoryUsageChart(lastInventoryUsageData);
        });

        new Thread(task).start();
    }
}
