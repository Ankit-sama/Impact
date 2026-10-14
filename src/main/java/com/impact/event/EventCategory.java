package com.impact.event;

public enum EventCategory {
    CLEANLINESS_DRIVE("Cleanliness drive"),
    CULTURAL_EVENT("Cultural event"),
    AWARENESS_CAMPAIGN("Awareness campaign"),
    DONATION_DRIVE("Donation drive"),
    EDUCATIONAL("Educational"),
    HEALTH_CAMP("Health camp"),
    ENVIRONMENT("Tree plantation / Environment"),
    FOOD_DISTRIBUTION("Food distribution"),
    DISASTER_RELIEF("Disaster relief"),
    ANIMAL_WELFARE("Animal welfare"),
    SKILL_DEVELOPMENT("Skill development"),
    OTHER("Other");

    private final String displayName;

    EventCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}