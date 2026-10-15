package com.impact.event.dto;

import com.impact.event.Event;
import com.impact.event.Registration;

public class RegistrationDetailsDto {
    private Registration registration;
    private Event event;

    public RegistrationDetailsDto(Registration registration, Event event) {
        this.registration = registration;
        this.event = event;
    }

    public Registration getRegistration() { return registration; }
    public Event getEvent() { return event; }
}