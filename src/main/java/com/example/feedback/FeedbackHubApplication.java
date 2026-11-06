package com.example.feedback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
public class FeedbackHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedbackHubApplication.class, args);
    }

    /** Injected wherever we need "now", so tests can use a fixed clock. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
