package product.orders.paymentservice.infrastructure.stripe;

import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Component;
import product.orders.paymentservice.application.dto.VerifiedStripeWebhookEvent;
import product.orders.paymentservice.application.port.StripeEventVerifier;
import product.orders.paymentservice.application.port.StripeWebHookParser;

import java.util.UUID;

/**
 * Utilizes the stripe webhook parser to verify stripe webhook events. Uses objects from the stripe API, but not
 * static methods
 */
@Component
public class StripeEventVerifierImpl implements StripeEventVerifier {
    private final StripeWebHookParser stripeWebHookParser;

    public StripeEventVerifierImpl(StripeWebHookParser stripeWebHookParser) {
        this.stripeWebHookParser = stripeWebHookParser;
    }

    @Override
    public VerifiedStripeWebhookEvent verify(String payload, String signature) {
        Event event = stripeWebHookParser.parse(payload, signature);
        UUID orderId = extractOrderId(event);

        return new VerifiedStripeWebhookEvent(
                event.getId(),
                event.getType(),
                orderId
        );
    }


    private UUID extractOrderId(Event event) {
        StripeObject object = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow();

        if (object instanceof Session session) {
            return UUID.fromString(session.getMetadata().get("orderId"));
        }

        throw new IllegalArgumentException("Unsupported Stripe object");
    }
}
