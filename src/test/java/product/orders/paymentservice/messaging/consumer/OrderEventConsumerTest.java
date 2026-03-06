package product.orders.paymentservice.messaging.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import product.orders.paymentservice.application.saga.PaymentSagaHandler;
import product.orders.paymentservice.messaging.event.InvalidPaymentMadeEvent;
import product.orders.paymentservice.messaging.event.OrderCancelledEvent;
import product.orders.paymentservice.messaging.event.OrderCreatedEvent;
import product.orders.paymentservice.persistance.ProcessedOrderServiceEvent;
import product.orders.paymentservice.repository.ProcessedOrderServiceEventRepository;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderEventConsumerTest {
    private PaymentSagaHandler sagaHandler;
    private ProcessedOrderServiceEventRepository processedRepository;

    private OrderEventConsumer consumer;

    private UUID orderId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        sagaHandler = mock(PaymentSagaHandler.class);
        processedRepository = mock(ProcessedOrderServiceEventRepository.class);
        consumer = new OrderEventConsumer(sagaHandler, processedRepository);
    }

    @Test
    void TestHandleEvent_SendValidOrderCreatedEvent_CallsCreatePayment() {
        // Arrange
        String rawJson = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "totalAmountCents": 1000,
                  "currency": "USD",
                  "customerId": "%s",
                  "customerEmail": "test@email.com",
                  "customerAddress": "address",
                  "occurredAt": "%s",
                  "items": [{"productId": "%s", "quantity": 1}]
                }
                """.formatted(
                eventId,
                orderId,
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID()
        );

        // Act
        consumer.handleEvent(rawJson, "OrderCreatedEvent");

        // Assert
        verify(processedRepository)
                .saveAndFlush(any(ProcessedOrderServiceEvent.class));

        verify(sagaHandler)
                .createPayment(any(OrderCreatedEvent.class));
    }


    @Test
    void testHandleEvent_SentValidOrderCancelledEvent_CallsRefundPayment(){
        // Arrange
        String rawJson = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "reason": "USER_CANCELLED",
                  "occurredAt": "%s"
                }
                """.formatted(
                eventId,
                orderId,
                Instant.now()
        );

        // Act
        consumer.handleEvent(rawJson, "OrderCancelledEvent");

        // Assert
        verify(sagaHandler).refundPayment(any(OrderCancelledEvent.class));
    }


    @Test
    void testHandleEvent_SentValidInvalidPaymentMadeEvent_CallsRefundPayment() {
        // Arrange
        String rawJson = getInvalidPaymentMethodMadeRawJson();

        // Act
        consumer.handleEvent(rawJson, "InvalidPaymentMadeEvent");

        // Assert
        verify(sagaHandler)
                .refundPayment(any(InvalidPaymentMadeEvent.class));
    }

    @Test
    void testHandleEvent_PassedDuplicateEvent_DoesNothing(){
        // Arrange
        String rawJson = getInvalidPaymentMethodMadeRawJson();
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(processedRepository)
                .saveAndFlush(any(ProcessedOrderServiceEvent.class));

        // Act
        consumer.handleEvent(rawJson, "InvalidPaymentMadeEvent");

        // Assert
        verifyNoInteractions(sagaHandler);
    }

    @Test
    void testHandleEvent_PassedUnknownEventType_ThrowsException() {
        // Arrange
        String rawJson = """
                {
                  "eventId": "%s"
                }
                """.formatted(eventId);

        // Act & Assert
        assertThatThrownBy(() ->
                consumer.handleEvent(rawJson, "UnknownEvent")
        ).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(sagaHandler);
    }

    @Test
    void testHandleEvent_PassedValidData_ProcessedEventUsesEventIdAsIdempotencyKey(){
        // Arrange
        String rawJson = getInvalidPaymentMethodMadeRawJson();
        ArgumentCaptor<ProcessedOrderServiceEvent> captor = ArgumentCaptor.forClass(ProcessedOrderServiceEvent.class);

        // Act
        consumer.handleEvent(rawJson, "InvalidPaymentMadeEvent");

        // Assert
        verify(processedRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(eventId);
    }

    private String getInvalidPaymentMethodMadeRawJson(){
        return """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "paymentId": "%s",
                  "occurredAt": "%s"
                }
                """.formatted(
                eventId,
                orderId,
                UUID.randomUUID(),
                Instant.now()
        );
    }

}
