
package com.example.feedback.entity;

import com.example.feedback.model.Sentiment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.example.feedback.model.ProcessedFeedback;

@Entity
@Table(name = "feedback")
public class FeedbackEntity {

    @Id
    private String id;

    private String customerName;

    private String email;

    private String product;

    private int rating;

    @Column(length = 1000)
    private String comment;

    private String source;

    @Enumerated(EnumType.STRING)
    private Sentiment sentiment;

    private String receivedAt;

    private String processedAt;

    // JPA requires a no-argument constructor.
    protected FeedbackEntity() {
    }
 // Convert application model into a database entity.
    public static FeedbackEntity from(ProcessedFeedback feedback) {

        FeedbackEntity entity = new FeedbackEntity();

        entity.id = feedback.id();
        entity.customerName = feedback.customerName();
        entity.email = feedback.email();
        entity.product = feedback.product();
        entity.rating = feedback.rating();
        entity.comment = feedback.comment();
        entity.source = feedback.source();
        entity.sentiment = feedback.sentiment();
        entity.receivedAt = feedback.receivedAt();
        entity.processedAt = feedback.processedAt();

        return entity;
    }

    // Convert database entity back into application model.
    public ProcessedFeedback toModel() {

        return new ProcessedFeedback(
                id,
                customerName,
                email,
                product,
                rating,
                comment,
                source,
                sentiment,
                receivedAt,
                processedAt
        );
    }
}
