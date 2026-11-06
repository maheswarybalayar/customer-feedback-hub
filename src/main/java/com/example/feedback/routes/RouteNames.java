package com.example.feedback.routes;

/** Names shared between routes, kept in one place to avoid typos. */
public final class RouteNames {

    /** Shared "submit feedback" route used by both REST and CSV. */
    public static final String SUBMIT = "direct:submit";

    /** Exchange property names. Properties stay on the exchange; headers are cleared before Kafka. */
    public static final String SOURCE = "source";
    public static final String FEEDBACK_ID = "feedbackId";
    public static final String SENTIMENT = "sentiment";

    private RouteNames() {
    }
}
