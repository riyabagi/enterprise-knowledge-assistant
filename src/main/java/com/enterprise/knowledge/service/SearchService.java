
package com.enterprise.knowledge.service;

import com.enterprise.knowledge.dto.*;
import com.enterprise.knowledge.entity.*;
import com.enterprise.knowledge.entity.KnowledgeDocument.AccessLevel;
import com.enterprise.knowledge.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final KnowledgeChunkRepository chunkRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    // Explicit constructor: no Lombok @RequiredArgsConstructor needed
    public SearchService(
            KnowledgeChunkRepository chunkRepository,
            SearchHistoryRepository searchHistoryRepository) {
        this.chunkRepository = chunkRepository;
        this.searchHistoryRepository = searchHistoryRepository;
    }

    @Transactional
    public SearchResponse search(
            SearchRequest request,
            AppUser user,
            List<AccessLevel> allowedLevels) {

        List<String> levelNames = allowedLevels.stream()
                .map(AccessLevel::name)
                .collect(Collectors.toList());

        String ftQuery = SearchQuery.booleanMode(request.getQuery());
        String category = request.getCategory() == null
                ? null
                : request.getCategory().name();

        long totalResults = chunkRepository.countFulltextDocumentSearch(
                ftQuery, levelNames, category);
        List<KnowledgeChunk> chunks;
        if (totalResults > 0) {
            chunks = chunkRepository.fulltextDocumentSearch(
                    ftQuery, levelNames, category, request.toPageable());
        } else {
            totalResults = chunkRepository.countFallbackDocumentSearch(
                    ftQuery, levelNames, category);
            chunks = chunkRepository.fallbackDocumentSearch(
                    ftQuery, levelNames, category, request.toPageable());
        }

        // Record search history
        SearchHistory history = SearchHistory.builder()
                .user(user)
                .query(request.getQuery())
                .categoryFilter(
                        request.getCategory() != null
                                ? request.getCategory().name()
                                : null)
                .resultCount(Math.toIntExact(totalResults))
                .build();

        searchHistoryRepository.save(history);

        List<SearchResultItem> results = chunks.stream()
                .map(c -> {
                    DocumentVersion version = c.getDocumentVersion();
                    KnowledgeDocument doc = version.getDocument();

                    String raw = c.getContent();
                    String snippet = raw.length() > 300
                            ? raw.substring(0, 300) + "..."
                            : raw;

                    return SearchResultItem.builder()
                            .chunkId(c.getId())
                            .documentId(doc.getId())
                            .documentTitle(doc.getTitle())
                            .category(doc.getCategory().name())
                            .accessLevel(doc.getAccessLevel().name())
                            .versionNumber(version.getVersionNumber())
                            .sectionReference(c.getSectionReference())
                            .snippet(snippet)
                            .build();
                })
                .collect(Collectors.toList());

        log.debug("Search returned {} distinct documents for user={}",
                results.size(), user.getUsername());

        return SearchResponse.builder()
                .query(request.getQuery())
                .totalResults(totalResults)
                .page(request.getPage())
                .results(results)
                .build();
    }

}
