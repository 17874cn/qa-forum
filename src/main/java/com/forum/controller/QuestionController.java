package com.forum.controller;

import com.forum.model.Question;
import com.forum.service.QuestionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Question> create(@RequestBody Question question) {
        return questionService.create(question);
    }

    @GetMapping
    public Flux<Question> findAll() {
        return questionService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<Question> findById(@PathVariable String id) {
        return questionService.findById(id);
    }

    @PutMapping("/{id}")
    public Mono<Question> update(@PathVariable String id, @RequestBody Question question) {
        return questionService.update(id, question);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable String id) {
        return questionService.delete(id);
    }

    @GetMapping("/{id}/answer-ids")
    public Flux<String> getAnswerIds(@PathVariable String id) {
        return questionService.getAnswerIdsByQuestionId(id);
    }
}
