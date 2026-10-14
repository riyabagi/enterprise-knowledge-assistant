package com.enterprise.knowledge.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EntityBuilderDefaultsTest {

    @Test
    void knowledgeDocumentBuilderUsesEntityDefaults() {
        KnowledgeDocument document = KnowledgeDocument.builder().build();

        assertEquals(KnowledgeDocument.Category.GENERAL, document.getCategory());
        assertEquals(KnowledgeDocument.AccessLevel.INTERNAL, document.getAccessLevel());
        assertEquals(KnowledgeDocument.Status.DRAFT, document.getStatus());
        assertFalse(document.getReindexRequired());
    }

    @Test
    void documentVersionBuilderUsesEntityDefaults() {
        DocumentVersion version = DocumentVersion.builder().build();

        assertEquals(1, version.getVersionNumber());
        assertEquals(DocumentVersion.Status.DRAFT, version.getStatus());
    }

    @Test
    void searchHistoryBuilderUsesEntityDefault() {
        SearchHistory history = SearchHistory.builder().build();

        assertEquals(0, history.getResultCount());
    }
}
