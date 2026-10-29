package com.markethub.model;

import com.markethub.exception.ValidationException;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Lifecycle of an order. The allowed transitions form a small state machine:
 * <pre>
 *   PROCESSING --> SHIPPED --> DELIVERED
 *        |
 *        +-------> CANCELLED
 * </pre>
 */
public enum OrderStatus {
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** Human-readable text, matching the labels used by the frontend. */
    public String getLabel() {
        return label;
    }

    /** @return the statuses an order in this state may move to next */
    public Set<OrderStatus> allowedNext() {
        switch (this) {
            case PROCESSING:
                return EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED:
                return EnumSet.of(DELIVERED);
            default:
                return EnumSet.noneOf(OrderStatus.class);
        }
    }

    public boolean canMoveTo(OrderStatus target) {
        return allowedNext().contains(target);
    }

    /** Accepts "shipped", "SHIPPED" or "Shipped". */
    public static OrderStatus parse(String text) throws ValidationException {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Status is required");
        }
        try {
            return valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown order status: " + text);
        }
    }
}
