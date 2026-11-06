package com.example.feedback.routes;

import com.example.feedback.service.FeedbackStore;
import com.example.feedback.service.SentimentClassifier;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import com.example.feedback.model.FeedbackEvent;
import com.example.feedback.model.ProcessedFeedback;
import com.example.feedback.model.Sentiment;
import org.apache.camel.model.dataformat.JsonLibrary;

import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.Exchange;

/**
 * Kafka consumer route for processing customer feedback.
 *
 * Day 3: - Consume feedback events from Kafka. - Classify sentiment and create
 * processed feedback records. - Save processed feedback and log the result.
 *
 * Day 4: - Send negative feedback to the configured alerts Kafka endpoint.
 *
 * Day 5: - Send failed messages to the dead letter topic. - Add an errorReason
 * header to help identify the failure.
 *
 * Kafka endpoints are configured in application.yml. This class uses
 * SentimentClassifier to classify feedback, FeedbackStore to save processed
 * records, and Clock to set the processing timestamp.
 */
@Component
public class FeedbackProcessingRoute extends RouteBuilder {

	private final SentimentClassifier classifier;
	private final FeedbackStore store;
	private final Clock clock;

	public FeedbackProcessingRoute(SentimentClassifier classifier, FeedbackStore store, Clock clock) {
		this.classifier = classifier;
		this.store = store;
		this.clock = clock;
	}

	@Override
	public void configure() {
		// Day 5: Send failed messages to the dead letter topic without retrying.
		errorHandler(deadLetterChannel("{{app.endpoints.dlq-out}}").useOriginalMessage().maximumRedeliveries(0)
				.onPrepareFailure(exchange -> {
					Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

					exchange.getMessage().setHeader("errorReason",
							exception != null ? exception.getMessage() : "Unknown error");
				}));
		// Day 3: Consume feedback events from the configured Kafka endpoint.
		from("{{app.endpoints.received-in}}")
				// Day 3: Convert each JSON message into a FeedbackEvent.
				.unmarshal().json(JsonLibrary.Jackson, FeedbackEvent.class).process(exchange -> {
					FeedbackEvent event = exchange.getMessage().getBody(FeedbackEvent.class);

					// Check whether this feedback was already processed.
					if (store.existsById(event.id())) {
						exchange.setProperty("duplicateFeedback", true);
						exchange.getMessage().setBody(event);
						return;
					}

					exchange.setProperty("duplicateFeedback", false);

					// Day 3: Classify sentiment, create the processed record, and save it.
					Sentiment sentiment = classifier.classify(event.rating(), event.comment());

					ProcessedFeedback processed = ProcessedFeedback.from(event, sentiment, clock.instant().toString());

					store.save(processed);

					exchange.setProperty(RouteNames.FEEDBACK_ID, processed.id());
					exchange.setProperty(RouteNames.SENTIMENT, sentiment);

					exchange.getMessage().setBody(processed);
				})
				// Day 4: Send negative feedback to the alerts topic.
				.choice()
			    	.when(exchangeProperty("duplicateFeedback").isEqualTo(true))
			        	.log("Duplicate feedback id=${body.id}; skipping processing")
			        .otherwise()
			        	.log("Processed feedback id=${body.id}, sentiment=${body.sentiment}")
			        	.choice()
			            	.when(exchangeProperty(RouteNames.SENTIMENT)
			            		.isEqualTo(Sentiment.NEGATIVE))
			                	.marshal().json(JsonLibrary.Jackson)
			                	.convertBodyTo(String.class)
			                	.removeHeaders("*")
			                	.setHeader(KafkaConstants.KEY, exchangeProperty(RouteNames.FEEDBACK_ID))
			                	.to("{{app.endpoints.alerts-out}}")
			             .end()
			    .end();
	}
}
