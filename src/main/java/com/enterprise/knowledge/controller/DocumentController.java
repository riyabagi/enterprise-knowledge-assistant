
package com.enterprise.knowledge.controller;

import com.enterprise.knowledge.dto.*;
import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.entity.KnowledgeDocument;
import com.enterprise.knowledge.entity.KnowledgeDocument.AccessLevel;
import com.enterprise.knowledge.repository.AppUserRepository;
import com.enterprise.knowledge.security.AccessLevelEvaluator;
import com.enterprise.knowledge.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final AppUserRepository userRepository;
    private final AccessLevelEvaluator accessLevelEvaluator;

    public DocumentController(
            DocumentService documentService,
            AppUserRepository userRepository,
            AccessLevelEvaluator accessLevelEvaluator) {
        this.documentService = documentService;
        this.userRepository = userRepository;
        this.accessLevelEvaluator = accessLevelEvaluator;
    }

    // Upload
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute DocumentUploadRequest request,
            Authentication auth) {
        AppUser user = resolveUser(auth);
        return ResponseEntity.status(201)
                .body(documentService.uploadDocument(file, request, user));
    }

    // Reindex
    @PutMapping(value = "/{id}/reindex", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> reindex(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        AppUser user = resolveUser(auth);
        return ResponseEntity.ok(documentService.reindex(id, file, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> updateMetadata(
            @PathVariable Long id,
            @Valid @RequestBody DocumentMetadataUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                documentService.updateMetadata(id, request, resolveUser(auth)));
    }

    // Archive
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> archive(
            @PathVariable Long id, Authentication auth) {
        AppUser user = resolveUser(auth);
        return ResponseEntity.ok(documentService.archive(id, user));
    }

    // Delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id, Authentication auth) {
        AppUser user = resolveUser(auth);
        documentService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    // List
    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        List<AccessLevel> levels = resolvedLevels(auth);
        KnowledgeDocument.Category cat = null;

        if (category != null && !category.isBlank()) {
            cat = KnowledgeDocument.Category.valueOf(category.toUpperCase());
        }

        PageRequest pageable = PageRequest.of(
                page,
                Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "uploadedAt"));

        return ResponseEntity.ok(
                documentService.listDocuments(levels, cat, pageable));
    }

    // Get single document
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> get(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(
                documentService.getDocument(
                        id, resolvedLevels(auth), isAdmin(auth)));
    }

    // Preview
    @GetMapping("/{id}/preview")
    public ResponseEntity<String> preview(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(
                documentService.getPreview(
                        id, resolvedLevels(auth), isAdmin(auth)));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<DocumentContentResponse> content(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(
                documentService.getContent(
                        id, resolvedLevels(auth), isAdmin(auth)));
    }

    // Version history
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> versions(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(
                documentService.getVersionHistory(
                        id, resolvedLevels(auth), isAdmin(auth)));
    }

    // Helpers
    private AppUser resolveUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in DB"));
    }

    private List<AccessLevel> resolvedLevels(Authentication auth) {
        return accessLevelEvaluator.getAllowedLevels(
                auth.getAuthorities().iterator().next().getAuthority());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
