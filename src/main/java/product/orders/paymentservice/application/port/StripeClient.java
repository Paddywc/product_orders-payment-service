package product.orders.paymentservice.application.port;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;

/**
 * Application boundary. An adapter for interacting with the Stripe API
 */
public interface StripeClient {

    /**
     * Create a Stripe Checkout session
     * @param params params for the session
     * @param options request options
     * @return the created session
     */
    Session createSession(SessionCreateParams params,
                          RequestOptions options) throws StripeException;
}
