package com.markethub.service;

import com.markethub.exception.ValidationException;
import com.markethub.model.Categories;
import com.markethub.util.Validators;

import java.math.BigDecimal;

/**
 * Data a seller or admin submits to create or edit a listing (the JSON request body).
 * Public fields let Gson fill it directly; {@link #validate()} cleans and checks them.
 */
public class ProductDraft {

    public String name;
    public String category;
    public BigDecimal price;
    public BigDecimal oldPrice;
    public Integer stock;
    public String image;
    public String badge;
    /** Only used when an admin creates a product on behalf of a seller. */
    public Long sellerId;

    /** Trims text, rounds prices and throws {@link ValidationException} for anything unacceptable. */
    public void validate() throws ValidationException {
        name = Validators.requireText("Name", name, 2, 150);
        if (!Categories.isValid(category)) {
            throw new ValidationException("Category must be one of " + Categories.ALL);
        }
        price = Validators.requirePrice("Price", price);
        oldPrice = Validators.optionalPrice("Old price", oldPrice);
        if (oldPrice.signum() > 0 && oldPrice.compareTo(price) <= 0) {
            throw new ValidationException("Old price must be higher than the price, or 0 for no discount");
        }
        if (stock == null) {
            throw new ValidationException("Stock is required");
        }
        Validators.requireRange("Stock", stock, 0, 100_000);
        image = image == null ? "" : Validators.requireText("Image", image, 0, 150);
        badge = badge == null ? "" : Validators.requireText("Badge", badge, 0, 40);
    }
}
