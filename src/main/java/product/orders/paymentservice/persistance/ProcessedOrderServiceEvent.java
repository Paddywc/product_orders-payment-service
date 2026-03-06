package product.orders.paymentservice.persistance;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An event from the order service that has been processed
 */
@Entity
@Table(name = "processed_order_service_event")
public class ProcessedOrderServiceEvent {

    /**
     * Use unique id rather than the eventId because otherwise it will try to merge when an order id is set, but
     * we want to throw an exception if the event has already been processed
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier of the event (comes from the order service)
     */
    @Column(nullable = false, updatable = false, unique = true, name = "event_id")
    private UUID eventId;

    @Column(nullable = false, updatable = false, name = "processed_at")
    private Instant processedAt;

    protected ProcessedOrderServiceEvent() {
        // JPA only
    }

    public ProcessedOrderServiceEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}