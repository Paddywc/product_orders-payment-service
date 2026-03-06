package product.orders.paymentservice.domain.model;

/**
 * The current status of a {@link Payment}
 */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
}