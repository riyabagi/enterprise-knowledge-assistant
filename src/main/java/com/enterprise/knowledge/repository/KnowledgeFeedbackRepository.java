package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.entity.KnowledgeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeFeedbackRepository extends JpaRepository<KnowledgeFeedback, Long> {
    @Query("SELECT AVG(f.rating) FROM KnowledgeFeedback f")
    Double averageRating();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE KnowledgeFeedback f SET f.document = NULL WHERE f.document.id = :documentId")
    int clearDocumentReference(@Param("documentId") Long documentId);
}
