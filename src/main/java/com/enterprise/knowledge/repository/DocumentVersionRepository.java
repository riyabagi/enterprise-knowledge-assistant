package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.entity.DocumentVersion;
import com.enterprise.knowledge.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocumentOrderByVersionNumberDesc(KnowledgeDocument document);

    Optional<DocumentVersion> findByDocumentAndStatus(KnowledgeDocument document, DocumentVersion.Status status);

    int countByDocument(KnowledgeDocument document);
}
