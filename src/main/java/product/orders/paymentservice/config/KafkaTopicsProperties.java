package product.orders.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registry of Kafka topics used in the Payments Service. Creates a string bean kafkaTopicsProperties
 */
@Configuration
@ConfigurationProperties(prefix = "kafka.topic")
public class KafkaTopicsProperties {

    /**
     * The name of the topic that stores the order service events
     */
    private String orderEvents;

    /**
     * The name of the topic that stores inventory service events
     */
    private String paymentEvents;

    public String getOrderEvents() {
        return orderEvents;
    }

    public void setOrderEvents(String orderEvents) {
        this.orderEvents = orderEvents;
    }

    public String getPaymentEvents() {
        return paymentEvents;
    }

    public void setPaymentEvents(String paymentEvents) {
        this.paymentEvents = paymentEvents;
    }
}
