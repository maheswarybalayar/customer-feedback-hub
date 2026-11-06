package com.example.feedback.routes;

import com.example.feedback.service.CsvLineMapper;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import com.example.feedback.service.FeedbackValidationException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;

/**
 * Camel route for importing customer feedback from CSV files.
 *
 * Day 4:
 * - Read CSV files from the configured inbox directory.
 * - Process each line separately and skip the header and blank lines.
 * - Convert CSV lines into feedback requests.
 * - Skip invalid feedback lines and log a warning.
 * - Move processed files to .done and failed files to .error.
 *
 * The inbox directory is configured in application.yml.
 * This class uses CsvLineMapper to convert CSV lines into
 * feedback requests and delegates submission to the shared
 * Camel submission route.
 */
@Component
public class CsvIntakeRoute extends RouteBuilder {

	private final CsvLineMapper mapper;

	public CsvIntakeRoute(CsvLineMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public void configure() {
		// Day 4: Log invalid CSV lines and continue processing.
		onException(FeedbackValidationException.class).handled(true).log(LoggingLevel.WARN,
				"Skipped CSV line: ${exception.message}");
		// Day 4: Read CSV files and move them after processing.
		from("file:{{app.intake.inbox-dir}}?antInclude=*.csv&move=.done&moveFailed=.error")
				// Mark the source so the shared route knows this feedback came from CSV.
				.setProperty(RouteNames.SOURCE, constant("CSV"))
				// Split the file into individual lines and process them one at a time.
				.split(body().tokenize("\n")).streaming()
				// Day 4: Skip the header and any blank lines.
				.filter(exchange -> exchange.getProperty(Exchange.SPLIT_INDEX, Integer.class) > 0
						&& exchange.getMessage().getBody(String.class) != null
						&& !exchange.getMessage().getBody(String.class).trim().isEmpty())
				// Convert each CSV line into a request and submit it for processing.
				.process(exchange -> {
					String line = exchange.getMessage().getBody(String.class);
					exchange.getMessage().setBody(mapper.toRequest(line));
				})
				.to(RouteNames.SUBMIT)
				.end()
				.end();
	}
}
