package dev.lydtech.tracking.service;

import dev.lydtech.tracking.message.DispatchCompleted;
import dev.lydtech.tracking.message.DispatchPreparing;
import dev.lydtech.tracking.message.TrackingStatus;
import dev.lydtech.tracking.message.TrackingStatusUpdated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    public static final String TRACKING_STATUS_TOPIC = "tracking.status";

    private final KafkaTemplate<String, Object> kafkaProducer;

    public void processDispatchPreparing(DispatchPreparing dispatchPreparing) throws ExecutionException, InterruptedException {
        log.info("Received dispatchPreparing message: {}", dispatchPreparing);

        TrackingStatusUpdated trackingStatusUpdated = TrackingStatusUpdated.builder()
            .orderId(dispatchPreparing.getOrderId())
            .status(TrackingStatus.PREPARING)
            .build();
        
        kafkaProducer.send(TRACKING_STATUS_TOPIC, trackingStatusUpdated).get();
        log.info("### dispatchPreparing tracking status message for order {} has been sent: {}", trackingStatusUpdated.getOrderId(), trackingStatusUpdated);
    }


    public void processDispatchCompleted(DispatchCompleted dispatchCompleted) throws ExecutionException, InterruptedException {
        log.info("Received dispatchCompleted message: {}", dispatchCompleted);

        TrackingStatusUpdated trackingStatusUpdated = TrackingStatusUpdated.builder()
            .orderId(dispatchCompleted.getOrderId())
            .status(TrackingStatus.DISPATCHED)
            .build();

        kafkaProducer.send(TRACKING_STATUS_TOPIC, trackingStatusUpdated).get();
        log.info("### dispatchCompleted tracking status message for order {} has been sent: {}", trackingStatusUpdated.getOrderId(), trackingStatusUpdated);
    }
}
