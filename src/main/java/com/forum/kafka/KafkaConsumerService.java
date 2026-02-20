package com.forum.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.config.KafkaConfig;
import com.forum.event.ForumEvent;
import com.forum.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public KafkaConsumerService(NotificationService notificationService,
                                ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_QUESTIONS, groupId = "qa-forum-group")
    public void consumeQuestionEvent(String message) {
        try {
            ForumEvent event = objectMapper.readValue(message, ForumEvent.class);
            log.info("Received question event: type={}, questionId={}",
                    event.getEventType(), event.getQuestionId());
        } catch (Exception e) {
            log.error("Failed to process question event: {}", message, e);
        }
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_ANSWERS, groupId = "qa-forum-group")
    public void consumeAnswerEvent(String message) {
        try {
            ForumEvent event = objectMapper.readValue(message, ForumEvent.class);
            log.info("Received answer event: type={}, questionId={}, answerId={}",
                    event.getEventType(), event.getQuestionId(), event.getAnswerId());

            switch (event.getEventType()) {
                case ANSWER_CREATED -> notificationService.handleAnswerCreated(event);
                case ANSWER_ACCEPTED -> notificationService.handleAnswerAccepted(event);
                case ANSWER_VOTED -> notificationService.handleAnswerVoted(event);
                default -> log.warn("Unhandled answer event type: {}", event.getEventType());
            }

        } catch (Exception e) {
            log.error("Failed to process answer event: {}", message, e);
        }
    }
}
