package com.markethub.service;

import com.markethub.concurrent.ActivityLog;
import com.markethub.dao.ProductDao;
import com.markethub.dao.UserDao;
import com.markethub.exception.ConflictException;
import com.markethub.exception.ForbiddenException;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.exception.ValidationException;
import com.markethub.model.Product;
import com.markethub.model.Role;
import com.markethub.model.User;
import com.markethub.util.Page;
import com.markethub.util.Validators;

import java.util.List;
import java.util.stream.Collectors;

/** Catalogue use-cases: browse, search, and (for sellers/admins) manage listings. */
public class ProductService {

    private final ProductDao products;
    private final UserDao users;
    private final ActivityLog activity;

    public ProductService(ProductDao products, UserDao users, ActivityLog activity) {
        this.products = products;
        this.users = users;
        this.activity = activity;
    }

    /** Filters in the database, then applies the sale filter, sort order and paging in memory. */
    public Page<Product> list(ProductQuery query) throws MarketHubException {
        List<Product> matches = products.search(query.getText(), query.getCategory());
        List<Product> result = matches.stream()
                .filter(p -> !query.isOnSaleOnly() || p.isOnSale())
                .sorted(query.getSort().comparator())
                .collect(Collectors.toList());
        return Page.of(result, query.getPage(), query.getSize());
    }

    public Product get(long id) throws MarketHubException {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
    }

    public List<Product> listForSeller(long sellerId) throws MarketHubException {
        return products.findBySeller(sellerId);
    }

    /** Sellers create listings for themselves; admins must name the seller. */
    public Product create(User actor, ProductDraft draft) throws MarketHubException {
        if (actor.getRole() == Role.BUYER) {
            throw new ForbiddenException("Buyers cannot list products");
        }
        draft.validate();
        long sellerId;
        if (actor.getRole() == Role.SELLER) {
            sellerId = actor.getId();
        } else {
            if (draft.sellerId == null) {
                throw new ValidationException("sellerId is required when an admin creates a product");
            }
            User seller = users.findById(draft.sellerId)
                    .orElseThrow(() -> new NotFoundException("Seller", draft.sellerId));
            if (seller.getRole() != Role.SELLER) {
                throw new ValidationException("User " + draft.sellerId + " is not a seller");
            }
            sellerId = seller.getId();
        }
        Product saved = products.save(new Product(null, sellerId, "", draft.name, draft.category,
                draft.price, draft.oldPrice, 0.0, 0, draft.image, draft.badge, draft.stock, null));
        activity.record(actor.getName(), "listed \"" + saved.getName() + "\"");
        return saved;
    }

    public Product update(User actor, long id, ProductDraft draft) throws MarketHubException {
        Product product = requireManageable(actor, id);
        draft.validate();
        product.setName(draft.name);
        product.setCategory(draft.category);
        product.setPrice(draft.price);
        product.setOldPrice(draft.oldPrice);
        product.setStock(draft.stock);
        product.setImage(draft.image);
        product.setBadge(draft.badge);
        Product saved = products.save(product);
        activity.record(actor.getName(), "updated \"" + saved.getName() + "\"");
        return saved;
    }

    /** Quick inventory edit used by the seller's stock table. */
    public Product setStock(User actor, long id, int stock) throws MarketHubException {
        Product product = requireManageable(actor, id);
        Validators.requireRange("Stock", stock, 0, 100_000);
        product.setStock(stock);
        Product saved = products.save(product);
        activity.record(actor.getName(), "set stock of \"" + saved.getName() + "\" to " + stock);
        return saved;
    }

    public void delete(User actor, long id) throws MarketHubException {
        Product product = requireManageable(actor, id);
        try {
            products.deleteById(id);
        } catch (ConflictException e) {
            throw new ConflictException("\"" + product.getName()
                    + "\" appears in past orders and cannot be deleted; set its stock to 0 instead", e);
        }
        activity.record(actor.getName(), "deleted \"" + product.getName() + "\"");
    }

    /** Loads the product and asks the <em>user object</em> whether it may be changed (polymorphism). */
    private Product requireManageable(User actor, long id) throws MarketHubException {
        Product product = get(id);
        if (!actor.canManageProduct(product)) {
            throw new ForbiddenException("You can only manage your own products");
        }
        return product;
    }
}
