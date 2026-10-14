
package com.enterprise.knowledge.controller;

import com.enterprise.knowledge.dto.FeedbackRequest;
import com.enterprise.knowledge.dto.FeedbackResponse;
import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.repository.AppUserRepository;
import com.enterprise.knowledge.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final AppUserRepository userRepository;

    public FeedbackController(
            FeedbackService feedbackService,
            AppUserRepository userRepository) {
        this.feedbackService = feedbackService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            Authentication auth) {

        AppUser user = resolveUser(auth);

        return ResponseEntity.ok(
                feedbackService.submit(request, user));
    }

    private AppUser resolveUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in DB"));
    }
}
