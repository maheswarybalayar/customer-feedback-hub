package com.example.feedback.service;

import com.example.feedback.model.FeedbackEvent;
import com.example.feedback.model.FeedbackRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/** Turns a request into the event we publish: adds an id, the source channel and a timestamp. */
@Component
public class FeedbackFactory {

    private final Clock clock;

    public FeedbackFactory(Clock clock) {
        this.clock = clock;
    }

    public FeedbackEvent toEvent(FeedbackRequest request, String source) {
        return new FeedbackEvent(
                UUID.randomUUID().toString(),
                request.customerName().trim(),
                request.email().trim(),
                request.product().trim(),
                request.rating(),
                request.comment() == null ? "" : request.comment().trim(),
                source,
                clock.instant().toString());
    }
}
