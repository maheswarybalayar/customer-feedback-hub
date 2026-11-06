# Feedback Hub — STARTER

This is the project you complete over 6 days. Follow `../docs/TRAINING-GUIDE.md`.

- It compiles and starts as-is. On Day 1 you will see a "Hello from Camel" log line every 5 seconds.
- The tests in `src/test` are your acceptance tests. Most fail today; all should pass by Day 5.
- Every file you need to change has a `TODO Day N` and step-by-step instructions in its comment.

```bash
mvn spring-boot:run                                          # with Kafka (docker compose up -d in the parent folder)
mvn spring-boot:run -Dspring-boot.run.profiles=inmemory      # without Docker
mvn test                                                     # run all tests
mvn test -Dtest=SentimentClassifierTest                      # run one test class
```
