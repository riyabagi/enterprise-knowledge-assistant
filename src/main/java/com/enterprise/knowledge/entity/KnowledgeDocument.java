
package com.enterprise.knowledge.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_document")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Category category = Category.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 20)
    @Builder.Default
    private AccessLevel accessLevel = AccessLevel.INTERNAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private DocumentVersion currentVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(name = "reindex_required", nullable = false)
    @Builder.Default
    private Boolean reindexRequired = false;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Required by JPA
    public KnowledgeDocument() {
    }

    // Explicit all-arguments constructor
    public KnowledgeDocument(
            Long id,
            String title,
            String description,
            Category category,
            AccessLevel accessLevel,
            AppUser owner,
            DocumentVersion currentVersion,
            Status status,
            Boolean reindexRequired,
            LocalDateTime uploadedAt,
            LocalDateTime updatedAt) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.accessLevel = accessLevel;
        this.owner = owner;
        this.currentVersion = currentVersion;
        this.status = status;
        this.reindexRequired = reindexRequired;
        this.uploadedAt = uploadedAt;
        this.updatedAt = updatedAt;
    }

    public enum Status {
        DRAFT, INDEXED, ARCHIVED
    }

    public enum Category {
        GENERAL, HR, IT, FINANCE, LEGAL, OPERATIONS
    }

    public enum AccessLevel {
        PUBLIC, INTERNAL, HR_ONLY, RESTRICTED
    }
}
