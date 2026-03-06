package product.orders.paymentservice.messaging.event;

import java.util.UUID;

/**
 * Snapshot of an order line item at order creation time.
 */
public record OrderItem(UUID productId, int quantity) {
    public OrderItem {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}