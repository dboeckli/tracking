package dev.lydtech.tracking.handler;

import dev.lydtech.tracking.message.DispatchPreparing;
import dev.lydtech.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchTrackingHandler {
    
    public static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";

    private final TrackingService trackingService;

    @KafkaListener(
        id = "dispatchTrackingConsumerClient",
        topics = DISPATCH_TRACKING_TOPIC,
        groupId = "tracking.dispatch.tracking",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(DispatchPreparing dispatchPreparing) {
        log.info("Received dispatch tracking message: {}", dispatchPreparing);
        try {
            trackingService.process(dispatchPreparing);
        } catch (Exception e) {
            log.error("Processing failure", e);
        }
    }
    
}
