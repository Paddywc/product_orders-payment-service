package product.orders.paymentservice.controller;

import com.stripe.exception.StripeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import product.orders.paymentservice.domain.exception.DuplicateOrderPaymentException;
import product.orders.paymentservice.domain.exception.OrderPaymentNotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.ResponseEntity.status;

@RestControllerAdvice(assignableTypes = PaymentController.class)
public class PaymentControllerAdvice {

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ApiError> handleStripeException(StripeException ex) {
        ApiError error = new ApiError("Stripe error: " + ex.getMessage(), null);
        return status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        ApiError apiError = new ApiError("Validation failed", fieldErrors);
        return status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        ApiError error = new ApiError(ex.getMessage(), null);
        return status(HttpStatus.BAD_REQUEST).body(error);
    }


    @ExceptionHandler(DuplicateOrderPaymentException.class)
    public ResponseEntity<ApiError> handleDuplicatePayment(DuplicateOrderPaymentException ex) {
        ApiError error = new ApiError(ex.getMessage(), null);
        return status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(OrderPaymentNotFoundException.class)
    public ResponseEntity<ApiError> handlePaymentNotFound(OrderPaymentNotFoundException ex) {
        ApiError error = new ApiError(ex.getMessage(), null);
        return status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex) {
        ApiError error = new ApiError(ex.getMessage(), null);
        return status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        ApiError error = new ApiError("Unexpected error", null);
        return status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    public record ApiError(String message, Map<String, String> fieldErrors) {
    }
}
