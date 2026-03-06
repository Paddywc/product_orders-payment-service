package product.orders.paymentservice.messaging.producer;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import product.orders.paymentservice.config.KafkaTopicsProperties;
import product.orders.paymentservice.messaging.event.*;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties topics;


    private PaymentEventProducer producer;

    private static final String TOPIC = "payment.events.v1";

    private UUID orderId;


    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        when(topics.getPaymentEvents()).thenReturn(TOPIC);
        producer = new PaymentEventProducer(kafkaTemplate, topics);
    }

    @Test
    void testPublish_PaymentCompletedEvent_SendsCorrectMessage() {
        // Arrange
        Long amount = 1000L;
        String currency = "USD";

        PaymentCompletedEvent event =
                PaymentCompletedEvent.of(orderId, UUID.randomUUID(), amount, currency);

        // Act
        producer.publish(event);

        // Assert
        verifyMessageSent(event, "PaymentCompletedEvent");
    }

    @Test
    void testPublish_PaymentFailedEvent_SendsCorrectMessage() {
        // Arrange
        PaymentFailedEvent event = PaymentFailedEvent.of(orderId, "failure");
        // Act
        producer.publish(event);
        // Assert
        verifyMessageSent(event, "PaymentFailedEvent");
    }

    @Test
    void testPublish_PaymentRefundedEvent_SendsCorrectMessage() {
        // Arrange
        PaymentRefundedEvent event = PaymentRefundedEvent.of(orderId);
        // Act
        producer.publish(event);
        // Assert
        verifyMessageSent(event, "PaymentRefundedEvent");
    }


    @Test
    void testPublish_PaymentRefundFailedEvent_SendsCorrectMessage() {
        // Arrange
        PaymentRefundFailedEvent event = PaymentRefundFailedEvent.of(orderId, "error");
        // Act
        producer.publish(event);
        // Assert
        verifyMessageSent(event, "PaymentRefundFailedEvent");
    }

    // --------------------------------------------------------
    // PaymentCreatedEvent
    // --------------------------------------------------------

    @Test
    void publish_PaymentCreatedEvent_SendsCorrectMessage() {
        // Arrange
        UUID paymentId = UUID.randomUUID();
        PaymentCreatedEvent event = PaymentCreatedEvent.of(orderId, paymentId);
        // Act
        producer.publish(event);
        // Assert
        verifyMessageSent(event, "PaymentCreatedEvent");
    }


    // --------------------------------------------------------
    // Helper
    // --------------------------------------------------------

    private void verifyMessageSent(Object event, String expectedType) {
        UUID eventId = extractEventId(event);

        ArgumentCaptor<Message<Object>> captor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(captor.capture());
        Message<Object> message = captor.getValue();


        assertThat(message.getPayload()).isEqualTo(event);
        assertThat(message.getHeaders()).hasFieldOrPropertyWithValue(KafkaHeaders.TOPIC, TOPIC);
        assertThat(message.getHeaders()).hasFieldOrPropertyWithValue(KafkaHeaders.KEY, eventId.toString());
        assertThat(message.getHeaders()).hasFieldOrPropertyWithValue("eventType", expectedType);
        assertThat(message.getHeaders()).hasFieldOrPropertyWithValue("orderId", orderId.toString());
    }

    private UUID extractEventId(Object event) {
        try {
            return (UUID) event.getClass().getMethod("eventId").invoke(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
