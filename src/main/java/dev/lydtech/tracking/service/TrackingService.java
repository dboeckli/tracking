package dev.lydtech.tracking.service;

import dev.lydtech.message.DispatchCompleted;
import dev.lydtech.message.DispatchPreparing;
import dev.lydtech.message.TrackingStatus;
import dev.lydtech.message.TrackingStatusUpdated;
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
        log.info("\n### dispatchPreparing tracking status message for order {}\nhas been sent: {}\nto {}",
            trackingStatusUpdated.getOrderId(), trackingStatusUpdated, TRACKING_STATUS_TOPIC);
    }


    public void processDispatchCompleted(DispatchCompleted dispatchCompleted) throws ExecutionException, InterruptedException {
        log.info("Received dispatchCompleted message: {}", dispatchCompleted);

        TrackingStatusUpdated trackingStatusUpdated = TrackingStatusUpdated.builder()
            .orderId(dispatchCompleted.getOrderId())
            .status(TrackingStatus.DISPATCHED)
            .build();

        kafkaProducer.send(TRACKING_STATUS_TOPIC, trackingStatusUpdated).get();
        log.info("\n### DispatchCompleted tracking status message for order {}\nhas been sent: {}\nto {}",
            trackingStatusUpdated.getOrderId(), trackingStatusUpdated, TRACKING_STATUS_TOPIC);
    }
}
