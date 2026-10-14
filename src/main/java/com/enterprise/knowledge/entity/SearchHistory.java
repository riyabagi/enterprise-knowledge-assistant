
package com.enterprise.knowledge.entity;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 1000)
    private String query;

    @Column(name = "category_filter", length = 100)
    private String categoryFilter;

    @Builder.Default
    @Column(name = "result_count", nullable = false)
    private Integer resultCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Required by JPA
    public SearchHistory() {
    }

    // Explicit all-arguments constructor
    public SearchHistory(
            Long id,
            AppUser user,
            String query,
            String categoryFilter,
            Integer resultCount,
            LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.query = query;
        this.categoryFilter = categoryFilter;
        this.resultCount = resultCount;
        this.createdAt = createdAt;
    }
}
