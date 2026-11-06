package com.example.feedback.service;

import com.example.feedback.model.FeedbackRequest;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FeedbackValidator {

    private final Validator validator;

    public FeedbackValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(FeedbackRequest request) {
        if (request == null) {
            throw new FeedbackValidationException(List.of("request body is required"));
        }
        List<String> errors = validator.validate(request).stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
        if (!errors.isEmpty()) {
            throw new FeedbackValidationException(errors);
        }
    }
}
