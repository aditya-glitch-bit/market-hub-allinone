package com.markethub.dao;

import com.markethub.exception.MarketHubException;
import com.markethub.model.Order;
import com.markethub.model.OrderStatus;

import java.util.List;

/** Database operations for orders and their line items. */
public interface OrderDao extends GenericDao<Order, Long> {

    List<Order> findByBuyer(long buyerId) throws MarketHubException;

    /** Orders that contain at least one product sold by this seller. */
    List<Order> findBySeller(long sellerId) throws MarketHubException;

    /**
     * Compare-and-set status change: only updates if the order is still in {@code expected}.
     * This stops two threads (a seller and the auto-shipper, say) from both applying a change.
     *
     * @return {@code true} if this call performed the change
     */
    boolean updateStatus(long orderId, OrderStatus expected, OrderStatus target) throws MarketHubException;

    /** Highest order number used so far, or 0 when there are no orders. */
    int maxOrderNumber() throws MarketHubException;
}
