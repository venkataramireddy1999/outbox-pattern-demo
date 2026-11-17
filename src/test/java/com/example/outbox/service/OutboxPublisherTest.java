package com.example.outbox.service;

import com.example.outbox.event.OutboxEvent;
import com.example.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboxPublisherTest {
    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final OutboxPublisher publisher = new OutboxPublisher(repository, kafka, "order-events");
    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        event = new OutboxEvent(UUID.randomUUID(), "order-1", "OrderCreated", "{}");
        when(repository.findByPublishedFalse())
            .thenReturn(List.of(event));
    }

    @Test
    void marksPublishedOnlyAfterAcknowledgement() {
        CompletableFuture<SendResult<String, String>> acknowledgement = new CompletableFuture<>();
        when(kafka.send("order-events", "order-1", "{}")).thenAnswer(invocation -> {
            assertThat(event.isPublished()).isFalse();
            verify(repository, never()).save(any());
            acknowledgement.complete(null);
            return acknowledgement;
        });

        publisher.publishPendingEvents();

        assertThat(event.isPublished()).isTrue();
        verify(repository).save(event);
    }

    @Test
    void failedSendRemainsPendingAndIsRetriedWithSamePayload() {
        when(kafka.send("order-events", "order-1", "{}"))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")))
            .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPendingEvents();
        assertThat(event.isPublished()).isFalse();
        verify(repository, never()).save(any());

        publisher.publishPendingEvents();
        verify(kafka, times(2)).send("order-events", "order-1", "{}");
        assertThat(event.isPublished()).isTrue();
    }

    @Test
    void interruptedSendRestoresInterruptFlagAndStopsPoll() throws Exception {
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, String>> acknowledgement = mock(CompletableFuture.class);
        when(acknowledgement.get()).thenThrow(new InterruptedException());
        when(kafka.send("order-events", "order-1", "{}")).thenReturn(acknowledgement);

        try {
            publisher.publishPendingEvents();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(event.isPublished()).isFalse();
            verify(repository, never()).save(any());
        } finally {
            Thread.interrupted();
        }
    }
}
