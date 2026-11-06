package com.example.feedback.service;

import com.example.feedback.model.Sentiment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Classifies customer feedback based on its rating and comment.
 *
 * Day 3:
 * - Mark ratings 4–5 as POSITIVE.
 * - Mark ratings 1–2 as NEGATIVE.
 * - Check neutral-rating comments for complaint keywords.
 * - Treat comments containing complaint keywords as NEGATIVE,
 *   ignoring uppercase and lowercase differences.
 *
 * A null comment is allowed. This class applies the sentiment
 * classification rules used by the feedback processing route.
 */
@Component
public class SentimentClassifier {

    static final List<String> COMPLAINT_KEYWORDS = List.of("refund", "broken", "damaged", "late", "worst");

    public Sentiment classify(int rating, String comment) {
    	// Day 3: Ratings of 4 or 5 indicate positive feedback.
    	if (rating >= 4) {
    	    return Sentiment.POSITIVE;
    	}
    	
    	// Day 3: Ratings of 1 or 2 indicate negative feedback.
    	if (rating <= 2) {
    	    return Sentiment.NEGATIVE;
    	}
    	
    	// Day 3: Check neutral-rating comments for complaint keywords.
    	if (comment != null) {
    	    String normalizedComment = comment.toLowerCase();

    	    for (String keyword : COMPLAINT_KEYWORDS) {
    	        if (normalizedComment.contains(keyword)) {
    	            return Sentiment.NEGATIVE;
    	        }
    	    }
    	}
    	
    	// Day 3: Return neutral when no complaint keyword is found.
    	return Sentiment.NEUTRAL;
    }
}
