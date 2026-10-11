package com.impact.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "otp_records")
public class OtpRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Getters and setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getCodeHash() { return codeHash; } public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}