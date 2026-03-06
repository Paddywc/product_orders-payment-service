CREATE TABLE `payment` (
                           `currency_code` varchar(3) NOT NULL,
                           `amount_cents` bigint(20) NOT NULL,
                           `created_at` datetime(6) NOT NULL,
                           `updated_at` datetime(6) DEFAULT NULL,
                           `version` bigint(20) DEFAULT NULL,
                           `order_id` binary(16) NOT NULL,
                           `payment_id` binary(16) NOT NULL,
                           `payment_status` enum('COMPLETED','FAILED','PENDING','REFUNDED') NOT NULL,
                           PRIMARY KEY (`payment_id`),
                           UNIQUE KEY `uk_payment_order` (`order_id`)
);

CREATE TABLE `processed_order_service_event` (
                                                 `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                                 `processed_at` datetime(6) NOT NULL,
                                                 `event_id` binary(16) NOT NULL,
                                                 PRIMARY KEY (`id`),
                                                 UNIQUE KEY `uk-processed_order_service_event-eid` (`event_id`)
);

CREATE TABLE `processed_stripe_event` (
                                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                          `processed_at` datetime(6) NOT NULL,
                                          `event_id` varchar(255) NOT NULL,
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk-processed_stripe_event-eid` (`event_id`)
);


