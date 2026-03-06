package product.orders.paymentservice.integration;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;
import product.orders.paymentservice.config.KafkaTopicsProperties;
import product.orders.paymentservice.domain.model.Payment;
import product.orders.paymentservice.domain.model.PaymentStatus;
import product.orders.paymentservice.messaging.event.CancellationReason;
import product.orders.paymentservice.messaging.event.OrderCancelledEvent;
import product.orders.paymentservice.messaging.event.OrderCreatedEvent;
import product.orders.paymentservice.messaging.event.OrderItem;
import product.orders.paymentservice.repository.PaymentRepository;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PaymentEndToEndIT {

    // ---------- Containers ----------
    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("paymentdb")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
            );


    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setupConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        testConsumer = new KafkaConsumer<>(props);
        testConsumer.subscribe(List.of(topics.getPaymentEvents()));
    }

    @AfterEach
    void tearDown() {
        testConsumer.close();
    }

    // ---------- Beans ----------
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    private Consumer<String, String> testConsumer;


    @Autowired
    private KafkaTopicsProperties topics;

    // ---------- Tests ----------
    @Test
    void testCreatePayment_ConsumesOrderCreatedEvent_CreatesPaymentAndPublishesEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event =
                OrderCreatedEvent.of(
                        orderId,
                        5000L,
                        "USD",
                        UUID.randomUUID(),
                        "email@example.com",
                        "Our House, Middle of the Street",
                        List.of(new OrderItem(UUID.randomUUID(), 2))
                );

        Message<OrderCreatedEvent> message =
                MessageBuilder
                        .withPayload(event)
                        .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                        .setHeader(KafkaHeaders.KEY, orderId.toString())
                        .setHeader("eventType", "OrderCreatedEvent")
                        .build();
        // Act

        kafkaTemplate.send(message);

        // Assert
        // Verify published to db
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Payment payment =
                            paymentRepository.findByOrderId(orderId);

                    assertThat(payment).isNotNull();
                    assertThat(payment.getStatus())
                            .isEqualTo(PaymentStatus.PENDING);
                });

        // Verify published to kafka
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    boolean found = recordsContainEventOfType("PaymentCreatedEvent");
                    assertTrue(found);
                });
    }

    @Test
    void testRefundPayment_ConsumesOrderCancelledEvent_RefundsPaymentAndPublishesEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        OrderCancelledEvent event = OrderCancelledEvent.of(
                orderId,
                CancellationReason.USER_CANCELLED
        );
        Message<OrderCancelledEvent> message =
                MessageBuilder
                        .withPayload(event)
                        .setHeader(KafkaHeaders.TOPIC, topics.getOrderEvents())
                        .setHeader(KafkaHeaders.KEY, orderId.toString())
                        .setHeader("eventType", "OrderCancelledEvent")
                        .build();

        // Create payment
        Payment payment = Payment.createPendingPayment(orderId, 2000L, Currency.getInstance("USD"));
        payment.complete();
        paymentRepository.save(payment);

        // Act
        kafkaTemplate.send(message);

        // Assert
        // Verify refunded
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    Payment updaedPayment = paymentRepository.findByOrderId(orderId);
                    assertThat(updaedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
                });

        // Verify PaymentRefundedEvent published
        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    boolean found = recordsContainEventOfType("PaymentRefundedEvent");
                    assertTrue(found);
                });
    }

    // ---------- Helpers ----------

    private boolean recordsContainEventOfType(String eventType) {
        ConsumerRecords<String, String> records = testConsumer.poll(Duration.ofMillis(100));
        boolean found = false;

        for (ConsumerRecord<String, String> record : records) {
            // Convert from byte[] to String
            String headerEventType = new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
            if (headerEventType.equals(eventType)) {
                found = true;
                break;
            }

        }

        return found;
    }
}
