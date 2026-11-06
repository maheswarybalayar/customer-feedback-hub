package com.example.feedback;

import com.example.feedback.model.FeedbackRequest;
import com.example.feedback.model.ProcessedFeedback;
import com.example.feedback.model.Sentiment;
import com.example.feedback.service.FeedbackStore;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end test of all routes. Profile "test" replaces Kafka with in-memory SEDA queues,
 * so this runs on any laptop without Docker.
 */
@CamelSpringBootTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes", "unchecked"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FeedbackFlowTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    @Autowired TestRestTemplate rest;
    @Autowired FeedbackStore store;
    @Autowired CamelContext camelContext;
    @Autowired ProducerTemplate producer;

    @Test
    void positiveFeedback_isAcceptedAndStored() {
        ResponseEntity<Map> response = submit(new FeedbackRequest("Asha", "asha@example.com", "Blender", 5, "Great"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String id = (String) response.getBody().get("id");
        assertThat(id).isNotBlank();

        await().atMost(TIMEOUT).untilAsserted(() -> {
            ResponseEntity<Map> found = rest.getForEntity("/api/feedback/" + id, Map.class);
            assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(found.getBody()).containsEntry("sentiment", "POSITIVE").containsEntry("source", "REST");
        });
    }

    @Test
    void invalidFeedback_returns400WithAllErrors() {
        ResponseEntity<Map> response = submit(new FeedbackRequest("", "bad", "Fan", 0, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((List<String>) response.getBody().get("errors"))
                .anyMatch(e -> e.startsWith("customerName"))
                .anyMatch(e -> e.startsWith("email"))
                .anyMatch(e -> e.startsWith("rating"));
    }

    @Test
    void negativeFeedback_writesAlertFile() throws IOException{
        String id = idOf(submit(new FeedbackRequest("Ravi", "ravi@example.com", "Kettle", 1, "Stopped working")));

        Path alert = Path.of("target/test-alerts/alert-" + id + ".txt");
        await().atMost(TIMEOUT).until(() -> Files.exists(alert));
        assertThat(Files.readString(alert)).contains("Kettle").contains("1 / 5");
    }

    @Test
    void neutralRatingWithComplaintKeyword_isTreatedAsNegative() {
        String id = idOf(submit(new FeedbackRequest("Meena", "meena@example.com", "Iron", 3, "Need a refund")));

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(store.findById(id))
                .map(ProcessedFeedback::sentiment).hasValue(Sentiment.NEGATIVE));
        await().atMost(TIMEOUT).until(() -> Files.exists(Path.of("target/test-alerts/alert-" + id + ".txt")));
    }

    @Test
    void unknownId_returns404() {
        assertThat(rest.getForEntity("/api/feedback/does-not-exist", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void summary_reflectsStoredFeedback() {
        submit(new FeedbackRequest("Sam", "sam@example.com", "Mixer", 4, "Nice"));

        await().atMost(TIMEOUT).untilAsserted(() -> {
            ResponseEntity<Map> summary = rest.getForEntity("/api/feedback/summary", Map.class);
            assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(((Number) summary.getBody().get("total")).intValue()).isPositive();
            assertThat(((Number) summary.getBody().get("positive")).intValue()).isPositive();
        });
    }

    @Test
    void csvFile_submitsGoodLines_andSkipsBadOnes() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String csv = """
                customerName,email,product,rating,comment
                Csv-%1$s-A,a@example.com,Fan,5,Quiet and fast

                Csv-%1$s-B,b@example.com,Heater,2,Too noisy, returned it
                Csv-%1$s-C,c@example.com,Lamp,seven,bad rating value
                """.formatted(tag);

        producer.sendBodyAndHeader("file:target/test-inbox", csv, Exchange.FILE_NAME, "feedback-" + tag + ".csv");

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(store.findAll())
                .filteredOn(f -> f.customerName().startsWith("Csv-" + tag))
                .extracting(ProcessedFeedback::customerName, ProcessedFeedback::sentiment, ProcessedFeedback::source)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Csv-" + tag + "-A", Sentiment.POSITIVE, "CSV"),
                        org.assertj.core.groups.Tuple.tuple("Csv-" + tag + "-B", Sentiment.NEGATIVE, "CSV")));
        await().atMost(TIMEOUT).until(() -> Files.exists(Path.of("target/test-inbox/.done/feedback-" + tag + ".csv")));
    }

    @Test
    void unreadableMessage_goesToDeadLetterQueue() {
        MockEndpoint dlq = camelContext.getEndpoint("mock:feedback-dlq", MockEndpoint.class);
        String poison = "this is not json " + UUID.randomUUID();

        producer.sendBody("seda:feedback-received", poison);

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(dlq.getReceivedExchanges())
                .anySatisfy(ex -> assertThat(ex.getIn().getBody(String.class)).isEqualTo(poison)));
    }

    private ResponseEntity<Map> submit(FeedbackRequest request) {
        return rest.postForEntity("/api/feedback", request, Map.class);
    }

    private static String idOf(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        return (String) response.getBody().get("id");
    }
}
