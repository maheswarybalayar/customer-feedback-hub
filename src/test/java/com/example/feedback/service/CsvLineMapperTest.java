package com.example.feedback.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvLineMapperTest {

    private final CsvLineMapper mapper = new CsvLineMapper();

    @Test
    void toRequest_mapsAllColumns() {
        var request = mapper.toRequest("Asha, asha@example.com, Blender, 4, Works well\r");

        assertThat(request.customerName()).isEqualTo("Asha");
        assertThat(request.email()).isEqualTo("asha@example.com");
        assertThat(request.product()).isEqualTo("Blender");
        assertThat(request.rating()).isEqualTo(4);
        assertThat(request.comment()).isEqualTo("Works well");
    }

    @Test
    void toRequest_keepsCommasInsideComment() {
        var request = mapper.toRequest("Ravi,ravi@example.com,Kettle,2,Leaks, stopped working, want refund");

        assertThat(request.comment()).isEqualTo("Leaks, stopped working, want refund");
    }

    @Test
    void toRequest_allowsMissingComment() {
        assertThat(mapper.toRequest("Meena,meena@example.com,Toaster,5").comment()).isEmpty();
    }

    @Test
    void toRequest_rejectsNonNumericRating() {
        assertThatThrownBy(() -> mapper.toRequest("Arun,arun@example.com,Fan,five,ok"))
                .isInstanceOf(FeedbackValidationException.class)
                .hasMessageContaining("rating");
    }

    @Test
    void toRequest_rejectsTooFewColumns() {
        assertThatThrownBy(() -> mapper.toRequest("Arun,arun@example.com"))
                .isInstanceOf(FeedbackValidationException.class);
    }
}
