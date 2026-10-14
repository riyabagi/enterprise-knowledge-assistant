
package com.enterprise.knowledge.dto;

import java.time.LocalDateTime;

public class FeedbackResponse {

    private Long id;
    private Long documentId;
    private Integer rating;
    private String comment;
    private String message;
    private LocalDateTime createdAt;

    public FeedbackResponse() {
    }

    public FeedbackResponse(
            Long id,
            Long documentId,
            Integer rating,
            String comment,
            String message,
            LocalDateTime createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.rating = rating;
        this.comment = comment;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long documentId;
        private Integer rating;
        private String comment;
        private String message;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder documentId(Long documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder rating(Integer rating) {
            this.rating = rating;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public FeedbackResponse build() {
            return new FeedbackResponse(
                    id, documentId, rating, comment, message, createdAt);
        }
    }
}
