package com.mycompany.logistic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drivers")
public class Driver {
    @Id
    @Column(length = 36)
    public String id;
    @Column(name = "full_name", nullable = false, length = 120)
    public String fullName;
    @Column(nullable = false, unique = true, length = 32)
    public String phone;
    @Column(nullable = false, length = 24)
    public String status;
    @Column(name = "warehouse_id", length = 36)
    public String warehouseId;
}
