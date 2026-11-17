package com.example.outbox.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private Instant createdAt;

    protected OrderEntity() { }

    public OrderEntity(String customerName, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.customerName = customerName;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCustomerName() { return customerName; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
