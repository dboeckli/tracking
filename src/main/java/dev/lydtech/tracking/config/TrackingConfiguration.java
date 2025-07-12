package dev.lydtech.tracking.config;

import dev.lydtech.message.DispatchCompleted;
import dev.lydtech.message.DispatchPreparing;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class TrackingConfiguration {

    public static final String TRUSTED_PACKAGES = DispatchPreparing.class.getPackageName();

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(ConsumerFactory<String, Object> consumerFactory) {
        final ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
        @Value("${spring.kafka.consumer.properties.allow.auto.create.topics}") Boolean allowAutoCreateTopics) {
        
        final Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // we do not read anymore from the application.yaml anymore
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, allowAutoCreateTopics);

        config.put(JsonDeserializer.TRUSTED_PACKAGES, TRUSTED_PACKAGES);

        // Add JsonDeserializer
        config.put(JsonDeserializer.TYPE_MAPPINGS, getTypeMappings());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        config.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);

        //config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MessageType.DISPATCH_PREPARING.getClazz().getName());

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // we do not read anymore from the application.yaml anymore
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        config.put(JsonSerializer.TYPE_MAPPINGS, getTypeMappings());

        return new DefaultKafkaProducerFactory<>(config);
    }

    public static String getTypeMappings() {
        return Arrays.stream(MessageType.values())
            .map(type -> type.getPrefix() + ":" + type.getClazz().getName())
            .collect(Collectors.joining(", "));
    }

    @Getter
    private enum MessageType {
        DISPATCH_PREPARING("dispatch-preparing-prefix", DispatchPreparing.class),
        DISPATCH_COMPLETED("dispatch-completed-prefix", DispatchCompleted.class);

        private final String prefix;
        private final Class<?> clazz;

        MessageType(String prefix, Class<?> clazz) {
            this.prefix = prefix;
            this.clazz = clazz;
        }

    }
    
}
