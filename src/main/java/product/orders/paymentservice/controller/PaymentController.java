package product.orders.paymentservice.controller;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import product.orders.paymentservice.application.dto.CreateCheckoutRequest;
import product.orders.paymentservice.application.StripePaymentService;
import product.orders.paymentservice.application.StripeWebhookService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final StripePaymentService stripePaymentService;

    private final StripeWebhookService stripeWebhookService;


    public PaymentController(StripePaymentService stripePaymentService, StripeWebhookService stripeWebhookService) {
        this.stripePaymentService = stripePaymentService;
        this.stripeWebhookService = stripeWebhookService;
    }


    @PostMapping("/stripe/checkout")
    public String createCheckoutSession(@RequestBody @Valid CreateCheckoutRequest request) throws StripeException {
        return stripePaymentService.createCheckoutSession(request);
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> stripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        stripeWebhookService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
