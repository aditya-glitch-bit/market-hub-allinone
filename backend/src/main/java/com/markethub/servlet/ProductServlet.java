package com.markethub.servlet;

import com.markethub.AppContext;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.exception.ValidationException;
import com.markethub.model.Categories;
import com.markethub.model.Product;
import com.markethub.model.Role;
import com.markethub.model.User;
import com.markethub.service.ProductDraft;
import com.markethub.service.ProductQuery;
import com.markethub.service.SortOrder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Catalogue endpoints.
 * <pre>
 *   GET    /api/products?q=&amp;category=&amp;sale=true&amp;sort=price-asc&amp;page=1&amp;size=12   public
 *   GET    /api/products/{id}                                                   public
 *   GET    /api/products/categories                                             public
 *   GET    /api/products/mine                                                   seller
 *   POST   /api/products                    ProductDraft                        seller / admin
 *   PUT    /api/products/{id}               ProductDraft                        owner / admin
 *   PUT    /api/products/{id}/stock         {stock}                             owner / admin
 *   DELETE /api/products/{id}                                                   owner / admin
 * </pre>
 */
public class ProductServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    /** Request body of /{id}/stock. */
    static class StockRequest {
        Integer stock;
    }

    public ProductServlet(AppContext ctx) {
        super(ctx);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        String[] parts = pathParts(req);
        if (parts.length == 0) {
            ProductQuery query = new ProductQuery()
                    .setText(req.getParameter("q"))
                    .setCategory(req.getParameter("category"))
                    .setOnSaleOnly(Boolean.parseBoolean(req.getParameter("sale")))
                    .setSort(SortOrder.parse(req.getParameter("sort")))
                    .setPage(intParam(req, "page", 1))
                    .setSize(intParam(req, "size", 12));
            sendOk(resp, ctx.products().list(query).map(Product::toView).toView());
        } else if (parts.length == 1 && "categories".equals(parts[0])) {
            sendOk(resp, Categories.ALL);
        } else if (parts.length == 1 && "mine".equals(parts[0])) {
            User seller = requireRole(req, Role.SELLER);
            sendOk(resp, ctx.products().listForSeller(seller.getId()).stream()
                    .map(Product::toView).collect(Collectors.toList()));
        } else if (parts.length == 1) {
            sendOk(resp, ctx.products().get(parseId(parts[0])).toView());
        } else {
            throw new NotFoundException("Unknown product URL");
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        if (pathParts(req).length != 0) {
            throw new NotFoundException("Unknown product URL");
        }
        User actor = requireRole(req, Role.SELLER, Role.ADMIN);
        Product created = ctx.products().create(actor, readBody(req, ProductDraft.class));
        sendJson(resp, HttpServletResponse.SC_CREATED, created.toView());
    }

    @Override
    protected void put(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        String[] parts = pathParts(req);
        User actor = requireRole(req, Role.SELLER, Role.ADMIN);
        if (parts.length == 1) {
            sendOk(resp, ctx.products().update(actor, parseId(parts[0]), readBody(req, ProductDraft.class)).toView());
        } else if (parts.length == 2 && "stock".equals(parts[1])) {
            StockRequest body = readBody(req, StockRequest.class);
            if (body.stock == null) {
                throw new ValidationException("stock is required");
            }
            sendOk(resp, ctx.products().setStock(actor, parseId(parts[0]), body.stock).toView());
        } else {
            throw new NotFoundException("Unknown product URL");
        }
    }

    @Override
    protected void delete(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        String[] parts = pathParts(req);
        if (parts.length != 1) {
            throw new NotFoundException("Unknown product URL");
        }
        User actor = requireRole(req, Role.SELLER, Role.ADMIN);
        ctx.products().delete(actor, parseId(parts[0]));
        sendNoContent(resp);
    }
}
