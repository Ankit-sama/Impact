package com.impact.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "impact")
public class ImpactProperties {
    
    public static class Scheduler { private int intervalSeconds; public int getIntervalSeconds() { return intervalSeconds; } public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; } }
    public static class Event { private int minDurationMinutes; private int maxDurationDays; private int readinessWindowMinutes; public int getMinDurationMinutes() { return minDurationMinutes; } public void setMinDurationMinutes(int minDurationMinutes) { this.minDurationMinutes = minDurationMinutes; } public int getMaxDurationDays() { return maxDurationDays; } public void setMaxDurationDays(int maxDurationDays) { this.maxDurationDays = maxDurationDays; } public int getReadinessWindowMinutes() { return readinessWindowMinutes; } public void setReadinessWindowMinutes(int readinessWindowMinutes) { this.readinessWindowMinutes = readinessWindowMinutes; } }
    public static class Proof { private int creatorWindowMinutes; private int resubmitWindowMinutes; public int getCreatorWindowMinutes() { return creatorWindowMinutes; } public void setCreatorWindowMinutes(int creatorWindowMinutes) { this.creatorWindowMinutes = creatorWindowMinutes; } public int getResubmitWindowMinutes() { return resubmitWindowMinutes; } public void setResubmitWindowMinutes(int resubmitWindowMinutes) { this.resubmitWindowMinutes = resubmitWindowMinutes; } }
    public static class Workforce { private int thresholdPercent; public int getThresholdPercent() { return thresholdPercent; } public void setThresholdPercent(int thresholdPercent) { this.thresholdPercent = thresholdPercent; } }
    public static class Similarity { private int windowDays; public int getWindowDays() { return windowDays; } public void setWindowDays(int windowDays) { this.windowDays = windowDays; } }
    public static class Upload { private String dir; public String getDir() { return dir; } public void setDir(String dir) { this.dir = dir; } }

    private Scheduler scheduler = new Scheduler();
    private Event event = new Event();
    private Proof proof = new Proof();
    private Workforce workforce = new Workforce();
    private Similarity similarity = new Similarity();
    private Upload upload = new Upload();

    // Getters and Setters
    public Scheduler getScheduler() { return scheduler; } public void setScheduler(Scheduler scheduler) { this.scheduler = scheduler; }
    public Event getEvent() { return event; } public void setEvent(Event event) { this.event = event; }
    public Proof getProof() { return proof; } public void setProof(Proof proof) { this.proof = proof; }
    public Workforce getWorkforce() { return workforce; } public void setWorkforce(Workforce workforce) { this.workforce = workforce; }
    public Similarity getSimilarity() { return similarity; } public void setSimilarity(Similarity similarity) { this.similarity = similarity; }
    public Upload getUpload() { return upload; } public void setUpload(Upload upload) { this.upload = upload; }
}