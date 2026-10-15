package com.impact.event.dto;

import com.impact.event.Registration;
import com.impact.user.User;

public class ParticipantDto {
    private Registration registration;
    private User user;

    public ParticipantDto(Registration registration, User user) {
        this.registration = registration;
        this.user = user;
    }

    public Registration getRegistration() { return registration; }
    public User getUser() { return user; }
}