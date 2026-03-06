package product.orders.paymentservice.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import product.orders.paymentservice.application.dto.VerifiedStripeWebhookEvent;
import product.orders.paymentservice.application.port.StripeEventVerifier;
import product.orders.paymentservice.domain.model.Payment;
import product.orders.paymentservice.domain.model.PaymentStatus;
import product.orders.paymentservice.messaging.event.PaymentCompletedEvent;
import product.orders.paymentservice.messaging.producer.PaymentEventProducer;
import product.orders.paymentservice.repository.PaymentRepository;
import product.orders.paymentservice.repository.ProcessedStripeOrderEventRepository;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StripeWebhookIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private StripeEventVerifier stripeEventVerifier;

    @MockitoBean
    private PaymentEventProducer paymentEventProducer;

    @Autowired
    private ProcessedStripeOrderEventRepository processedRepository;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        Payment payment = Payment.createPendingPayment(orderId, 5000L, Currency.getInstance("EUR"));
        paymentRepository.saveAndFlush(payment);

        processedRepository.deleteAll();
    }

    @Test
    void testWebhook_VerifiesEvent_CompletesPaymentAndPersistsProcessedEvent() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        VerifiedStripeWebhookEvent verifiedEvent = new VerifiedStripeWebhookEvent(
                eventId,
                "checkout.session.completed",
                orderId
        );
        when(stripeEventVerifier.verify(anyString(), anyString())).thenReturn(verifiedEvent);

        // Act

        mockMvc.perform(post("/api/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "sig")
                        .content("{}"))
                // Assert
                .andExpect(status().isOk());

        Payment updated = paymentRepository.findByOrderId(orderId);

        assertEquals(PaymentStatus.COMPLETED, updated.getStatus());
        assertTrue(processedRepository.existsByEventId(eventId));
    }

    @Test
    void testWebhook_DuplicateWebhook_NotProcessedTwice() throws Exception {
        // Arrange
        String eventId = UUID.randomUUID().toString();

        when(stripeEventVerifier.verify(anyString(), anyString()))
                .thenReturn(new VerifiedStripeWebhookEvent(
                        eventId,
                        "checkout.session.completed",
                        orderId
                ));

        // Act
        // first call
        mockMvc.perform(post("/api/payments/stripe/webhook")
                        .header("Stripe-Signature", "sig")
                        .content("{}"))
                .andExpect(status().isOk());

        // second call (duplicate)
        mockMvc.perform(post("/api/payments/stripe/webhook")
                        .header("Stripe-Signature", "sig")
                        .content("{}"))
                .andExpect(status().isOk());

        // Assert
        Payment updated = paymentRepository.findByOrderId(orderId);
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // still only one processed event stored
        assertThat(processedRepository.count()).isEqualTo(1);

        verify(paymentEventProducer, times(1)).publish(any(PaymentCompletedEvent.class));
    }

    void testWebhook_PaymentExpiredAndPaymentPending_MarksFailed() throws Exception {
        // Arrange
        VerifiedStripeWebhookEvent verifiedEvent =
                new VerifiedStripeWebhookEvent(
                        "evt_456",
                        "checkout.session.expired",
                        orderId
                );
        when(stripeEventVerifier.verify(anyString(), anyString())).thenReturn(verifiedEvent);

        // Act
        mockMvc.perform(post("/api/payments/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"fake\": \"payload\" }")
                        .header("Stripe-Signature", "fake-signature"))
                // Assert
                .andExpect(status().isOk());

        Payment updated = paymentRepository.findByOrderId(orderId);

        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }
}
