package com.markethub.servlet;

import com.markethub.AppContext;
import com.markethub.exception.AuthenticationException;
import com.markethub.exception.ForbiddenException;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.MethodNotAllowedException;
import com.markethub.exception.ValidationException;
import com.markethub.model.Cart;
import com.markethub.model.Role;
import com.markethub.model.User;
import com.markethub.util.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class of all REST servlets (template-method pattern).
 *
 * <p>{@link #service} is the single entry point: it routes the HTTP method to {@link #get},
 * {@link #post}, {@link #put} or {@link #delete} and catches every {@link MarketHubException}
 * in one place, turning it into a JSON error with the right status code. Subclasses override
 * only the methods they support and may simply {@code throw} on failure. Anything not
 * overridden answers 405.</p>
 */
public abstract class ApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ApiServlet.class.getName());

    static final String SESSION_USER_ID = "userId";
    static final String SESSION_CART = "cart";

    protected final transient AppContext ctx;

    protected ApiServlet(AppContext ctx) {
        this.ctx = ctx;
    }

    // ------------------------------------------------------------ methods subclasses override

    protected void get(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        throw new MethodNotAllowedException("GET");
    }

    protected void post(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        throw new MethodNotAllowedException("POST");
    }

    protected void put(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        throw new MethodNotAllowedException("PUT");
    }

    protected void delete(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        throw new MethodNotAllowedException("DELETE");
    }

    // ------------------------------------------------------------ dispatch and error handling

    @Override
    protected final void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        try {
            switch (req.getMethod()) {
                case "GET":
                    get(req, resp);
                    break;
                case "POST":
                    post(req, resp);
                    break;
                case "PUT":
                    put(req, resp);
                    break;
                case "DELETE":
                    delete(req, resp);
                    break;
                default:
                    throw new MethodNotAllowedException(req.getMethod());
            }
        } catch (MarketHubException e) {
            if (e.getHttpStatus() >= 500) {
                LOG.log(Level.SEVERE, "Request failed: " + req.getMethod() + " " + req.getRequestURI(), e);
            }
            sendError(resp, e.getHttpStatus(), e.getHttpStatus() >= 500 ? "Internal server error" : e.getMessage());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected failure: " + req.getMethod() + " " + req.getRequestURI(), e);
            sendError(resp, 500, "Internal server error");
        }
    }

    // ------------------------------------------------------------ response helpers

    protected static void sendJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(Json.write(body));
    }

    protected static void sendOk(HttpServletResponse resp, Object body) throws IOException {
        sendJson(resp, HttpServletResponse.SC_OK, body);
    }

    protected static void sendNoContent(HttpServletResponse resp) {
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private static void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        if (resp.isCommitted()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("status", status);
        sendJson(resp, status, body);
    }

    // ------------------------------------------------------------ request helpers

    protected static <T> T readBody(HttpServletRequest req, Class<T> type) throws MarketHubException, IOException {
        return Json.read(req.getReader(), type);
    }

    /** Path after the servlet mapping split into segments: {@code /api/orders/5/status} gives [5, status]. */
    protected static String[] pathParts(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(info.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    protected static long parseId(String text) throws ValidationException {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid id: " + text);
        }
    }

    protected static int intParam(HttpServletRequest req, String name, int fallback) throws ValidationException {
        String value = req.getParameter(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Parameter " + name + " must be a whole number");
        }
    }

    // ------------------------------------------------------------ session / security helpers

    /** The signed-in user, re-loaded on every request so suspensions apply immediately. */
    protected User requireUser(HttpServletRequest req) throws MarketHubException {
        HttpSession session = req.getSession(false);
        Object id = session == null ? null : session.getAttribute(SESSION_USER_ID);
        if (!(id instanceof Long)) {
            throw new AuthenticationException("Please sign in");
        }
        return ctx.accounts().requireActive((Long) id);
    }

    protected User requireRole(HttpServletRequest req, Role... allowed) throws MarketHubException {
        User user = requireUser(req);
        for (Role role : allowed) {
            if (user.getRole() == role) {
                return user;
            }
        }
        throw new ForbiddenException("Your account type cannot do this");
    }

    /** Binds the session to the user. A fresh session id is issued to prevent session fixation. */
    protected static void signIn(HttpServletRequest req, User user) {
        if (req.getSession(false) != null) {
            req.changeSessionId();
        }
        req.getSession(true).setAttribute(SESSION_USER_ID, user.getId());
    }

    /** The shopper's bag, created on first use. Synchronised so parallel requests share one bag. */
    protected static Cart cartOf(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        synchronized (session) {
            Object existing = session.getAttribute(SESSION_CART);
            if (existing instanceof Cart) {
                return (Cart) existing;
            }
            Cart cart = new Cart();
            session.setAttribute(SESSION_CART, cart);
            return cart;
        }
    }
}
