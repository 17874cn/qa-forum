package com.forum.config;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Configuration
public class ConcurrencyConfig {

    @Value("${forum.threadPool.questionFetch:5}")
    private int questionFetchPoolSize;

    @Value("${forum.threadPool.answerFetch:5}")
    private int answerFetchPoolSize;

    @Value("${forum.threadPool.searchProcessing:3}")
    private int searchProcessingPoolSize;

    @Value("${forum.threadPool.voteCalculation:3}")
    private int voteCalculationPoolSize;

    @Value("${forum.threadPool.batchProcessing:5}")
    private int batchProcessingPoolSize;

    @Bean(name = "questionFetchScheduler")
    public Scheduler questionFetchScheduler() {
        return Schedulers.fromExecutor(
                Executors.newFixedThreadPool(questionFetchPoolSize)
        );
    }

    @Bean(name = "answerFetchScheduler")
    public Scheduler answerFetchScheduler() {
        return Schedulers.fromExecutor(
                Executors.newFixedThreadPool(answerFetchPoolSize)
        );
    }

    @Bean(name = "searchProcessingScheduler")
    public Scheduler searchProcessingScheduler() {
        return Schedulers.fromExecutor(
                Executors.newFixedThreadPool(searchProcessingPoolSize)
        );
    }

    @Bean(name = "voteCalculationScheduler")
    public Scheduler voteCalculationScheduler() {
        return Schedulers.fromExecutor(
                Executors.newFixedThreadPool(voteCalculationPoolSize)
        );
    }

    @Bean(name = "batchTaskExecutor")
    public ExecutorService batchTaskExecutor() {
        return Executors.newFixedThreadPool(batchProcessingPoolSize);
    }

    @PreDestroy
    public void cleanup() {
    }
}
