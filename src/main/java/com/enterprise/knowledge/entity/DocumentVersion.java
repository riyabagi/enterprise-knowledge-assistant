
package com.enterprise.knowledge.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_version")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private KnowledgeDocument document;

    @Builder.Default
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Lob
    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    // Required by JPA
    public DocumentVersion() {
    }

    // Explicit all-arguments constructor
    public DocumentVersion(
            Long id,
            KnowledgeDocument document,
            Integer versionNumber,
            String originalFilename,
            String contentType,
            Long fileSizeBytes,
            String checksum,
            String extractedText,
            Status status,
            LocalDateTime createdAt,
            AppUser createdBy) {

        this.id = id;
        this.document = document;
        this.versionNumber = versionNumber;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.checksum = checksum;
        this.extractedText = extractedText;
        this.status = status;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public enum Status {
        DRAFT, PUBLISHED, OBSOLETE
    }
}
