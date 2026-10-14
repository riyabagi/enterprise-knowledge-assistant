
package com.enterprise.knowledge.dto;

public class CitationDto {

    private Integer citationIndex;
    private Long chunkId;
    private Long documentId;
    private String documentTitle;
    private Integer versionNumber;
    private String sectionReference;
    private String excerpt;

    public CitationDto() {
    }

    public CitationDto(
            Integer citationIndex,
            Long chunkId,
            Long documentId,
            String documentTitle,
            Integer versionNumber,
            String sectionReference,
            String excerpt) {
        this.citationIndex = citationIndex;
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.versionNumber = versionNumber;
        this.sectionReference = sectionReference;
        this.excerpt = excerpt;
    }

    public Integer getCitationIndex() { return citationIndex; }
    public void setCitationIndex(Integer citationIndex) { this.citationIndex = citationIndex; }

    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public String getSectionReference() { return sectionReference; }
    public void setSectionReference(String sectionReference) { this.sectionReference = sectionReference; }

    public String getExcerpt() { return excerpt; }
    public void setExcerpt(String excerpt) { this.excerpt = excerpt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer citationIndex;
        private Long chunkId;
        private Long documentId;
        private String documentTitle;
        private Integer versionNumber;
        private String sectionReference;
        private String excerpt;

        public Builder citationIndex(Integer citationIndex) { this.citationIndex = citationIndex; return this; }
        public Builder chunkId(Long chunkId) { this.chunkId = chunkId; return this; }
        public Builder documentId(Long documentId) { this.documentId = documentId; return this; }
        public Builder documentTitle(String documentTitle) { this.documentTitle = documentTitle; return this; }
        public Builder versionNumber(Integer versionNumber) { this.versionNumber = versionNumber; return this; }
        public Builder sectionReference(String sectionReference) { this.sectionReference = sectionReference; return this; }
        public Builder excerpt(String excerpt) { this.excerpt = excerpt; return this; }

        public CitationDto build() {
            return new CitationDto(
                    citationIndex, chunkId, documentId, documentTitle,
                    versionNumber, sectionReference, excerpt);
        }
    }
}
