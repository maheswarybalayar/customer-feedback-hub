package com.example.feedback.service;

import com.example.feedback.model.FeedbackRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackValidatorTest {

    private final FeedbackValidator validator =
            new FeedbackValidator(Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void validate_acceptsGoodRequest() {
        assertThatCode(() -> validator.validate(
                new FeedbackRequest("Asha", "asha@example.com", "Blender", 5, "Great")))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_listsEveryProblem() {
        assertThatThrownBy(() -> validator.validate(new FeedbackRequest("", "not-an-email", "", 9, null)))
                .isInstanceOf(FeedbackValidationException.class)
                .satisfies(ex -> assertThat(((FeedbackValidationException) ex).getErrors())
                        .anyMatch(e -> e.startsWith("customerName"))
                        .anyMatch(e -> e.startsWith("email"))
                        .anyMatch(e -> e.startsWith("product"))
                        .anyMatch(e -> e.startsWith("rating")));
    }
}
