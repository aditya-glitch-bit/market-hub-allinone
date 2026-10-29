package com.markethub.servlet;

import com.markethub.AppContext;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.model.Role;
import com.markethub.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Role-specific dashboards (the role comes from the session, never from the URL, so a buyer
 * cannot ask for the admin dashboard).
 * <pre>
 *   GET /api/dashboard            summary for the signed-in user's role
 *   GET /api/dashboard/sales.csv  seller sales export
 * </pre>
 */
public class DashboardServlet extends ApiServlet {

    private static final long serialVersionUID = 1L;

    public DashboardServlet(AppContext ctx) {
        super(ctx);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws MarketHubException, IOException {
        String[] parts = pathParts(req);
        if (parts.length == 0) {
            sendOk(resp, ctx.dashboards().build(requireUser(req)));
        } else if (parts.length == 1 && "sales.csv".equals(parts[0])) {
            User seller = requireRole(req, Role.SELLER);
            String csv = ctx.dashboards().salesCsv(seller);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/csv");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"sales.csv\"");
            resp.getWriter().write(csv);
        } else {
            throw new NotFoundException("Unknown dashboard URL");
        }
    }
}
