package com.mycompany.logistic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class InventoryItem {
    @Id
    @Column(length = 36)
    public String id;
    @Column(name = "warehouse_id", nullable = false, length = 36)
    public String warehouseId;
    @Column(name = "sku", nullable = false, length = 40)
    public String sku;
    @Column(name = "product_name", nullable = false, length = 140)
    public String productName;
    @Column(nullable = false)
    public int quantity;
    @Column(name = "reorder_level", nullable = false)
    public int reorderLevel;
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;
}
