package product.orders.paymentservice.infrastructure.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import product.orders.paymentservice.application.port.StripeWebHookParser;

/**
 * Wrapper around the static method from the Stripe API that parses stripe webhook events
 */
@Component
public class StripeWebHookParserImpl implements StripeWebHookParser {

    private final String webhookSecret;

    public StripeWebHookParserImpl(@Value("${stripe.webhook-secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
    @Override
    public Event parse(String payload, String signature) {
        try {
            return Webhook.constructEvent(payload, signature, webhookSecret);
        }catch (SignatureVerificationException e){
            throw new IllegalArgumentException("Invalid Stripe signature", e);
        }
    }
}
