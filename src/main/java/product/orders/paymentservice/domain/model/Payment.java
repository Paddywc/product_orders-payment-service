package product.orders.paymentservice.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(
        name = "payment",
        // Only one payment per order
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_order",
                        columnNames = "order_id"
                )
        }
)
public class Payment {

    @Id
    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    /**
     * Amount in cents (or the smallest unit of the currency)
     */
    @Column(name = "amount_cents", nullable = false)
    private long amountInCents;

    @Column(nullable = false, length = 3, name = "currency_code")
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "payment_status")
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    protected Payment() {
    }

    /**
     * Create a payment with the pending status for the given order.
     *
     * @param orderId       the id of the oder
     * @param amountInCents the amount in cents (or smallest unit of the currency)
     * @param currency      the currency code for the currency the payment is in
     * @return the created payment
     */
    public static Payment createPendingPayment(UUID orderId, long amountInCents, Currency currency) {
        Payment payment = new Payment();
        payment.paymentId = UUID.randomUUID();
        payment.orderId = orderId;
        payment.amountInCents = amountInCents;
        payment.currency = currency;
        payment.status = PaymentStatus.PENDING;
        payment.createdAt = Instant.now();
        payment.updatedAt = Instant.now();
        return payment;
    }

    // Getters

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public long getAmountInCents() {
        return amountInCents;
    }

    public Currency getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }


    // Domain

    /**
     * Set the status to complete. Throw an error if payment is not pending.
     */
    public void complete() {
        if (status == PaymentStatus.COMPLETED) {
            return; // idempotent
        }
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot complete payment in state " + status
            );
        }
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }


    /**
     * Mark the payment as failed. Will throw an error if the payment is already completed.
     */
    public void fail() {
        if (status == PaymentStatus.FAILED) {
            return; // idempotent
        }
        if (status == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot fail a completed payment");
        }
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    /**
     * Refund the payment. Will throw an error if the payment is not completed.
     */
    public void refund() {
        if (status == PaymentStatus.REFUNDED) {
            return; // idempotent
        }
        if (status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Only completed payments can be refunded"
            );
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }
}
