package com.mycompany.logistic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {
    @Id
    @Column(length = 36)
    public String id;
    @Column(name = "order_number", nullable = false, unique = true, length = 32)
    public String orderNumber;
    @Column(name = "customer_name", nullable = false, length = 120)
    public String customerName;
    @Column(name = "destination", nullable = false, length = 180)
    public String destination;
    @Column(nullable = false, length = 24)
    public String status;
    @Column(name = "total_weight_kg", nullable = false, precision = 10, scale = 2)
    public BigDecimal totalWeightKg;
    @Column(name = "priority", nullable = false, length = 16)
    public String priority;
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
