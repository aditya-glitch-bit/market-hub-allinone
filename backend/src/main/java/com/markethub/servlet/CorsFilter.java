package com.markethub.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Lets the React dev server (a different origin, e.g. http://localhost:5173) call this API with
 * the session cookie. Only the one configured origin is allowed: a wildcard cannot be combined
 * with credentials, and echoing any origin would let any website use a visitor's session.
 */
public class CorsFilter implements Filter {

    private final String allowedOrigin;

    public CorsFilter(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String origin = req.getHeader("Origin");
        if (origin != null && origin.equals(allowedOrigin)) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
            resp.addHeader("Vary", "Origin");
        }
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT); // pre-flight answered here
            return;
        }
        chain.doFilter(request, response);
    }
}
