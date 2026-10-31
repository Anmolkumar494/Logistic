package com.mycompany.logistic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "registration_number", nullable = false, unique = true, length = 24)
    public String registrationNumber;
    @Column(name = "vehicle_type", nullable = false, length = 32)
    public String vehicleType;
    @Column(name = "capacity_kg", nullable = false, precision = 10, scale = 2)
    public BigDecimal capacityKg;
    @Column(nullable = false, length = 20)
    public String status;
    @Column(name = "driver_id", length = 36)
    public String driverId;
    @Column(name = "warehouse_id", length = 36)
    public String warehouseId;
    @Column(name = "cost_per_km", nullable = false, precision = 8, scale = 2)
    public BigDecimal costPerKm;
}
