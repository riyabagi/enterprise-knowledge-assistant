package com.enterprise.knowledge.dto;

import com.enterprise.knowledge.entity.KnowledgeDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DocumentMetadataUpdateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private KnowledgeDocument.Category category;

    @NotNull(message = "Access level is required")
    private KnowledgeDocument.AccessLevel accessLevel;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public KnowledgeDocument.Category getCategory() {
        return category;
    }

    public void setCategory(KnowledgeDocument.Category category) {
        this.category = category;
    }

    public KnowledgeDocument.AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(KnowledgeDocument.AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }
}
