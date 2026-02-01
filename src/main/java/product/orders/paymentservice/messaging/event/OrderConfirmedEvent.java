package product.orders.paymentservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Local service event that follows the json schema of the order canceled event published by the orders service.

 * Emitted when an order has been fully processed and confirmed.

 * This event represents the successful completion of the order saga.
 * Downstream services should treat this as a terminal success fact.
 */
public record OrderConfirmedEvent(UUID eventId, UUID orderId, Instant occurredAt) {

}
