package product.orders.paymentservice.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import product.orders.paymentservice.domain.exception.DuplicateOrderPaymentException;
import product.orders.paymentservice.domain.exception.OrderPaymentNotFoundException;
import product.orders.paymentservice.domain.model.Payment;
import product.orders.paymentservice.domain.model.PaymentStatus;
import product.orders.paymentservice.messaging.event.PaymentCompletedEvent;
import product.orders.paymentservice.messaging.event.PaymentCreatedEvent;
import product.orders.paymentservice.messaging.event.PaymentFailedEvent;
import product.orders.paymentservice.messaging.event.PaymentRefundedEvent;
import product.orders.paymentservice.messaging.producer.PaymentEventProducer;
import product.orders.paymentservice.repository.PaymentRepository;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceImplTest {
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventProducer eventProducer;

    @InjectMocks
    private PaymentApplicationServiceImpl service;

    private UUID orderId;
    private Currency currency;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        currency = Currency.getInstance("USD");
    }

    @Test
    void testCreate_WhenValid_SavesPaymentAndPublishesEvent() {
        // Arrange
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(false);
        // Act
        service.crete(orderId, 2000L, currency);
        // Assert
        verify(paymentRepository).save(any(Payment.class));
        verify(eventProducer).publish(any(PaymentCreatedEvent.class));
    }

    @Test
    void testCreate_WhenPaymentAlreadyExists_ThrowsDuplicateException() {
        // Arrange
        when(paymentRepository.existsByOrderId(orderId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() ->
                service.crete(orderId, 1000L, currency))
                .isInstanceOf(DuplicateOrderPaymentException.class);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventProducer);
    }

    @Test
    void testComplete_WhenPending_CompletesAndPublishesEvent() {
        // Arrange
        Payment payment = Payment.createPendingPayment(orderId, 1000L, currency);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(payment);

        // Act
        service.complete(orderId);

        // Assert
        verify(paymentRepository).save(payment);
        verify(eventProducer).publish(any(PaymentCompletedEvent.class));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void testComplete_WhenPaymentNotFound_ThrowsException() {
        // Arrange
        when(paymentRepository.findByOrderId(orderId)).thenReturn(null);
        // Act & Assert
        assertThatThrownBy(() -> service.complete(orderId))
                .isInstanceOf(OrderPaymentNotFoundException.class);
    }

    @Test
    void testComplete_WhenAlreadyCompleted_ReturnsWithoutPublishingEvent() {
        // Arrange
        Payment payment = Payment.createPendingPayment(orderId, 1000L, currency);
        payment.complete();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(payment);

        // Act
        Payment result = service.complete(orderId);

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(eventProducer, never()).publish(any(PaymentCompletedEvent.class));
    }

    @Test
    void testRefund_WhenCompleted_RefundsAndPublishesEvent() {
        // Arrange
        Payment payment = Payment.createPendingPayment(orderId, 1000L, currency);
        payment.complete();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(payment);

        // Act
        service.refund(orderId);

        // Assert
        verify(paymentRepository).save(payment);
        verify(eventProducer).publish(any(PaymentRefundedEvent.class));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }


    @Test
    void testRefund_WhenPaymentNotFound_ThrowsException() {
        // Arrange
        when(paymentRepository.findByOrderId(orderId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> service.refund(orderId))
                .isInstanceOf(OrderPaymentNotFoundException.class);
    }

    @Test
    void testRefund_WhenAlreadyRefunded_DoesNothing() {
        // Arrange
        Payment payment = Payment.createPendingPayment(orderId, 1000L, Currency.getInstance("USD"));
        payment.complete();
        payment.refund();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(payment);

        // Act
        service.refund(orderId);

        // Assert
        verify(eventProducer, never()).publish(any(PaymentRefundedEvent.class));
    }

    @Test
    void testMarkFailedIfExists_WhenPaymentExists_MarksFailedAndPublishesEvent() {
        // Arrange
        Payment payment = Payment.createPendingPayment(orderId, 1000L, currency);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(payment);

        // Act
        service.markFailedIfExists(orderId, "card declined");

        // Assert
        verify(paymentRepository).save(payment);
        verify(eventProducer).publish(any(PaymentFailedEvent.class));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }


    @Test
    void testMarkFailedIfExists_WhenPaymentNotFound_DoesNothing() {
        // Arrange
        when(paymentRepository.findByOrderId(orderId)).thenReturn(null);
        // Act
        service.markFailedIfExists(orderId, "error");
        // Assert
        verifyNoInteractions(eventProducer);
    }


    @Test
    void testGetPaymentStatus_WhenPaymentExists_ReturnsStatus() {
        // Arrange
        Payment payment = Payment.createPendingPayment(orderId, 1000L, currency);
        when(paymentRepository.findByOrderId(orderId)).thenReturn(payment);
        // Act
        PaymentStatus status = service.getPaymentStatus(orderId);
        // Assert
        assertThat(status).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void testGetPaymentStatus_WhenPaymentNotFound_ThrowsException() {
        // Arrange
        when(paymentRepository.findByOrderId(orderId)).thenReturn(null);
        // Act & Assert
        assertThatThrownBy(() -> service.getPaymentStatus(orderId))
                .isInstanceOf(OrderPaymentNotFoundException.class);
    }





}
