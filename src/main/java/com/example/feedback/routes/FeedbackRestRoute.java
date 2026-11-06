package com.example.feedback.routes;

import com.example.feedback.service.FeedbackStore;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import com.example.feedback.model.ErrorResponse;
import com.example.feedback.model.FeedbackRequest;
import com.example.feedback.model.SubmitResponse;
import com.example.feedback.service.FeedbackValidationException;
import org.apache.camel.Exchange;
import org.apache.camel.model.rest.RestBindingMode;

/**
 * REST API routes for the Customer Feedback Hub.
 *
 * Day 2:
 * - Accept feedback through POST /api/feedback.
 * - Return HTTP 202 when submission succeeds.
 * - Return HTTP 400 when feedback validation fails.
 *
 * Day 3:
 * - GET /api/feedback         : List all processed feedback.
 * - GET /api/feedback/summary : Return feedback statistics.
 * - GET /api/feedback/{id}    : Find feedback by ID or return HTTP 404.
 *
 * The /api prefix is configured in application.yml.
 * This class delegates feedback submission to the shared Camel
 * submission route and uses FeedbackStore for GET operations.
 */

@Component
public class FeedbackRestRoute extends RouteBuilder {

	private final FeedbackStore store;

	public FeedbackRestRoute(FeedbackStore store) {
		this.store = store;
	}

	@Override
	public void configure() {
        // Day 2: Return validation errors to the client with HTTP 400.
		onException(FeedbackValidationException.class).handled(true)
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400)).process(exchange -> {
					FeedbackValidationException ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
							FeedbackValidationException.class);

					exchange.getMessage().setBody(new ErrorResponse(ex.getErrors()));
				}).end();
		
        // Configure REST endpoints to use JSON through the Servlet component.
		restConfiguration()
				.component("servlet")
				.bindingMode(RestBindingMode.json)
				.host("localhost")
				.port(8080)
				.apiHost("localhost:8080")
				.apiContextPath("/api-doc")
				.contextPath("/api")
				.apiProperty("api.title", "Customer Feedback Hub API")
				.apiProperty("api.version", "1.0.0")
				.apiProperty("api.description","REST APIs for submitting and retrieving customer feedback");;
		
        // Day 2: Accept feedback submissions; Day 3: expose the GET endpoints.
		rest("/feedback").post().type(FeedbackRequest.class).to("direct:rest-submit").get().to("direct:rest-list")
				.get("/summary").to("direct:rest-summary").get("/product/{product}").to("direct:rest-product").get("/{id}").to("direct:rest-find");
		
        // Day 2: Pass REST requests to the shared submission route and return HTTP 202.
		from("direct:rest-submit")
				.log("REST submission received")
				.setProperty(RouteNames.SOURCE, constant("REST"))
				.to(RouteNames.SUBMIT)
				.process(exchange -> {
					String id = exchange.getProperty(RouteNames.FEEDBACK_ID, String.class);
					exchange.getMessage().setBody(new SubmitResponse(id, "ACCEPTED"));
					exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 202);
				});
		
		 // Day 3: Return all feedback currently available in the store.
		from("direct:rest-list")	
				.process(exchange -> exchange.getMessage().setBody(store.findAll()));
		
		// Return feedback filtered by product name.
		from("direct:rest-product")
		        .process(exchange -> {
		            String product = exchange.getMessage().getHeader("product", String.class);
		            exchange.getMessage().setBody(store.findByProduct(product));
		        });
		 // Day 3: Return the overall feedback summary.
		from("direct:rest-summary")
				.process(exchange -> exchange.getMessage().setBody(store.summary()));
		
		// Day 3: Find feedback by ID, or return HTTP 404 if it is missing.
		from("direct:rest-find")
				.process(exchange -> {
					String id = exchange.getMessage().getHeader("id", String.class);
					var feedback = store.findById(id);
					if (feedback.isPresent()) {
						exchange.getMessage().setBody(feedback.get());
					} else {
						exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);

						exchange.getMessage().setBody(new ErrorResponse(java.util.List.of("Feedback not found: " + id)));
					}
				});
	}
}
