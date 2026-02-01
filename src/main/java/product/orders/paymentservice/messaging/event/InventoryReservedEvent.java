package product.orders.paymentservice.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Local service event that follows the json schema of the inventory reserved event published by the inventory service.
 */
public record InventoryReservedEvent(UUID eventId, UUID orderId, Instant occurredAt) {

}
