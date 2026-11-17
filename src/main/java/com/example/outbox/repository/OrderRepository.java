package com.example.outbox.repository;

import com.example.outbox.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> { }
