package product.orders.paymentservice.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/**
 * Snapshot of an item in a checkout request. Passed as part of a {@link CreateCheckoutRequest}
 * @param productId unique identifier of the product
 * @param quantity quantity of the product
 * @param name snapshot of the product name
 * @param priceInCents snapshot of the product price in cents
 */
public record CheckoutRequestItem(@NotNull UUID productId,
                                  @Positive Long quantity,
                                  @NotEmpty String name,
                                  @PositiveOrZero Long priceInCents) {
}
