package dev.lydtech.tracking.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaTopicInitializerTest {

    private static final String TOPIC = "dispatch.tracking";

    private AdminClient adminClient;

    private MockedStatic<AdminClient> adminClientStaticMock;

    private KafkaTopicInitializer initializer;

    private List<NewTopic> topics;

    @BeforeEach
    void setup() {
        adminClient = mock(AdminClient.class);
        KafkaAdmin kafkaAdmin = mock(KafkaAdmin.class);
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(Map.of());

        adminClientStaticMock = mockStatic(AdminClient.class);
        adminClientStaticMock.when(() -> AdminClient.create(anyMap())).thenReturn(adminClient);

        topics = List.of(new NewTopic(TOPIC, 1, (short) 1));
        initializer = new KafkaTopicInitializer(kafkaAdmin, topics);
    }

    @AfterEach
    void tearDown() {
        adminClientStaticMock.close();
    }

    @Test
    void onApplicationEvent_createsAndDescribesTopics() {
        stubCreateTopicsSuccess();
        DescribeTopicsResult describeTopicsResult = describeTopicsResult(descriptions());
        when(adminClient.describeTopics(anyCollection())).thenReturn(describeTopicsResult);
        stubDescribeConfigsSuccess();

        assertThatCode(this::fireContextRefreshedEvent).doesNotThrowAnyException();

        verify(adminClient).createTopics(anyCollection());
    }

    @Test
    void onApplicationEvent_logsWarningWhenTopicAlreadyExists() {
        CreateTopicsResult createTopicsResult = mock(CreateTopicsResult.class);
        when(createTopicsResult.all()).thenReturn(failedFuture(new TopicExistsException("Topic exists")));
        when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
        DescribeTopicsResult describeTopicsResult = describeTopicsResult(descriptions());
        when(adminClient.describeTopics(anyCollection())).thenReturn(describeTopicsResult);
        stubDescribeConfigsSuccess();

        assertThatCode(this::fireContextRefreshedEvent).doesNotThrowAnyException();
    }

    @Test
    void onApplicationEvent_logsErrorWhenTopicCreationFailsUnexpectedly() {
        CreateTopicsResult createTopicsResult = mock(CreateTopicsResult.class);
        when(createTopicsResult.all()).thenReturn(failedFuture(new RuntimeException("creation failed")));
        when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
        DescribeTopicsResult describeTopicsResult = describeTopicsResult(descriptions());
        when(adminClient.describeTopics(anyCollection())).thenReturn(describeTopicsResult);
        stubDescribeConfigsSuccess();

        assertThatCode(this::fireContextRefreshedEvent).doesNotThrowAnyException();
    }

    @Test
    void onApplicationEvent_retriesTopicLookupUntilTopicsAreAvailable() {
        stubCreateTopicsSuccess();
        DescribeTopicsResult describeTopicsResult = describeTopicsResult(descriptions());
        when(adminClient.describeTopics(anyCollection())).thenThrow(new RuntimeException("not ready"))
            .thenReturn(describeTopicsResult);
        stubDescribeConfigsSuccess();

        assertThatCode(this::fireContextRefreshedEvent).doesNotThrowAnyException();
    }

    @Test
    void onApplicationEvent_waitsUntilAllTopicsAreDescribed() {
        stubCreateTopicsSuccess();
        DescribeTopicsResult incompleteDescription = describeTopicsResult(KafkaFuture.completedFuture(Map.of()));
        DescribeTopicsResult completeDescription = describeTopicsResult(descriptions());
        when(adminClient.describeTopics(anyCollection())).thenReturn(incompleteDescription, completeDescription);
        stubDescribeConfigsSuccess();

        assertThatCode(this::fireContextRefreshedEvent).doesNotThrowAnyException();
    }

    @Test
    void onApplicationEvent_logsErrorWhenDescribingTopicsFails() {
        stubCreateTopicsSuccess();
        DescribeTopicsResult successfulDescription = describeTopicsResult(descriptions());
        DescribeTopicsResult failingDescription = describeTopicsResult(
                failedFuture(new RuntimeException("describe failed")));
        when(adminClient.describeTopics(anyCollection())).thenReturn(successfulDescription, failingDescription);

        assertThatCode(this::fireContextRefreshedEvent).doesNotThrowAnyException();
    }

    @Test
    void onApplicationEvent_logsErrorWhenDescribingTopicsTimesOut() throws Exception {
        stubCreateTopicsSuccess();
        DescribeTopicsResult successfulDescription = describeTopicsResult(descriptions());
        DescribeTopicsResult timeoutDescription = describeTopicsResult(timeoutFuture());
        when(adminClient.describeTopics(anyCollection())).thenReturn(successfulDescription, timeoutDescription);

        assertThatCode(this::fireContextRefreshedEvent).doesNotThrowAnyException();
    }

    @Test
    void onApplicationEvent_reinterruptsWhenTopicCreationIsInterrupted() throws Exception {
        CreateTopicsResult createTopicsResult = mock(CreateTopicsResult.class);
        KafkaFuture<Void> interruptedCreation = interruptedFuture();
        when(createTopicsResult.all()).thenReturn(interruptedCreation);
        when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
        DescribeTopicsResult describeTopicsResult = describeTopicsResult(descriptions());
        when(adminClient.describeTopics(anyCollection())).thenReturn(describeTopicsResult);
        stubDescribeConfigsSuccess();

        try {
            assertThatCode(this::fireContextRefreshedEvent).doesNotThrowAnyException();

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        }
        finally {
            Thread.interrupted();
        }
    }

    @Test
    void onApplicationEvent_reinterruptsWhenDescribingTopicsIsInterrupted() throws Exception {
        stubCreateTopicsSuccess();
        DescribeTopicsResult successfulDescription = describeTopicsResult(descriptions());
        DescribeTopicsResult interruptedDescription = describeTopicsResult(interruptedFuture());
        when(adminClient.describeTopics(anyCollection())).thenReturn(successfulDescription, interruptedDescription);

        try {
            assertThatCode(this::fireContextRefreshedEvent).doesNotThrowAnyException();

            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        }
        finally {
            Thread.interrupted();
        }
    }

    @Test
    void onApplicationEvent_reinterruptsWhenTopicLookupIsInterrupted() throws Exception {
        stubCreateTopicsSuccess();
        DescribeTopicsResult interruptedDescription = describeTopicsResult(interruptedFuture());
        DescribeTopicsResult successfulDescription = describeTopicsResult(descriptions());
        when(adminClient.describeTopics(anyCollection())).thenReturn(interruptedDescription, successfulDescription);
        stubDescribeConfigsSuccess();

        try {
            assertThatCode(this::fireContextRefreshedEvent).doesNotThrowAnyException();
        }
        finally {
            Thread.interrupted();
        }
    }

    private void fireContextRefreshedEvent() {
        initializer.onApplicationEvent(new ContextRefreshedEvent(mock(ApplicationContext.class)));
    }

    private void stubCreateTopicsSuccess() {
        CreateTopicsResult createTopicsResult = mock(CreateTopicsResult.class);
        when(createTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
    }

    private DescribeTopicsResult describeTopicsResult(KafkaFuture<Map<String, TopicDescription>> descriptions) {
        DescribeTopicsResult describeTopicsResult = mock(DescribeTopicsResult.class);
        when(describeTopicsResult.allTopicNames()).thenReturn(descriptions);
        return describeTopicsResult;
    }

    private KafkaFuture<Map<String, TopicDescription>> descriptions() {
        return KafkaFuture.completedFuture(Map.of(TOPIC, mock(TopicDescription.class)));
    }

    private void stubDescribeConfigsSuccess() {
        Config config = mock(Config.class);
        when(config.entries()).thenReturn(List.of(
                new ConfigEntry("retention.ms", "1000", ConfigEntry.ConfigSource.DEFAULT_CONFIG, false, false,
                        List.of(), ConfigEntry.ConfigType.STRING, null),
                new ConfigEntry("cleanup.policy", "delete", ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG, false, false,
                        List.of(), ConfigEntry.ConfigType.STRING, null)));

        DescribeConfigsResult describeConfigsResult = mock(DescribeConfigsResult.class);
        when(describeConfigsResult.all()).thenReturn(
                KafkaFuture.completedFuture(Map.of(new ConfigResource(ConfigResource.Type.TOPIC, TOPIC), config)));
        when(adminClient.describeConfigs(anyCollection())).thenReturn(describeConfigsResult);
    }

    @SuppressWarnings("unchecked")
    private static <T> KafkaFuture<T> interruptedFuture() throws Exception {
        KafkaFuture<T> future = mock(KafkaFuture.class);
        when(future.get(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException("interrupted"));
        return future;
    }

    @SuppressWarnings("unchecked")
    private static KafkaFuture<Map<String, TopicDescription>> timeoutFuture() throws Exception {
        KafkaFuture<Map<String, TopicDescription>> future = mock(KafkaFuture.class);
        when(future.get(anyLong(), any(TimeUnit.class))).thenThrow(new TimeoutException("timeout"));
        return future;
    }

    private static <T> KafkaFuture<T> failedFuture(Throwable throwable) {
        KafkaFutureImpl<T> future = new KafkaFutureImpl<>();
        future.completeExceptionally(throwable);
        return future;
    }

}
