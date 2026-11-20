package dev.lydtech.tracking.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.clients.admin.ListGroupsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaHealthIndicatorTest {

    private KafkaTemplate<String, Object> kafkaTemplate;

    private AdminClient adminClient;

    private MockedStatic<AdminClient> adminClientStaticMock;

    private KafkaHealthIndicator indicator;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        kafkaTemplate = mock(KafkaTemplate.class);
        adminClient = mock(AdminClient.class);
        KafkaAdmin kafkaAdmin = mock(KafkaAdmin.class);
        when(kafkaAdmin.getConfigurationProperties()).thenReturn(Map.of());

        adminClientStaticMock = mockStatic(AdminClient.class);
        adminClientStaticMock.when(() -> AdminClient.create(anyMap())).thenReturn(adminClient);

        indicator = new KafkaHealthIndicator(kafkaTemplate, kafkaAdmin, "localhost:9092");
    }

    @AfterEach
    void tearDown() {
        adminClientStaticMock.close();
    }

    @Test
    void health_returnsUpWithDetailsForRepeatedSuccessfulChecks() {
        stubProducerSuccess();
        stubAdminClientSuccess();

        Health firstCheck = indicator.health();
        Health secondCheck = indicator.health();

        assertThat(firstCheck.getStatus()).isEqualTo(Status.UP);
        assertThat(secondCheck.getStatus()).isEqualTo(Status.UP);
        assertThat(firstCheck.getDetails()).containsEntry("kafkaBootstrapServers", "localhost:9092")
            .containsEntry("kafkaResponse", "Topic: health-check, Partition: 0, Offset: 0")
            .containsEntry("clusterId", "cluster-id");
    }

    @Test
    void health_returnsDownWhenKafkaIsUnavailable() {
        when(kafkaTemplate.send(anyString(), any())).thenThrow(new RuntimeException("Kafka is down"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("kafkaBootstrapServers", "localhost:9092");
    }

    @Test
    void health_returnsDownAndReinterruptsWhenInterrupted() throws Exception {
        CompletableFuture<SendResult<String, Object>> future = mock(CompletableFuture.class);
        when(future.get(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(future);

        try {
            Health health = indicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        }
        finally {
            Thread.interrupted();
        }
    }

    @Test
    void shutdown_closesAdminClient() {
        indicator.shutdown();

        verify(adminClient).close();
    }

    @Test
    void shutdown_swallowsExceptionWhileClosingAdminClient() {
        doThrow(new RuntimeException("close failed")).when(adminClient).close();

        assertThatCode(() -> indicator.shutdown()).doesNotThrowAnyException();
    }

    private void stubProducerSuccess() {
        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        when(recordMetadata.topic()).thenReturn("health-check");
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(0L);

        SendResult<String, Object> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        when(kafkaTemplate.send("health-check", "health-check-message"))
            .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(sendResult));
    }

    private void stubAdminClientSuccess() {
        GroupListing groupListing = mock(GroupListing.class);
        when(groupListing.groupId()).thenReturn("tracking.dispatch.tracking");
        ListGroupsResult listGroupsResult = mock(ListGroupsResult.class);
        when(listGroupsResult.all()).thenReturn(KafkaFuture.completedFuture(List.of(groupListing)));
        when(adminClient.listGroups(any())).thenReturn(listGroupsResult);

        DescribeClusterResult describeClusterResult = mock(DescribeClusterResult.class);
        when(describeClusterResult.clusterId()).thenReturn(KafkaFuture.completedFuture("cluster-id"));
        when(describeClusterResult.nodes())
            .thenReturn(KafkaFuture.completedFuture(List.<Node>of(new Node(0, "localhost", 9092))));
        when(adminClient.describeCluster(any())).thenReturn(describeClusterResult);

        TopicListing topicListing = mock(TopicListing.class);
        when(topicListing.name()).thenReturn("health-check");
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(listTopicsResult.listings()).thenReturn(KafkaFuture.completedFuture(List.of(topicListing)));
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
    }

}
