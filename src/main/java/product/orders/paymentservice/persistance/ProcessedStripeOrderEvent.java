package product.orders.paymentservice.persistance;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * An unique stripe event that has been processed
 */
@Entity
@Table(name = "processed_stripe_event")
public class ProcessedStripeOrderEvent {

    /**
     * Use unique id rather than the eventId because otherwise it will try to merge when an order id is set, but
     * we want to throw an exception if the event has already been processed
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The id of the event that has been processed (comes from stripe)
     */
    @Column(nullable = false, updatable = false, unique = true, name = "event_id")
    private String eventId;

    @Column(nullable = false, updatable = false, name = "processed_at")
    private Instant processedAt;

    protected ProcessedStripeOrderEvent() {
        // JPA only
    }

    public ProcessedStripeOrderEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}