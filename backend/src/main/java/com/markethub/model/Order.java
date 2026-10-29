package com.markethub.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** A purchase made by one {@link Buyer}; contains one or more {@link OrderItem}s. */
public class Order implements Identifiable<Long> {

    private Long id;
    private final int orderNumber;
    private final long buyerId;
    private final String buyerName;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private final List<OrderItem> items;

    public Order(Long id, int orderNumber, long buyerId, String buyerName, OrderStatus status,
                 LocalDateTime createdAt, List<OrderItem> items) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.status = status;
        this.createdAt = createdAt;
        this.items = new ArrayList<>(items);
    }

    /** Customer-facing reference such as {@code MH-1048}. */
    public String getCode() {
        return "MH-" + orderNumber;
    }

    /** Sum of all line totals. */
    public BigDecimal getTotal() {
        return items.stream().map(OrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** @return {@code true} if at least one line belongs to the given seller */
    public boolean containsSeller(long sellerId) {
        return items.stream().anyMatch(i -> i.getSellerId() == sellerId);
    }

    /** @return only the lines sold by the given seller */
    public List<OrderItem> itemsOf(long sellerId) {
        return items.stream().filter(i -> i.getSellerId() == sellerId).collect(Collectors.toList());
    }

    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", id);
        view.put("code", getCode());
        view.put("customer", buyerName);
        view.put("buyerId", buyerId);
        view.put("date", createdAt == null ? null : createdAt.toString());
        view.put("status", status.getLabel());
        view.put("total", getTotal());
        view.put("items", items.stream().map(OrderItem::toView).collect(Collectors.toList()));
        return view;
    }

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getOrderNumber() { return orderNumber; }
    public long getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }

    /** Replaces the lines; used by the DAO after it has loaded them in a separate query. */
    public void setItems(List<OrderItem> newItems) {
        items.clear();
        items.addAll(newItems);
    }
}
