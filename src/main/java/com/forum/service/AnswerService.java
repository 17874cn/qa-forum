package com.forum.service;

import com.forum.activity.ActivityLogService;
import com.forum.activity.ActivityType;
import com.forum.event.ForumEvent;
import com.forum.kafka.KafkaProducerService;
import com.forum.model.Answer;
import com.forum.repository.AnswerRepository;
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
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final KafkaProducerService kafkaProducerService;
    private final ActivityLogService activityLogService;
    private final Scheduler answerFetchScheduler;
    private final Scheduler voteCalculationScheduler;

    public AnswerService(AnswerRepository answerRepository,
                         QuestionRepository questionRepository,
                         KafkaProducerService kafkaProducerService,
                         ActivityLogService activityLogService,
                         @Qualifier("answerFetchScheduler") Scheduler answerFetchScheduler,
                         @Qualifier("voteCalculationScheduler") Scheduler voteCalculationScheduler) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.activityLogService = activityLogService;
        this.answerFetchScheduler = answerFetchScheduler;
        this.voteCalculationScheduler = voteCalculationScheduler;
    }

    public Mono<Answer> create(String questionId, Answer answer) {
        answer.setQuestionId(questionId);

        return questionRepository.findById(questionId)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found")))
                .flatMap(question -> answerRepository.save(answer)
                        .publishOn(answerFetchScheduler)
                        .doOnSuccess(saved -> {
                            // Kafka event to sending with rate limit
                            ForumEvent event = new ForumEvent(ForumEvent.EventType.ANSWER_CREATED);
                            event.setQuestionId(questionId);
                            event.setAnswerId(saved.getId());
                            event.setAuthorId(saved.getAuthorId());
                            event.setQuestionAuthorId(question.getAuthorId());
                            event.setTitle(question.getTitle());
                            kafkaProducerService.publishEvent(event); // Kafka consumer service

                            // Activity log
                            activityLogService.log(saved.getAuthorId(),
                                    ActivityType.ANSWER_CREATED,
                                    "Answered question: " + question.getTitle(),
                                    saved.getId(),
                                    Map.of("questionId", questionId));
                        })
                );
    }

    public Flux<Answer> findByQuestionId(String questionId) {
        return answerRepository.findByQuestionId(questionId)
                .publishOn(answerFetchScheduler);
    }

    public Mono<Answer> vote(String id, int delta) {
        return answerRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found")))
                .publishOn(voteCalculationScheduler)
                .flatMap(answer -> {
                    answer.setVotes(answer.getVotes() + delta);
                    answer.setUpdatedAt(Instant.now());
                    return answerRepository.save(answer)
                            .flatMap(saved -> questionRepository.findById(saved.getQuestionId())
                                    .doOnSuccess(question -> {
                                        // Kafka event
                                        ForumEvent event = new ForumEvent(ForumEvent.EventType.ANSWER_VOTED);
                                        event.setQuestionId(saved.getQuestionId());
                                        event.setAnswerId(saved.getId());
                                        event.setAuthorId(saved.getAuthorId());
                                        event.setTitle(question != null ? question.getTitle() : "");
                                        kafkaProducerService.publishEvent(event);

                                        // Activity log
                                        activityLogService.log(saved.getAuthorId(),
                                                ActivityType.ANSWER_VOTED,
                                                "Vote " + (delta > 0 ? "up" : "down") + " on answer",
                                                saved.getId(),
                                                Map.of("delta", String.valueOf(delta),
                                                        "newVoteCount", String.valueOf(saved.getVotes())));
                                    })
                                    .thenReturn(saved)
                            );
                });
    }

    public Mono<Answer> accept(String id) {
        return answerRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer not found")))
                .publishOn(answerFetchScheduler)
                .flatMap(answer -> {
                    answer.setAccepted(true);
                    answer.setUpdatedAt(Instant.now());
                    return answerRepository.save(answer)
                            .flatMap(saved -> questionRepository.findById(saved.getQuestionId())
                                    .doOnSuccess(question -> {
                                        // Kafka event
                                        ForumEvent event = new ForumEvent(ForumEvent.EventType.ANSWER_ACCEPTED);
                                        event.setQuestionId(saved.getQuestionId());
                                        event.setAnswerId(saved.getId());
                                        event.setAuthorId(saved.getAuthorId());
                                        event.setTitle(question != null ? question.getTitle() : "");
                                        kafkaProducerService.publishEvent(event);

                                        // Activity log
                                        activityLogService.log(saved.getAuthorId(),
                                                ActivityType.ANSWER_ACCEPTED,
                                                "Answer accepted on: " + (question != null ? question.getTitle() : ""),
                                                saved.getId(),
                                                Map.of("questionId", saved.getQuestionId()));
                                    })
                                    .thenReturn(saved)
                            );
                });
    }
}
