package com.mycompany.logistic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
public class Shipment {
    @Id
    @Column(length = 36)
    public String id;
    @Column(name = "tracking_number", nullable = false, unique = true, length = 32)
    public String trackingNumber;
    @Column(name = "order_id", length = 36)
    public String orderId;
    @Column(name = "recipient_name", nullable = false, length = 120)
    public String recipientName;
    @Column(nullable = false, length = 180)
    public String destination;
    @Column(nullable = false, length = 32)
    public String status;
    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 2)
    public BigDecimal weightKg;
    @Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
    public BigDecimal distanceKm;
    @Column(nullable = false, length = 16)
    public String priority = "NORMAL";
    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 2)
    public BigDecimal estimatedCost;
    @Column(name = "vehicle_id")
    public Long vehicleId;
    @Column(name = "warehouse_id", length = 36)
    public String warehouseId;
    @Column(name = "estimated_delivery")
    public LocalDateTime estimatedDelivery;
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;
}
