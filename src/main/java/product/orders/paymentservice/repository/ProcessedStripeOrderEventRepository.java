package product.orders.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import product.orders.paymentservice.persistance.ProcessedStripeOrderEvent;

/**
 * Repository for processed stripe events
 */
public interface ProcessedStripeOrderEventRepository extends JpaRepository<ProcessedStripeOrderEvent, Long> {

    boolean existsByEventId(String eventId);
}
