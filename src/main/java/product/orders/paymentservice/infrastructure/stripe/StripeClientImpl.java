package product.orders.paymentservice.infrastructure.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;
import product.orders.paymentservice.application.port.StripeClient;

/**
 * Wrapper around the static method from the Stripe API that creates checkout sessions
 */
@Component
public class StripeClientImpl implements StripeClient {

    /**
     * {@inheritDoc}
     */
    @Override
    public Session createSession(SessionCreateParams params, RequestOptions options) throws StripeException {
        return Session.create(params, options);
    }
}
