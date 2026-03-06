package product.orders.paymentservice.messaging.consumer;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import product.orders.paymentservice.domain.model.Payment;
import product.orders.paymentservice.domain.model.PaymentStatus;
import product.orders.paymentservice.messaging.event.CancellationReason;
import product.orders.paymentservice.messaging.event.OrderCancelledEvent;
import product.orders.paymentservice.messaging.event.OrderCreatedEvent;
import product.orders.paymentservice.messaging.event.OrderItem;
import product.orders.paymentservice.repository.PaymentRepository;

import java.time.Duration;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
class PaymentOrderConsumerIT {

    // ---------------------------------------------------------
    // Containers
    // ---------------------------------------------------------

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("payment_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers);

        registry.add("spring.datasource.url",
                mysql::getJdbcUrl);

        registry.add("spring.datasource.username",
                mysql::getUsername);

        registry.add("spring.datasource.password",
                mysql::getPassword);
    }

    // ---------------------------------------------------------
    // Beans
    // ---------------------------------------------------------

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${kafka.topic.order-events}")
    private String topic;

    @BeforeEach
    void setup() {
        paymentRepository.deleteAll();
    }

    // ---------------------------------------------------------
    // Tests
    // ---------------------------------------------------------
    @Test
    void testOrderCreatedEvent_NewEvent_CreatesPayment() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        OrderCreatedEvent event = OrderCreatedEvent.of(
                orderId,
                2000L,
                "EUR",
                UUID.randomUUID(),
                "email@example",
                "123 Road",
                List.of(new OrderItem(UUID.randomUUID(), 10))
        );

        Message<OrderCreatedEvent> message =
                MessageBuilder
                        .withPayload(event)
                        .setHeader(KafkaHeaders.TOPIC, topic)
                        .setHeader(KafkaHeaders.KEY, orderId.toString())
                        .setHeader("eventType", "OrderCreatedEvent")
                        .build();


        // Act
        kafkaTemplate.send(message);

        // Assert
        // 🔥 Await until DB reflects change
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Payment payment =
                            paymentRepository.findByOrderId(orderId);

                    assertNotNull(payment);
                    assertEquals(PaymentStatus.PENDING, payment.getStatus());
                });
    }


    @Test
    void testOrderCreatedEvent_DuplicateEvent_DoesNotSave() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        OrderCreatedEvent event = OrderCreatedEvent.of(
                orderId,
                2000L,
                "EUR",
                UUID.randomUUID(),
                "email@example",
                "123 Road",
                List.of(new OrderItem(UUID.randomUUID(), 10))
        );

        Message<OrderCreatedEvent> message =
                MessageBuilder
                        .withPayload(event)
                        .setHeader(KafkaHeaders.TOPIC, topic)
                        .setHeader(KafkaHeaders.KEY, orderId.toString())
                        .setHeader("eventType", "OrderCreatedEvent")
                        .build();

        // Act
        kafkaTemplate.send(message);
        kafkaTemplate.send(message);

        // Assert
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertEquals(1, paymentRepository.count()));
    }

    @Test
    void testOrderCancelledEvent_NewEvent_RefundsPayment() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        OrderCancelledEvent event = OrderCancelledEvent.of(
                orderId,
                CancellationReason.SYSTEM_ERROR
        );
        Message<OrderCancelledEvent> message =
                MessageBuilder
                        .withPayload(event)
                        .setHeader(KafkaHeaders.TOPIC, topic)
                        .setHeader(KafkaHeaders.KEY, orderId.toString())
                        .setHeader("eventType", "OrderCancelledEvent")
                        .build();

        Payment payment = Payment.createPendingPayment(orderId, 1000L, Currency.getInstance("USD"));
        payment.complete();
        paymentRepository.save(payment);

        // Act
        kafkaTemplate.send(message);

        // Assert
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Payment updaedPayment = paymentRepository.findByOrderId(orderId);
                    assertEquals(PaymentStatus.REFUNDED, updaedPayment.getStatus());
                });

    }

}
