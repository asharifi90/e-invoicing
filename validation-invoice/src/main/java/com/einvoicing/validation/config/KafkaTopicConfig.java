package com.einvoicing.validation.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_REJECTED = "invoice.rejected";
    public static final String TOPIC_VALIDATED = "invoice.validated";

    @Bean
    public NewTopic invoiceRejectedTopic() {
        return TopicBuilder.name(TOPIC_REJECTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic invoiceValidatedTopic() {
        return TopicBuilder.name(TOPIC_VALIDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
