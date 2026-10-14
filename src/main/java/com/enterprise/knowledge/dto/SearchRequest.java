package com.enterprise.knowledge.dto;

import com.enterprise.knowledge.entity.KnowledgeDocument;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Data
public class SearchRequest {
    @NotBlank(message = "Query is required")
    @Size(min = 2, max = 200, message = "Query must be between 2 and 200 characters")
    private String query;

    private KnowledgeDocument.Category category;

    @Min(value = 0, message = "Page must be zero or greater")
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 50, message = "Page size must not exceed 50")
    private int size = 10;

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
