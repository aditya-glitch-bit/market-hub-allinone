package com.markethub.servlet;

import com.markethub.AppContext;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.exception.ValidationException;
import com.markethub.model.Cart;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Shopping bag stored in the session (works before sign-in, like the storefront).
 * <pre>
 *   GET    /api/cart
 *   POST   /api/cart              {productId, quantity=1}    add units
 *   PUT    /api/cart              {productId, quantity}      set exact quantity (0 removes)
 *   DELETE /api/cart/{productId}                             remove one line
 *   DELETE /api/cart                                         empty the bag
 * </pre>
 */
public class CartServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    /** Request body of POST and PUT. */
    static class LineRequest {
        Long productId;
        Integer quantity;
    }

    public CartServlet(AppContext ctx) {
        super(ctx);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        sendOk(resp, ctx.carts().describe(cartOf(req)));
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        LineRequest body = readLine(req);
        Cart cart = cartOf(req);
        ctx.carts().add(cart, body.productId, body.quantity == null ? 1 : body.quantity);
        sendOk(resp, ctx.carts().describe(cart));
    }

    @Override
    protected void put(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        LineRequest body = readLine(req);
        if (body.quantity == null) {
            throw new ValidationException("quantity is required");
        }
        Cart cart = cartOf(req);
        ctx.carts().setQuantity(cart, body.productId, body.quantity);
        sendOk(resp, ctx.carts().describe(cart));
    }

    @Override
    protected void delete(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        String[] parts = pathParts(req);
        Cart cart = cartOf(req);
        if (parts.length == 0) {
            cart.clear();
        } else if (parts.length == 1) {
            cart.remove(parseId(parts[0]));
        } else {
            throw new NotFoundException("Unknown cart URL");
        }
        sendOk(resp, ctx.carts().describe(cart));
    }

    private static LineRequest readLine(HttpServletRequest req) throws MarketHubException, IOException {
        LineRequest body = readBody(req, LineRequest.class);
        if (body.productId == null) {
            throw new ValidationException("productId is required");
        }
        return body;
    }
}
