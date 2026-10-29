package com.markethub.service.dashboard;

import com.markethub.exception.MarketHubException;
import com.markethub.model.Role;
import com.markethub.model.User;

import java.util.Map;

/**
 * Builds the dashboard data for one role (strategy pattern). {@code DashboardService} keeps one
 * provider per {@link Role} and picks the right one at runtime.
 */
public interface DashboardProvider {

    /** @return the role whose dashboard this provider builds */
    Role role();

    /** @return the summary cards, tables and chart data for the user, as a JSON-friendly map */
    Map<String, Object> build(User user) throws MarketHubException;
}
