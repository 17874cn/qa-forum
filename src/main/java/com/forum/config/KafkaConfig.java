package com.forum.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_QUESTIONS = "qa-forum.questions";
    public static final String TOPIC_ANSWERS = "qa-forum.answers";

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.admin.auto-create", havingValue = "true", matchIfMissing = true)
    public NewTopic questionsTopic() {
        return TopicBuilder.name(TOPIC_QUESTIONS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.admin.auto-create", havingValue = "true", matchIfMissing = true)
    public NewTopic answersTopic() {
        return TopicBuilder.name(TOPIC_ANSWERS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
