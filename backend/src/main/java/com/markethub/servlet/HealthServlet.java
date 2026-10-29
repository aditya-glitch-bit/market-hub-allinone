package com.markethub.servlet;

import com.markethub.AppContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** {@code GET /api/health} – quick "is the server up?" check. */
public class HealthServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    public HealthServlet(AppContext ctx) {
        super(ctx);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("time", Instant.now().toString());
        sendOk(resp, body);
    }
}
