package product.orders.paymentservice.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when no payment is found for an order
 */
public class OrderPaymentNotFoundException extends RuntimeException {
    public OrderPaymentNotFoundException(UUID orderId) {
        super("No payment found for order:" + orderId);
    }
}
