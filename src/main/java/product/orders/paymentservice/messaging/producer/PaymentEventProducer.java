package product.orders.paymentservice.messaging.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import product.orders.paymentservice.config.KafkaTopicsProperties;
import product.orders.paymentservice.messaging.event.*;

import java.util.UUID;


/**
 * The PaymentEventProducer is responsible for publishing payment-related events to a Kafka topic.
 * It handles multiple types of events such as payment completion, failure, refunds, and refund failures.
 * The events are serialized and sent to the appropriate topic defined in the Kafka configuration. All events are
 * sent with an 'eventType' header that identifies the event type by the simple class name.
 * <p>
 * This class uses the KafkaTemplate to send messages and leverages Kafka headers to provide metadata
 * such as the event type, order ID, and event ID.

 *
 */
@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties topics;


    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate, KafkaTopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    private String getTopicName() {
        return topics.getPaymentEvents();
    }

    private <T> Message<T> buildMessage(T event, UUID eventId, UUID orderId) {
        return MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, getTopicName())
                .setHeader(KafkaHeaders.KEY, eventId.toString())
                .setHeader("eventType", event.getClass().getSimpleName())
                .setHeader("orderId", orderId.toString())
                .build();
    }

    public void publish(PaymentCompletedEvent event) {
        Message<Object> message = buildMessage(event, event.eventId(), event.orderId());
        kafkaTemplate.send(message);
    }

    public void publish(PaymentFailedEvent event) {
        Message<Object> message = buildMessage(event, event.eventId(), event.orderId());
        kafkaTemplate.send(message);
    }

    public void publish(PaymentRefundedEvent event) {
        Message<Object> message = buildMessage(event, event.eventId(), event.orderId());
        kafkaTemplate.send(message);
    }

    public void publish(PaymentRefundFailedEvent event) {
        Message<Object> message = buildMessage(event, event.eventId(), event.orderId());
        kafkaTemplate.send(message);
    }

    public void publish(PaymentCreatedEvent event) {
        Message<Object> message = buildMessage(event, event.eventId(), event.orderId());
        kafkaTemplate.send(message);
    }


}
