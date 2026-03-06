package product.orders.paymentservice.messaging.event;

/**
 * Reason for an {@link OrderCancelledEvent}
 */
public enum CancellationReason {
    INVENTORY_RESERVATION_FAILED,
    PAYMENT_FAILED,
    ORDER_NOT_FOUND,
    USER_CANCELLED,
    TIMEOUT,
    SYSTEM_ERROR
}
