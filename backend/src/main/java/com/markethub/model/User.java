package com.markethub.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class of every account (inheritance).
 *
 * <p>Behaviour that differs per role is declared {@code abstract} here and implemented by
 * {@link Admin}, {@link Seller} and {@link Buyer}. Callers hold a plain {@code User} and the
 * correct rule is chosen at runtime (polymorphism), so there are no {@code if (role == ...)}
 * chains in the service layer.</p>
 */
public abstract class User implements Identifiable<Long> {

    private Long id;
    private final String name;
    private final String email;
    /** Salted PBKDF2 hash – never sent to the client, see {@link #toView()}. */
    private final String passwordHash;
    private boolean active;
    private final LocalDateTime createdAt;

    protected User(Long id, String name, String email, String passwordHash,
                   boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.createdAt = createdAt;
    }

    /** @return which kind of account this is */
    public abstract Role getRole();

    /** @return heading shown at the top of this user's dashboard */
    public abstract String getDashboardTitle();

    /** @return whether this user may edit or delete the given product */
    public abstract boolean canManageProduct(Product product);

    /** @return whether this user may move orders through fulfilment states (ship, deliver) */
    public abstract boolean canUpdateOrderStatus();

    /** Public JSON-friendly representation; subclasses add their own fields. */
    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", id);
        view.put("name", name);
        view.put("email", email);
        view.put("role", getRole().name());
        view.put("active", active);
        view.put("createdAt", createdAt == null ? null : createdAt.toString());
        return view;
    }

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        User that = (User) other;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public String toString() {
        return getRole() + "[" + id + ", " + email + "]";
    }
}
