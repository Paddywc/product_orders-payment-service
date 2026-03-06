package product.orders.paymentservice.application;

import com.stripe.exception.StripeException;
import product.orders.paymentservice.application.dto.CreateCheckoutRequest;

public interface StripePaymentService {
    /**
     * Create a stripe checkout session for the given items and return the URL of the created session
     * @param request containing the details of the checkout session to create
     * @return the URL of the created checkout session
     * @throws StripeException if there is an error creating the checkout session
     */
    String createCheckoutSession(CreateCheckoutRequest request) throws StripeException;
}
