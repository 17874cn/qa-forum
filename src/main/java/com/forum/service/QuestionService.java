package com.forum.service;

import com.forum.activity.ActivityLogService;
import com.forum.activity.ActivityType;
import com.forum.event.ForumEvent;
import com.forum.kafka.KafkaProducerService;
import com.forum.model.Question;
import com.forum.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Instant;
import java.util.Map;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ActivityLogService activityLogService;
    private final Scheduler questionFetchScheduler;

    public QuestionService(QuestionRepository questionRepository,
                           KafkaProducerService kafkaProducerService,
                           ActivityLogService activityLogService,
                           @Qualifier("questionFetchScheduler") Scheduler questionFetchScheduler) {
        this.questionRepository = questionRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.activityLogService = activityLogService;
        this.questionFetchScheduler = questionFetchScheduler;
    }

    public Mono<Question> create(Question question) {
        return questionRepository.save(question)
                .publishOn(questionFetchScheduler)
                .doOnSuccess(saved -> {
                    // Kafka event
                    ForumEvent event = new ForumEvent(ForumEvent.EventType.QUESTION_CREATED);
                    event.setQuestionId(saved.getId());
                    event.setAuthorId(saved.getAuthorId());
                    event.setTitle(saved.getTitle());
                    event.setBody(saved.getBody());
                    kafkaProducerService.publishEvent(event);

                    // Activity log
                    activityLogService.log(saved.getAuthorId(),
                            ActivityType.QUESTION_CREATED,
                            "Created question: " + saved.getTitle(),
                            saved.getId(),
                            Map.of("tags", String.join(",", saved.getTags())));
                });
    }

    public Flux<Question> findAll() {
        return questionRepository.findAll()
                .publishOn(questionFetchScheduler);
    }

    public Mono<Question> findById(String id) {
        return questionRepository.findById(id)
                .publishOn(questionFetchScheduler)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found")));
    }

    public Mono<Question> update(String id, Question updated) {
        return findById(id)
                .flatMap(existing -> {
                    existing.setTitle(updated.getTitle());
                    existing.setBody(updated.getBody());
                    existing.setTags(updated.getTags());
                    existing.setUpdatedAt(Instant.now());
                    return questionRepository.save(existing);
                })
                .doOnSuccess(saved ->
                        activityLogService.log(saved.getAuthorId(),
                                ActivityType.QUESTION_UPDATED,
                                "Updated question: " + saved.getTitle(),
                                saved.getId())
                );
    }

    public Mono<Void> delete(String id) {
        return findById(id)
                .doOnSuccess(question ->
                        activityLogService.log(question.getAuthorId(),
                                ActivityType.QUESTION_DELETED,
                                "Deleted question: " + question.getTitle(),
                                question.getId())
                )
                .flatMap(question -> questionRepository.deleteById(id));
    }
}
