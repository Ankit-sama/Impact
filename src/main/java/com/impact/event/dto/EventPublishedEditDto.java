package com.impact.event.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EventPublishedEditDto {
    private Long id;

    @NotBlank(message = "Purpose is required")
    private String purpose;

    private String address;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getPurpose() { return purpose; } public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getAddress() { return address; } public void setAddress(String address) { this.address = address; }
    public Integer getCapacity() { return capacity; } public void setCapacity(Integer capacity) { this.capacity = capacity; }
}