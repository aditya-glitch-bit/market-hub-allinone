package com.markethub.exception;

/** Not enough units are left to satisfy an order line (HTTP 409). */
public class OutOfStockException extends ConflictException {

    private static final long serialVersionUID = 1L;

    private final long productId;

    public OutOfStockException(long productId, String productName) {
        super("Not enough stock for \"" + productName + "\"");
        this.productId = productId;
    }

    public long getProductId() {
        return productId;
    }
}
