package product.orders.paymentservice.repository;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import product.orders.paymentservice.domain.model.Payment;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    void testFindByOrderID_OrderExists_ReturnsPayment() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.createPendingPayment(
                orderId,
                5000L,
                Currency.getInstance("USD")
        );

        paymentRepository.saveAndFlush(payment);

        // Act
        Payment found = paymentRepository.findByOrderId(orderId);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getOrderId()).isEqualTo(orderId);
        assertThat(found.getAmountInCents()).isEqualTo(5000L);
        assertThat(found.getCurrency().getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void testExistsByOrderId_PaymentExists_ReturnsTrue() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.createPendingPayment(
                orderId,
                1000L,
                Currency.getInstance("EUR")
        );

        paymentRepository.saveAndFlush(payment);

        // Act
        boolean exists = paymentRepository.existsByOrderId(orderId);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByOrderId_PaymentDoesNotExist_ReturnsFalse() {
        // Act
        boolean exists = paymentRepository.existsByOrderId(UUID.randomUUID());

        // Assert
        assertThat(exists).isFalse();
    }


    @Test
    void testSave_DuplicateOrderId_ThrowsException() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        Payment first = Payment.createPendingPayment(
                orderId,
                1000L,
                Currency.getInstance("USD")
        );

        Payment second = Payment.createPendingPayment(
                orderId,
                2000L,
                Currency.getInstance("USD")
        );

        paymentRepository.saveAndFlush(first);

        // Act + Assert
        assertThatThrownBy(() -> paymentRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
