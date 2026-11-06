package com.example.feedback.service;

import com.example.feedback.model.ProcessedFeedback;
import org.springframework.stereotype.Component;

/** Builds the text of the alert file the support team reads. */
@Component
public class AlertFormatter {

    public String format(ProcessedFeedback feedback) {
        return """
                NEGATIVE FEEDBACK ALERT
                -----------------------
                Feedback id : %s
                Customer    : %s <%s>
                Product     : %s
                Rating      : %d / 5
                Comment     : %s
                Received at : %s
                Source      : %s
                """.formatted(feedback.id(), feedback.customerName(), feedback.email(), feedback.product(),
                feedback.rating(), feedback.comment(), feedback.receivedAt(), feedback.source());
    }
}
