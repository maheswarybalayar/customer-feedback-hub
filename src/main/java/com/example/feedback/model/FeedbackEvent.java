package com.example.feedback.model;

/** The message we put on the feedback.received topic. */
public record FeedbackEvent(
        String id,
        String customerName,
        String email,
        String product,
        int rating,
        String comment,
        String source,
        String receivedAt) {
}
