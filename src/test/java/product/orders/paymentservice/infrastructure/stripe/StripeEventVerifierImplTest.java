package product.orders.paymentservice.infrastructure.stripe;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import product.orders.paymentservice.application.dto.VerifiedStripeWebhookEvent;
import product.orders.paymentservice.application.port.StripeWebHookParser;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeEventVerifierImplTest {
    @Mock
    private StripeWebHookParser parser;

    @InjectMocks
    private StripeEventVerifierImpl verifier;

    @Test
    void testVerify_ValidSessionEvent_ReturnsVerifiedEvent() {
        // Arrange
        UUID orderId = UUID.randomUUID();

        // Mock Stripe Event
        Event stripeEvent = mock(Event.class);
        String eventId = "evt_" + Math.random();
        String eventType = "checkout.session.completed";
        when(stripeEvent.getId()).thenReturn(eventId);
        when(stripeEvent.getType()).thenReturn(eventType);

        // Mock deserializer + object (needed for extracting orderId)
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        Session session = mock(Session.class);

        when(stripeEvent.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(session));
        when(session.getMetadata()).thenReturn(Map.of("orderId", orderId.toString()));

        when(parser.parse("payload", "sig")).thenReturn(stripeEvent);

        // Act
        VerifiedStripeWebhookEvent result =
                verifier.verify("payload", "sig");

        // Assert
        assertEquals(eventId, result.eventId());
        assertEquals(eventType, result.eventType());
        assertEquals(orderId, result.orderId());
    }


    @Test
    void testVerify_UnsupportedStripeObject_ThrowsException() {
        // Arrange
        Event stripeEvent = mock(Event.class);
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);

        when(stripeEvent.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(mock(StripeObject.class)));

        when(parser.parse(anyString(),anyString())).thenReturn(stripeEvent);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> verifier.verify("payload", "sig"));
    }

}
