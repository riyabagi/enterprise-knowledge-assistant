package com.enterprise.knowledge.dto;

public record DocumentFeedbackSummary(
        Long documentId,
        String documentTitle,
        long feedbackCount,
        Double averageRating,
        long oneStarCount,
        long twoStarCount,
        long threeStarCount,
        long fourStarCount,
        long fiveStarCount) {
}
