package com.markethub.model;

import java.time.LocalDateTime;

/**
 * Factory that turns a {@link Role} into the matching {@link User} subclass.
 * Used by the DAO when it reads rows and by the service when it registers accounts, so the
 * "which class?" decision lives in exactly one place.
 */
public final class UserFactory {

    private UserFactory() { }

    public static User create(Role role, Long id, String name, String email, String passwordHash,
                              boolean active, LocalDateTime createdAt) {
        switch (role) {
            case ADMIN:
                return new Admin(id, name, email, passwordHash, active, createdAt);
            case SELLER:
                return new Seller(id, name, email, passwordHash, active, createdAt);
            case BUYER:
            default:
                return new Buyer(id, name, email, passwordHash, active, createdAt);
        }
    }
}
