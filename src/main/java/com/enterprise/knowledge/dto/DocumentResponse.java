
package com.enterprise.knowledge.dto;

import com.enterprise.knowledge.entity.KnowledgeDocument;
import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private String title;
    private String description;
    private KnowledgeDocument.Category category;
    private KnowledgeDocument.AccessLevel accessLevel;
    private KnowledgeDocument.Status status;
    private String ownerUsername;
    private Integer currentVersion;
    private String originalFilename;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private Boolean reindexRequired;

    public DocumentResponse() {
    }

    public DocumentResponse(
            Long id,
            String title,
            String description,
            KnowledgeDocument.Category category,
            KnowledgeDocument.AccessLevel accessLevel,
            KnowledgeDocument.Status status,
            String ownerUsername,
            Integer currentVersion,
            String originalFilename,
            LocalDateTime uploadedAt,
            LocalDateTime updatedAt,
            Boolean reindexRequired) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.accessLevel = accessLevel;
        this.status = status;
        this.ownerUsername = ownerUsername;
        this.currentVersion = currentVersion;
        this.originalFilename = originalFilename;
        this.uploadedAt = uploadedAt;
        this.updatedAt = updatedAt;
        this.reindexRequired = reindexRequired;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public KnowledgeDocument.Category getCategory() { return category; }
    public void setCategory(KnowledgeDocument.Category category) { this.category = category; }

    public KnowledgeDocument.AccessLevel getAccessLevel() { return accessLevel; }
    public void setAccessLevel(KnowledgeDocument.AccessLevel accessLevel) { this.accessLevel = accessLevel; }

    public KnowledgeDocument.Status getStatus() { return status; }
    public void setStatus(KnowledgeDocument.Status status) { this.status = status; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public Integer getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getReindexRequired() { return reindexRequired; }
    public void setReindexRequired(Boolean reindexRequired) { this.reindexRequired = reindexRequired; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String title;
        private String description;
        private KnowledgeDocument.Category category;
        private KnowledgeDocument.AccessLevel accessLevel;
        private KnowledgeDocument.Status status;
        private String ownerUsername;
        private Integer currentVersion;
        private String originalFilename;
        private LocalDateTime uploadedAt;
        private LocalDateTime updatedAt;
        private Boolean reindexRequired;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder category(KnowledgeDocument.Category category) { this.category = category; return this; }
        public Builder accessLevel(KnowledgeDocument.AccessLevel accessLevel) { this.accessLevel = accessLevel; return this; }
        public Builder status(KnowledgeDocument.Status status) { this.status = status; return this; }
        public Builder ownerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; return this; }
        public Builder currentVersion(Integer currentVersion) { this.currentVersion = currentVersion; return this; }
        public Builder originalFilename(String originalFilename) { this.originalFilename = originalFilename; return this; }
        public Builder uploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder reindexRequired(Boolean reindexRequired) { this.reindexRequired = reindexRequired; return this; }

        public DocumentResponse build() {
            return new DocumentResponse(
                    id, title, description, category, accessLevel, status,
                    ownerUsername, currentVersion, originalFilename,
                    uploadedAt, updatedAt, reindexRequired);
        }
    }
}
