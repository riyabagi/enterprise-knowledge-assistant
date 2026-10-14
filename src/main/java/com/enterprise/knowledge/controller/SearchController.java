
package com.enterprise.knowledge.controller;

import com.enterprise.knowledge.dto.SearchRequest;
import com.enterprise.knowledge.dto.SearchResponse;
import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.entity.KnowledgeDocument.AccessLevel;
import com.enterprise.knowledge.repository.AppUserRepository;
import com.enterprise.knowledge.security.AccessLevelEvaluator;
import com.enterprise.knowledge.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/search")
public class SearchController {

    private final SearchService searchService;
    private final AppUserRepository userRepository;
    private final AccessLevelEvaluator accessLevelEvaluator;

    public SearchController(
            SearchService searchService,
            AppUserRepository userRepository,
            AccessLevelEvaluator accessLevelEvaluator) {
        this.searchService = searchService;
        this.userRepository = userRepository;
        this.accessLevelEvaluator = accessLevelEvaluator;
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @Valid SearchRequest request,
            Authentication auth) {
        return ResponseEntity.ok(executeSearch(request, auth));
    }

    @PostMapping
    public ResponseEntity<SearchResponse> searchPost(
            @Valid @RequestBody SearchRequest request,
            Authentication auth) {
        return ResponseEntity.ok(executeSearch(request, auth));
    }

    private SearchResponse executeSearch(
            SearchRequest request,
            Authentication auth) {
        AppUser user = resolveUser(auth);

        List<AccessLevel> levels = accessLevelEvaluator.getAllowedLevels(
                auth.getAuthorities().iterator().next().getAuthority());

        return searchService.search(request, user, levels);
    }

    private AppUser resolveUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in DB"));
    }
}
