package com.enterprise.knowledge.service;

import com.enterprise.knowledge.dto.SearchRequest;
import com.enterprise.knowledge.dto.SearchResponse;
import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.entity.KnowledgeDocument;
import com.enterprise.knowledge.entity.KnowledgeDocument.AccessLevel;
import com.enterprise.knowledge.repository.KnowledgeChunkRepository;
import com.enterprise.knowledge.repository.SearchHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private KnowledgeChunkRepository chunkRepository;
    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    private SearchService searchService;

    @Test
    void returnsTotalMatchCountRatherThanCurrentPageSize() {
        SearchRequest request = new SearchRequest();
        request.setQuery("policy");
        request.setPage(2);
        request.setSize(10);
        AppUser user = AppUser.builder().id(4L).username("employee").build();
        List<String> allowedLevels = List.of(KnowledgeDocument.AccessLevel.INTERNAL.name());

        when(chunkRepository.fulltextDocumentSearch(any(), any(), any(), any())).thenReturn(List.of());
        when(chunkRepository.countFulltextDocumentSearch(any(), any(), any())).thenReturn(27L);

        SearchResponse response = searchService.search(
                request, user, List.of(KnowledgeDocument.AccessLevel.INTERNAL));

        assertEquals(27L, response.getTotalResults());
        assertEquals(2, response.getPage());
        assertEquals(0, response.getResults().size());
        ArgumentCaptor<com.enterprise.knowledge.entity.SearchHistory> historyCaptor =
                ArgumentCaptor.forClass(com.enterprise.knowledge.entity.SearchHistory.class);
        verify(searchHistoryRepository).save(historyCaptor.capture());
        assertEquals(27, historyCaptor.getValue().getResultCount());
        verify(chunkRepository).fulltextDocumentSearch(
                "policy", allowedLevels, null, request.toPageable());
    }

    @Test
    void searchesTitlesAndDescriptionsWhenFulltextHasNoMatches() {
        SearchRequest request = new SearchRequest();
        request.setQuery("company VPN");
        AppUser user = AppUser.builder().id(4L).username("employee").build();
        List<AccessLevel> allowedLevels = List.of(KnowledgeDocument.AccessLevel.INTERNAL);
        List<String> levelNames = List.of(KnowledgeDocument.AccessLevel.INTERNAL.name());

        when(chunkRepository.countFulltextDocumentSearch("company vpn", levelNames, null)).thenReturn(0L);
        when(chunkRepository.countFallbackDocumentSearch("company vpn", levelNames, null)).thenReturn(1L);
        when(chunkRepository.fallbackDocumentSearch(any(), any(), any(), any())).thenReturn(List.of());

        SearchResponse response = searchService.search(request, user, allowedLevels);

        assertEquals(1L, response.getTotalResults());
        verify(chunkRepository).fallbackDocumentSearch(
                "company vpn", levelNames, null, request.toPageable());
    }

    @Test
    void normalizesNaturalLanguageAndPunctuationWithoutLosingTwoCharacterKeywords() {
        assertEquals("company vpn", SearchQuery.booleanMode("What is the company VPN?"));
        assertEquals("it", SearchQuery.booleanMode("IT!"));
        assertEquals("", SearchQuery.booleanMode("..."));
    }
}
