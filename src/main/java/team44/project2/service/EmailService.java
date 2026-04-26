package team44.project2.service;

import io.quarkus.logging.Log;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    public record ReceiptItem(String name, String size, String sweetness, String iceLevel, List<String> addOns, int quantity, BigDecimal unitPrice) {}

    public boolean sendReceipt(
            String toEmail,
            int orderId,
            BigDecimal subtotal,
            BigDecimal tip,
            BigDecimal total,
            int pointsEarned,
            List<ReceiptItem> items
    ) {
        try {
            String html = buildReceiptHtml(orderId, subtotal, tip, total, pointsEarned, items);
            mailer.send(Mail.withHtml(toEmail, "Your Boba Receipt – Order #" + orderId, html));
            return true;
        } catch (Exception e) {
            Log.error("Failed to send receipt email to " + toEmail, e);
            return false;
        }
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
            String customization = buildCustomizationText(item);
            itemRows.append("""
                    <tr>
                      <td style="padding:8px 0;border-bottom:1px solid #f0e6d3;">
                        <strong>%s</strong>%s
                        %s
                      </td>
                      <td style="padding:8px 0;border-bottom:1px solid #f0e6d3;text-align:right;white-space:nowrap;">
                        x%d &nbsp; $%s
                      </td>
                    </tr>
                    """.formatted(
                    item.name(),
                    item.size() != null && !item.size().isBlank() ? " (" + item.size() + ")" : "",
                    customization,
                    item.quantity(),
                    item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))
                            .setScale(2, RoundingMode.HALF_UP)
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

    private String buildCustomizationText(ReceiptItem item) {
        StringBuilder sb = new StringBuilder();
        boolean hasMods = (item.sweetness() != null && !item.sweetness().isBlank())
                || (item.iceLevel() != null && !item.iceLevel().isBlank())
                || (item.addOns() != null && !item.addOns().isEmpty());
        if (!hasMods) return "";

        sb.append("<br><span style=\"font-size:12px;color:#888;\">");
        if (item.sweetness() != null && !item.sweetness().isBlank()) sb.append(item.sweetness()).append(" sweet");
        if (item.iceLevel() != null && !item.iceLevel().isBlank()) {
            if (sb.length() > 49) sb.append(", ");
            sb.append(item.iceLevel());
        }
        if (item.addOns() != null && !item.addOns().isEmpty()) {
            if (sb.length() > 49) sb.append(" + ");
            sb.append(String.join(", ", item.addOns()));
        }
        sb.append("</span>");
        return sb.toString();
    }
}
