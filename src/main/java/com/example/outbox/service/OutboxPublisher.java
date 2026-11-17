package com.example.outbox.service;

import com.example.outbox.event.OutboxEvent;
import com.example.outbox.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OutboxPublisher(OutboxEventRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate,
                           @Value("${app.kafka.order-topic:order-events}") String topic) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    // Single-instance demo: poll again 3 seconds after the previous poll finishes.
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:3000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findByPublishedFalse();

        for (OutboxEvent event : pending) {
            try {
                // Wait for Kafka acknowledgement before marking the event published.
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get();
                // A crash before this save can cause duplicates; consumers use eventId.
                event.setPublished(true);
                outboxRepository.save(event);
                log.info("Published outbox event {} ({})", event.getId(), event.getEventType());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                // Stop this poll; the event stays pending.
                return;
            } catch (Exception ex) {
                // Keep the event pending. It will be retried during a future poll.
                log.warn("Could not publish outbox event {}. It will be retried.", event.getId(), ex);
            }
        }
    }
}
