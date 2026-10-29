package com.markethub.servlet;

import com.markethub.AppContext;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.exception.ValidationException;
import com.markethub.model.Role;
import com.markethub.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Admin user management.
 * <pre>
 *   GET /api/users                  list every account
 *   PUT /api/users/{id}/active      {active: true|false}   suspend or re-enable
 * </pre>
 */
public class UserServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    /** Request body of /{id}/active. */
    static class ActiveRequest {
        Boolean active;
    }

    public UserServlet(AppContext ctx) {
        super(ctx);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        if (pathParts(req).length != 0) {
            throw new NotFoundException("Unknown user URL");
        }
        User admin = requireRole(req, Role.ADMIN);
        sendOk(resp, ctx.accounts().listAll(admin).stream().map(User::toView).collect(Collectors.toList()));
    }

    @Override
    protected void put(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        String[] parts = pathParts(req);
        if (parts.length != 2 || !"active".equals(parts[1])) {
            throw new NotFoundException("Unknown user URL");
        }
        User admin = requireRole(req, Role.ADMIN);
        ActiveRequest body = readBody(req, ActiveRequest.class);
        if (body.active == null) {
            throw new ValidationException("active is required");
        }
        sendOk(resp, ctx.accounts().setActive(admin, parseId(parts[0]), body.active).toView());
    }
}
