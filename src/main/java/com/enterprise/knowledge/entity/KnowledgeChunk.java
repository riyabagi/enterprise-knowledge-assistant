
package com.enterprise.knowledge.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "knowledge_chunk",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"document_version_id", "chunk_index"}
        )
)
@Getter
@Setter
@Builder
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "section_reference", length = 300)
    private String sectionReference;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Lob
    @Column(name = "searchable_text", columnDefinition = "TEXT")
    private String searchableText;

    // Required by JPA
    public KnowledgeChunk() {
    }

    // Explicit all-arguments constructor
    public KnowledgeChunk(
            Long id,
            DocumentVersion documentVersion,
            Integer chunkIndex,
            String sectionReference,
            String content,
            String searchableText) {

        this.id = id;
        this.documentVersion = documentVersion;
        this.chunkIndex = chunkIndex;
        this.sectionReference = sectionReference;
        this.content = content;
        this.searchableText = searchableText;
    }
}
