package com.markethub.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A listing sold by one {@link Seller}.
 *
 * <p>Implements {@link Identifiable} (primary key), {@link Discountable} (sale logic) and
 * {@link Comparable} (natural order = alphabetical by name).</p>
 */
public class Product implements Identifiable<Long>, Discountable, Comparable<Product> {

    private Long id;
    private final long sellerId;
    private final String sellerName;
    private String name;
    private String category;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private final double rating;
    private final int reviews;
    private String image;
    private String badge;
    private int stock;
    private final LocalDateTime createdAt;

    public Product(Long id, long sellerId, String sellerName, String name, String category,
                   BigDecimal price, BigDecimal oldPrice, double rating, int reviews,
                   String image, String badge, int stock, LocalDateTime createdAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.name = name;
        this.category = category;
        this.price = price;
        this.oldPrice = oldPrice == null ? BigDecimal.ZERO : oldPrice;
        this.rating = rating;
        this.reviews = reviews;
        this.image = image;
        this.badge = badge == null ? "" : badge;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    /** @return {@code true} when stock is positive but at or below the alert threshold */
    public boolean isLowStock(int threshold) {
        return stock > 0 && stock <= threshold;
    }

    /** JSON shape consumed by the React storefront (field names match {@code demo.ts}). */
    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", id);
        view.put("name", name);
        view.put("seller", sellerName);
        view.put("sellerId", sellerId);
        view.put("category", category);
        view.put("price", price);
        view.put("oldPrice", oldPrice);
        view.put("onSale", isOnSale());
        view.put("discountPercent", getDiscountPercent());
        view.put("rating", rating);
        view.put("reviews", reviews);
        view.put("image", image);
        view.put("badge", badge);
        view.put("stock", stock);
        return view;
    }

    @Override
    public int compareTo(Product other) {
        return name.compareToIgnoreCase(other.name);
    }

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    @Override
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    @Override
    public BigDecimal getOldPrice() { return oldPrice; }
    public void setOldPrice(BigDecimal oldPrice) { this.oldPrice = oldPrice == null ? BigDecimal.ZERO : oldPrice; }
    public double getRating() { return rating; }
    public int getReviews() { return reviews; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge == null ? "" : badge; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "Product[" + id + ", " + name + "]";
    }
}
