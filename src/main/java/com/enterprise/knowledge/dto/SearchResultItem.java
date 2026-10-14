
package com.enterprise.knowledge.dto;

public class SearchResultItem {

    private Long chunkId;
    private Long documentId;
    private String documentTitle;
    private Integer versionNumber;
    private String sectionReference;
    private String snippet;
    private String accessLevel;
    private String category;

    public SearchResultItem() {
    }

    public SearchResultItem(
            Long chunkId,
            Long documentId,
            String documentTitle,
            Integer versionNumber,
            String sectionReference,
            String snippet,
            String accessLevel,
            String category) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.versionNumber = versionNumber;
        this.sectionReference = sectionReference;
        this.snippet = snippet;
        this.accessLevel = accessLevel;
        this.category = category;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getSectionReference() {
        return sectionReference;
    }

    public void setSectionReference(String sectionReference) {
        this.sectionReference = sectionReference;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long chunkId;
        private Long documentId;
        private String documentTitle;
        private Integer versionNumber;
        private String sectionReference;
        private String snippet;
        private String accessLevel;
        private String category;

        public Builder chunkId(Long chunkId) {
            this.chunkId = chunkId;
            return this;
        }

        public Builder documentId(Long documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder documentTitle(String documentTitle) {
            this.documentTitle = documentTitle;
            return this;
        }

        public Builder versionNumber(Integer versionNumber) {
            this.versionNumber = versionNumber;
            return this;
        }

        public Builder sectionReference(String sectionReference) {
            this.sectionReference = sectionReference;
            return this;
        }

        public Builder snippet(String snippet) {
            this.snippet = snippet;
            return this;
        }

        public Builder accessLevel(String accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public SearchResultItem build() {
            return new SearchResultItem(
                    chunkId,
                    documentId,
                    documentTitle,
                    versionNumber,
                    sectionReference,
                    snippet,
                    accessLevel,
                    category);
        }
    }
}
