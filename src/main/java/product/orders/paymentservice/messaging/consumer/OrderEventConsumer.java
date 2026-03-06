package product.orders.paymentservice.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import product.orders.paymentservice.application.saga.PaymentSagaHandler;
import product.orders.paymentservice.messaging.event.InvalidPaymentMadeEvent;
import product.orders.paymentservice.messaging.event.OrderCancelledEvent;
import product.orders.paymentservice.messaging.event.OrderCreatedEvent;
import product.orders.paymentservice.persistance.ProcessedOrderServiceEvent;
import product.orders.paymentservice.repository.ProcessedOrderServiceEventRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;


/**
 * Consumes order events from Kafka and handles them using the saga handler
 */
@Component
public class OrderEventConsumer {

    private final PaymentSagaHandler sagaHandler;

    private final ProcessedOrderServiceEventRepository processedEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderEventConsumer(PaymentSagaHandler sagaHandler, ProcessedOrderServiceEventRepository processedEventRepository) {
        this.sagaHandler = sagaHandler;
        this.processedEventRepository = processedEventRepository;
    }


    /**
     * Handles incoming Kafka events related to orders and delegates tasks to the saga handler based on the event type.
     * Duplicate events are ignored, and processing is skipped for already handled events. Payments are created
     * for OrderCreatedEvents, and refunded for OrderCancelledEvents and InvalidPaymentMadeEvents.
     *
     * @param rawJson   the raw JSON payload of the event received from Kafka
     * @param eventType the type of the event received (e.g., "OrderCreatedEvent", "OrderCancelledEvent",
     *                  "InvalidPaymentMadeEvent")
     */
    @KafkaListener(topics = "#{@kafkaTopicsProperties.orderEvents}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleEvent(String rawJson,
                            @Header("eventType") String eventType) {
        JsonNode node = objectMapper.readValue(rawJson, JsonNode.class);
        String eventIdKey = node.get("eventId").asString();

        // Do not process duplicate events
        try {
            processedEventRepository.saveAndFlush(new ProcessedOrderServiceEvent(UUID.fromString(eventIdKey)));
        } catch (DataIntegrityViolationException e) {
            // Event already exists. Exit. Catch exception rather than checking to
            // avoid idempotency issues
            Logger logger = LoggerFactory.getLogger(getClass());
            logger.debug("Event {} already processed", eventIdKey);
            return;
        }


        switch (eventType) {
            case "OrderCreatedEvent" ->
                    sagaHandler.createPayment(objectMapper.treeToValue(node, OrderCreatedEvent.class));
            case "OrderCancelledEvent" ->
                    sagaHandler.refundPayment(objectMapper.treeToValue(node, OrderCancelledEvent.class));
            case "InvalidPaymentMadeEvent" ->
                    sagaHandler.refundPayment(objectMapper.treeToValue(node, InvalidPaymentMadeEvent.class));
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        }

    }


}
