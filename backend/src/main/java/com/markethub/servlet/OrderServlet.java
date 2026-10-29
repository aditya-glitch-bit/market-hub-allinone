package com.markethub.servlet;

import com.markethub.AppContext;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.model.Order;
import com.markethub.model.OrderStatus;
import com.markethub.model.User;
import com.markethub.service.OrderViews;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Order endpoints (all require sign-in).
 * <pre>
 *   GET  /api/orders              orders visible to the caller (buyer: own, seller: containing their products, admin: all)
 *   GET  /api/orders/{id}
 *   POST /api/orders              checkout: turns the session bag into an order (buyers only)
 *   PUT  /api/orders/{id}/status  {status: "shipped"|"delivered"|"cancelled"}
 * </pre>
 */
public class OrderServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    /** Request body of /{id}/status. */
    static class StatusRequest {
        String status;
    }

    public OrderServlet(AppContext ctx) {
        super(ctx);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        User user = requireUser(req);
        String[] parts = pathParts(req);
        if (parts.length == 0) {
            sendOk(resp, ctx.orders().listFor(user).stream()
                    .map(order -> OrderViews.forViewer(user, order))
                    .collect(Collectors.toList()));
        } else if (parts.length == 1) {
            sendOk(resp, OrderViews.forViewer(user, ctx.orders().getFor(user, parseId(parts[0]))));
        } else {
            throw new NotFoundException("Unknown order URL");
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        if (pathParts(req).length != 0) {
            throw new NotFoundException("Unknown order URL");
        }
        User buyer = requireUser(req);
        Order order = ctx.orders().checkout(buyer, cartOf(req));
        sendJson(resp, HttpServletResponse.SC_CREATED, OrderViews.forViewer(buyer, order));
    }

    @Override
    protected void put(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        String[] parts = pathParts(req);
        if (parts.length != 2 || !"status".equals(parts[1])) {
            throw new NotFoundException("Unknown order URL");
        }
        User actor = requireUser(req);
        OrderStatus target = OrderStatus.parse(readBody(req, StatusRequest.class).status);
        Order updated = ctx.orders().updateStatus(actor, parseId(parts[0]), target);
        sendOk(resp, OrderViews.forViewer(actor, updated));
    }
}
