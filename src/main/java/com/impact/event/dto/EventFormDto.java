package com.impact.event.dto;

import com.impact.event.EventCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class EventFormDto {

    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Category is required")
    private EventCategory category;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startAtLocal; // Captured from HTML5 datetime-local (no timezone natively)

    @NotNull(message = "End time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endAtLocal;

    @NotNull(message = "Expected hours is required")
    @Min(value = 1, message = "Expected hours must be at least 1")
    private Integer expectedHours;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "City is required")
    private String city;

    private String address;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    // Getters and Setters
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public EventCategory getCategory() { return category; } public void setCategory(EventCategory category) { this.category = category; }
    public String getPurpose() { return purpose; } public void setPurpose(String purpose) { this.purpose = purpose; }
    public LocalDateTime getStartAtLocal() { return startAtLocal; } public void setStartAtLocal(LocalDateTime startAtLocal) { this.startAtLocal = startAtLocal; }
    public LocalDateTime getEndAtLocal() { return endAtLocal; } public void setEndAtLocal(LocalDateTime endAtLocal) { this.endAtLocal = endAtLocal; }
    public Integer getExpectedHours() { return expectedHours; } public void setExpectedHours(Integer expectedHours) { this.expectedHours = expectedHours; }
    public String getState() { return state; } public void setState(String state) { this.state = state; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public Integer getCapacity() { return capacity; } public void setCapacity(Integer capacity) { this.capacity = capacity; }
}