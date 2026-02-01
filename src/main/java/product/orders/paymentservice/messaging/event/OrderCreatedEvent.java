package product.orders.paymentservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Local service event that follows the json schema of the order canceled event published by the orders service.

 * Emitted when an order is successfully created and the order saga should begin.

 * This event represents the successful completion of the order saga.
 * Downstream services should treat this as a terminal success fact.
 */
public record OrderCreatedEvent(UUID eventId, UUID orderId, Instant occurredAt, CancellationReason reason) {

}
