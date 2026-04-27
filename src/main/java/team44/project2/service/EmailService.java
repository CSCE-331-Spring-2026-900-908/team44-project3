package team44.project2.service;

import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@ApplicationScoped
public class EmailService {

    @Inject
    ReactiveMailer mailer;

    public record AddOnItem(String name, BigDecimal price) {}

    public record ReceiptItem(
            String name,
            String size,
            String sweetness,
            String iceLevel,
            List<AddOnItem> addOns,
            int quantity,
            BigDecimal unitPrice
    ) {}

    public Uni<Void> sendReceipt(
            String toEmail,
            int orderId,
            BigDecimal subtotal,
            BigDecimal tip,
            BigDecimal total,
            int pointsEarned,
            List<ReceiptItem> items
    ) {
        String html = buildReceiptHtml(orderId, subtotal, tip, total, pointsEarned, items);
        return mailer.send(Mail.withHtml(toEmail, "Your Boba Receipt – Order #" + orderId, html))
                .onItem().invoke(() -> Log.infof("Receipt email sent to %s for order #%d", toEmail, orderId))
                .onFailure().invoke(e -> Log.errorf(e, "Failed to send receipt email to %s for order #%d", toEmail, orderId));
    }

    private String buildReceiptHtml(
            int orderId,
            BigDecimal subtotal,
            BigDecimal tip,
            BigDecimal total,
            int pointsEarned,
            List<ReceiptItem> items
    ) {
        StringBuilder itemRows = new StringBuilder();
        for (ReceiptItem item : items) {
            BigDecimal lineTotal = item.unitPrice()
                    .multiply(BigDecimal.valueOf(item.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            // Size / sweetness / ice lines
            StringBuilder modLines = new StringBuilder();
            if (item.size() != null && !item.size().isBlank()) {
                modLines.append("<div style=\"font-size:13px;color:#888;\">Size: ").append(item.size()).append("</div>");
            }
            if (item.sweetness() != null && !item.sweetness().isBlank()) {
                modLines.append("<div style=\"font-size:13px;color:#888;\">Sweetness: ").append(item.sweetness()).append("</div>");
            }
            if (item.iceLevel() != null && !item.iceLevel().isBlank()) {
                modLines.append("<div style=\"font-size:13px;color:#888;\">").append(item.iceLevel()).append("</div>");
            }

            // Add-ons section
            StringBuilder addOnRows = new StringBuilder();
            if (item.addOns() != null && !item.addOns().isEmpty()) {
                addOnRows.append("<div style=\"margin-top:8px;\">");
                addOnRows.append("<div style=\"font-size:12px;font-weight:700;color:#555;letter-spacing:0.04em;margin-bottom:4px;\">ADD-ONS:</div>");
                for (AddOnItem addOn : item.addOns()) {
                    addOnRows.append("""
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td style="font-size:13px;color:#555;padding:1px 0 1px 8px;">%s</td>
                                <td style="font-size:13px;color:#555;text-align:right;padding:1px 0;">$%s</td>
                              </tr>
                            </table>
                            """.formatted(
                            addOn.name(),
                            addOn.price().setScale(2, RoundingMode.HALF_UP)
                    ));
                }
                addOnRows.append("</div>");
            }

            itemRows.append("""
                    <tr>
                      <td colspan="2" style="padding:12px 0;border-bottom:1px solid #f0e6d3;">
                        <table width="100%%" cellpadding="0" cellspacing="0">
                          <tr>
                            <td style="font-weight:700;font-size:15px;">%s</td>
                            <td style="font-weight:700;font-size:15px;text-align:right;white-space:nowrap;">
                              x%d &nbsp; $%s
                            </td>
                          </tr>
                        </table>
                        %s
                        %s
                      </td>
                    </tr>
                    """.formatted(
                    item.name(),
                    item.quantity(),
                    lineTotal,
                    modLines,
                    addOnRows
            ));
        }

        String pointsRow = pointsEarned > 0
                ? "<p style=\"color:#8b5e3c;font-size:14px;\">You earned <strong>%d points</strong> on this order!</p>".formatted(pointsEarned)
                : "";

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#faf5ee;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#faf5ee;padding:32px 0;">
                    <tr><td align="center">
                      <table width="480" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                        <tr>
                          <td style="background:#8b5e3c;padding:24px 32px;text-align:center;">
                            <h1 style="margin:0;color:#fff;font-size:24px;">Thank You!</h1>
                            <p style="margin:6px 0 0;color:#f0e6d3;font-size:14px;">Order #%d</p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:24px 32px;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              %s
                            </table>
                            <table width="100%%" cellpadding="0" cellspacing="0" style="margin-top:16px;">
                              <tr>
                                <td style="padding:4px 0;color:#666;">Subtotal</td>
                                <td style="padding:4px 0;text-align:right;">$%s</td>
                              </tr>
                              <tr>
                                <td style="padding:4px 0;color:#666;">Tip</td>
                                <td style="padding:4px 0;text-align:right;">$%s</td>
                              </tr>
                              <tr>
                                <td style="padding:8px 0 4px;font-weight:bold;font-size:16px;border-top:2px solid #f0e6d3;">Total</td>
                                <td style="padding:8px 0 4px;text-align:right;font-weight:bold;font-size:16px;border-top:2px solid #f0e6d3;">$%s</td>
                              </tr>
                            </table>
                            %s
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#faf5ee;padding:16px 32px;text-align:center;color:#999;font-size:12px;">
                            Thank you for visiting us!
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                orderId,
                itemRows,
                subtotal.setScale(2, RoundingMode.HALF_UP),
                tip.setScale(2, RoundingMode.HALF_UP),
                total.setScale(2, RoundingMode.HALF_UP),
                pointsRow
        );
    }
}
