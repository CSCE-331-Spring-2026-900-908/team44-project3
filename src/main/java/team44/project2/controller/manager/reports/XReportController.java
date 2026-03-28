package team44.project2.controller.manager.reports;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import team44.project2.service.ReportService;
import team44.project2.model.report.PaymentMethodSummary;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * Controller for the X-report screen in the manager dashboard. Displays the number of
 * orders and total sales broken down by payment method for a specific hour of the
 * current business day.
 */
@Dependent
public class XReportController implements Initializable {

    @FXML
    private TextField hourInputField;

    @FXML
    private Button hourEnterButton;

    //payments methods
    @FXML
    private Label giftCardOrdersAMT;
    @FXML
    private Label cashOrderAMT;
    @FXML
    private Label cardOrderAMT;
    @FXML
    private Label onFileOrdersAMT;

    //sales on payment methods
    @FXML
    private Label salesGiftCard;
    @FXML
    private Label salesCash;
    @FXML
    private Label cardSales;
    @FXML
    private Label onFileSales;

    @FXML
    private Button backButtonXReport;

    @Inject
    Instance<Object> cdiInstance;

    
    @Inject
    ReportService reportService;

    /**
     * Registers the back-button action and attaches the hour-input handler to the enter
     * button. The handler parses the entered time, fetches the hourly payment summary
     * from the service, and updates each payment-method label pair.
     *
     * @param location  Unused; may be {@code null}.
     * @param resources Unused; may be {@code null}.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        backButtonXReport.setOnAction(e -> navigateToBack());

        //Parses hour input to make sure its in a good format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm", Locale.US);
        hourEnterButton.setOnAction(e -> {
            String text = hourInputField.getText();
            if (text == null || text.isBlank()) {
                return;
            }
            try {
                java.time.LocalTime time = java.time.LocalTime.parse(text, formatter);
                int hour = time.getHour();
                

                //Gets list of all orders in the hour 
                List<PaymentMethodSummary> summaries =
                        reportService.getHourlyPaymentSummary(hour);

                int giftCount = 0, cashCount = 0, cardCount = 0, onFileCount = 0;
                java.math.BigDecimal giftTotal = java.math.BigDecimal.ZERO;
                java.math.BigDecimal cashTotal = java.math.BigDecimal.ZERO;
                java.math.BigDecimal cardTotal = java.math.BigDecimal.ZERO;
                java.math.BigDecimal onFileTotal = java.math.BigDecimal.ZERO;

                for (PaymentMethodSummary s : summaries) {
                    String method = s.paymentMethod().toLowerCase(Locale.US);
                    switch (method) {
                        case "gift card", "giftcard" -> {
                            giftCount += s.orderCount();
                            giftTotal = giftTotal.add(s.totalSales());
                        }
                        case "cash" -> {
                            cashCount += s.orderCount();
                            cashTotal = cashTotal.add(s.totalSales());
                        }
                        case "credit card", "card", "debit card" -> {
                           
                            cardCount += s.orderCount();
                            cardTotal = cardTotal.add(s.totalSales());
                        }
                        case "card on file", "on file", "mobile pay" -> {
                            
                            onFileCount += s.orderCount();
                            onFileTotal = onFileTotal.add(s.totalSales());
                        }
                        default -> {
                            
                        }
                    }
                }

                giftCardOrdersAMT.setText(String.valueOf(giftCount));
                cashOrderAMT.setText(String.valueOf(cashCount));
                cardOrderAMT.setText(String.valueOf(cardCount));
                onFileOrdersAMT.setText(String.valueOf(onFileCount));

                salesGiftCard.setText(giftTotal.toPlainString());
                salesCash.setText(cashTotal.toPlainString());
                cardSales.setText(cardTotal.toPlainString());
                onFileSales.setText(onFileTotal.toPlainString());

            } catch (Exception ex) {
                Log.error("Failed to parse hour or load x-report", ex);
            }
        });
    }

    /**
     * Navigates back to the ordering-trends report screen.
     */
    private void navigateToBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/manager/reports/OrderingTrends.fxml"));
            loader.setControllerFactory(clazz -> cdiInstance.select(clazz).get());

            Stage stage = (Stage) backButtonXReport.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            Log.error("Failed to navigate back to ordering trends", e);
        }
    }
}
