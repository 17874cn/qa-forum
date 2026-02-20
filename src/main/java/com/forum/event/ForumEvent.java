package com.forum.event;

import java.time.Instant;

public class ForumEvent {

    public enum EventType {
        QUESTION_CREATED,
        ANSWER_CREATED,
        ANSWER_ACCEPTED,
        ANSWER_VOTED
    }

    private EventType eventType;
    private String questionId;
    private String answerId;
    private String authorId;
    private String questionAuthorId;
    private String title;
    private String body;
    private Instant timestamp;

    public ForumEvent() {
        this.timestamp = Instant.now();
    }

    public ForumEvent(EventType eventType) {
        this.eventType = eventType;
        this.timestamp = Instant.now();
    }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getAnswerId() { return answerId; }
    public void setAnswerId(String answerId) { this.answerId = answerId; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getQuestionAuthorId() { return questionAuthorId; }
    public void setQuestionAuthorId(String questionAuthorId) { this.questionAuthorId = questionAuthorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
