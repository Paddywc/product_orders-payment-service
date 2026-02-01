package product.orders.paymentservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Local service event that follows the json schema of the order canceled event published by the orders service.

 * Emitted when an order is cancelled.

 * This event is terminal for the order saga and is used by downstream services to perform
 * compensation actions.
 */
public record OrderCancelledEvent(UUID eventId, UUID orderId, Instant occurredAt, CancellationReason reason) {

}
