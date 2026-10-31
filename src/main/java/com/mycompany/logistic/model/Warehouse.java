package com.mycompany.logistic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouses")
public class Warehouse {
    @Id
    @Column(length = 36)
    public String id;
    @Column(nullable = false, unique = true, length = 24)
    public String code;
    @Column(nullable = false, length = 120)
    public String name;
    @Column(nullable = false, length = 180)
    public String location;
    @Column(name = "manager_id", length = 36)
    public String managerId;
}
