package com.example.feedback.service;

import com.example.feedback.model.ProcessedFeedback;
import com.example.feedback.model.Sentiment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
//Added aftr jpa addition(database implementation
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(FeedbackStore.class)
class FeedbackStoreTest {

    //private final FeedbackStore store = new FeedbackStore();
	@Autowired
	private FeedbackStore store;
	
    @Test
    void summary_countsSentimentsAndAveragesRatings() {
        store.save(feedback("1", 5, Sentiment.POSITIVE, "2026-01-01T10:00:00Z"));
        store.save(feedback("2", 4, Sentiment.POSITIVE, "2026-01-01T10:01:00Z"));
        store.save(feedback("3", 1, Sentiment.NEGATIVE, "2026-01-01T10:02:00Z"));

        var summary = store.summary();

        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.positive()).isEqualTo(2);
        assertThat(summary.neutral()).isZero();
        assertThat(summary.negative()).isEqualTo(1);
        assertThat(summary.averageRating()).isEqualTo(3.33);
    }

    @Test
    void summary_isZero_whenEmpty() {
        assertThat(store.summary().averageRating()).isZero();
    }

    @Test
    void findAll_returnsNewestFirst() {
        store.save(feedback("old", 3, Sentiment.NEUTRAL, "2026-01-01T09:00:00Z"));
        store.save(feedback("new", 3, Sentiment.NEUTRAL, "2026-01-01T11:00:00Z"));

        assertThat(store.findAll()).extracting(ProcessedFeedback::id).containsExactly("new", "old");
    }

    private static ProcessedFeedback feedback(String id, int rating, Sentiment sentiment, String processedAt) {
        return new ProcessedFeedback(id, "Test", "t@example.com", "Fan", rating, "", "REST",
                sentiment, processedAt, processedAt);
    }
}
