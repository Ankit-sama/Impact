package com.impact.common;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "attachments")
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerType;
    private Long ownerId;
    
    @Column(nullable = false, unique = true)
    private String storagePath;
    
    private String mimeType;
    private Long size;
    private String label;

    public Attachment() {
        this.storagePath = UUID.randomUUID().toString();
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getOwnerType() { return ownerType; } public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; } public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getStoragePath() { return storagePath; } public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getMimeType() { return mimeType; } public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getSize() { return size; } public void setSize(Long size) { this.size = size; }
    public String getLabel() { return label; } public void setLabel(String label) { this.label = label; }
}