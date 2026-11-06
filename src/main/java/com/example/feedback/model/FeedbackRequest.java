package com.example.feedback.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What a customer sends us, from the REST API or from one CSV line. */
public record FeedbackRequest(
        @NotBlank @Size(max = 80) String customerName,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 60) String product,
        @Min(1) @Max(5) int rating,
        @Size(max = 1000) String comment) {
}
