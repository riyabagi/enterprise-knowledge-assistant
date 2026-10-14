package com.enterprise.knowledge.dto;

import com.enterprise.knowledge.entity.KnowledgeDocument;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentUploadRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private KnowledgeDocument.Category category;

    @NotNull(message = "Access level is required")
    private KnowledgeDocument.AccessLevel accessLevel;
}
