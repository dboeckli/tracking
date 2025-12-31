package dev.lydtech.tracking.handler;

import dev.lydtech.message.DispatchCompleted;
import dev.lydtech.message.DispatchPreparing;
import dev.lydtech.message.TrackingStatusUpdated;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static dev.lydtech.tracking.config.TrackingConfiguration.TRUSTED_PACKAGES;
import static dev.lydtech.tracking.config.TrackingConfiguration.getTypeMappings;
import static dev.lydtech.tracking.handler.DispatchTrackingHandler.DISPATCH_TRACKING_TOPIC;
import static dev.lydtech.tracking.service.TrackingService.TRACKING_STATUS_TOPIC;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("docker")
@DirtiesContext
@Slf4j
public class DispatchTrackingHandlerIT {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    private KafkaConsumer<String, DispatchPreparing> consumerDispatchPreparing;
    private KafkaConsumer<String, DispatchCompleted> consumerDispatchCompleted;
    
    private KafkaConsumer<String, TrackingStatusUpdated> consumerTrackingStatusUpdated;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        consumerDispatchPreparing = new KafkaConsumer<>(createConsumerProps("consumerDispatchPreparing"));
        consumerDispatchCompleted = new KafkaConsumer<>(createConsumerProps("consumerDispatchCompleted"));

        consumerTrackingStatusUpdated = new KafkaConsumer<>(createConsumerProps("consumerTrackingStatusUpdated"));

        log.info("### Subscribing consumerDispatchPreparing to {} topic", DISPATCH_TRACKING_TOPIC);
        consumerDispatchPreparing.subscribe(Collections.singletonList(DISPATCH_TRACKING_TOPIC));
        log.info("### consumerDispatchPreparing Subscribed to {}", consumerDispatchPreparing.subscription());

        log.info("### Subscribing consumerDispatchCompleted to {} topic", DISPATCH_TRACKING_TOPIC);
        consumerDispatchCompleted.subscribe(Collections.singletonList(DISPATCH_TRACKING_TOPIC));
        log.info("### consumerDispatchCompleted Subscribed to {}", consumerDispatchCompleted.subscription());

        log.info("### Subscribing consumerTrackingStatusUpdated to {} topic", TRACKING_STATUS_TOPIC);
        consumerTrackingStatusUpdated.subscribe(Collections.singletonList(TRACKING_STATUS_TOPIC));
        log.info("### consumerTrackingStatusUpdated Subscribed to {}", consumerTrackingStatusUpdated.subscription());
    }

    @AfterEach
    void tearDown() {
        if (consumerDispatchPreparing != null) {
            consumerDispatchPreparing.close();
        }
        if (consumerDispatchCompleted != null) {
            consumerDispatchCompleted.close();
        }
        if (consumerTrackingStatusUpdated != null) {
            consumerTrackingStatusUpdated.close();
        }
    }

    @Test
    public void testOrderCreatedHandlerForDispatchPreparedMessage() {
        DispatchPreparing givenDispatchPreparing = DispatchPreparing.builder()
            .orderId(UUID.randomUUID())
            .build();

        assertDoesNotThrow(() -> {
            kafkaTemplate.send(DISPATCH_TRACKING_TOPIC, givenDispatchPreparing).get();
        });
        log.info("Sent order: {}", givenDispatchPreparing);

        UUID orderIdDispatchedTrackingForConsumerDispatchPreparing = geMessageIDFromConsumerDispatchedTrackingForConsumerDispatchPreparing(consumerDispatchPreparing);
        UUID orderIdTrackingStatus = geMessageIDFromConsumerTrackingStatus(consumerTrackingStatusUpdated);

        assertAll("Order IDs should match across all messages",
            () -> assertNotNull(orderIdDispatchedTrackingForConsumerDispatchPreparing),
            () -> assertNotNull(orderIdTrackingStatus),
            () -> assertEquals(givenDispatchPreparing.getOrderId(), orderIdDispatchedTrackingForConsumerDispatchPreparing),
            () -> assertEquals(givenDispatchPreparing.getOrderId(), orderIdTrackingStatus)
        );
    }

    @Test
    public void testOrderCreatedHandlerForDispatchCompletedMessage() {
        DispatchCompleted givenDispatchCompleted = DispatchCompleted.builder()
            .orderId(UUID.randomUUID())
            .build();

        assertDoesNotThrow(() -> {
            kafkaTemplate.send(DISPATCH_TRACKING_TOPIC, givenDispatchCompleted).get();
        });
        log.info("Sent order: {}", givenDispatchCompleted);

        UUID orderIdDispatchedTrackingForConsumerDispatchCompleted = geMessageIDFromConsumerDispatchedTrackingForConsumerDispatchCompleted(consumerDispatchCompleted);
        UUID orderIdTrackingStatus = geMessageIDFromConsumerTrackingStatus(consumerTrackingStatusUpdated);

        assertAll("Order IDs should match across all messages",
            () -> assertNotNull(orderIdDispatchedTrackingForConsumerDispatchCompleted),
            () -> assertNotNull(orderIdTrackingStatus),
            () -> assertEquals(givenDispatchCompleted.getOrderId(), orderIdDispatchedTrackingForConsumerDispatchCompleted),
            () -> assertEquals(givenDispatchCompleted.getOrderId(), orderIdTrackingStatus)
        );
    }

    private UUID geMessageIDFromConsumerDispatchedTrackingForConsumerDispatchPreparing(KafkaConsumer<String, DispatchPreparing> consumer) {
        // Wait for DispatchPreparing message
        ConsumerRecords<String, DispatchPreparing> records = consumer.poll(Duration.ofSeconds(10));

        log.info("### DispatchPreparing message count: {}", records.count());
        assertFalse(records.isEmpty() || records.count() == 0, "Expected DispatchPreparing records, but none were received");

        ConsumerRecord<String, DispatchPreparing> record = null;
        for (ConsumerRecord<String, DispatchPreparing> r : records) {
            if (r.value() instanceof DispatchPreparing) {
                record = r;
                break;
            }
        }
        assertNotNull(record, "DispatchPreparing record is null");
        DispatchPreparing orderDispatchPreparing = record.value();

        assertNotNull(orderDispatchPreparing, "DispatchPreparing message is null");
        return orderDispatchPreparing.getOrderId();
    }

    private UUID geMessageIDFromConsumerDispatchedTrackingForConsumerDispatchCompleted(KafkaConsumer<String, DispatchCompleted> consumer) {
        // Wait for DispatchPreparing message
        ConsumerRecords<String, DispatchCompleted> records = consumer.poll(Duration.ofSeconds(10));

        log.info("### DispatchCompleted message count: {}", records.count());
        assertFalse(records.isEmpty() || records.count() == 0, "Expected DispatchCompleted records, but none were received");

        ConsumerRecord<String, DispatchCompleted> record = null;
        for (ConsumerRecord<String, DispatchCompleted> r : records) {
            if (r.value() instanceof DispatchCompleted) {
                record = r;
                break;
            }
        }
        assertNotNull(record, "DispatchCompleted record is null");
        DispatchCompleted orderDispatchCompleted = record.value();

        assertNotNull(orderDispatchCompleted, "DispatchCompleted message is null");
        return orderDispatchCompleted.getOrderId();
    }

    private UUID geMessageIDFromConsumerTrackingStatus(KafkaConsumer<String, TrackingStatusUpdated> consumer) {
        // Wait for DispatchPreparing message
        ConsumerRecords<String, TrackingStatusUpdated> records = consumer.poll(Duration.ofSeconds(10));

        log.info("### TrackingStatusUpdated message count: {}", records.count());
        assertFalse(records.isEmpty() || records.count() == 0, "Expected TrackingStatusUpdated records, but none were received");
        
        ConsumerRecord<String, TrackingStatusUpdated> record = null;
        for (ConsumerRecord<String, TrackingStatusUpdated> r : records) {
            record = r;
        }
        assertNotNull(record, "TrackingStatusUpdated record is null");
        TrackingStatusUpdated trackingStatusUpdated = record.value();

        assertNotNull(trackingStatusUpdated, "TrackingStatusUpdated message is null");
        return trackingStatusUpdated.getOrderId();
    }

    private Properties createConsumerProps(String groupId) {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        consumerProps.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, TRUSTED_PACKAGES);
        consumerProps.put(JacksonJsonDeserializer.TYPE_MAPPINGS, getTypeMappings());
        return consumerProps;
    }
    
}
