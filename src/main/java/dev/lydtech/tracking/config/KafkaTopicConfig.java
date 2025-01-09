package dev.lydtech.tracking.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.*;

import static dev.lydtech.tracking.handler.DispatchTrackingHandler.DISPATCH_TRACKING_TOPIC;
import static dev.lydtech.tracking.service.TrackingService.TRACKING_STATUS_TOPIC;

@Configuration
public class KafkaTopicConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public List<NewTopic> topics() {
        List<NewTopic> topics = new ArrayList<>();
        topics.add(new NewTopic(DISPATCH_TRACKING_TOPIC, 1, (short) 1)
            .configs(Collections.singletonMap("retention.ms", String.valueOf(3600000))));
        topics.add(new NewTopic(TRACKING_STATUS_TOPIC, 1, (short) 1)
            .configs(Collections.singletonMap("retention.ms", String.valueOf(3600000))));
        return topics;
    }
    
}
