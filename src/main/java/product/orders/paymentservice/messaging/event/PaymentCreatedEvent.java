package product.orders.paymentservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a pending payment has been created
 * <p>
 * This event exists outside the Saga and is emitted by the payment service first creates a payment.
 */
public record PaymentCreatedEvent(
        UUID eventId,
        UUID orderId,
        UUID paymentId,
        Instant occurredAt

) {

    public PaymentCreatedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }
    }

    public static PaymentCreatedEvent of(UUID orderId, UUID paymentId) {
        return new PaymentCreatedEvent(
                UUID.randomUUID(),
                orderId,
                paymentId,
                Instant.now()
        );
    }

}
