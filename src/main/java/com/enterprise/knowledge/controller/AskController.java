
package com.enterprise.knowledge.controller;

import com.enterprise.knowledge.dto.AskRequest;
import com.enterprise.knowledge.dto.AskResponse;
import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.entity.KnowledgeDocument.AccessLevel;
import com.enterprise.knowledge.repository.AppUserRepository;
import com.enterprise.knowledge.security.AccessLevelEvaluator;
import com.enterprise.knowledge.service.AskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/ask")
public class AskController {

    private final AskService askService;
    private final AppUserRepository userRepository;
    private final AccessLevelEvaluator accessLevelEvaluator;

    public AskController(
            AskService askService,
            AppUserRepository userRepository,
            AccessLevelEvaluator accessLevelEvaluator) {
        this.askService = askService;
        this.userRepository = userRepository;
        this.accessLevelEvaluator = accessLevelEvaluator;
    }

    @PostMapping
    public ResponseEntity<AskResponse> ask(
            @Valid @RequestBody AskRequest request,
            Authentication auth) {

        AppUser user = resolveUser(auth);

        List<AccessLevel> levels = accessLevelEvaluator.getAllowedLevels(
                auth.getAuthorities().iterator().next().getAuthority());

        AskResponse response = askService.ask(
                request.getQuestion(), user, levels);

        return ResponseEntity.ok(response);
    }

    private AppUser resolveUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found in DB"));
    }
}
