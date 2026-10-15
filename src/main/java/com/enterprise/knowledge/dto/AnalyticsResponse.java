
package com.enterprise.knowledge.dto;

import java.util.List;

public class AnalyticsResponse {

    private long totalDocuments;
    private long indexedDocuments;
    private long totalSearches;
    private long totalQuestions;
    private long answeredQuestions;
    private double averageFeedbackRating;
    private List<DocumentCategoryStatusCount> documentCountsByCategoryAndStatus = List.of();
    private List<PendingReindexDocument> pendingReindexDocuments = List.of();
    private List<SearchCategoryCount> mostFrequentlySearchedCategories = List.of();
    private List<DocumentFeedbackSummary> feedbackByDocument = List.of();

    public AnalyticsResponse() {
    }

    public AnalyticsResponse(
            long totalDocuments,
            long indexedDocuments,
            long totalSearches,
            long totalQuestions,
            long answeredQuestions,
            double averageFeedbackRating) {
        this.totalDocuments = totalDocuments;
        this.indexedDocuments = indexedDocuments;
        this.totalSearches = totalSearches;
        this.totalQuestions = totalQuestions;
        this.answeredQuestions = answeredQuestions;
        this.averageFeedbackRating = averageFeedbackRating;
    }

    public long getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(long totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public long getIndexedDocuments() {
        return indexedDocuments;
    }

    public void setIndexedDocuments(long indexedDocuments) {
        this.indexedDocuments = indexedDocuments;
    }

    public long getTotalSearches() {
        return totalSearches;
    }

    public void setTotalSearches(long totalSearches) {
        this.totalSearches = totalSearches;
    }

    public long getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(long totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public long getAnsweredQuestions() {
        return answeredQuestions;
    }

    public void setAnsweredQuestions(long answeredQuestions) {
        this.answeredQuestions = answeredQuestions;
    }

    public double getAverageFeedbackRating() {
        return averageFeedbackRating;
    }

    public void setAverageFeedbackRating(double averageFeedbackRating) {
        this.averageFeedbackRating = averageFeedbackRating;
    }

    public List<DocumentCategoryStatusCount> getDocumentCountsByCategoryAndStatus() {
        return documentCountsByCategoryAndStatus;
    }

    public void setDocumentCountsByCategoryAndStatus(
            List<DocumentCategoryStatusCount> documentCountsByCategoryAndStatus) {
        this.documentCountsByCategoryAndStatus = documentCountsByCategoryAndStatus;
    }

    public List<PendingReindexDocument> getPendingReindexDocuments() {
        return pendingReindexDocuments;
    }

    public void setPendingReindexDocuments(List<PendingReindexDocument> pendingReindexDocuments) {
        this.pendingReindexDocuments = pendingReindexDocuments;
    }

    public List<SearchCategoryCount> getMostFrequentlySearchedCategories() {
        return mostFrequentlySearchedCategories;
    }

    public void setMostFrequentlySearchedCategories(
            List<SearchCategoryCount> mostFrequentlySearchedCategories) {
        this.mostFrequentlySearchedCategories = mostFrequentlySearchedCategories;
    }

    public List<DocumentFeedbackSummary> getFeedbackByDocument() {
        return feedbackByDocument;
    }

    public void setFeedbackByDocument(List<DocumentFeedbackSummary> feedbackByDocument) {
        this.feedbackByDocument = feedbackByDocument;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalDocuments;
        private long indexedDocuments;
        private long totalSearches;
        private long totalQuestions;
        private long answeredQuestions;
        private double averageFeedbackRating;
        private List<DocumentCategoryStatusCount> documentCountsByCategoryAndStatus = List.of();
        private List<PendingReindexDocument> pendingReindexDocuments = List.of();
        private List<SearchCategoryCount> mostFrequentlySearchedCategories = List.of();
        private List<DocumentFeedbackSummary> feedbackByDocument = List.of();

        public Builder totalDocuments(long value) {
            this.totalDocuments = value;
            return this;
        }

        public Builder indexedDocuments(long value) {
            this.indexedDocuments = value;
            return this;
        }

        public Builder totalSearches(long value) {
            this.totalSearches = value;
            return this;
        }

        public Builder totalQuestions(long value) {
            this.totalQuestions = value;
            return this;
        }

        public Builder answeredQuestions(long value) {
            this.answeredQuestions = value;
            return this;
        }

        public Builder averageFeedbackRating(double value) {
            this.averageFeedbackRating = value;
            return this;
        }

        public Builder documentCountsByCategoryAndStatus(
                List<DocumentCategoryStatusCount> value) {
            this.documentCountsByCategoryAndStatus = value;
            return this;
        }

        public Builder pendingReindexDocuments(List<PendingReindexDocument> value) {
            this.pendingReindexDocuments = value;
            return this;
        }

        public Builder mostFrequentlySearchedCategories(List<SearchCategoryCount> value) {
            this.mostFrequentlySearchedCategories = value;
            return this;
        }

        public Builder feedbackByDocument(List<DocumentFeedbackSummary> value) {
            this.feedbackByDocument = value;
            return this;
        }

        public AnalyticsResponse build() {
            AnalyticsResponse response = new AnalyticsResponse(
                    totalDocuments,
                    indexedDocuments,
                    totalSearches,
                    totalQuestions,
                    answeredQuestions,
                    averageFeedbackRating);
            response.setDocumentCountsByCategoryAndStatus(documentCountsByCategoryAndStatus);
            response.setPendingReindexDocuments(pendingReindexDocuments);
            response.setMostFrequentlySearchedCategories(mostFrequentlySearchedCategories);
            response.setFeedbackByDocument(feedbackByDocument);
            return response;
        }
    }
}
