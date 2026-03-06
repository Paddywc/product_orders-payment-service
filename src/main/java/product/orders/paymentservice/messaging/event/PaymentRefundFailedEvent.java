package product.orders.paymentservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when payment has been successfully refunded
 * <p>
 * This event exists outside the Saga and is emitted by the payment service when a payment refund fails.
 */
public record PaymentRefundFailedEvent(
        UUID eventId,
        UUID orderId,
        String reason,
        Instant occurredAt

) {

    public PaymentRefundFailedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
    }

    public static PaymentRefundFailedEvent of(UUID orderId, String reason) {
        return new PaymentRefundFailedEvent(
                UUID.randomUUID(),
                orderId,
                reason,
                Instant.now()
        );
    }

}
