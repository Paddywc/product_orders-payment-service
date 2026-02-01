package product.orders.paymentservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when payment has been successfully confirmed
 *
 * This event indicates that the payment Service has completed its saga step successfully.
 */
public record PaymentCompletedEvent(
        UUID eventId,
        UUID orderId,
        Long amountInCents,

        String currency,

        Instant occurredAt
) {

    public PaymentCompletedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        if (amountInCents <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    public static PaymentCompletedEvent of(UUID orderId, Long amountInCents, String currency){
        return new PaymentCompletedEvent(
                UUID.randomUUID(),
                orderId,
                amountInCents,
                currency,
                Instant.now()
        );
    }

}
