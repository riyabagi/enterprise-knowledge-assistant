package com.enterprise.knowledge.service;

import com.enterprise.knowledge.dto.FeedbackRequest;
import com.enterprise.knowledge.dto.FeedbackResponse;
import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.entity.AnswerRecord;
import com.enterprise.knowledge.entity.KnowledgeDocument;
import com.enterprise.knowledge.entity.KnowledgeFeedback;
import com.enterprise.knowledge.exception.ResourceNotFoundException;
import com.enterprise.knowledge.repository.AnswerRecordRepository;
import com.enterprise.knowledge.repository.KnowledgeDocumentRepository;
import com.enterprise.knowledge.repository.KnowledgeFeedbackRepository;
import com.enterprise.knowledge.security.AccessLevelEvaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private KnowledgeFeedbackRepository feedbackRepository;
    @Mock
    private KnowledgeDocumentRepository documentRepository;
    @Mock
    private AnswerRecordRepository answerRecordRepository;
    @Mock
    private AccessLevelEvaluator accessLevelEvaluator;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void submitsFeedbackForAccessibleDocumentAndOwnedAnswer() {
        AppUser user = user(7L, AppUser.Role.EMPLOYEE);
        KnowledgeDocument document = document(11L, KnowledgeDocument.AccessLevel.INTERNAL);
        AnswerRecord answer = answer(21L, user);
        FeedbackRequest request = request(11L, 21L);

        when(documentRepository.findById(11L)).thenReturn(Optional.of(document));
        when(accessLevelEvaluator.getAllowedLevels(AppUser.Role.EMPLOYEE))
                .thenReturn(List.of(KnowledgeDocument.AccessLevel.INTERNAL));
        when(answerRecordRepository.findById(21L)).thenReturn(Optional.of(answer));
        when(feedbackRepository.save(any(KnowledgeFeedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackResponse response = feedbackService.submit(request, user);

        ArgumentCaptor<KnowledgeFeedback> feedbackCaptor = ArgumentCaptor.forClass(KnowledgeFeedback.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        assertEquals(user, feedbackCaptor.getValue().getUser());
        assertEquals(document, feedbackCaptor.getValue().getDocument());
        assertEquals(answer, feedbackCaptor.getValue().getAnswerRecord());
        assertEquals(11L, response.getDocumentId());
        assertEquals(5, response.getRating());
    }

    @Test
    void rejectsFeedbackForDocumentOutsideCallersAccessPolicy() {
        AppUser user = user(7L, AppUser.Role.EMPLOYEE);
        when(documentRepository.findById(11L))
                .thenReturn(Optional.of(document(11L, KnowledgeDocument.AccessLevel.HR_ONLY)));
        when(accessLevelEvaluator.getAllowedLevels(AppUser.Role.EMPLOYEE))
                .thenReturn(List.of(KnowledgeDocument.AccessLevel.PUBLIC, KnowledgeDocument.AccessLevel.INTERNAL));

        assertThrows(AccessDeniedException.class,
                () -> feedbackService.submit(request(11L, null), user));

        verify(feedbackRepository, never()).save(any(KnowledgeFeedback.class));
        verify(answerRecordRepository, never()).findById(any());
    }

    @Test
    void rejectsMissingAnswerRecordInsteadOfDroppingAssociation() {
        AppUser user = user(7L, AppUser.Role.EMPLOYEE);
        when(documentRepository.findById(11L))
                .thenReturn(Optional.of(document(11L, KnowledgeDocument.AccessLevel.INTERNAL)));
        when(accessLevelEvaluator.getAllowedLevels(AppUser.Role.EMPLOYEE))
                .thenReturn(List.of(KnowledgeDocument.AccessLevel.INTERNAL));
        when(answerRecordRepository.findById(21L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> feedbackService.submit(request(11L, 21L), user));

        verify(feedbackRepository, never()).save(any(KnowledgeFeedback.class));
    }

    @Test
    void rejectsAnswerOwnedByAnotherNonAdminUser() {
        AppUser user = user(7L, AppUser.Role.EMPLOYEE);
        AppUser otherUser = user(8L, AppUser.Role.HR);
        when(documentRepository.findById(11L))
                .thenReturn(Optional.of(document(11L, KnowledgeDocument.AccessLevel.INTERNAL)));
        when(accessLevelEvaluator.getAllowedLevels(AppUser.Role.EMPLOYEE))
                .thenReturn(List.of(KnowledgeDocument.AccessLevel.INTERNAL));
        when(answerRecordRepository.findById(21L))
                .thenReturn(Optional.of(answer(21L, otherUser)));

        assertThrows(AccessDeniedException.class,
                () -> feedbackService.submit(request(11L, 21L), user));

        verify(feedbackRepository, never()).save(any(KnowledgeFeedback.class));
    }

    private static FeedbackRequest request(Long documentId, Long answerRecordId) {
        FeedbackRequest request = new FeedbackRequest();
        request.setDocumentId(documentId);
        request.setAnswerRecordId(answerRecordId);
        request.setRating(5);
        request.setQuestion("Question");
        request.setComment("Helpful");
        return request;
    }

    private static AppUser user(Long id, AppUser.Role role) {
        return AppUser.builder().id(id).username("user-" + id).role(role).build();
    }

    private static KnowledgeDocument document(Long id, KnowledgeDocument.AccessLevel accessLevel) {
        return KnowledgeDocument.builder()
                .id(id)
                .title("Document " + id)
                .accessLevel(accessLevel)
                .build();
    }

    private static AnswerRecord answer(Long id, AppUser owner) {
        return AnswerRecord.builder().id(id).user(owner).build();
    }
}
