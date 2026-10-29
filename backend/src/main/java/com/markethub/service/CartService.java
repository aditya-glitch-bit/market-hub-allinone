package com.markethub.service;

import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.model.Cart;
import com.markethub.model.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shopping-bag use-cases. The bag itself lives in the HTTP session ({@link Cart}). */
public class CartService {

    private final ProductService products;

    public CartService(ProductService products) {
        this.products = products;
    }

    public void add(Cart cart, long productId, int quantity) throws MarketHubException {
        products.get(productId); // 404 if the product does not exist
        cart.add(productId, quantity);
    }

    public void setQuantity(Cart cart, long productId, int quantity) throws MarketHubException {
        cart.set(productId, quantity);
    }

    /**
     * Prices the bag using today's catalogue. Lines whose product has since been deleted are
     * dropped from the bag instead of failing the whole request.
     */
    public Map<String, Object> describe(Cart cart) throws MarketHubException {
        List<Map<String, Object>> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int itemCount = 0;
        for (Map.Entry<Long, Integer> line : cart.snapshot().entrySet()) {
            Product product;
            try {
                product = products.get(line.getKey());
            } catch (NotFoundException e) {
                cart.remove(line.getKey());
                continue;
            }
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(line.getValue()));
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("product", product.toView());
            view.put("quantity", line.getValue());
            view.put("lineTotal", lineTotal);
            lines.add(view);
            subtotal = subtotal.add(lineTotal);
            itemCount += line.getValue();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", lines);
        result.put("itemCount", itemCount);
        result.put("subtotal", subtotal);
        return result;
    }
}
