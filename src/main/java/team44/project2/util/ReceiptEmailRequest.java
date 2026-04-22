package team44.project2.dto;

import java.util.List;

public class ReceiptEmailRequest {
    public String orderId;
    public String rewardsEmail;
    public String customerName;
    public List<ReceiptItem> items;
    public double subtotal;
    public double tax;
    public double total;
}