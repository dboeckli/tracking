package dev.lydtech.tracking.service;

import dev.lydtech.tracking.message.DispatchPreparing;
import dev.lydtech.tracking.message.TrackingStatusUpdated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static dev.lydtech.tracking.service.TrackingService.TRACKING_STATUS_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TrackingServiceTest {

    private KafkaTemplate kafkaProducerMock;
    private TrackingService service;

    @BeforeEach
    public void setup() {
        kafkaProducerMock = mock(KafkaTemplate.class);
        service = new TrackingService(kafkaProducerMock);
    }

    @Test
    public void process_Success() throws Exception {
        when(kafkaProducerMock.send(anyString(), any(TrackingStatusUpdated.class))).thenReturn(mock(CompletableFuture.class));

        DispatchPreparing testEvent = DispatchPreparing.builder()
            .orderId(UUID.randomUUID())
            .build();
        service.process(testEvent);

        verify(kafkaProducerMock, times(1)).send(eq(TRACKING_STATUS_TOPIC), any(TrackingStatusUpdated.class));
    }

    @Test
    public void process_DispatchPreparingProducerThrowsException() {
        String errorMessage = "dispatch preparing producer failure";
        doThrow(new RuntimeException(errorMessage)).when(kafkaProducerMock).send(eq(TRACKING_STATUS_TOPIC), any(TrackingStatusUpdated.class));

        DispatchPreparing testEvent = DispatchPreparing.builder()
            .orderId(UUID.randomUUID())
            .build();
        Exception exception = assertThrows(RuntimeException.class, () -> service.process(testEvent));

        verify(kafkaProducerMock, times(1)).send(eq(TRACKING_STATUS_TOPIC), any(TrackingStatusUpdated.class));
        assertThat(exception.getMessage(), equalTo(errorMessage));
    }

}
