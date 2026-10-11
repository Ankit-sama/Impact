package com.impact.organization;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "organizations")
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long managerId;

    @Column(nullable = false)
    private String name;

    private String causes;
    private String city;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String contactEmail;
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrgStatus status;

    private String reviewReason;
    private Long reviewedBy;
    private Instant reviewedAt;

    // Getters and Setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getManagerId() { return managerId; } public void setManagerId(Long managerId) { this.managerId = managerId; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getCauses() { return causes; } public void setCauses(String causes) { this.causes = causes; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getContactEmail() { return contactEmail; } public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getRegistrationNumber() { return registrationNumber; } public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public OrgStatus getStatus() { return status; } public void setStatus(OrgStatus status) { this.status = status; }
    public String getReviewReason() { return reviewReason; } public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
    public Long getReviewedBy() { return reviewedBy; } public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; } public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
}