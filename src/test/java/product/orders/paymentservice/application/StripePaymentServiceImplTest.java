package product.orders.paymentservice.application;

import com.stripe.exception.PermissionException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import product.orders.paymentservice.application.dto.CheckoutRequestItem;
import product.orders.paymentservice.application.dto.CreateCheckoutRequest;
import product.orders.paymentservice.application.port.StripeClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StripePaymentServiceImplTest {

    private StripeClient stripeClient;

    private StripePaymentServiceImpl stripePaymentService;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        stripeClient = mock(StripeClient.class);
        stripePaymentService = new StripePaymentServiceImpl(stripeClient);
        orderId = UUID.randomUUID();
    }

    @Test
    void testCreateCheckoutSession_ValidRequest_ReturnsSessionUrl() throws StripeException {
        // Arrange
        CreateCheckoutRequest request = makeValidCheckoutRequest();

        // Mock the returned value for the session
        Session mockSession = mock(Session.class);
        String sessionUrl = "https://stripe.com/checkout/session/123";
        when(mockSession.getUrl()).thenReturn(sessionUrl);
        when(stripeClient.createSession(
                any(SessionCreateParams.class),
                any(RequestOptions.class)))
                .thenReturn(mockSession);

        // Act
        String result = stripePaymentService.createCheckoutSession(request);

        // Assert
        assertThat(result).isEqualTo(sessionUrl);
        verify(stripeClient).createSession(any(SessionCreateParams.class), any(RequestOptions.class));
    }

    @Test
    void testCreateSession_ValidRequest_UsesRequestDataAsParamsAndOrderIdAsIdempotencyKey() throws Exception {
        // Arrange
        String email = "test@example.com";
        String currency = "USD";
        String successUrl = "https://example.com/success";
        String cancelUrl = "https://example.com/cancel";
        UUID lineItemId = UUID.randomUUID();
        String lineItemName = "Test Product";
        long lineItemPrice = 1000L;
        Long lineItemQuantity = 1L;
        CreateCheckoutRequest request = new CreateCheckoutRequest(
                orderId,
               List.of(new CheckoutRequestItem(lineItemId, lineItemQuantity, lineItemName, lineItemPrice)),
                email,
                currency,
                successUrl,
                cancelUrl);

        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn("url");
        when(stripeClient.createSession(any(), any())).thenReturn(mockSession);

        ArgumentCaptor<SessionCreateParams> paramsCaptor =
                ArgumentCaptor.forClass(SessionCreateParams.class);

        ArgumentCaptor<RequestOptions> optionsCaptor =
                ArgumentCaptor.forClass(RequestOptions.class);

        // Act
        stripePaymentService.createCheckoutSession(request);

        // Assert
        verify(stripeClient).createSession(paramsCaptor.capture(), optionsCaptor.capture());

        SessionCreateParams params = paramsCaptor.getValue();
        RequestOptions options = optionsCaptor.getValue();

        // Basic Fields
        assertThat(params.getCustomerEmail()).isEqualTo(email);
        assertThat(params.getMode()).isEqualTo(SessionCreateParams.Mode.PAYMENT);
        assertThat(params.getSuccessUrl()).isEqualTo(successUrl);
        assertThat(params.getCancelUrl()).isEqualTo(cancelUrl);
        assertThat(params.getCurrency()).isEqualTo(currency);

        // Metadata
        assertThat(params.getMetadata()).hasFieldOrPropertyWithValue("orderId", orderId.toString());

        // Line Items
        List<SessionCreateParams.LineItem> lineItems = params.getLineItems();
        assertThat(lineItems).hasSize(1);
        assertThat(lineItems.get(0).getPriceData().getCurrency()).isEqualTo(currency);
        assertThat(lineItems.get(0).getPriceData().getUnitAmount()).isEqualTo(lineItemPrice);
        assertThat(lineItems.get(0).getQuantity()).isEqualTo(lineItemQuantity);


        // Idempotency Key
        assertThat(options.getIdempotencyKey()).isEqualTo(orderId.toString());
    }

    @Test
    void testCreateSession_PassedMultipleLineItems_AddsAllLineItemsToParams() throws StripeException {
        // Arrange
        CreateCheckoutRequest request = makeValidCheckoutRequest(3);
        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn("url");
        when(stripeClient.createSession(any(), any())).thenReturn(mockSession);

        ArgumentCaptor<SessionCreateParams> paramsCaptor =
                ArgumentCaptor.forClass(SessionCreateParams.class);

        ArgumentCaptor<RequestOptions> optionsCaptor =
                ArgumentCaptor.forClass(RequestOptions.class);

        // Act
        stripePaymentService.createCheckoutSession(request);

        // Assert
        verify(stripeClient).createSession(paramsCaptor.capture(), optionsCaptor.capture());
        SessionCreateParams params = paramsCaptor.getValue();
        assertThat(params.getLineItems()).hasSize(3);
    }

    @Test
    void testCreateCheckoutSession_StripeThrowsException_Propagates() throws StripeException {
        // Arrange
        CreateCheckoutRequest request = makeValidCheckoutRequest();
        when(stripeClient.createSession(any(), any()))
                // Stripe exception itself is abstract so throw a child exception
                .thenThrow(new PermissionException("Stripe failed", null, null, 0));

        // Act & Assert
        assertThatThrownBy(() -> stripePaymentService.createCheckoutSession(request))
                .isInstanceOf(StripeException.class);
    }


    // ----------------------------------------------------
    // Helper
    // ----------------------------------------------------
    private CreateCheckoutRequest makeValidCheckoutRequest(int numberOfItems) {
        ArrayList<CheckoutRequestItem> items = new ArrayList<>();
        for (int i = 0; i < numberOfItems; i++) {
            CheckoutRequestItem item = new CheckoutRequestItem(
                    UUID.randomUUID(),
                    2000L,
                    "product_" + i,
                    5000L
            );
            items.add(item);
        }

        return new CreateCheckoutRequest(orderId,
                items,
                "user@example.com",
                "USD",
                "successUrl",
                "cancelUrl");
    }

    private CreateCheckoutRequest makeValidCheckoutRequest() {
        return makeValidCheckoutRequest(1);
    }

}
