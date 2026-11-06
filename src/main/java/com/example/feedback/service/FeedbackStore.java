package com.example.feedback.service;

import com.example.feedback.model.FeedbackSummary;
import com.example.feedback.model.ProcessedFeedback;
import com.example.feedback.model.Sentiment;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
import com.example.feedback.entity.FeedbackEntity;
import com.example.feedback.repository.FeedbackRepository;

/**
 * In-memory store for processed customer feedback.
 *
 * Day 3:
 * - Save processed feedback and find records by ID.
 * - Return all feedback sorted by processing time, newest first.
 * - Calculate the total count, sentiment counts, and average rating.
 *
 * ConcurrentHashMap makes the store thread-safe for concurrent access.
 * This in-memory implementation is suitable for the POC and can be
 * replaced with a database as a future enhancement.
 */
@Component
public class FeedbackStore {

	//private final Map<String, ProcessedFeedback> items = new ConcurrentHashMap<>();
	private final FeedbackRepository repository;
	
	public FeedbackStore(FeedbackRepository repository) {
	    this.repository = repository;
	}

    public void save(ProcessedFeedback feedback) {
        //items.put(feedback.id(), feedback);
    	FeedbackEntity entity = FeedbackEntity.from(feedback);
        repository.save(entity);
    }

    public Optional<ProcessedFeedback> findById(String id) {
        //return Optional.ofNullable(items.get(id));
    	  return repository.findById(id)
    	            .map(FeedbackEntity::toModel);
    }
    
    public List<ProcessedFeedback> findByProduct(String product){
    	return repository.findByProductIgnoreCase(product).stream()
    			.map(FeedbackEntity::toModel)
    			.sorted(Comparator.comparing(ProcessedFeedback::processedAt).reversed())
    			.toList();
    }

    public List<ProcessedFeedback> findAll() {
//        return items.values().stream()
//                .sorted(Comparator.comparing(ProcessedFeedback::processedAt).reversed())
//                .toList();
    	return repository.findAll().stream()
                .map(FeedbackEntity::toModel)
                .sorted(Comparator.comparing(ProcessedFeedback::processedAt).reversed())
                .toList();
    }
    // Check whether feedback has already been processed.
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    /**
     * Day 3: Calculate feedback totals, counts for each sentiment,
     * and the average rating rounded to two decimal places.
     * Return 0.0 as the average when the store is empty.
     */
    public FeedbackSummary summary() {
    	// Day 3: Get all feedback and calculate the required statistics.
    	List<ProcessedFeedback> all = findAll();

    	long total = all.size();
    	long positive = count(all, Sentiment.POSITIVE);
    	long neutral = count(all, Sentiment.NEUTRAL);
    	long negative = count(all, Sentiment.NEGATIVE);
    	
    	// Day 3: Calculate the average rating and round it to two decimals.
    	double averageRating = all.stream()
    	        .mapToInt(ProcessedFeedback::rating)
    	        .average()
    	        .orElse(0.0);

    	averageRating = Math.round(averageRating * 100.0) / 100.0;

    	return new FeedbackSummary(
    	        total, positive, neutral, negative, averageRating
    	);
    }
    // Day 3: Count feedback records matching the given sentiment.
    private static long count(List<ProcessedFeedback> all, Sentiment sentiment) {
        return all.stream().filter(f -> f.sentiment() == sentiment).count();
    }
}
