package com.impact.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String message;

    private String link;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Getters and Setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getMessage() { return message; } public void setMessage(String message) { this.message = message; }
    public String getLink() { return link; } public void setLink(String link) { this.link = link; }
    public boolean isRead() { return isRead; } public void setRead(boolean read) { isRead = read; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}