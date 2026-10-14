
package com.enterprise.knowledge.dto;

import java.util.List;

public class SearchResponse {

    private String query;
    private long totalResults;
    private int page;
    private List<SearchResultItem> results;

    public SearchResponse() {
    }

    public SearchResponse(
            String query,
            long totalResults,
            int page,
            List<SearchResultItem> results) {
        this.query = query;
        this.totalResults = totalResults;
        this.page = page;
        this.results = results;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(long totalResults) {
        this.totalResults = totalResults;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<SearchResultItem> getResults() {
        return results;
    }

    public void setResults(List<SearchResultItem> results) {
        this.results = results;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query;
        private long totalResults;
        private int page;
        private List<SearchResultItem> results;

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder totalResults(long totalResults) {
            this.totalResults = totalResults;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder results(List<SearchResultItem> results) {
            this.results = results;
            return this;
        }

        public SearchResponse build() {
            return new SearchResponse(
                    query,
                    totalResults,
                    page,
                    results);
        }
    }
}
