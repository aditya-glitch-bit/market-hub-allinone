package com.markethub.servlet;

import com.markethub.AppContext;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Collections;

/**
 * Account endpoints.
 * <pre>
 *   POST /api/auth/register   {name, email, password, role: "buyer"|"seller"}
 *   POST /api/auth/login      {email, password}
 *   POST /api/auth/logout
 *   GET  /api/auth/me
 * </pre>
 */
public class AuthServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    /** Request body of /register. */
    static class RegisterRequest {
        String name;
        String email;
        String password;
        String role;
    }

    /** Request body of /login. */
    static class LoginRequest {
        String email;
        String password;
    }

    public AuthServlet(AppContext ctx) {
        super(ctx);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        String action = pathParts(req).length == 1 ? pathParts(req)[0] : "";
        switch (action) {
            case "register": {
                RegisterRequest body = readBody(req, RegisterRequest.class);
                User user = ctx.accounts().register(body.name, body.email, body.password,
                        body.role == null ? "buyer" : body.role);
                signIn(req, user);
                sendJson(resp, HttpServletResponse.SC_CREATED, user.toView());
                break;
            }
            case "login": {
                LoginRequest body = readBody(req, LoginRequest.class);
                User user = ctx.accounts().login(body.email, body.password);
                signIn(req, user);
                sendOk(resp, user.toView());
                break;
            }
            case "logout": {
                HttpSession session = req.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                sendOk(resp, Collections.singletonMap("message", "Signed out"));
                break;
            }
            default:
                throw new NotFoundException("Unknown auth action");
        }
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        if (pathParts(req).length == 1 && "me".equals(pathParts(req)[0])) {
            sendOk(resp, requireUser(req).toView());
            return;
        }
        throw new NotFoundException("Unknown auth action");
    }
}
