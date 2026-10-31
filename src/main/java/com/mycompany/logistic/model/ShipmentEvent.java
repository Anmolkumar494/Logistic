package com.mycompany.logistic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_events")
public class ShipmentEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "shipment_id", nullable = false, length = 36)
    public String shipmentId;
    @Column(nullable = false, length = 32)
    public String status;
    @Column(length = 240)
    public String note;
    @Column(name = "changed_by", length = 36)
    public String changedBy;
    @Column(name = "occurred_at", nullable = false)
    public LocalDateTime occurredAt;
}
