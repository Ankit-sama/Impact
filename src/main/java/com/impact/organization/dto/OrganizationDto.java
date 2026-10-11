package com.impact.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class OrganizationDto {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Causes are required")
    private String causes;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Valid email is required")
    private String contactEmail;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    // Getters and Setters
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getCauses() { return causes; } public void setCauses(String causes) { this.causes = causes; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getContactEmail() { return contactEmail; } public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getRegistrationNumber() { return registrationNumber; } public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
}