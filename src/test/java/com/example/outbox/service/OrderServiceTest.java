package com.example.outbox.service;

import com.example.outbox.entity.OrderEntity;
import com.example.outbox.repository.OrderRepository;
import com.example.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    @Test
    void serializationFailureThrowsUncheckedExceptionForRollback() throws Exception {
        OrderRepository orders = mock(OrderRepository.class);
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        ObjectMapper mapper = mock(ObjectMapper.class);
        OrderService service = new OrderService(orders, outbox, mapper);
        OrderEntity order = new OrderEntity("Alice", new BigDecimal("10.00"));
        when(orders.save(any(OrderEntity.class))).thenReturn(order);
        JsonProcessingException failure = new JsonProcessingException("Cannot serialize") { };
        when(mapper.writeValueAsString(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.createOrder("Alice", new BigDecimal("10.00")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not serialize OrderCreated event")
            .hasCause(failure);
        verifyNoInteractions(outbox);
    }
}
