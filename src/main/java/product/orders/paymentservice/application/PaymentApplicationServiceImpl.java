package product.orders.paymentservice.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.orders.paymentservice.messaging.event.PaymentCompletedEvent;
import product.orders.paymentservice.messaging.event.PaymentFailedEvent;
import product.orders.paymentservice.messaging.event.PaymentRefundedEvent;
import product.orders.paymentservice.domain.exception.DuplicateOrderPaymentException;
import product.orders.paymentservice.domain.exception.OrderPaymentNotFoundException;
import product.orders.paymentservice.domain.model.Payment;
import product.orders.paymentservice.domain.model.PaymentStatus;
import product.orders.paymentservice.messaging.event.PaymentCreatedEvent;
import product.orders.paymentservice.messaging.producer.PaymentEventProducer;
import product.orders.paymentservice.repository.PaymentRepository;

import java.util.Currency;
import java.util.UUID;

@Service
public class PaymentApplicationServiceImpl implements PaymentApplicationService {
    private final PaymentRepository paymentRepository;

    private final PaymentEventProducer eventProducer;

    Logger logger = LoggerFactory.getLogger(PaymentApplicationServiceImpl.class);

    public PaymentApplicationServiceImpl(PaymentRepository paymentRepository, PaymentEventProducer eventProducer) {
        this.paymentRepository = paymentRepository;
        this.eventProducer = eventProducer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void crete(UUID orderId, long amountInCents, Currency currency) {
        // Throw exception if payment already exists for the order (the db would enforce this later anyway)
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new DuplicateOrderPaymentException(orderId);
        }

        Payment payment = Payment.createPendingPayment(orderId, amountInCents, currency);
        paymentRepository.save(payment);
        eventProducer.publish(PaymentCreatedEvent.of(orderId, payment.getPaymentId()));
        logger.info("Payment created for order {}", orderId);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Payment complete(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            throw new OrderPaymentNotFoundException(orderId);
        }
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            logger.info("Payment already completed for order {}", orderId);
            return payment;
        }

        payment.complete();
        paymentRepository.save(payment);

        eventProducer.publish(PaymentCompletedEvent.of(orderId, payment.getPaymentId(), payment.getAmountInCents(), payment.getCurrency().getCurrencyCode()));

        logger.info("Payment completed for order {}", orderId);

        return payment;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void refund(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            throw new OrderPaymentNotFoundException(orderId);
        }

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            logger.info("Payment already refunded for order {}", orderId);
            return;
        }

        payment.refund();
        paymentRepository.save(payment);

        eventProducer.publish(PaymentRefundedEvent.of(orderId));

        logger.info("Payment refunded for order {}", orderId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void markFailedIfExists(UUID orderId, String reason) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            logger.info("Can not mark payment failed, no payment found for order {}", orderId);
            return;
        }
        payment.fail();
        paymentRepository.save(payment);

        eventProducer.publish(PaymentFailedEvent.of(orderId, reason));
        logger.info("Payment failed for order {}: {}", orderId, reason);
    }

    @Override
    public PaymentStatus getPaymentStatus(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            throw new OrderPaymentNotFoundException(orderId);
        }
        return payment.getStatus();
    }


}
