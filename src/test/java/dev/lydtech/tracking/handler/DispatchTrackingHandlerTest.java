package dev.lydtech.tracking.handler;

import dev.lydtech.message.DispatchCompleted;
import dev.lydtech.message.DispatchPreparing;
import dev.lydtech.tracking.service.TrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DispatchTrackingHandlerTest {

    private TrackingService trackingServiceMock;

    private DispatchTrackingHandler handler;

    @BeforeEach
    void setup() {
        trackingServiceMock = mock(TrackingService.class);
        handler = new DispatchTrackingHandler(trackingServiceMock);
    }

    @Test
    void handleUnknown_logsMessage() {
        handler.handleUnknown("unknown-message");

        verifyNoInteractions(trackingServiceMock);
    }

    @Test
    void listen_dispatchPreparing_Success() throws Exception {
        DispatchPreparing testEvent = DispatchPreparing.builder().orderId(UUID.randomUUID()).build();

        handler.listen(testEvent);
        verify(trackingServiceMock, times(1)).processDispatchPreparing(testEvent);
    }

    @Test
    void listen_dispatchCompleted_Success() throws Exception {
        DispatchCompleted testEvent = DispatchCompleted.builder()
            .orderId(UUID.randomUUID())
            .dispatchedDate(LocalDate.now().toString())
            .build();

        handler.listen(testEvent);
        verify(trackingServiceMock, times(1)).processDispatchCompleted(testEvent);
    }

    @Test
    void listen_dispatchPreparing_ServiceThrowsException() throws Exception {
        DispatchPreparing testEvent = DispatchPreparing.builder().orderId(UUID.randomUUID()).build();

        doThrow(new RuntimeException("Service failure")).when(trackingServiceMock).processDispatchPreparing(testEvent);

        handler.listen(testEvent);

        verify(trackingServiceMock, times(1)).processDispatchPreparing(testEvent);
    }

    @Test
    void listen_dispatchCompleted_ServiceThrowsException() throws Exception {
        DispatchCompleted testEvent = DispatchCompleted.builder()
            .orderId(UUID.randomUUID())
            .dispatchedDate(LocalDate.now().toString())
            .build();

        doThrow(new RuntimeException("Service failure")).when(trackingServiceMock).processDispatchCompleted(testEvent);

        handler.listen(testEvent);

        verify(trackingServiceMock, times(1)).processDispatchCompleted(testEvent);
    }

    @Test
    void listen_dispatchPreparing_ServiceIsInterrupted() throws Exception {
        DispatchPreparing testEvent = DispatchPreparing.builder().orderId(UUID.randomUUID()).build();
        doAnswer(invocation -> {
            throw new InterruptedException("interrupted");
        }).when(trackingServiceMock).processDispatchPreparing(testEvent);

        try {
            handler.listen(testEvent);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        }
        finally {
            Thread.interrupted();
        }
    }

    @Test
    void listen_dispatchCompleted_ServiceIsInterrupted() throws Exception {
        DispatchCompleted testEvent = DispatchCompleted.builder()
            .orderId(UUID.randomUUID())
            .dispatchedDate(LocalDate.now().toString())
            .build();
        doAnswer(invocation -> {
            throw new InterruptedException("interrupted");
        }).when(trackingServiceMock).processDispatchCompleted(testEvent);

        try {
            handler.listen(testEvent);

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        }
        finally {
            Thread.interrupted();
        }
    }

}
