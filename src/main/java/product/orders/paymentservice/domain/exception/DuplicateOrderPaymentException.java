package product.orders.paymentservice.domain.exception;

import java.util.UUID;

/**
 * Exception thrown when trying to create a duplicate payment for an order
 */
public class DuplicateOrderPaymentException extends RuntimeException {
    public DuplicateOrderPaymentException(UUID orderId) {
        super("Order " + orderId + " already has a payment created");
    }
}
