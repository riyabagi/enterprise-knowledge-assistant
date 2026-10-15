package com.enterprise.knowledge.dto;

import com.enterprise.knowledge.entity.KnowledgeDocument.AccessLevel;
import com.enterprise.knowledge.entity.KnowledgeDocument.Category;
import com.enterprise.knowledge.entity.KnowledgeDocument.Status;

public record AccessibleDocumentSummary(
        Long id,
        String title,
        Category category,
        AccessLevel accessLevel,
        Status status) {
}
