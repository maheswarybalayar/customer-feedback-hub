package com.example.feedback;

import com.example.feedback.model.FeedbackRequest;
import com.example.feedback.model.ProcessedFeedback;
import com.example.feedback.model.Sentiment;
import com.example.feedback.service.FeedbackStore;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Same flow over REAL Kafka: @EmbeddedKafka starts a broker inside the test JVM (no Docker).
 * No profile is active, so the kafka: endpoints from application.yml are used.
 */
@CamelSpringBootTest
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=18092",
                "app.intake.inbox-dir=target/it-inbox",
                "app.alerts.output-dir=target/it-alerts"
        })
@EmbeddedKafka(
        partitions = 1,
        topics = {"feedback.received", "feedback.alerts", "feedback.dlq"},
        bootstrapServersProperty = "app.kafka.brokers")
@DirtiesContext
@SuppressWarnings("rawtypes")
class FeedbackKafkaIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired FeedbackStore store;

    @Test
    void negativeFeedback_travelsThroughBothTopics() {
        var response = rest.postForEntity("/api/feedback",
                new FeedbackRequest("Kiran", "kiran@example.com", "Mixer", 2, "Motor burnt out"), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String id = (String) response.getBody().get("id");

        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> assertThat(store.findById(id))
                .map(ProcessedFeedback::sentiment).hasValue(Sentiment.NEGATIVE));
        await().atMost(Duration.ofSeconds(60))
                .until(() -> Files.exists(Path.of("target/it-alerts/alert-" + id + ".txt")));
    }
}
