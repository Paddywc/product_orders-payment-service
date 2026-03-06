package product.orders.paymentservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * A payment was made to an order that is invalid. The payment will be refunded.
 */
public record InvalidPaymentMadeEvent(UUID eventId, UUID orderId, UUID paymentId, Instant occurredAt) {

    public static InvalidPaymentMadeEvent of(UUID orderId, UUID paymentId) {
        return new InvalidPaymentMadeEvent(UUID.randomUUID(), orderId, paymentId, Instant.now());
    }

}
