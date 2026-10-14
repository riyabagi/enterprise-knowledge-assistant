package com.enterprise.knowledge.dto;

public class DocumentContentResponse {

    private Long documentId;
    private String documentTitle;
    private Integer versionNumber;
    private String originalFilename;
    private String content;

    public DocumentContentResponse() {
    }

    public DocumentContentResponse(
            Long documentId,
            String documentTitle,
            Integer versionNumber,
            String originalFilename,
            String content) {
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.versionNumber = versionNumber;
        this.originalFilename = originalFilename;
        this.content = content;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContent() {
        return content;
    }
}
