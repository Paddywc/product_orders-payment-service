package product.orders.paymentservice.application;

import product.orders.paymentservice.domain.model.Payment;
import product.orders.paymentservice.domain.model.PaymentStatus;

import java.util.Currency;
import java.util.UUID;

public interface PaymentApplicationService {
    /**
     * Create a pending payment for the parameter order id. Fire a payment created event
     *
     * @param orderId       the order to create the payment for
     * @param amountInCents the amount in cents (or single unit of the chosen currency)
     * @param currency  the currency used in the payment
     */
    void crete(UUID orderId, long amountInCents, Currency currency);

    /**
     * Mark the payment for the parameter order as completed. Fire a payment completed event
     *
     * @param orderId the id of the order whose payment should be marked as completed
     * @return the payment which has been completed
     */
    Payment complete(UUID orderId);

    /**
     * Refund the payment for the parameter order. Fire a payment refunded event
     *
     * @param orderId the id of the order whose payment should be refunded
     */
    void refund(UUID orderId);



    /**
     *  If a payment exists for the parameter order, mark it is failed. Fire a payment failed event
     * @param orderId the id of the order to mark the payment failed for
     * @param reason the reason for marking the payment failed
     */
    void markFailedIfExists(UUID orderId, String reason);

    PaymentStatus getPaymentStatus(UUID orderId);
}
