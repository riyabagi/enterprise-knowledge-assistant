package com.enterprise.knowledge.dto;

import com.enterprise.knowledge.entity.KnowledgeDocument.AccessLevel;
import com.enterprise.knowledge.entity.KnowledgeDocument.Category;
import com.enterprise.knowledge.entity.KnowledgeDocument.Status;

import java.time.LocalDateTime;

public record PendingReindexDocument(
        Long id,
        String title,
        Category category,
        Status status,
        Boolean reindexRequired,
        AccessLevel accessLevel,
        LocalDateTime updatedAt) {
}
