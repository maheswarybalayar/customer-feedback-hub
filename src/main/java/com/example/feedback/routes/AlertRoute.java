package com.example.feedback.routes;

import com.example.feedback.service.AlertFormatter;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import com.example.feedback.model.ProcessedFeedback;
import org.apache.camel.Exchange;
import org.apache.camel.model.dataformat.JsonLibrary;

/**
 * Camel route for creating alerts from negative customer feedback.
 *
 * Day 4:
 * - Consume processed feedback from the configured Kafka endpoint.
 * - Convert JSON messages into ProcessedFeedback objects.
 * - Format each feedback as a text alert file.
 * - Log a warning when an alert file is created.
 *
 * The Kafka endpoint and alert output directory are configured
 * in application.yml. This class uses AlertFormatter to prepare
 * the alert content.
 */
@Component
public class AlertRoute extends RouteBuilder {

    private final AlertFormatter formatter;

    public AlertRoute(AlertFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void configure() {
    	//For Retry mechanism - DLQ
    	errorHandler(
    		    deadLetterChannel("{{app.endpoints.dlq-out}}")
    		        .useOriginalMessage()
    		        .maximumRedeliveries(2)
    		        .redeliveryDelay(1000)
    		        .onRedelivery(exchange -> {
    		            Integer retryCount = exchange.getIn()
    		                .getHeader(Exchange.REDELIVERY_COUNTER, Integer.class);
    		            exchange.setProperty("alertRetryCount", retryCount);
    		        })
    		        .onPrepareFailure(exchange -> {
    		            Exception exception = exchange.getProperty(
    		                Exchange.EXCEPTION_CAUGHT, Exception.class);

    		            exchange.getMessage().setHeader(
    		                "errorReason",
    		                exception != null ? exception.getMessage() : "Unknown error");
    		            Integer retryCount = exchange.getProperty(
    		                    "alertRetryCount", Integer.class);

    		            exchange.getMessage().setHeader("retryCount", retryCount);
    		        })
    		);
    	 // Day 4: Read feedback messages from the configured alerts endpoint.
    	 from("{{app.endpoints.alerts-in}}")
    	 // Day 4: Convert the JSON message into a ProcessedFeedback object.
         .unmarshal().json(JsonLibrary.Jackson, ProcessedFeedback.class)
         .process(exchange -> {
             ProcessedFeedback feedback =
                     exchange.getMessage().getBody(ProcessedFeedback.class);
             // Day 4: Set the alert file name and prepare its content.
             exchange.getMessage().setHeader(
                     Exchange.FILE_NAME,
                     "alert-" + feedback.id() + ".txt");

             exchange.getMessage().setBody(formatter.format(feedback));
         })
         // Day 4: Write the alert file to the configured output directory.
         .to("file:{{app.alerts.output-dir}}")
         // Day 4: Log the name of the created alert file.
         .log("WARN: Alert file created: ${header.CamelFileName}");
    }
}
