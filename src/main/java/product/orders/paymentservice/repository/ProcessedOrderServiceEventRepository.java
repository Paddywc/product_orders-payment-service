package product.orders.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import product.orders.paymentservice.persistance.ProcessedOrderServiceEvent;

/**
 * Repository for processed events from the order service
 */
public interface ProcessedOrderServiceEventRepository extends JpaRepository<ProcessedOrderServiceEvent, Long> {
}
