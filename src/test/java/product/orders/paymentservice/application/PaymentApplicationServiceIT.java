package product.orders.paymentservice.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Mock Kafka producer but test real JPA, DB, Flyway migration
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PaymentApplicationServiceIT {

    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("paymentdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    PaymentApplicationService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    // We mock only Kafka producer — everything else is real
    @MockitoBean
    PaymentEventProducer eventProducer;

    @Test
    void testCreatePayment_CreatePendingPayment_PersistsAndPublishesEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        long amountInCents = 500L;

        // Act
        paymentService.crete(orderId, amountInCents, Currency.getInstance("EUR"));
        Payment payment = paymentRepository.findByOrderId(orderId);

        // Assert
        assertNotNull(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getAmountInCents()).isEqualTo(amountInCents);
        verify(eventProducer).publish(any(PaymentCreatedEvent.class));
    }

    @Test
    void testCreatePayment_DuplicateOrder_ThrowsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        paymentService.crete(
                orderId,
                1000L,
                Currency.getInstance("USD")
        );

        // Act + Assert
        assertThatThrownBy(() ->
                paymentService.crete(
                        orderId,
                        1000L,
                        Currency.getInstance("USD")
                )
        ).isInstanceOf(RuntimeException.class);
    }

    @Test
    void testCompletePayment_PassedPendingPayment_PersistsAsCompletedAndPublishesEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.createPendingPayment(orderId, 1000L, Currency.getInstance("EUR"));
        paymentRepository.save(payment);

        // Act
        paymentService.complete(orderId);

        // Assert
        payment = paymentRepository.findByOrderId(orderId);
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        verify(eventProducer).publish(any(PaymentCompletedEvent.class));
    }

    @Test
    void testRefundPayment_PassedCompletedPayment_SavesAsRefundedAndPublishesEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        paymentService.crete(orderId, 1000L, Currency.getInstance("EUR"));
        paymentService.complete(orderId);

        // Act
        paymentService.refund(orderId);

        // Assert
        Payment payment = paymentRepository.findByOrderId(orderId);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        verify(eventProducer).publish(any(PaymentRefundedEvent.class));
    }

    @Test
    void testMarkFailedIfExists_OrderExists_PersistsAsFailedAndPublishesEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        paymentService.crete(orderId, 1000L, Currency.getInstance("EUR"));

        // Act
        paymentService.markFailedIfExists(orderId, "Payment failed");

        // Assert
        Payment payment = paymentRepository.findByOrderId(orderId);
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(eventProducer).publish(any(PaymentFailedEvent.class));
    }
}
