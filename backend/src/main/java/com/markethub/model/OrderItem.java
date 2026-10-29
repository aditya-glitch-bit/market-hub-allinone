package com.markethub.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One line of an order. The product name and unit price are copied at purchase time so later
 * price edits never change history.
 */
public class OrderItem {

    private final long productId;
    private final long sellerId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;

    public OrderItem(long productId, long sellerId, String productName, BigDecimal unitPrice, int quantity) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("productId", productId);
        view.put("product", productName);
        view.put("unitPrice", unitPrice);
        view.put("quantity", quantity);
        view.put("lineTotal", getLineTotal());
        return view;
    }

    public long getProductId() { return productId; }
    public long getSellerId() { return sellerId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
