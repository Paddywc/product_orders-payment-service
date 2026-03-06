package product.orders.paymentservice.application.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import product.orders.paymentservice.application.PaymentApplicationService;
import product.orders.paymentservice.domain.exception.DuplicateOrderPaymentException;
import product.orders.paymentservice.domain.exception.OrderPaymentNotFoundException;
import product.orders.paymentservice.domain.model.PaymentStatus;
import product.orders.paymentservice.messaging.event.*;
import product.orders.paymentservice.messaging.producer.PaymentEventProducer;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PaymentSagaHandlerTest {

    private PaymentApplicationService paymentApplicationService;

    private PaymentEventProducer eventProducer;

    private PaymentSagaHandler sagaHandler;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        paymentApplicationService = mock(PaymentApplicationService.class);
        eventProducer = mock(PaymentEventProducer.class);
        sagaHandler = new PaymentSagaHandler(paymentApplicationService, eventProducer);
    }

    // ======================================================
    // createPayment
    // ======================================================


    @Test
    void testCreatePayment_ValidRequest_CallsApplicationServiceCreate(){
        // Arrange
        OrderCreatedEvent event = makeValidOrderCreatedEvent();
        // Act
        sagaHandler.createPayment(event);
        // Assert
        verify(paymentApplicationService).crete(
                event.orderId(),
                event.totalAmountCents(),
                Currency.getInstance(event.currency())
        );
        verifyNoInteractions(eventProducer);
    }

    @Test
    void testCreatePayment_DuplicatePaymentExceptionThrown_HandlesWithoutThrowing(){
        // Arrange
        OrderCreatedEvent event = makeValidOrderCreatedEvent();
        doThrow(new DuplicateOrderPaymentException(orderId))
                .when(paymentApplicationService)
                .crete(any(UUID.class), anyLong(), any(Currency.class));

        // Act
        sagaHandler.createPayment(event);

        // Assert
        verify(paymentApplicationService).crete(
                event.orderId(),
                event.totalAmountCents(),
                Currency.getInstance(event.currency())
        );
        verifyNoInteractions(eventProducer);
    }

    @Test
    void testCreatePayment_ExceptionOtherThanDuplicatePayment_MarksFailedAndPublishesFailedEventAndThrowsException(){
        // Arrange
        OrderCreatedEvent event = makeValidOrderCreatedEvent();
        doThrow(new RuntimeException("some other exception"))
                .when(paymentApplicationService)
                .crete(any(UUID.class), anyLong(), any(Currency.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> sagaHandler.createPayment(event));
        verify(paymentApplicationService).markFailedIfExists(eq(event.orderId()), anyString());
        verify(eventProducer).publish(any(PaymentFailedEvent.class));
    }

    // ======================================================
    // refundPayment (OrderCancelledEvent)
    // ======================================================


    @Test
    void testRefundPayment_WhenCompleted_RefundsAndPublishesEvent() {
        // Arrange
        OrderCancelledEvent event = OrderCancelledEvent.of(orderId, CancellationReason.USER_CANCELLED);
        when(paymentApplicationService.getPaymentStatus(orderId))
                .thenReturn(PaymentStatus.COMPLETED);

        // Act
        sagaHandler.refundPayment(event);

        // Assert
        verify(paymentApplicationService).refund(orderId);
        verify(eventProducer).publish(any(PaymentRefundedEvent.class));
    }

    @Test
    void testRefundPayment_PaymentStatusNotCompleted_SkipsRefund() {
        // Arrange
        OrderCancelledEvent event = OrderCancelledEvent.of(orderId, CancellationReason.USER_CANCELLED);
        when(paymentApplicationService.getPaymentStatus(orderId))
                .thenReturn(PaymentStatus.PENDING);

        // Act
        sagaHandler.refundPayment(event);

        // Assert
        verify(paymentApplicationService, never()).refund(any());
        verifyNoInteractions(eventProducer);
    }

    @Test
    void testRefundPayment_PaymentNotFoundException_SkipsRefund() {
        // Arrange
        OrderCancelledEvent event = OrderCancelledEvent.of(orderId, CancellationReason.USER_CANCELLED);
        when(paymentApplicationService.getPaymentStatus(orderId))
                .thenThrow(new OrderPaymentNotFoundException(orderId));

        // Act
        sagaHandler.refundPayment(event);

        // Assert
        verify(paymentApplicationService, never()).refund(any());
        verifyNoInteractions(eventProducer);
    }

    @Test
    void testRefundPayment_ExceptionOtherThanPaymentNotFound_PublishesRefundFailedEvent() {
        // Arrange
        OrderCancelledEvent event = OrderCancelledEvent.of(orderId, CancellationReason.USER_CANCELLED);
        when(paymentApplicationService.getPaymentStatus(orderId))
                .thenReturn(PaymentStatus.COMPLETED);

        doThrow(new RuntimeException("Stripe error"))
                .when(paymentApplicationService)
                .refund(orderId);

        // Act
        sagaHandler.refundPayment(event);

        // Assert
        verify(eventProducer).publish(any(PaymentRefundFailedEvent.class));
    }

    // ======================================================
    // refundPayment (OrderCancelledEvent)
    // ======================================================

    @Test
    void testRefundPayment_PassedInvalidPaymentMadeEvent_RefundsAndPublishesEvent(){
        // Arrange
        InvalidPaymentMadeEvent event = InvalidPaymentMadeEvent.of(orderId, UUID.randomUUID());
        // Act
        sagaHandler.refundPayment(event);
        // Assert
        verify(paymentApplicationService).refund(orderId);
        verify(eventProducer).publish(any(PaymentRefundedEvent.class));
    }

    @Test
    void testRefundPayment_PassedInvalidPaymentMadeAndThrowsException_PublishesFailedEvent(){
        // Arrange
        InvalidPaymentMadeEvent event = InvalidPaymentMadeEvent.of(orderId, UUID.randomUUID());
        doThrow(new RuntimeException("Stripe down"))
                .when(paymentApplicationService)
                .refund(orderId);

        // Act
        sagaHandler.refundPayment(event);

        // Assert
        verify(eventProducer).publish(any(PaymentRefundFailedEvent.class));
    }



    // --------------------------------------------------
    // Helper
    // --------------------------------------------------
    private OrderCreatedEvent makeValidOrderCreatedEvent(){
        return OrderCreatedEvent.of(
                orderId,
                100L,
                "EUR",
                UUID.randomUUID(),
                "email@example.com",
                "123 Road St",
                List.of(new OrderItem(UUID.randomUUID(), 10)));
    }

}
