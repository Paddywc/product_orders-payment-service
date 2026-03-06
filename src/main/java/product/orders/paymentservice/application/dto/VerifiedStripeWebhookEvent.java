package product.orders.paymentservice.application.dto;

import java.util.UUID;

/**
 * Information about a stripe webhook event which has been verified by the stripe API
 */
public record VerifiedStripeWebhookEvent(String eventId, String eventType, UUID orderId) {


}
