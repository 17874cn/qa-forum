package com.forum.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.config.KafkaConfig;
import com.forum.event.ForumEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    @Nullable
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaProducerService(@Nullable KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;

        if (kafkaTemplate == null) {
            log.warn("Kafka is disabled — events will be logged only");
        }
    }

    public void publishEvent(ForumEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String topic = resolveTopic(event);
            String key = resolveKey(event);

            if (kafkaTemplate != null) {
                log.info("Publishing event: type={}, topic={}, key={}", event.getEventType(), topic, key);
                kafkaTemplate.send(topic, key, payload);
            } else {
                log.info("[KAFKA DISABLED] Event: type={}, topic={}, key={}, payload={}",
                        event.getEventType(), topic, key, payload);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event.getEventType(), e);
        }
    }

    private String resolveTopic(ForumEvent event) {
        return switch (event.getEventType()) {
            case QUESTION_CREATED -> KafkaConfig.TOPIC_QUESTIONS;
            case ANSWER_CREATED, ANSWER_ACCEPTED, ANSWER_VOTED -> KafkaConfig.TOPIC_ANSWERS;
        };
    }

    private String resolveKey(ForumEvent event) {
        return event.getQuestionId() != null ? event.getQuestionId() : event.getAnswerId();
    }
}
