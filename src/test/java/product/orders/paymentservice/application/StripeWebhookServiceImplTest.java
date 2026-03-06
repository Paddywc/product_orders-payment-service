package product.orders.paymentservice.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import product.orders.paymentservice.application.dto.VerifiedStripeWebhookEvent;
import product.orders.paymentservice.application.port.StripeEventVerifier;
import product.orders.paymentservice.domain.model.PaymentStatus;
import product.orders.paymentservice.persistance.ProcessedStripeOrderEvent;
import product.orders.paymentservice.repository.ProcessedStripeOrderEventRepository;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceImplTest {


    @Mock
    private StripeEventVerifier stripeEventVerifier;

    @Mock
    private ProcessedStripeOrderEventRepository processedRepo;

    @Mock
    private PaymentApplicationService paymentApplicationService;

    private StripeWebhookServiceImpl stripeWebhookService;

    private UUID orderId;

    private String eventId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        eventId = "evt_" + Math.random();
        stripeWebhookService = new StripeWebhookServiceImpl(stripeEventVerifier, processedRepo, paymentApplicationService);
    }

    @Test
    void testHandleWebhook_CheckoutCompletedEvent_CallsCompleteAndSavesEvent() {
        // Arrange
        VerifiedStripeWebhookEvent event = new VerifiedStripeWebhookEvent(eventId, "checkout.session.completed", orderId);

        when(stripeEventVerifier.verify(any(String.class), any(String.class))).thenReturn(event);
        when(processedRepo.existsByEventId(eventId)).thenReturn(false);

        // Act
        stripeWebhookService.handleWebhook("payload", "sig");

        verify(paymentApplicationService).complete(orderId);
        verify(processedRepo).save(any(ProcessedStripeOrderEvent.class));
    }


    @Test
    void testHandleWebhook_WhenEventAlreadyProcessed_DoesNotCallPaymentApplicationService() {
        // Arrange
        VerifiedStripeWebhookEvent event = new VerifiedStripeWebhookEvent(eventId, "checkout.session.complete", orderId);

        when(stripeEventVerifier.verify(any(String.class), any(String.class))).thenReturn(event);
        when(processedRepo.existsByEventId(eventId)).thenReturn(true);

        // Act
        stripeWebhookService.handleWebhook("payload", "sig");

        // Assert
        verify(paymentApplicationService, never()).complete(orderId);
        verify(processedRepo, never()).save(any(ProcessedStripeOrderEvent.class));
    }

    @Test
    void testHandleWebhook_CheckoutExpiredEventWithPendingPayment_MarksFailed(){
        // Arrange
        VerifiedStripeWebhookEvent event = new VerifiedStripeWebhookEvent(eventId, "checkout.session.expired", orderId);
        when(stripeEventVerifier.verify(any(String.class), any(String.class))).thenReturn(event);
        when(processedRepo.existsByEventId(eventId)).thenReturn(false);
        when(paymentApplicationService.getPaymentStatus(orderId)).thenReturn(PaymentStatus.PENDING);

        // Act
        stripeWebhookService.handleWebhook("payload", "sig");

        // Assert
        verify(paymentApplicationService).markFailedIfExists(orderId, "Payment failed");
        verify(processedRepo).save(any(ProcessedStripeOrderEvent.class));
    }

    @Test
    void testHandleWebhook_CheckoutExpiredAndPaymentNoPending_DoesNotMarkFailed(){
        // Arrange
        VerifiedStripeWebhookEvent event = new VerifiedStripeWebhookEvent(eventId, "checkout.session.expired", orderId);
        when(stripeEventVerifier.verify(any(String.class), any(String.class))).thenReturn(event);
        when(processedRepo.existsByEventId(eventId)).thenReturn(false);
        when(paymentApplicationService.getPaymentStatus(orderId)).thenReturn(PaymentStatus.COMPLETED);

        // Act
        stripeWebhookService.handleWebhook("payload", "sig");

        // Assert
        verify(paymentApplicationService, never()).markFailedIfExists(orderId, "Payment failed");
        verify(processedRepo).save(any(ProcessedStripeOrderEvent.class));
    }

    @Test
    void testHandleWebhook_UnknownEventType_IgnoresButSavesEvent() {
        VerifiedStripeWebhookEvent event =
                new VerifiedStripeWebhookEvent(eventId, "some.unknown.event", orderId);

        when(stripeEventVerifier.verify(any(), any())).thenReturn(event);
        when(processedRepo.existsByEventId(eventId)).thenReturn(false);

        stripeWebhookService.handleWebhook("payload", "sig");

        verifyNoInteractions(paymentApplicationService);
        verify(processedRepo).save(any(ProcessedStripeOrderEvent.class));
    }

}
