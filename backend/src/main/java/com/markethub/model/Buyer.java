package com.markethub.model;

import java.time.LocalDateTime;

/** A shopper: can browse, fill a cart and place orders, but cannot manage catalogue data. */
public class Buyer extends User {

    public Buyer(Long id, String name, String email, String passwordHash,
                 boolean active, LocalDateTime createdAt) {
        super(id, name, email, passwordHash, active, createdAt);
    }

    @Override
    public Role getRole() { return Role.BUYER; }

    @Override
    public String getDashboardTitle() { return "My account"; }

    @Override
    public boolean canManageProduct(Product product) { return false; }

    @Override
    public boolean canUpdateOrderStatus() { return false; }
}
