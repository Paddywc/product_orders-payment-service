package product.orders.paymentservice.application.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.orders.paymentservice.application.PaymentApplicationService;
import product.orders.paymentservice.domain.exception.DuplicateOrderPaymentException;
import product.orders.paymentservice.domain.exception.OrderPaymentNotFoundException;
import product.orders.paymentservice.domain.model.PaymentStatus;
import product.orders.paymentservice.messaging.event.*;
import product.orders.paymentservice.messaging.producer.PaymentEventProducer;

import java.util.Currency;

/**
 * The PaymentSagaHandler class handles the lifecycle of payments within a distributed saga process.
 * It listens for specific events related to orders and performs corresponding payment actions
 * such as creating a payment, refunding a payment, or managing failures.
 * <br>
 * This class integrates with the {@link PaymentApplicationService} to perform payment-related operations
 * and communicates changes with the system by publishing events via {@link PaymentEventProducer}.
 */
@Service
public class PaymentSagaHandler {

    private final PaymentApplicationService paymentApplicationService;

    private final PaymentEventProducer eventProducer;

    private final Logger logger = LoggerFactory.getLogger(PaymentSagaHandler.class);

    public PaymentSagaHandler(PaymentApplicationService paymentApplicationService, PaymentEventProducer eventProducer) {
        this.paymentApplicationService = paymentApplicationService;
        this.eventProducer = eventProducer;
    }

    /**
     * Create a payment for the given order. Payment application service will fire a payment created event on success.
     * Will mark the payment as failed and publish a payment failed event if the payment creation fails. Attempts to
     * make duplicate payments are ignored.
     *
     * @param event order details
     */
    @Transactional(value = "transactionManager")
    public void createPayment(OrderCreatedEvent event) {
        try {
            paymentApplicationService.crete(event.orderId(), event.totalAmountCents(), Currency.getInstance(event.currency()));
            logger.info("Payment created for order {}", event.orderId());
        } catch (DuplicateOrderPaymentException e) {
            logger.info("Payment already created for order {}", event.orderId());
        } catch (Exception e) {
            String failureReason = "Payment creation failed: " + e.getMessage();
            logger.error(failureReason, e);
            eventProducer.publish(PaymentFailedEvent.of(event.orderId(), failureReason));
            paymentApplicationService.markFailedIfExists(event.orderId(), failureReason);
            throw e;
        }
    }


    /**
     * Refund a payment and (via the payment application service) publish a payment refunded event. If the
     * payment is in a completed state or the order does not exist, do nothing. For other exception publish
     * a payment refund failed event.
     * @param event containing order details
     */
    @Transactional(value = "transactionManager")
    public void refundPayment(OrderCancelledEvent event) {
        try {
            if (paymentApplicationService.getPaymentStatus(event.orderId()) != PaymentStatus.COMPLETED) {
                logger.info("Payment not completed for order {}. Skipping refund", event.orderId());
                return;
            }
            paymentApplicationService.refund(event.orderId());
            eventProducer.publish(PaymentRefundedEvent.of(event.orderId()));
            logger.info("Payment refunded for order {}", event.orderId());
        } catch (OrderPaymentNotFoundException e) {
            logger.info("Payment not found for order {}. Skipping refund", event.orderId());
        } catch (Exception e) {
            eventProducer.publish(PaymentRefundFailedEvent.of(
                    event.orderId(),
                    e.getMessage()
            ));
            logger.error("Payment refund failed for order {}", event.orderId(), e);
        }
    }

    /**
     * An invalid payment was made. Refund the payment and publish a payment refunded event. Publish a
     * payment refund failed event if the refund fails.
     *
     * @param event the event fired when an invalid payment is made
     */
    @Transactional(value = "transactionManager")
    public void refundPayment(InvalidPaymentMadeEvent event) {
        try {
            logger.info("Invalid payment made for order {}. Refunding payment {}", event.orderId(), event.paymentId());
            paymentApplicationService.refund(event.orderId());
            eventProducer.publish(PaymentRefundedEvent.of(event.orderId()));
            logger.info("Payment refunded for order {}", event.orderId());
        } catch (Exception e) {
            eventProducer.publish(PaymentRefundFailedEvent.of(event.orderId(), e.getMessage()));
            logger.error("Payment refund failed for order {}", event.orderId(), e);
        }
    }
}
