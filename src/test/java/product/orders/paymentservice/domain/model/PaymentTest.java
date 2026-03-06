package product.orders.paymentservice.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class PaymentTest {

    @Test
    void testCreatePendingPayment_ValidInput_SetsPendingStatusAndFields() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        long amount = 5000L;
        Currency currency = Currency.getInstance("USD");

        // Act
        Payment payment = Payment.createPendingPayment(orderId, amount, currency);


        // Assert
        assertThat(payment.getPaymentId()).isNotNull();
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getAmountInCents()).isEqualTo(amount);
        assertThat(payment.getCurrency()).isEqualTo(currency);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCreatedAt()).isNotNull();
    }

    @Test
    void testComplete_WhenPending_ChangesStatusToCompleted() {
        // Arrange
        Payment payment = newPendingPayment();
        // Act
        payment.complete();
        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void testComplete_WhenAlreadyCompleted_IdempotentNoChange() {
        // Arrange
        Payment payment = newPendingPayment();
        // Act
        payment.complete();
        payment.complete(); // second call
        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void testComplete_WhenFailed_ThrowsException() {
        // Arrange
        Payment payment = newPendingPayment();
        payment.fail();

        // Act & Assert
        assertThatThrownBy(payment::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot complete payment");
    }

    @Test
    void testFail_WhenPending_ChangesStatusToFailed() {
        // Arrange
        Payment payment = newPendingPayment();
        // Act
        payment.fail();
        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void testFail_WhenAlreadyFailed_IdempotentNoChange() {
        // Arrange
        Payment payment = newPendingPayment();
        // Act
        payment.fail();
        payment.fail();
        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void testFail_WhenCompleted_ThrowsException() {
        // Arrange
        Payment payment = newPendingPayment();
        payment.complete();

        // Act & Assert
        assertThatThrownBy(() -> payment.fail())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot fail a completed payment");
    }

    @Test
    void testRefund_WhenCompleted_ChangesStatusToRefunded() {
        // Arrange
        Payment payment = newPendingPayment();
        payment.complete();

        // Act
        payment.refund();

        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void testRefund_WhenAlreadyRefunded_IdempotentNoChange() {
        // Arrange
        Payment payment = newPendingPayment();
        payment.complete();
        // Act
        payment.refund();
        payment.refund();
        // Assert
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void testRefund_WhenNotCompleted_ThrowsException() {
        // Arrange
        Payment payment = newPendingPayment();
        // Act & Asserts
        assertThatThrownBy(payment::refund)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only completed payments can be refunded");
    }


    // ----------------------------------------------------
    // Helper
    // ----------------------------------------------------

    private Payment newPendingPayment() {
        return Payment.createPendingPayment(
                UUID.randomUUID(),
                1000L,
                Currency.getInstance("USD")
        );
    }

}
