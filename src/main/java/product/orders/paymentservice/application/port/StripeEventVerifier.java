package product.orders.paymentservice.application.port;

import product.orders.paymentservice.application.dto.VerifiedStripeWebhookEvent;

/**
 * Validates stripe webhook events using the stripe API. Adapter for the stripe API; boundary of the application
 * layer
 */
public interface StripeEventVerifier {

    VerifiedStripeWebhookEvent verify(String payload, String signature);
}
