package product.orders.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import product.orders.paymentservice.domain.model.Payment;

import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Payment findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);
}
