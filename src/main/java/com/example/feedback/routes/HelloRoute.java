package com.example.feedback.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Day 1 warm-up: the smallest possible Camel route. Every 5 seconds a timer fires and we log a message.
 * Switch it on with app.hello.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "app.hello.enabled", havingValue = "true")
public class HelloRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("timer:hello?period=5000")
                .routeId("hello")
                .setBody(simple("Hello from Camel at ${date:now:HH:mm:ss}"))
                .log("${body}")
              .to("kafka:hello.topic?brokers=localhost:9092");
      from("kafka:hello.topic?brokers=localhost:9092&groupId=hello-reader")
        		.log("Got: ${body}");
    }
}
