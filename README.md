# Simple Polling Outbox Pattern

Spring Boot + PostgreSQL + Kafka. Run one application instance for this learning example.

## Why an outbox?

Saving an order and sending to Kafka are separate writes. One can succeed while the other fails: this is the dual write problem.

Instead, `OrderService` saves the order and an event in one database transaction. Both commit or both roll back. `OutboxPublisher` polls unpublished events, sends them to Kafka, and sets `published = true` after acknowledgement.

```text
Create order -> save order + outbox row (same transaction)
Poll outbox  -> send to Kafka -> mark published
```

The code contains a commented `kafkaTemplate.send(...)` to show the direct publishing approach we avoid.

## Run

Requires Java 21, Maven 3.9+, and Docker Compose. Commands use Bash syntax.

```bash
docker compose up -d
mvn spring-boot:run
```

Wait for PostgreSQL and Kafka to start before starting the app.

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerName":"Rami","amount":1499.00}'
```

Read events:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic order-events --from-beginning
```

Inspect the outbox:

```bash
docker compose exec postgres psql -U outbox -d outbox_demo \
  -c 'SELECT id, event_type, published FROM outbox;'
```

Stop Kafka with `docker compose stop kafka`, then create an order. Its event stays unpublished until Kafka recovers. Restart Kafka with `docker compose start kafka`; the poller retries. Kafka timeouts can delay retries.

## Keep in mind

- This demo loads all unpublished events and uses one publisher. Larger systems need batching, safe row claiming, retry backoff, and cleanup.
- A crash after sending but before marking published can cause duplicates. Consumers must handle them using the stable `eventId` in the payload.
- The API confirms order creation, not Kafka delivery. Strict event ordering is not guaranteed.
- Hibernate manages the demo schema. Use database migrations for real deployments.
- This version uses the `outbox` table. If you ran the earlier version, its `outbox_events` table is no longer read; any pending events there are not migrated automatically.

## Tests

`mvn test` checks acknowledgement, retries, and interruption handling in the publisher. It does not run PostgreSQL or Kafka.

## License

MIT - see [LICENSE](LICENSE).
