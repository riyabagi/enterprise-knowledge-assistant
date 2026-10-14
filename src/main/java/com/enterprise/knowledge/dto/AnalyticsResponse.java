
package com.enterprise.knowledge.dto;

public class AnalyticsResponse {

    private long totalDocuments;
    private long indexedDocuments;
    private long totalSearches;
    private long totalQuestions;
    private long answeredQuestions;
    private double averageFeedbackRating;

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

        public AnalyticsResponse build() {
            return new AnalyticsResponse(
                    totalDocuments,
                    indexedDocuments,
                    totalSearches,
                    totalQuestions,
                    answeredQuestions,
                    averageFeedbackRating);
        }
    }
}
