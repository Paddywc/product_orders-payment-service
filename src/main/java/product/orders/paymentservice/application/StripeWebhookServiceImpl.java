package product.orders.paymentservice.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import product.orders.paymentservice.application.dto.VerifiedStripeWebhookEvent;
import product.orders.paymentservice.application.port.StripeEventVerifier;
import product.orders.paymentservice.domain.model.PaymentStatus;
import product.orders.paymentservice.persistance.ProcessedStripeOrderEvent;
import product.orders.paymentservice.repository.ProcessedStripeOrderEventRepository;


@Service
public class StripeWebhookServiceImpl implements StripeWebhookService {


    private final StripeEventVerifier stripeEventVerifier;
    private final ProcessedStripeOrderEventRepository processedStripeOrderEventRepository;

    private final PaymentApplicationService paymentApplicationService;

    Logger logger = LoggerFactory.getLogger(StripeWebhookServiceImpl.class);

    public StripeWebhookServiceImpl(StripeEventVerifier stripeEventVerifier, ProcessedStripeOrderEventRepository processedStripeOrderEventRepository, PaymentApplicationService paymentApplicationService) {
        this.stripeEventVerifier = stripeEventVerifier;
        this.processedStripeOrderEventRepository = processedStripeOrderEventRepository;
        this.paymentApplicationService = paymentApplicationService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleWebhook(String payload, String signature) {
        VerifiedStripeWebhookEvent event = stripeEventVerifier.verify(payload, signature);

        if (processedStripeOrderEventRepository.existsByEventId(event.eventId())) {
            logger.info("Event already processed: {}", event.eventId());
            return;
        }


        switch (event.eventType()) {
            case "checkout.session.completed":
                paymentApplicationService.complete(event.orderId());
                break;
            case "checkout.session.expired":
                if (paymentApplicationService.getPaymentStatus(event.orderId()) == PaymentStatus.PENDING) {
                    paymentApplicationService.markFailedIfExists(event.orderId(), "Payment failed");
                }

                break;
            default:
                logger.warn("Unhandled event type: {}", event.eventType());
                break;
        }

        processedStripeOrderEventRepository.save(new ProcessedStripeOrderEvent(event.eventId()));
    }


}
