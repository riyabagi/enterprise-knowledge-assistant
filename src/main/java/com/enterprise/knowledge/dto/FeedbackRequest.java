package com.enterprise.knowledge.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class FeedbackRequest {

    @NotNull(message = "Document ID is required")
    private Long documentId;

    private Long answerRecordId;

    @Size(max = 500, message = "Question must not exceed 500 chars")
    private String question;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 2000, message = "Comment must not exceed 2000 chars")
    private String comment;
}
