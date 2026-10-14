package com.enterprise.knowledge.repository;

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
}
