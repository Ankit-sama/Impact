package com.impact.admin;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private String action;

    private String targetType;
    private Long targetId;
    
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Getters and Setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getActorId() { return actorId; } public void setActorId(Long actorId) { this.actorId = actorId; }
    public String getAction() { return action; } public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; } public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; } public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getDetails() { return details; } public void setDetails(String details) { this.details = details; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}