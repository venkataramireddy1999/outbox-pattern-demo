# Polling Outbox Pattern

A simple example using Spring Boot, PostgreSQL, and Kafka.

## The dual write problem

An order service needs to save an order and publish an `OrderCreated` event to Kafka. These are two independent writes:

- The database commits, but publishing fails: the order exists without an event.
- Publishing succeeds, but the database rolls back: consumers receive an event for an order that does not exist.

A normal database transaction cannot make a database write and a Kafka send atomic.

## The outbox solution

Instead of sending directly to Kafka, save the order and its event in an outbox table within the **same database transaction**. Both are saved, or both are rolled back.

A background poller publishes the saved events later.

```text
Create order
    |
    +-- Save order      }
    +-- Save outbox row } Same database transaction

Background poller
    |
    +-- Read unpublished events
    +-- Send each event to Kafka
    +-- Wait for Kafka acknowledgement
    +-- Mark the event as published
```

## How this project implements it

1. **OrderService** saves the order and an `OutboxEvent` using `@Transactional`. JSON serialization failures are rethrown as an unchecked exception so the transaction rolls back.
2. **OutboxEvent** stores the event ID, order ID, event type, JSON payload, and a `published` flag that starts as `false`.
3. **OutboxEventRepository** fetches unpublished events with `findByPublishedFalse()`.
4. **OutboxPublisher** uses `@Scheduled` to poll. It calls `kafkaTemplate.send(...).get()` to wait for acknowledgement before saving `published = true`.

If sending fails, the database row remains unpublished and can be retried during a later poll. Order creation does not need to wait for Kafka.

## Why duplicates are possible

Kafka may acknowledge an event just before the application crashes, leaving the database row unpublished. The next poll can send the same event again.

This is **at-least-once delivery**. Consumers should use the stable `eventId` in the payload to avoid processing the same event twice.

## Scope of this example

This demo uses one publisher instance and loads all unpublished events. Batching, safe row claiming for multiple publishers, retry backoff, and cleanup are left out to keep the example simple. Strict event ordering is not guaranteed.

The outbox makes the **order and event record atomic in the database**. Delivery to Kafka happens separately, after commit.
