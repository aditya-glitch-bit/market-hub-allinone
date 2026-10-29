package com.markethub.service;

import com.markethub.concurrent.ActivityLog;
import com.markethub.concurrent.OrderProcessor;
import com.markethub.dao.OrderDao;
import com.markethub.dao.ProductDao;
import com.markethub.dao.TransactionManager;
import com.markethub.exception.ConflictException;
import com.markethub.exception.ForbiddenException;
import com.markethub.exception.MarketHubException;
import com.markethub.exception.NotFoundException;
import com.markethub.exception.ValidationException;
import com.markethub.model.Cart;
import com.markethub.model.Order;
import com.markethub.model.OrderItem;
import com.markethub.model.OrderStatus;
import com.markethub.model.Product;
import com.markethub.model.Role;
import com.markethub.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Checkout and order management. */
public class OrderService {

    private final OrderDao orders;
    private final ProductDao products;
    private final InventoryService inventory;
    private final TransactionManager transactions;
    private final OrderProcessor processor;
    private final ActivityLog activity;
    /** Source of order numbers; atomic so two simultaneous checkouts never get the same number. */
    private final AtomicInteger lastOrderNumber;

    public OrderService(OrderDao orders, ProductDao products, InventoryService inventory,
                        TransactionManager transactions, OrderProcessor processor,
                        ActivityLog activity) throws MarketHubException {
        this.orders = orders;
        this.products = products;
        this.inventory = inventory;
        this.transactions = transactions;
        this.processor = processor;
        this.activity = activity;
        this.lastOrderNumber = new AtomicInteger(Math.max(1000, orders.maxOrderNumber()));
    }

    /**
     * Turns the bag into an order.
     *
     * <p>Everything happens in one transaction: stock is reserved and the order is saved, or
     * nothing changes. On success the bag is emptied and the order is queued for shipping.</p>
     */
    public Order checkout(User buyer, Cart cart) throws MarketHubException {
        if (buyer.getRole() != Role.BUYER) {
            throw new ForbiddenException("Only buyers can place orders");
        }
        Map<Long, Integer> lines = cart.snapshot();
        if (lines.isEmpty()) {
            throw new ValidationException("Your bag is empty");
        }

        Order saved = transactions.inTransaction(() -> {
            List<OrderItem> items = new ArrayList<>();
            for (Map.Entry<Long, Integer> line : lines.entrySet()) {
                Product product = products.findById(line.getKey())
                        .orElseThrow(() -> new NotFoundException("Product", line.getKey()));
                items.add(new OrderItem(product.getId(), product.getSellerId(), product.getName(),
                        product.getPrice(), line.getValue()));
            }
            inventory.reserve(lines);
            try {
                Order order = new Order(null, lastOrderNumber.incrementAndGet(), buyer.getId(),
                        buyer.getName(), OrderStatus.PROCESSING, LocalDateTime.now(), items);
                return orders.save(order);
            } catch (MarketHubException | RuntimeException e) {
                inventory.release(lines); // give the stock back if saving the order failed
                throw e;
            }
        });

        cart.clear();
        processor.submit(saved.getId());
        activity.record(buyer.getName(), "placed order " + saved.getCode());
        return saved;
    }

    /** Orders visible to the user: all (admin), their own (buyer) or those containing their products (seller). */
    public List<Order> listFor(User actor) throws MarketHubException {
        List<Order> result;
        switch (actor.getRole()) {
            case ADMIN:
                result = new ArrayList<>(orders.findAll());
                break;
            case SELLER:
                result = new ArrayList<>(orders.findBySeller(actor.getId()));
                break;
            default:
                result = new ArrayList<>(orders.findByBuyer(actor.getId()));
        }
        result.sort(Comparator.comparing(Order::getId).reversed());
        return result;
    }

    public Order getFor(User actor, long orderId) throws MarketHubException {
        Order order = orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order", orderId));
        boolean allowed;
        switch (actor.getRole()) {
            case ADMIN:
                allowed = true;
                break;
            case SELLER:
                allowed = order.containsSeller(actor.getId());
                break;
            default:
                allowed = order.getBuyerId() == actor.getId();
        }
        if (!allowed) {
            // Same message as "missing" so order ids cannot be probed.
            throw new NotFoundException("Order", orderId);
        }
        return order;
    }

    /**
     * Moves an order to a new status.
     *
     * <ul>
     *   <li>Admins and involved sellers may ship, deliver or cancel.</li>
     *   <li>Buyers may only cancel their own order.</li>
     *   <li>The change must be a legal step of the {@link OrderStatus} state machine.</li>
     *   <li>Cancelling puts the stock back, in the same transaction as the status change.</li>
     * </ul>
     */
    public Order updateStatus(User actor, long orderId, OrderStatus target) throws MarketHubException {
        Order order = getFor(actor, orderId);
        authorizeChange(actor, order, target);
        OrderStatus current = order.getStatus();
        if (!current.canMoveTo(target)) {
            throw new ConflictException("An order that is " + current.getLabel()
                    + " cannot become " + target.getLabel());
        }
        transactions.inTransaction(() -> {
            // Compare-and-set: fails if another thread changed the status after we read it.
            if (!orders.updateStatus(orderId, current, target)) {
                throw new ConflictException("The order was just changed by someone else; reload and try again");
            }
            if (target == OrderStatus.CANCELLED) {
                Map<Long, Integer> quantities = new LinkedHashMap<>();
                for (OrderItem item : order.getItems()) {
                    quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
                }
                inventory.release(quantities);
            }
            return null;
        });
        order.setStatus(target);
        activity.record(actor.getName(), order.getCode() + " -> " + target.getLabel());
        return order;
    }

    private static void authorizeChange(User actor, Order order, OrderStatus target) throws ForbiddenException {
        if (actor.getRole() == Role.BUYER) {
            if (order.getBuyerId() != actor.getId() || target != OrderStatus.CANCELLED) {
                throw new ForbiddenException("Buyers can only cancel their own orders");
            }
        } else if (!actor.canUpdateOrderStatus()) {
            throw new ForbiddenException("You cannot change order status");
        }
    }
}
