package dev.lydtech.tracking.service;

import dev.lydtech.message.DispatchCompleted;
import dev.lydtech.message.DispatchPreparing;
import dev.lydtech.message.TrackingStatusUpdated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
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
    public void processDispatchPreparing_Success() throws Exception {
        when(kafkaProducerMock.send(anyString(), any(TrackingStatusUpdated.class))).thenReturn(mock(CompletableFuture.class));

        DispatchPreparing testEvent = DispatchPreparing.builder()
            .orderId(UUID.randomUUID())
            .build();
        service.processDispatchPreparing(testEvent);

        verify(kafkaProducerMock, times(1)).send(eq(TRACKING_STATUS_TOPIC), any(TrackingStatusUpdated.class));
    }

    @Test
    public void processDispatchPreparing_throwsException() {
        String errorMessage = "dispatch preparing producer failure";
        doThrow(new RuntimeException(errorMessage)).when(kafkaProducerMock).send(eq(TRACKING_STATUS_TOPIC), any(TrackingStatusUpdated.class));

        DispatchPreparing testEvent = DispatchPreparing.builder()
            .orderId(UUID.randomUUID())
            .build();
        Exception exception = assertThrows(RuntimeException.class, () -> service.processDispatchPreparing(testEvent));

        verify(kafkaProducerMock, times(1)).send(eq(TRACKING_STATUS_TOPIC), any(TrackingStatusUpdated.class));
        assertThat(exception.getMessage(), equalTo(errorMessage));
    }

    @Test
    public void processDispatchCompleted_Success() throws Exception {
        when(kafkaProducerMock.send(anyString(), any(TrackingStatusUpdated.class))).thenReturn(mock(CompletableFuture.class));

        DispatchCompleted testEvent = DispatchCompleted.builder()
            .orderId(UUID.randomUUID())
            .dispatchedDate(LocalDate.now().toString())
            .build();
        service.processDispatchCompleted(testEvent);

        verify(kafkaProducerMock, times(1)).send(eq(TRACKING_STATUS_TOPIC), any(TrackingStatusUpdated.class));
    }

    @Test
    public void processDispatchCompleted_throwsException() {
        String errorMessage = "dispatch completed producer failure";
        doThrow(new RuntimeException(errorMessage)).when(kafkaProducerMock).send(eq(TRACKING_STATUS_TOPIC), any(TrackingStatusUpdated.class));

        DispatchCompleted testEvent = DispatchCompleted.builder()
            .orderId(UUID.randomUUID())
            .dispatchedDate(LocalDate.now().toString())
            .build();
        Exception exception = assertThrows(RuntimeException.class, () -> service.processDispatchCompleted(testEvent));

        verify(kafkaProducerMock, times(1)).send(eq(TRACKING_STATUS_TOPIC), any(TrackingStatusUpdated.class));
        assertThat(exception.getMessage(), equalTo(errorMessage));
    }
}
