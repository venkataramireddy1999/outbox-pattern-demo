package com.example.outbox.controller;

import com.example.outbox.entity.OrderEntity;
import com.example.outbox.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedOrder create(@Valid @RequestBody CreateOrderRequest request) {
        OrderEntity order = orderService.createOrder(request.customerName(), request.amount());
        return new CreatedOrder(order.getId(), order.getCustomerName(), order.getAmount());
    }

    public record CreateOrderRequest(
        @NotBlank @Size(max = 255) String customerName,
        // Match the database's decimal(12, 2) so the event and stored amount agree.
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount
    ) { }

    public record CreatedOrder(UUID id, String customerName, BigDecimal amount) { }
}
