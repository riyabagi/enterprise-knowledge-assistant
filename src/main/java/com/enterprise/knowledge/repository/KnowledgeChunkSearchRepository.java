package com.enterprise.knowledge.repository;

import com.enterprise.knowledge.entity.KnowledgeChunk;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface KnowledgeChunkSearchRepository {

    List<KnowledgeChunk> fulltextSearch(String query, Collection<String> accessLevels, Pageable pageable);

    long countFulltextSearch(String query, Collection<String> accessLevels);

    List<KnowledgeChunk> fulltextSearchWithCategory(
            String query, Collection<String> accessLevels, String category, Pageable pageable);

    long countFulltextSearchWithCategory(String query, Collection<String> accessLevels, String category);

    List<KnowledgeChunk> fulltextDocumentSearch(
            String query, Collection<String> accessLevels, String category, Pageable pageable);

    long countFulltextDocumentSearch(
            String query, Collection<String> accessLevels, String category);

    List<KnowledgeChunk> fallbackDocumentSearch(
            String query, Collection<String> accessLevels, String category, Pageable pageable);

    long countFallbackDocumentSearch(
            String query, Collection<String> accessLevels, String category);

    List<KnowledgeChunk> fallbackContentSearch(
            String query, Collection<String> accessLevels, Pageable pageable);
}
