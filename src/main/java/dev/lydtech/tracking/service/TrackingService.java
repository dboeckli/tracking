package dev.lydtech.tracking.service;

import dev.lydtech.tracking.message.DispatchPreparing;
import dev.lydtech.tracking.message.TrackingStatus;
import dev.lydtech.tracking.message.TrackingStatusUpdated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    public static final String TRACKING_STATUS_TOPIC = "tracking.status";

    private final KafkaTemplate<String, Object> kafkaProducer;

    public void process(DispatchPreparing dispatchPreparing) throws Exception {
        log.info("Received dispatch preparing message {}", dispatchPreparing);

        TrackingStatusUpdated trackingStatusUpdated = TrackingStatusUpdated.builder()
            .orderId(dispatchPreparing.getOrderId())
            .status(TrackingStatus.PREPARING)
            .build();
        
        kafkaProducer.send(TRACKING_STATUS_TOPIC, trackingStatusUpdated).get();
        log.info("### dispatch tracking status message for order {} has been sent", trackingStatusUpdated);
    }
    
    
}
