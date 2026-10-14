package com.enterprise.knowledge.service;

import com.enterprise.knowledge.dto.DocumentUploadRequest;
import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.entity.KnowledgeDocument;
import com.enterprise.knowledge.exception.ResourceNotFoundException;
import com.enterprise.knowledge.repository.DocumentVersionRepository;
import com.enterprise.knowledge.repository.KnowledgeChunkRepository;
import com.enterprise.knowledge.repository.KnowledgeDocumentRepository;
import com.enterprise.knowledge.repository.KnowledgeFeedbackRepository;
import com.enterprise.knowledge.security.AccessLevelEvaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceAuthorizationTest {

    @Mock
    private KnowledgeDocumentRepository documentRepository;
    @Mock
    private DocumentVersionRepository versionRepository;
    @Mock
    private KnowledgeChunkRepository chunkRepository;
    @Mock
    private AccessLevelEvaluator accessLevelEvaluator;
    @Mock
    private KnowledgeFeedbackRepository feedbackRepository;
    @Mock
    private MultipartFile file;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void rejectsUploadAtAccessLevelNotGrantedToUserBeforeReadingFile() {
        AppUser uploader = AppUser.builder().id(7L).role(AppUser.Role.EMPLOYEE).build();
        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setAccessLevel(KnowledgeDocument.AccessLevel.HR_ONLY);

        when(accessLevelEvaluator.getAllowedLevels(AppUser.Role.EMPLOYEE))
                .thenReturn(List.of(KnowledgeDocument.AccessLevel.PUBLIC, KnowledgeDocument.AccessLevel.INTERNAL));

        assertThrows(AccessDeniedException.class,
                () -> documentService.uploadDocument(file, request, uploader));

        verify(file, never()).isEmpty();
        verify(documentRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(versionRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(chunkRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void hidesArchivedDocumentFromOrdinaryDetailPreviewAndVersionRequests() {
        KnowledgeDocument archived = KnowledgeDocument.builder()
                .id(11L)
                .title("Archived")
                .accessLevel(KnowledgeDocument.AccessLevel.INTERNAL)
                .status(KnowledgeDocument.Status.ARCHIVED)
                .build();
        when(documentRepository.findById(11L)).thenReturn(Optional.of(archived));
        List<KnowledgeDocument.AccessLevel> allowedLevels =
                List.of(KnowledgeDocument.AccessLevel.INTERNAL);

        assertThrows(ResourceNotFoundException.class,
                () -> documentService.getDocument(11L, allowedLevels, false));
        assertThrows(ResourceNotFoundException.class,
                () -> documentService.getPreview(11L, allowedLevels, false));
        assertThrows(ResourceNotFoundException.class,
                () -> documentService.getVersionHistory(11L, allowedLevels, false));

        verify(versionRepository, never()).findByDocumentOrderByVersionNumberDesc(archived);
    }

    @Test
    void allowsAdminToReadArchivedDocumentWhenExplicitlyRequested() {
        KnowledgeDocument archived = KnowledgeDocument.builder()
                .id(11L)
                .title("Archived")
                .accessLevel(KnowledgeDocument.AccessLevel.INTERNAL)
                .status(KnowledgeDocument.Status.ARCHIVED)
                .build();
        when(documentRepository.findById(11L)).thenReturn(Optional.of(archived));

        var response = documentService.getDocument(
                11L, List.of(KnowledgeDocument.AccessLevel.INTERNAL), true);

        assertEquals(11L, response.getId());
    }
}
