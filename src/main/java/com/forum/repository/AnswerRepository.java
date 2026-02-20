package com.forum.repository;

import com.forum.model.Answer;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface AnswerRepository extends ReactiveMongoRepository<Answer, String> {
    Flux<Answer> findByQuestionId(String questionId);
}
