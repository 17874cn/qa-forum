package com.forum.controller;

import com.forum.model.Answer;
import com.forum.service.AnswerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class  AnswerController {

    private final AnswerService answerService;

    public AnswerController(AnswerService answerService) {
        this.answerService = answerService;
    }

    @PostMapping("/questions/{questionId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Answer> create(@PathVariable String questionId, @RequestBody Answer answer) {
        return answerService.create(questionId, answer);
    }

    @GetMapping("/questions/{questionId}/answers")
    public Flux<Answer> findByQuestion(@PathVariable String questionId) {
        return answerService.findByQuestionId(questionId);
    }

    @PutMapping("/answers/{id}/vote")
    public Mono<Answer> vote(@PathVariable String id, @RequestBody Map<String, Integer> body) {
        int delta = body.getOrDefault("delta", 1);
        return answerService.vote(id, delta);
    }

    @PutMapping("/answers/{id}/accept")
    public Mono<Answer> accept(@PathVariable String id) {
        return answerService.accept(id);
    }
}
