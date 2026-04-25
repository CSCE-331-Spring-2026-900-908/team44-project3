package team44.project2.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import team44.project2.dto.ReceiptEmailRequest;
import team44.project2.dto.ReceiptItem;

@ApplicationScoped
public class EmailService {

    @Inject
    Mailer mailer;

    // Simple in-memory duplicate protection for now
    private final Set<String> sentOrderIds = ConcurrentHashMap.newKeySet();

    public boolean sendEmail(ReceiptEmailRequest request) {
        if (request == null || request.orderId == null || request.orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }

        if (request.rewardsEmail == null || request.rewardsEmail.isBlank()) {
            throw new IllegalArgumentException("rewardsEmail is required");
        }

        // Prevent duplicate sends for same order while app is running
        if (!sentOrderIds.add(request.orderId)) {
            return false;
        }

        String subject = "Your receipt from Boba Bob's";
        String html = buildReceiptHtml(request);
        String text = buildReceiptText(request);

        Mail mail = Mail.withHtml(
                request.rewardsEmail,
                subject,
                html
        )
        .setText(text)
        .setFrom("Boba Bob's <bobabob@poob.store>");

        mailer.send(mail);
        return true;
    }

    private String buildReceiptText(ReceiptEmailRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thanks for your order from Boba Bob's!\n\n");
        sb.append("Order #: ").append(request.orderId).append("\n");
        sb.append("Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n\n");

        if (request.items != null) {
            for (ReceiptItem item : request.items) {
                sb.append(item.quantity)
                  .append(" x ")
                  .append(item.name)
                  .append(" - $")
                  .append(String.format("%.2f", item.price * item.quantity))
                  .append("\n");
            }
        }

        sb.append("\nSubtotal: $").append(String.format("%.2f", request.subtotal));
        sb.append("\nTax: $").append(String.format("%.2f", request.tax));
        sb.append("\nTotal: $").append(String.format("%.2f", request.total));
        sb.append("\n\nThis receipt was sent to your rewards email.");
        return sb.toString();
    }

    private String buildReceiptHtml(ReceiptEmailRequest request) {
        String customerName = request.customerName == null || request.customerName.isBlank()
                ? "Customer"
                : escapeHtml(request.customerName);

        StringBuilder itemRows = new StringBuilder();
        if (request.items != null) {
            for (ReceiptItem item : request.items) {
                itemRows.append("""
                    <tr>
                        <td style="padding: 10px 8px; border-bottom: 1px solid #eee;">%d</td>
                        <td style="padding: 10px 8px; border-bottom: 1px solid #eee;">%s</td>
                        <td style="padding: 10px 8px; border-bottom: 1px solid #eee; text-align: right;">$%.2f</td>
                    </tr>
                    """.formatted(
                        item.quantity,
                        escapeHtml(item.name),
                        item.price * item.quantity
                ));
            }
        }

        return """
            <div style="margin:0; padding:0; background:#f6f4ef; font-family:Arial, Helvetica, sans-serif; color:#2b2b2b;">
              <div style="max-width:640px; margin:24px auto; background:#ffffff; border-radius:16px; overflow:hidden; border:1px solid #e7e0d4;">
                <div style="background:#d4712a; color:#ffffff; padding:24px 28px;">
                  <h1 style="margin:0; font-size:28px;">Boba Bob's</h1>
                  <p style="margin:8px 0 0 0; font-size:14px; opacity:0.9;">Thanks for your order, %s.</p>
                </div>

                <div style="padding:28px;">
                  <p style="margin:0 0 16px 0; font-size:15px;">Your receipt has been sent to your rewards email.</p>

                  <div style="background:#faf8f4; border:1px solid #eee3d2; border-radius:12px; padding:16px; margin-bottom:20px;">
                    <p style="margin:0 0 6px 0;"><strong>Order #:</strong> %s</p>
                    <p style="margin:0;"><strong>Date:</strong> %s</p>
                  </div>

                  <table style="width:100%%; border-collapse:collapse; font-size:14px;">
                    <thead>
                      <tr>
                        <th style="text-align:left; padding:10px 8px; border-bottom:2px solid #d9cfbf;">Qty</th>
                        <th style="text-align:left; padding:10px 8px; border-bottom:2px solid #d9cfbf;">Item</th>
                        <th style="text-align:right; padding:10px 8px; border-bottom:2px solid #d9cfbf;">Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      %s
                    </tbody>
                  </table>

                  <div style="margin-top:20px; margin-left:auto; width:240px; font-size:14px;">
                    <div style="display:flex; justify-content:space-between; padding:6px 0;">
                      <span>Subtotal</span>
                      <span>$%.2f</span>
                    </div>
                    <div style="display:flex; justify-content:space-between; padding:6px 0;">
                      <span>Tax</span>
                      <span>$%.2f</span>
                    </div>
                    <div style="display:flex; justify-content:space-between; padding:10px 0 0 0; margin-top:8px; border-top:2px solid #d9cfbf; font-size:16px; font-weight:bold;">
                      <span>Total</span>
                      <span>$%.2f</span>
                    </div>
                  </div>

                  <p style="margin-top:28px; font-size:13px; color:#666;">
                    This is an automated receipt from Boba Bob's Boba Shop. © 2026 Team 44
                  </p>
                </div>
              </div>
            </div>
            """.formatted(
                customerName,
                escapeHtml(request.orderId),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                itemRows.toString(),
                request.subtotal,
                request.tax,
                request.total
        );
    }

    private String escapeHtml(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}