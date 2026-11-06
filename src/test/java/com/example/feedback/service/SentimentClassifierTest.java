package com.example.feedback.service;

import com.example.feedback.model.Sentiment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SentimentClassifierTest {

    private final SentimentClassifier classifier = new SentimentClassifier();

    @ParameterizedTest(name = "rating {0}, comment ''{1}'' -> {2}")
    @CsvSource({
            "5, Loved it,                    POSITIVE",
            "4, '',                          POSITIVE",
            "3, It was okay,                 NEUTRAL",
            "3, Arrived LATE again,          NEGATIVE",
            "3, I want a refund,             NEGATIVE",
            "2, Not great,                   NEGATIVE",
            "1, '',                          NEGATIVE",
            "5, Wanted a refund but got fixed, POSITIVE"
    })
    void classify_appliesRatingAndKeywordRules(int rating, String comment, Sentiment expected) {
        assertThat(classifier.classify(rating, comment)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"3, NEUTRAL"})
    void classify_handlesNullComment(int rating, Sentiment expected) {
        assertThat(classifier.classify(rating, null)).isEqualTo(expected);
    }
}
