
package com.enterprise.knowledge.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "answer_record")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
public class AnswerRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 600)
    private String question;

    @Lob
    @Column(name = "answer_text", columnDefinition = "LONGTEXT")
    private String answerText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Lob
    @Column(name = "citation_json", columnDefinition = "TEXT")
    private String citationJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Required by JPA
    public AnswerRecord() {
    }

    // Explicit all-arguments constructor
    public AnswerRecord(
            Long id,
            AppUser user,
            String question,
            String answerText,
            Status status,
            String citationJson,
            LocalDateTime createdAt) {

        this.id = id;
        this.user = user;
        this.question = question;
        this.answerText = answerText;
        this.status = status;
        this.citationJson = citationJson;
        this.createdAt = createdAt;
    }

    public enum Status {
        ANSWERED,
        INSUFFICIENT_EVIDENCE,
        CONFLICTING_EVIDENCE,
        AI_UNAVAILABLE
    }
}
