package product.orders.paymentservice.application;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import product.orders.paymentservice.application.dto.CheckoutRequestItem;
import product.orders.paymentservice.application.dto.CreateCheckoutRequest;
import product.orders.paymentservice.application.port.StripeClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class StripePaymentServiceImpl implements StripePaymentService {

    private final StripeClient stripeClient;

    @Value("${stripe.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public StripePaymentServiceImpl(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    @Override
    public String createCheckoutSession(CreateCheckoutRequest request)
            throws StripeException {

        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.successUrl())
                .setCancelUrl(request.cancelUrl())
                .setCurrency(request.currency())
                .setCustomerEmail(request.userEmail())
                // Link is only shown on checkout redirect. If not paid immediately it won't be paid
                .setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond())
                .putMetadata("orderId", request.orderId().toString());

        for (CheckoutRequestItem item : request.items()) {

            var productData =
                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName(item.name())
                            .build();

            var priceData =
                    SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(request.currency())
                            .setUnitAmount(item.priceInCents())
                            .setProductData(productData)
                            .build();

            var lineItem =
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(item.quantity())
                            .setPriceData(priceData)
                            .build();

            paramsBuilder.addLineItem(lineItem);
        }

        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey(request.orderId().toString())
                .build();

        Session session = stripeClient.createSession(
                paramsBuilder.build(),
                requestOptions
        );

        return session.getUrl();
    }
}