package com.forum.repository;

import com.forum.model.Question;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface QuestionRepository extends ReactiveMongoRepository<Question, String> {
    Flux<Question> findByAuthorId(String authorId);
    Flux<Question> findByTagsContaining(String tag);
}
