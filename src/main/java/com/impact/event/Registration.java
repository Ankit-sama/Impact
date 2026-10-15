package com.impact.event;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "registrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"eventId", "userId"})
})
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    @Column(nullable = false)
    private Integer hoursCredited = 0;

    @Column(nullable = false)
    private Instant registeredAt;

    // Getters and Setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getEventId() { return eventId; } public void setEventId(Long eventId) { this.eventId = eventId; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public RegistrationStatus getStatus() { return status; } public void setStatus(RegistrationStatus status) { this.status = status; }
    public Integer getHoursCredited() { return hoursCredited; } public void setHoursCredited(Integer hoursCredited) { this.hoursCredited = hoursCredited; }
    public Instant getRegisteredAt() { return registeredAt; } public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
}