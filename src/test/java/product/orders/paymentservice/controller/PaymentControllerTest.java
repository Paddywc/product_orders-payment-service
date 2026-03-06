package product.orders.paymentservice.controller;

import com.stripe.exception.IdempotencyException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import product.orders.paymentservice.application.StripePaymentService;
import product.orders.paymentservice.application.StripeWebhookService;
import product.orders.paymentservice.application.dto.CheckoutRequestItem;
import product.orders.paymentservice.application.dto.CreateCheckoutRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PaymentController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet
                        .OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(PaymentControllerAdvice.class)
class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StripePaymentService stripePaymentService;

    @MockitoBean
    private StripeWebhookService stripeWebhookService;


    @Test
    void testCreateCheckoutSession_ValidRequest_CallsApplicationCreateCheckoutSession() throws Exception {
        // Arrange
        CreateCheckoutRequest request = makeValidCheckoutRequest();
        String returnUrl = "https://example.com/success";
        when(stripePaymentService.createCheckoutSession(request)).thenReturn(returnUrl);

        // Act
        mockMvc.perform(post("/api/payments/stripe/checkout")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert
                .andExpect(status().isOk())
                .andExpect(content().string(returnUrl));

        verify(stripePaymentService).createCheckoutSession(request);
    }

    @Test
    void testCreateCheckoutSession_StripeThrowsException_ReturnsBadGateway() throws Exception {
        // Arrange
        CreateCheckoutRequest request = makeValidCheckoutRequest();

        when(stripePaymentService.createCheckoutSession(request))
                .thenThrow(new IdempotencyException("Stripe error", null, null, 500));

        // Act
        mockMvc.perform(post("/api/payments/stripe/checkout")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert
                .andExpect(status().isBadGateway());

        verify(stripePaymentService).createCheckoutSession(request);
    }

    @Test
    void testCreateCheckoutSession_InvalidData_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateCheckoutRequest request = new CreateCheckoutRequest(null, null, null, null, null, null);

        // Act
        mockMvc.perform(post("/api/payments/stripe/checkout")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Assert
                .andExpect(status().isBadRequest());
    }

    @Test
    void testStripeWebhook_ValidRequest_CallsServiceAndReturnsOk() throws Exception {
        // Arrange
        String payload = "{\"event\":\"data\"}";
        String signature = "stripe-signature";

        // Act
        mockMvc.perform(post("/api/payments/stripe/webhook")
                        .contentType(APPLICATION_JSON)
                        .content(payload)
                        .header("Stripe-Signature", signature))
                // Assert
                .andExpect(status().isOk());
        verify(stripeWebhookService).handleWebhook(payload, signature);
    }

    @Test
    void testStripeWebhook_missingSignatureHeader_returnsBadRequest() throws Exception {
        // Arrange
        String payload = "{ \"id\": \"evt_123\" }";

        // Act
        mockMvc.perform(post("/api/payments/stripe/webhook")
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                // Assert
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stripeWebhookService);
    }


    private CreateCheckoutRequest makeValidCheckoutRequest() {
        return new CreateCheckoutRequest(
                UUID.randomUUID(),
                List.of(new CheckoutRequestItem(UUID.randomUUID(), 1000L, "productName", 2000L)),
                "test@example.com",
                "USD",
                "success",
                "confirm"
        );
    }


}
