package com.markethub.service;

import com.markethub.exception.ValidationException;
import com.markethub.model.Product;

import java.util.Comparator;
import java.util.Locale;

/** Catalogue sort options; each constant carries the {@link Comparator} that implements it. */
public enum SortOrder {
    /** Most reviewed first – the storefront's default "featured" ordering. */
    FEATURED(Comparator.comparingInt(Product::getReviews).reversed()),
    PRICE_ASC(Comparator.comparing(Product::getPrice)),
    PRICE_DESC(Comparator.comparing(Product::getPrice).reversed()),
    RATING(Comparator.comparingDouble(Product::getRating).reversed()),
    NAME(Comparator.<Product>naturalOrder());

    private final Comparator<Product> comparator;

    SortOrder(Comparator<Product> comparator) {
        this.comparator = comparator;
    }

    public Comparator<Product> comparator() {
        return comparator;
    }

    /** Accepts {@code price-asc}, {@code PRICE_ASC}, {@code price_asc}; blank means FEATURED. */
    public static SortOrder parse(String text) throws ValidationException {
        if (text == null || text.isBlank()) {
            return FEATURED;
        }
        try {
            return valueOf(text.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown sort order: " + text);
        }
    }
}
