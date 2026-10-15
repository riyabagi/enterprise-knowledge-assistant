package com.enterprise.knowledge.dto;

import com.enterprise.knowledge.entity.KnowledgeDocument.Category;
import com.enterprise.knowledge.entity.KnowledgeDocument.Status;

public record DocumentCategoryStatusCount(
        Category category,
        Status status,
        long documentCount) {
}
