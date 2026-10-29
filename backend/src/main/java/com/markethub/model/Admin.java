package com.markethub.model;

import java.time.LocalDateTime;

/** Marketplace operator: can manage every product, order and account. */
public class Admin extends User {

    public Admin(Long id, String name, String email, String passwordHash,
                 boolean active, LocalDateTime createdAt) {
        super(id, name, email, passwordHash, active, createdAt);
    }

    @Override
    public Role getRole() { return Role.ADMIN; }

    @Override
    public String getDashboardTitle() { return "Admin dashboard"; }

    @Override
    public boolean canManageProduct(Product product) { return true; }

    @Override
    public boolean canUpdateOrderStatus() { return true; }
}
