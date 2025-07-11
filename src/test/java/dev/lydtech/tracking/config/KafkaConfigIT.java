package dev.lydtech.tracking.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static dev.lydtech.tracking.handler.DispatchTrackingHandler.DISPATCH_TRACKING_TOPIC;
import static dev.lydtech.tracking.handler.DispatchTrackingHandler.DISPATCH_TRACKING_TOPIC_GROUP;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
@ActiveProfiles("docker")
class KafkaConfigIT {

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Test
    void shouldHaveRequiredTopics() throws Exception {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            ListTopicsResult topicsResult = adminClient.listTopics();
            Collection<TopicListing> topics = topicsResult.listings().get(5, TimeUnit.SECONDS);

            ListConsumerGroupsResult consumerGroupsResult = adminClient.listConsumerGroups();
            Collection<ConsumerGroupListing> groups = consumerGroupsResult.all().get(5, TimeUnit.SECONDS);

            assertThat(topics)
                .extracting(TopicListing::name)
                .contains(
                    DISPATCH_TRACKING_TOPIC
                );

            assertThat(groups)
                .extracting(ConsumerGroupListing::groupId)
                .contains(
                    DISPATCH_TRACKING_TOPIC_GROUP
                );
        }
    }

}