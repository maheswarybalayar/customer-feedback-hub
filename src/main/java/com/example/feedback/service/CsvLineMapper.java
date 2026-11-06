package com.example.feedback.service;

import com.example.feedback.model.FeedbackRequest;
import org.springframework.stereotype.Component;
import java.util.List;
/**
 * Converts CSV lines into customer feedback requests.
 *
 * Day 4:
 * - Read customer details from individual CSV lines.
 * - Trim values and support comments containing commas.
 * - Validate the required columns and rating format.
 * - Throw a FeedbackValidationException for invalid CSV lines.
 *
 * This class prepares feedback requests for the shared Camel
 * submission route, where the requests are validated and processed.
 */
@Component
public class CsvLineMapper {

    public FeedbackRequest toRequest(String line) {
    	// Day 4: Split into at most five columns so comments can contain commas.
    	String[] columns = line.split(",", 5);
    	// Day 4: Reject lines that don't contain the four required columns.
        if (columns.length < 4) {
            throw new FeedbackValidationException(
                    List.of("CSV line must contain at least 4 columns"));
        }
        // Day 4: Extract and trim the customer details.
        String customerName = columns[0].trim();
        String email = columns[1].trim();
        String product = columns[2].trim();
        
        // Day 4: Convert the rating to a number or report an invalid value.
        int rating;
        try {
            rating = Integer.parseInt(columns[3].trim());
        } catch (NumberFormatException e) {
            throw new FeedbackValidationException(
                    List.of("CSV rating must be a number"));
        }
        // Day 4: Treat the comment as optional.
        String comment = columns.length == 5 ? columns[4].trim() : "";
     
        // Day 4: Build the request for the shared submission route.
        return new FeedbackRequest(
                customerName,
                email,
                product,
                rating,
                comment
        );
    }
}
