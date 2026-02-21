# LLM Integration Plan — QA Forum Answer Ranking

## Overview

This document describes how to integrate an LLM (Large Language Model) into the QA Forum
to automatically score and rank answers by quality, combining AI scoring with the existing
vote-based system.

---

## Goal

When a user posts an answer, the LLM will:
1. Read the question and the answer
2. Rate the answer quality from **0 to 10**
3. Provide a short **feedback summary**
4. Store the score in MongoDB

Answers will then be sorted by a **composite score** combining votes, acceptance status,
and the LLM quality score.

---

## Chosen LLM: Ollama (Local)

| Option        | Cost       | Privacy        | Recommended for         |
|---------------|------------|----------------|--------------------------|
| Ollama        | Free       | Fully local    | Development / Self-hosted |
| Claude API    | Pay/token  | Data sent out  | Production (best quality) |
| OpenAI GPT-4o | Pay/token  | Data sent out  | Production (alternative)  |

**We start with Ollama** because:
- No API key required
- Data never leaves your machine
- Easy to swap to Claude/OpenAI later via Spring AI

### Ollama Setup (one-time)

```bash
# Install Ollama
brew install ollama

# Pull the model (Llama 3.2 — ~2GB)
ollama pull llama3.2

# Start Ollama server (runs on http://localhost:11434)
ollama serve
```

---

## Integration Framework: Spring AI

Spring AI is the official Spring framework for AI integration. It supports:
- Ollama, Claude, OpenAI, Gemini, and more
- Easy provider switching (change config, not code)
- Works naturally with Spring Boot and WebFlux

---

## Architecture

```
POST /api/questions/{id}/answers
        │
        ▼
AnswerService.create()
        │  saves answer to MongoDB
        │  publishes ANSWER_CREATED to Kafka
        │
        ▼
KafkaConsumerService (async)
        │  receives ANSWER_CREATED event
        │
        ▼
LlmScoringService.scoreAnswer(answerId)
        │  fetches question + answer from MongoDB
        │  calls Ollama (llama3.2) with scoring prompt
        │  parses JSON response → score + feedback
        │
        ▼
MongoDB: updates Answer with llmScore, llmFeedback, llmScoredAt

GET /api/questions/{id}/answers
        │
        ▼
AnswerService.findByQuestionId()
        │  sorts by compositeScore (descending)
        ▼
Returns ranked answers to client
```

### Key Design Decisions

- **Async via Kafka** — LLM scoring happens after the answer is saved, so the user gets
  an instant response. The score appears in the background.
- **Non-blocking** — LLM call runs on `Schedulers.boundedElastic()` to avoid blocking
  the reactive event loop.
- **Fail-safe** — If the LLM call fails, the answer is still saved with `llmScore = null`.
  It will appear at the bottom of the ranked list until rescored.

---

## Composite Ranking Formula

```
compositeScore = (votes × 2)
               + (accepted ? 10 : 0)
               + (llmScore != null ? llmScore : 0)
```

| Signal       | Weight | Reason                                      |
|--------------|--------|---------------------------------------------|
| votes        | × 2    | Community-validated quality                 |
| accepted     | +10    | Definitively correct answer                 |
| llmScore     | 0–10   | AI-assessed clarity, correctness, relevance |

### Example

| Answer | Votes | Accepted | LLM Score | Composite |
|--------|-------|----------|-----------|-----------|
| A      | 5     | yes      | 8.5       | 28.5      |
| B      | 8     | no       | 6.0       | 22.0      |
| C      | 3     | no       | 9.0       | 15.0      |

---

## Files to Change

### 1. `build.gradle.kts`
Add Spring AI BOM and Ollama starter dependency.

```kotlin
dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
    }
}

dependencies {
    implementation("org.springframework.ai:spring-ai-ollama-spring-boot-starter")
}
```

---

### 2. `application.yml`
Add Ollama and LLM configuration.

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: llama3.2
        options:
          temperature: 0.1   # low = consistent, deterministic scores

forum:
  llm:
    scoring:
      enabled: true
      max-tokens: 200
```

---

### 3. `Answer.java` — New Fields

```java
private Double llmScore;       // 0.0 to 10.0
private String llmFeedback;    // short justification from LLM
private Instant llmScoredAt;   // when it was scored
```

---

### 4. `LlmScoringService.java` — New Service

Core logic:
1. Fetch the `Answer` and its parent `Question` from MongoDB
2. Build a structured prompt
3. Call Ollama via Spring AI `ChatClient`
4. Parse JSON response into `score` and `feedback`
5. Save updated `Answer` back to MongoDB

**Prompt sent to LLM:**
```
You are a forum answer quality reviewer.
Rate the following answer from 0 to 10 based on:
- Correctness and accuracy
- Completeness
- Clarity and readability
- Relevance to the question

Question: <question body>
Answer: <answer body>

Respond ONLY with valid JSON in this format:
{"score": 7.5, "feedback": "Clear and accurate, but missing edge cases."}
```

---

### 5. `KafkaConsumerService.java` — Trigger on ANSWER_CREATED

```java
case ANSWER_CREATED -> {
    notificationService.handleAnswerCreated(event);
    llmScoringService.scoreAnswer(event.getAnswerId()).subscribe();
}
```

---

### 6. `AnswerService.java` — Sort by Composite Score

```java
public Flux<Answer> findByQuestionId(String questionId) {
    return answerRepository.findByQuestionId(questionId)
        .publishOn(answerFetchScheduler)
        .sort(Comparator.comparingDouble(this::compositeScore).reversed());
}

private double compositeScore(Answer a) {
    return (a.getVotes() * 2.0)
         + (a.isAccepted() ? 10.0 : 0.0)
         + (a.getLlmScore() != null ? a.getLlmScore() : 0.0);
}
```

---

### 7. `AnswerController.java` — Manual Rescore Endpoint

Allows re-triggering LLM scoring on demand (e.g. if Ollama was down when answer was created).

```
PUT /api/answers/{id}/score
```

---

## API Response Changes

`GET /api/questions/{id}/answers` will return answers with new fields:

```json
[
  {
    "id": "abc123",
    "body": "You should use a HashMap for O(1) lookup...",
    "votes": 5,
    "accepted": false,
    "llmScore": 8.5,
    "llmFeedback": "Clear explanation with correct time complexity analysis.",
    "llmScoredAt": "2026-02-20T18:00:00Z"
  }
]
```

---

## Error Handling

| Scenario                  | Behaviour                                          |
|---------------------------|----------------------------------------------------|
| Ollama not running        | Answer saved, `llmScore = null`, warning logged    |
| LLM returns invalid JSON  | Score parse fails, `llmScore = null`, error logged |
| LLM timeout               | Retry once, then skip scoring                      |
| Score out of range        | Clamped to 0–10                                    |

---

## Swapping to Claude / OpenAI Later

Only two changes needed:

**`build.gradle.kts`** — swap starter:
```kotlin
// Remove:
implementation("org.springframework.ai:spring-ai-ollama-spring-boot-starter")
// Add:
implementation("org.springframework.ai:spring-ai-anthropic-spring-boot-starter")
```

**`application.yml`** — swap config:
```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        model: claude-opus-4-6
```

No Java code changes required.

---

## Summary of New Files / Changes

| File                        | Action   | Purpose                          |
|-----------------------------|----------|----------------------------------|
| `build.gradle.kts`          | Modified | Add Spring AI + Ollama           |
| `application.yml`           | Modified | Ollama config                    |
| `Answer.java`               | Modified | Add llmScore, llmFeedback fields |
| `LlmScoringService.java`    | **New**  | Core LLM scoring logic           |
| `KafkaConsumerService.java` | Modified | Trigger scoring on ANSWER_CREATED|
| `AnswerService.java`        | Modified | Composite sort + rescore method  |
| `AnswerController.java`     | Modified | Add PUT /answers/{id}/score      |
