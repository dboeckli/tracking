package dev.lydtech.tracking.handler;

import dev.lydtech.tracking.message.DispatchPreparing;
import dev.lydtech.tracking.message.TrackingStatusUpdated;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.lydtech.tracking.handler.DispatchTrackingHandler.DISPATCH_TRACKING_TOPIC;
import static dev.lydtech.tracking.service.TrackingService.TRACKING_STATUS_TOPIC;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest
@ActiveProfiles("test-embedded-kafka")
@DirtiesContext
@Slf4j
@EmbeddedKafka(controlledShutdown = true)
// TODO: ADD TEST FOR MODIFIED HANDLER
public class DispatchTrackingHandlerWithEmbeddedKafkaIT {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private KafkaTestListener testListener;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public KafkaTestListener testListener() {
            return new KafkaTestListener();
        }

    }

    protected static class KafkaTestListener {
        AtomicInteger dispatchPreparingCounter = new AtomicInteger(0);
        AtomicInteger trackingStatusUpdatedCounter = new AtomicInteger(0);

        @KafkaListener(groupId = "KafkaIntegrationTest", topics = DISPATCH_TRACKING_TOPIC)
        void receiveDispatchPreparing(@Payload DispatchPreparing payload) {
            log.info("Received DispatchPreparing: " + payload);
            dispatchPreparingCounter.incrementAndGet();
        }

        @KafkaListener(groupId = "KafkaIntegrationTest", topics = TRACKING_STATUS_TOPIC)
        void receiveTrackingStatusUpdated(@Payload TrackingStatusUpdated payload) {
            log.info("Received TrackingStatusUpdated: " + payload);
            trackingStatusUpdatedCounter.incrementAndGet();
        }
    }

    @BeforeEach
    public void setUp() {
        testListener.dispatchPreparingCounter.set(0);
        testListener.trackingStatusUpdatedCounter.set(0);

        // Wait until the partitions are assigned.
        registry.getListenerContainers().forEach(container ->
            ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
    }
    
    @Test
    public void testOrderCreatedHandler() throws Exception {
        DispatchPreparing givenDispatchPreparing = DispatchPreparing.builder()
            .orderId(UUID.randomUUID())
            .build();
        
        sendMessage(DISPATCH_TRACKING_TOPIC, givenDispatchPreparing);

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
            .until(testListener.dispatchPreparingCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
            .until(testListener.trackingStatusUpdatedCounter::get, equalTo(1));
    }

    private void sendMessage(String topic, Object data) throws Exception {
        kafkaTemplate.send(MessageBuilder
            .withPayload(data)
            .setHeader(KafkaHeaders.TOPIC, topic)
            .build()).get();
    }
    
}
