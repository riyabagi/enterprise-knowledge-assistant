
package com.enterprise.knowledge.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_feedback")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
public class KnowledgeFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(length = 600)
    private String question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_record_id")
    private AnswerRecord answerRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private KnowledgeDocument document;

    @Column(nullable = false)
    private Byte rating;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Required by JPA
    public KnowledgeFeedback() {
    }

    // Explicit all-arguments constructor
    public KnowledgeFeedback(
            Long id,
            AppUser user,
            String question,
            AnswerRecord answerRecord,
            KnowledgeDocument document,
            Byte rating,
            String comment,
            LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.question = question;
        this.answerRecord = answerRecord;
        this.document = document;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }
}
