package com.example.feedback.model;

public record FeedbackSummary(long total, long positive, long neutral, long negative, double averageRating) {
}
