package product.orders.paymentservice.application.port;

import com.stripe.model.Event;

/**
 * Interface for directly parsing stripe webhook events
 */
public interface StripeWebHookParser {
    Event parse(String payload, String signature);
}
