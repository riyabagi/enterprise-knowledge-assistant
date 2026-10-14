
package com.enterprise.knowledge.dto;

import com.enterprise.knowledge.entity.DocumentVersion;
import java.time.LocalDateTime;

public class DocumentVersionResponse {

    private Long id;
    private Integer versionNumber;
    private String originalFilename;
    private String contentType;
    private Long fileSizeBytes;
    private String checksum;
    private DocumentVersion.Status status;
    private LocalDateTime createdAt;
    private String createdByUsername;

    public DocumentVersionResponse() {
    }

    public DocumentVersionResponse(
            Long id,
            Integer versionNumber,
            String originalFilename,
            String contentType,
            Long fileSizeBytes,
            String checksum,
            DocumentVersion.Status status,
            LocalDateTime createdAt,
            String createdByUsername) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.checksum = checksum;
        this.status = status;
        this.createdAt = createdAt;
        this.createdByUsername = createdByUsername;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public DocumentVersion.Status getStatus() { return status; }
    public void setStatus(DocumentVersion.Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Integer versionNumber;
        private String originalFilename;
        private String contentType;
        private Long fileSizeBytes;
        private String checksum;
        private DocumentVersion.Status status;
        private LocalDateTime createdAt;
        private String createdByUsername;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder versionNumber(Integer versionNumber) { this.versionNumber = versionNumber; return this; }
        public Builder originalFilename(String originalFilename) { this.originalFilename = originalFilename; return this; }
        public Builder contentType(String contentType) { this.contentType = contentType; return this; }
        public Builder fileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; return this; }
        public Builder checksum(String checksum) { this.checksum = checksum; return this; }
        public Builder status(DocumentVersion.Status status) { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder createdByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; return this; }

        public DocumentVersionResponse build() {
            return new DocumentVersionResponse(
                    id, versionNumber, originalFilename, contentType,
                    fileSizeBytes, checksum, status, createdAt, createdByUsername);
        }
    }
}
