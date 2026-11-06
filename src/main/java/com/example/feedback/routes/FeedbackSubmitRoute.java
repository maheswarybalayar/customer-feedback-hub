package com.example.feedback.routes;

import com.example.feedback.service.FeedbackFactory;
import com.example.feedback.service.FeedbackValidator;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import com.example.feedback.model.FeedbackRequest;
import com.example.feedback.model.FeedbackEvent;
import org.apache.camel.Exchange;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
/**
 * DAY 2 TASK: the Kafka PRODUCER route, shared by REST (Day 2) and CSV (Day 4).
 *
 * from(RouteNames.SUBMIT)     body arrives as a FeedbackRequest
 *   1. validator.validate(request)                      (throws FeedbackValidationException)
 *   2. event = factory.toEvent(request, <property RouteNames.SOURCE>)
 *   3. save event.id() in property RouteNames.FEEDBACK_ID, set body = event
 *   4. .marshal().json(JsonLibrary.Jackson) then .convertBodyTo(String.class)
 *   5. .removeHeaders("*")  so HTTP headers are not copied into Kafka
 *   6. header KafkaConstants.KEY = the feedback id   (same key -> same partition)
 *   7. .to("{{app.endpoints.received-out}}")         never hard-code "kafka:..." in the route
 *
 * Keep errorHandler(noErrorHandler()) so exceptions go back to the calling route.
 *
 * Check yourself: POST something, then open Kafka UI (http://localhost:8090) -> topic feedback.received -> Messages
 */
@Component
public class FeedbackSubmitRoute extends RouteBuilder {

    private final FeedbackValidator validator;
    private final FeedbackFactory factory;

    public FeedbackSubmitRoute(FeedbackValidator validator, FeedbackFactory factory) {
        this.validator = validator;
        this.factory = factory;
    }

    @Override
    public void configure() {
    	// Day 2: Let exceptions return to the calling route.
        errorHandler(noErrorHandler());
        // Day 2: Validate the request, create the event, and save its ID.
        from(RouteNames.SUBMIT)
        .process(exchange -> {
            FeedbackRequest request =
                    exchange.getIn().getBody(FeedbackRequest.class);

            validator.validate(request);

            String source = exchange.getProperty(
                    RouteNames.SOURCE, String.class);

            FeedbackEvent event = factory.toEvent(request, source);

            exchange.setProperty(RouteNames.FEEDBACK_ID, event.id());
            exchange.getIn().setBody(event);
        })
        // Day 2: Convert the event to JSON before sending it to Kafka.
        .marshal().json(JsonLibrary.Jackson)
        .convertBodyTo(String.class)
        // Day 2: Remove incoming headers so they aren't sent to Kafka.
        .removeHeaders("*")
        // Day 2: Use the feedback ID as the Kafka message key.
        .setHeader(KafkaConstants.KEY, exchangeProperty(RouteNames.FEEDBACK_ID))
        // Day 2: Publish the event using the configured Kafka endpoint.
        .to("{{app.endpoints.received-out}}");
    }
}
