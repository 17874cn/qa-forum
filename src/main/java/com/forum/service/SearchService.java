package com.forum.service;

import com.forum.model.Question;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

@Service
public class SearchService {

    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private final Scheduler searchProcessingScheduler;

    public SearchService(ReactiveMongoTemplate reactiveMongoTemplate,
                         @Qualifier("searchProcessingScheduler") Scheduler searchProcessingScheduler) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
        this.searchProcessingScheduler = searchProcessingScheduler;
    }

    public Flux<Question> search(String keyword) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage().matchingPhrase(keyword);
        Query query = TextQuery.queryText(criteria).sortByScore();
        return reactiveMongoTemplate.find(query, Question.class)
                .publishOn(searchProcessingScheduler);
    }
}
