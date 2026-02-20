package com.forum.service;

import com.forum.model.Answer;
import com.forum.model.Question;
import com.forum.repository.AnswerRepository;
import com.forum.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CPU-bound batch processing service using ExecutorService thread pools.
 * Follows the same pattern as StonehengeUtility parallel encryption
 * from the fe-rule-service (invokeAll + Future.get + AtomicInteger).
 */
@Service
public class VoteTallyService {

    private static final Logger log = LoggerFactory.getLogger(VoteTallyService.class);

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final ExecutorService batchTaskExecutor;

    public VoteTallyService(AnswerRepository answerRepository,
                            QuestionRepository questionRepository,
                            @Qualifier("batchTaskExecutor") ExecutorService batchTaskExecutor) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.batchTaskExecutor = batchTaskExecutor;
    }

    /**
     * Recalculate vote scores for a batch of questions in parallel.
     * Uses ExecutorService invokeAll pattern for CPU-bound score computation.
     *
     * Pattern reference: StonehengeUtility.getEncryptedObjectList()
     * - Build list of Callable tasks
     * - Execute all via invokeAll() (parallel)
     * - Wait for completion via Future.get()
     * - Track progress with AtomicInteger (thread-safe)
     */
    public Mono<Map<String, Object>> recalculateVotesForQuestions(List<Question> questions) {
        return Mono.fromCallable(() -> {
            AtomicInteger processedCount = new AtomicInteger(0);
            AtomicInteger totalVotes = new AtomicInteger(0);

            // 1. Build callable tasks (one per question)
            List<Callable<Void>> tasks = new ArrayList<>();

            for (Question question : questions) {
                tasks.add(() -> {
                    // CPU-bound: calculate aggregate vote score
                    List<Answer> answers = answerRepository
                            .findByQuestionId(question.getId())
                            .collectList()
                            .block(); // OK to block inside thread pool thread

                    if (answers != null) {
                        int score = answers.stream()
                                .mapToInt(Answer::getVotes)
                                .sum();
                        totalVotes.addAndGet(score);
                    }

                    processedCount.getAndIncrement(); // Thread-safe increment
                    return null;
                });
            }

            try {
                // 2. Execute all tasks in parallel via thread pool
                List<Future<Void>> futures = batchTaskExecutor.invokeAll(tasks);

                // 3. Wait for all to complete
                for (Future<Void> future : futures) {
                    future.get(); // Blocks until this task completes
                }

            } catch (InterruptedException e) {
                log.error("Vote tally interrupted", e);
                Thread.currentThread().interrupt(); // Restore interrupt status
                throw new RuntimeException("Vote tally interrupted", e);
            } catch (ExecutionException e) {
                log.error("Vote tally execution error", e);
                throw new RuntimeException("Vote tally failed", e);
            }

            log.info("Vote tally complete: processed={}, totalVotes={}",
                    processedCount.get(), totalVotes.get());

            return Map.<String, Object>of(
                    "questionsProcessed", processedCount.get(),
                    "totalVotes", totalVotes.get()
            );
        }).subscribeOn(Schedulers.boundedElastic()); // Run blocking code on bounded elastic
    }
}
