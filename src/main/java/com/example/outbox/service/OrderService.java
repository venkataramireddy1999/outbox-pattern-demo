package com.example.outbox.service;

import com.example.outbox.entity.OrderEntity;
import com.example.outbox.event.OutboxEvent;
import com.example.outbox.repository.OrderRepository;
import com.example.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        OutboxEventRepository outboxRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    // Save the order and its event together: both commit or both roll back.
    @Transactional
    public OrderEntity createOrder(String customerName, BigDecimal amount) {
        OrderEntity order = orderRepository.save(new OrderEntity(customerName, amount));

        try {
            // Generate once and persist it: retries must reuse the same event ID.
            UUID eventId = UUID.randomUUID();
            String payload = objectMapper.writeValueAsString(Map.of(
                "eventId", eventId.toString(),
                "eventType", "OrderCreated",
                "order", order
            ));
            // Direct publishing would create the dual write problem:
            // kafkaTemplate.send("order-events", order.getId().toString(), payload);
            // DB commit and Kafka send can fail independently. Save an outbox row instead.
            OutboxEvent event = new OutboxEvent(
                eventId, order.getId().toString(), "OrderCreated", payload
            );
            outboxRepository.save(event);
        } catch (JsonProcessingException ex) {
            // Throwing causes the order insert to roll back as well.
            throw new IllegalStateException("Could not serialize OrderCreated event", ex);
        }
        return order;
    }
}
