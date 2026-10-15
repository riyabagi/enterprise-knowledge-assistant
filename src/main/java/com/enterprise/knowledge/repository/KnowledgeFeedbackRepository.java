package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.dto.DocumentFeedbackSummary;
import com.enterprise.knowledge.entity.KnowledgeFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeFeedbackRepository extends JpaRepository<KnowledgeFeedback, Long> {
    @Query("SELECT AVG(f.rating) FROM KnowledgeFeedback f")
    Double averageRating();

    @Query("""
            SELECT new com.enterprise.knowledge.dto.DocumentFeedbackSummary(
                d.id, d.title, COUNT(f.id), AVG(f.rating),
                SUM(CASE WHEN f.rating = 1 THEN 1 ELSE 0 END),
                SUM(CASE WHEN f.rating = 2 THEN 1 ELSE 0 END),
                SUM(CASE WHEN f.rating = 3 THEN 1 ELSE 0 END),
                SUM(CASE WHEN f.rating = 4 THEN 1 ELSE 0 END),
                SUM(CASE WHEN f.rating = 5 THEN 1 ELSE 0 END))
            FROM KnowledgeDocument d
            LEFT JOIN KnowledgeFeedback f ON f.document = d
            GROUP BY d.id, d.title
            ORDER BY CASE WHEN AVG(f.rating) IS NULL THEN 1 ELSE 0 END,
                     AVG(f.rating) DESC, d.title ASC
            """)
    List<DocumentFeedbackSummary> summarizeRatingsByDocument();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE KnowledgeFeedback f SET f.document = NULL WHERE f.document.id = :documentId")
    int clearDocumentReference(@Param("documentId") Long documentId);
}
