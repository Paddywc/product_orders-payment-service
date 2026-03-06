package product.orders.paymentservice.messaging.event;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Emitted when an order is successfully created and
 * the order saga should begin.
 */
public record OrderCreatedEvent(

        UUID eventId,


        UUID orderId,


        @Positive long totalAmountCents,

        String currency,


        UUID customerId,

        @Email String customerEmail,


        @NotNull @Size(max = 2000) String customerAddress,

        Instant occurredAt,

        List<OrderItem> items) {

    public OrderCreatedEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId must not be null");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
    }

    public static OrderCreatedEvent of(UUID orderId,
                                       Long totalAmountCents,
                                       String currency,
                                       UUID customerId,
                                       String customerEmail,
                                       String customerAddress,
                                       List<OrderItem> items) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),   // event identity
                orderId,
                totalAmountCents,
                currency,
                customerId,
                customerEmail,
                customerAddress,
                Instant.now(),
                List.copyOf(items)   // defensive copy
        );
    }
}