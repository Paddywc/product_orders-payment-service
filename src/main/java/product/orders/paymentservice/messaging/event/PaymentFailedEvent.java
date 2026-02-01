package product.orders.paymentservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a payment has failed.
 * <p>
 * This event is terminal for the payment saga and is used
 * by downstream services to perform compensation actions.
 */
public record PaymentFailedEvent(UUID eventId, UUID orderId, Instant occurredAt) {

    public PaymentFailedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }

    }

    public static PaymentFailedEvent of(UUID orderId) {
        return new PaymentFailedEvent(
                UUID.randomUUID(),
                orderId,
                Instant.now()
        );
    }

}
