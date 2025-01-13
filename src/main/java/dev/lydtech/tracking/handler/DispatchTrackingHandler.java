package dev.lydtech.tracking.handler;

import dev.lydtech.tracking.message.DispatchCompleted;
import dev.lydtech.tracking.message.DispatchPreparing;
import dev.lydtech.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
    id = "dispatchTrackingConsumerClient",
    topics = DispatchTrackingHandler.DISPATCH_TRACKING_TOPIC,
    groupId = DispatchTrackingHandler.DISPATCH_TRACKING_TOPIC_GROUP,
    containerFactory = "kafkaListenerContainerFactory"
)
public class DispatchTrackingHandler {
    
    public static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    public static final String DISPATCH_TRACKING_TOPIC_GROUP = "tracking.dispatch.tracking";

    private final TrackingService trackingService;
    
    @KafkaHandler
    public void listen(DispatchPreparing dispatchPreparing) {
        log.info("Received dispatchPreparing tracking message: {}", dispatchPreparing);
        try {
            trackingService.processDispatchPreparing(dispatchPreparing);
        } catch (Exception e) {
            log.error("Error processing payload {}", dispatchPreparing, e);
        }
    }

    @KafkaHandler
    public void listen(DispatchCompleted dispatchCompleted) {
        log.info("Received dispatchCompleted tracking message: {}", dispatchCompleted);
        try {
            trackingService.processDispatchCompleted(dispatchCompleted);
        } catch (Exception e) {
            log.error("Error processing payload {}", dispatchCompleted, e);
        }
    }
    
}
