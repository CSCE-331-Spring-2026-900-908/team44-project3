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
import javafx.stage.Stage;

import team44.project2.service.ReportService;
import team44.project2.model.report.PaymentMethodSummary;
import team44.project2.model.report.ZReportResult;

import java.net.URL;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for the Z-report screen in the manager dashboard. Generates the end-of-day
 * Z-report (once per business day) which totals gross sales, tips, and payment-method
 * breakdowns, and resets the daily running totals.
 */
@Dependent
public class ZReportController implements Initializable {
    @FXML private Button generateZButton;
    @FXML private Button backButtonZReport;

    @FXML private Label statusLabel;

    @FXML private Label grossSalesLabel;
    @FXML private Label tipsLabel;

    //type counts
    @FXML private Label giftCardOrdersAMT;
    @FXML private Label cashOrderAMT;
    @FXML private Label cardOrderAMT;
    @FXML private Label onFileOrdersAMT;

    //type totals
    @FXML private Label salesGiftCard;
    @FXML private Label salesCash;
    @FXML private Label cardSales;
    @FXML private Label onFileSales;

    @FXML private Label signaturesLabel;

    @Inject Instance<Object> cdiInstance;
    @Inject ReportService reportService;

    /**
     * Wires the back-button action and the generate-Z-report button action. On click,
     * the generate button calls the report service, populates all payment-method labels,
     * and disables the button to prevent running it twice in the same day.
     *
     * @param location  Unused; may be {@code null}.
     * @param resources Unused; may be {@code null}.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        backButtonZReport.setOnAction(e -> navigateToBack());

        if (reportService.hasZRunToday()) {
            statusLabel.setText("Z-report already generated today.");
            generateZButton.setDisable(true);
        } else {
            statusLabel.setText("");
            generateZButton.setDisable(false);
        }

        generateZButton.setOnAction(e -> {
            try {
                ZReportResult result = reportService.runZReport("manager");

                // overall totals
                grossSalesLabel.setText(result.totals().grossSales().toPlainString());
                tipsLabel.setText(result.totals().tips().toPlainString());

                // X-style payment parsing
                List<PaymentMethodSummary> summaries = result.paymentBreakdown();

                int giftCount = 0, cashCount = 0, cardCount = 0, onFileCount = 0;

                java.math.BigDecimal giftTotal = java.math.BigDecimal.ZERO;
                java.math.BigDecimal cashTotal = java.math.BigDecimal.ZERO;
                java.math.BigDecimal cardTotal = java.math.BigDecimal.ZERO;
                java.math.BigDecimal onFileTotal = java.math.BigDecimal.ZERO;

                for (PaymentMethodSummary s : summaries) {

                    String method = (s.paymentMethod() == null)
                            ? ""
                            : s.paymentMethod().toLowerCase(Locale.US);

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

                        default -> { }
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

                // signatures
                if (result.signedEmployeeIds() == null || result.signedEmployeeIds().isEmpty()) {
                    signaturesLabel.setText("None");
                } else {
                    signaturesLabel.setText(
                            result.signedEmployeeIds().stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(", "))
                    );
                }

                statusLabel.setText("Z-report generated. Totals reset.");
                generateZButton.setDisable(true);

            } catch (IllegalStateException ex) {
                statusLabel.setText(ex.getMessage());
                generateZButton.setDisable(true);
            } catch (Exception ex) {
                Log.error("Failed to generate Z-report", ex);
                statusLabel.setText("Failed to generate Z-report.");
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

            Stage stage = (Stage) backButtonZReport.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            Log.error("Failed to navigate back to ordering trends", e);
        }
    }
}