package product.orders.paymentservice.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateCheckoutRequest(@NotNull UUID orderId,
                                    @NotNull @Valid List<CheckoutRequestItem> items,
                                    @NotEmpty String userEmail,
                                    @NotEmpty String currency,
                                    @NotEmpty String successUrl,
                                    @NotEmpty String cancelUrl) {
}
