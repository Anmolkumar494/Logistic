package com.mycompany.logistic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @Column(length = 36)
    public String id;
    @Column(nullable = false, unique = true, length = 80)
    public String username;
    @Column(name = "password_hash", nullable = false, length = 160)
    public String passwordHash;
    @Column(name = "full_name", nullable = false, length = 120)
    public String fullName;
    @Column(nullable = false, length = 24)
    public String role;
    @Column(nullable = false)
    public boolean active = true;
}
