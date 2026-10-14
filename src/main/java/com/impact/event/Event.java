package com.impact.event;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long creatorId;

    private Long organizationId; // Null for volunteers

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    @Column(nullable = false)
    private Integer expectedHours;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    private Instant publishedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Default constructor
    public Event() {}

    // Getters and Setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getCreatorId() { return creatorId; } public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Long getOrganizationId() { return organizationId; } public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public EventCategory getCategory() { return category; } public void setCategory(EventCategory category) { this.category = category; }
    public String getPurpose() { return purpose; } public void setPurpose(String purpose) { this.purpose = purpose; }
    public Instant getStartAt() { return startAt; } public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; } public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public Integer getExpectedHours() { return expectedHours; } public void setExpectedHours(Integer expectedHours) { this.expectedHours = expectedHours; }
    public String getState() { return state; } public void setState(String state) { this.state = state; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public Integer getCapacity() { return capacity; } public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public EventStatus getStatus() { return status; } public void setStatus(EventStatus status) { this.status = status; }
    public Instant getPublishedAt() { return publishedAt; } public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}