package com.impact.auth.dto;

import com.impact.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.multipart.MultipartFile;

public class RegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", 
             message = "Password must be at least 8 characters and contain at least one letter and one digit")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    @NotBlank(message = "City is required")
    private String city;

    private String orgName; // Required only if role is ORG_MANAGER

    private MultipartFile photo;

    // Getters and Setters
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; } public void setRole(Role role) { this.role = role; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public String getOrgName() { return orgName; } public void setOrgName(String orgName) { this.orgName = orgName; }
    public MultipartFile getPhoto() { return photo; } public void setPhoto(MultipartFile photo) { this.photo = photo; }
}