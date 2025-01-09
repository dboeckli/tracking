package dev.lydtech.tracking.handler;

import dev.lydtech.tracking.message.DispatchPreparing;
import dev.lydtech.tracking.message.TrackingStatusUpdated;
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
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static dev.lydtech.tracking.handler.DispatchTrackingHandler.DISPATCH_TRACKING_TOPIC;
import static dev.lydtech.tracking.service.TrackingService.TRACKING_STATUS_TOPIC;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(DispatchTrackingHandler.class)
@ActiveProfiles("docker")
@Slf4j
public class DispatchTrackingHandlerIT {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    private KafkaConsumer<String, DispatchPreparing> consumerDispatchedTracking;
    private KafkaConsumer<String, TrackingStatusUpdated> consumerTrackingStatus;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        consumerDispatchedTracking = new KafkaConsumer<>(createConsumerProps("consumerDispatchedTracking"));
        consumerTrackingStatus = new KafkaConsumer<>(createConsumerProps("consumerTrackingStatus"));

        log.info("### Subscribing to {} topic", DISPATCH_TRACKING_TOPIC);
        consumerDispatchedTracking.subscribe(Collections.singletonList(DISPATCH_TRACKING_TOPIC));
        log.info("### Subscribed to {}", consumerDispatchedTracking.subscription());

        log.info("### Subscribing to {} topic", TRACKING_STATUS_TOPIC);
        consumerTrackingStatus.subscribe(Collections.singletonList(TRACKING_STATUS_TOPIC));
        log.info("### Subscribed to {}", consumerTrackingStatus.subscription());
    }

    @AfterEach
    void tearDown() {
        if (consumerDispatchedTracking != null) {
            consumerDispatchedTracking.close();
        }
        if (consumerTrackingStatus != null) {
            consumerTrackingStatus.close();
        }
    }

    @Test
    public void testOrderCreatedHandler() {
        DispatchPreparing givenDispatchPreparing = DispatchPreparing.builder()
            .orderId(UUID.randomUUID())
            .build();

        assertDoesNotThrow(() -> {
            kafkaTemplate.send(DISPATCH_TRACKING_TOPIC, givenDispatchPreparing).get();
        });
        log.info("Sent order: {}", givenDispatchPreparing);

        UUID orderIdDispatchedTracking = geMessageIDFromConsumerDispatchedTracking(consumerDispatchedTracking);
        UUID orderIdTrackingStatus = geMessageIDFromConsumerTrackingStatus(consumerTrackingStatus);

        assertAll("Order IDs should match across all messages",
            () -> assertNotNull(orderIdDispatchedTracking),
            () -> assertNotNull(orderIdTrackingStatus),
            () -> assertEquals(givenDispatchPreparing.getOrderId(), orderIdDispatchedTracking),
            () -> assertEquals(givenDispatchPreparing.getOrderId(), orderIdTrackingStatus)
        );
    }

    private UUID geMessageIDFromConsumerDispatchedTracking(KafkaConsumer<String, DispatchPreparing> consumer) {
        // Wait for DispatchPreparing message
        ConsumerRecords<String, DispatchPreparing> records = consumer.poll(Duration.ofSeconds(10));

        log.info("### DispatchPreparing message count: {}", records.count());
        if (records.isEmpty() || records.count() == 0) {
            return null;
        }

        ConsumerRecord<String, DispatchPreparing> record = null;
        for (ConsumerRecord<String, DispatchPreparing> r : records) {
            record = r;
        }
        if (record == null) {
            return null;
        }
        return record.value().getOrderId();
    }

    private UUID geMessageIDFromConsumerTrackingStatus(KafkaConsumer<String, TrackingStatusUpdated> consumer) {
        // Wait for DispatchPreparing message
        ConsumerRecords<String, TrackingStatusUpdated> records = consumer.poll(Duration.ofSeconds(10));

        log.info("### TrackingStatusUpdated message count: {}", records.count());
        if (records.isEmpty() || records.count() == 0) {
            return null;
        }

        ConsumerRecord<String, TrackingStatusUpdated> record = null;
        for (ConsumerRecord<String, TrackingStatusUpdated> r : records) {
            record = r;
        }
        if (record == null) {
            return null;
        }
        return record.value().getOrderId();
    }

    private Properties createConsumerProps(String groupId) {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return consumerProps;
    }
    
}
