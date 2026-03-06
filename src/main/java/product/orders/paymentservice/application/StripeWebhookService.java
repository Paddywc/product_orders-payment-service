package product.orders.paymentservice.application;

public interface StripeWebhookService {
    /**
     * Handle a webhook event from Stripe. Stripe provides the payload and signature of the webhook event. If the
     * event is a checkout.session.completed event, create a payment for the order associated with the session. If the
     * event is not a checkout.session.completed event, if the payment is still pending, mark the payment as failed.
     * @param payload the payload of the webhook event
     * @param signature the signature of the webhook event
     */
    void handleWebhook(String payload, String signature);
}
