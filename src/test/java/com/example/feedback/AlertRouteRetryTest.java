package com.example.feedback;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

@CamelSpringBootTest
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
        "server.port=18093",
        "app.alerts.output-dir=target/retry-blocked-output"
    }
)
@ActiveProfiles("test")
@DirtiesContext
class AlertRouteRetryTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producer;
    
    @Test
    void alertFileWriteFailure_sendsMessageToDlq() throws Exception {

        // Arrange: Create a file where Camel expects an output directory.
        Path blockedPath = Path.of("target/retry-blocked-output");
        Files.createDirectories(blockedPath.getParent());
        Files.deleteIfExists(blockedPath);
        Files.writeString(blockedPath, "This is a file, not a directory");

        // Get the DLQ mock endpoint.
        MockEndpoint dlq =
                camelContext.getEndpoint("mock:feedback-dlq", MockEndpoint.class);

        dlq.expectedMessageCount(1);

        // Act: Send feedback directly to the alert route.
        String message = """
                {
                  "id": "RETRY-TEST-001",
                  "customerName": "Asha",
                  "email": "asha@example.com",
                  "product": "Blender",
                  "rating": 1,
                  "comment": "Stopped working",
                  "source": "REST",
                  "sentiment": "NEGATIVE",
                  "receivedAt": "2026-11-05T10:00:00Z",
                  "processedAt": "2026-11-05T10:01:00Z"
                }
                """;

        producer.sendBody("seda:feedback-alerts", message);

        // Assert: The failed message eventually reaches the DLQ.
        dlq.assertIsSatisfied();

        assertThat(dlq.getReceivedExchanges().get(0)
                .getMessage().getHeader("errorReason", String.class))
                .isNotBlank();
        assertThat(dlq.getReceivedExchanges().get(0)
                .getMessage().getHeader("retryCount", Integer.class))
                .isEqualTo(2);
    }

}