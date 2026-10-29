package com.markethub.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The fixed list of product categories (same names as the storefront's category filter). */
public final class Categories {

    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            "Electronics", "Fashion", "Home & Living", "Beauty & Care", "Sports & Outdoors", "Books & More"));

    private Categories() { }

    public static boolean isValid(String category) {
        return category != null && ALL.contains(category);
    }
}
