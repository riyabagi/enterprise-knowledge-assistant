package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.dto.DocumentCategoryStatusCount;
import com.enterprise.knowledge.dto.PendingReindexDocument;
import com.enterprise.knowledge.entity.KnowledgeDocument;
import com.enterprise.knowledge.entity.KnowledgeDocument.AccessLevel;
import com.enterprise.knowledge.entity.KnowledgeDocument.Category;
import com.enterprise.knowledge.entity.KnowledgeDocument.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    Page<KnowledgeDocument> findByStatusAndAccessLevelIn(Status status, Collection<AccessLevel> accessLevels,
            Pageable pageable);

    Page<KnowledgeDocument> findByStatusAndCategoryAndAccessLevelIn(
            Status status, Category category, Collection<AccessLevel> accessLevels, Pageable pageable);

    @Query("SELECT d FROM KnowledgeDocument d WHERE d.status = :status AND d.accessLevel IN :levels ORDER BY d.uploadedAt DESC")
    Page<KnowledgeDocument> findAccessibleDocuments(
            @Param("status") Status status,
            @Param("levels") Collection<AccessLevel> levels,
            Pageable pageable);

    long countByStatus(Status status);

    @Query("""
            SELECT new com.enterprise.knowledge.dto.DocumentCategoryStatusCount(
                d.category, d.status, COUNT(d))
            FROM KnowledgeDocument d
            GROUP BY d.category, d.status
            ORDER BY d.category, d.status
            """)
    List<DocumentCategoryStatusCount> countDocumentsByCategoryAndStatus();

    @Query("""
            SELECT new com.enterprise.knowledge.dto.PendingReindexDocument(
                d.id, d.title, d.category, d.status, d.reindexRequired,
                d.accessLevel, d.updatedAt)
            FROM KnowledgeDocument d
            WHERE d.reindexRequired = true
            ORDER BY d.updatedAt ASC, d.id ASC
            """)
    List<PendingReindexDocument> findPendingReindexDocuments();

    List<KnowledgeDocument> findByStatusAndAccessLevelInOrderByTitleAsc(
            Status status, Collection<AccessLevel> accessLevels);
}
