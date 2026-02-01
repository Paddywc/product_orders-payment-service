package product.orders.paymentservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when payment has been successfully refunded
 *
 * This event indicates that the payment Service has successfully undone its work for the given order
 */
public record PaymentRefundedEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt

) {

    public PaymentRefundedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
    }

    public static PaymentRefundedEvent of(UUID orderId){
        return new PaymentRefundedEvent(
                UUID.randomUUID(),
                orderId,

                Instant.now()
        );
    }

}
