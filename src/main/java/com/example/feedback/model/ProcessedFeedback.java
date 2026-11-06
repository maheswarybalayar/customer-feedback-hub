package com.example.feedback.model;

/** A feedback after the consumer has classified it. This is what we store and return from GET. */
public record ProcessedFeedback(
        String id,
        String customerName,
        String email,
        String product,
        int rating,
        String comment,
        String source,
        Sentiment sentiment,
        String receivedAt,
        String processedAt) {

    public static ProcessedFeedback from(FeedbackEvent event, Sentiment sentiment, String processedAt) {
        return new ProcessedFeedback(event.id(), event.customerName(), event.email(), event.product(),
                event.rating(), event.comment(), event.source(), sentiment, event.receivedAt(), processedAt);
    }
}
