package com.markethub.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * A shop that lists products. The account name <em>is</em> the store name
 * (e.g. "Sound Collective"), matching the storefront's seller labels.
 */
public class Seller extends User {

    public Seller(Long id, String storeName, String email, String passwordHash,
                  boolean active, LocalDateTime createdAt) {
        super(id, storeName, email, passwordHash, active, createdAt);
    }

    public String getStoreName() { return getName(); }

    @Override
    public Role getRole() { return Role.SELLER; }

    @Override
    public String getDashboardTitle() { return "Seller dashboard – " + getStoreName(); }

    /** A seller may only touch their own listings. */
    @Override
    public boolean canManageProduct(Product product) {
        return product != null && getId() != null && getId().equals(product.getSellerId());
    }

    @Override
    public boolean canUpdateOrderStatus() { return true; }

    @Override
    public Map<String, Object> toView() {
        Map<String, Object> view = super.toView();
        view.put("storeName", getStoreName());
        return view;
    }
}
