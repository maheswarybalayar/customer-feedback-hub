package com.example.feedback.service;

import java.util.List;

public class FeedbackValidationException extends RuntimeException {

    private final List<String> errors;

    public FeedbackValidationException(List<String> errors) {
        super("Invalid feedback: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
