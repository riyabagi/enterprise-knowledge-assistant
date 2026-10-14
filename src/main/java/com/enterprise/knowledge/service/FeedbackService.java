
package com.enterprise.knowledge.service;

import com.enterprise.knowledge.dto.FeedbackRequest;
import com.enterprise.knowledge.dto.FeedbackResponse;
import com.enterprise.knowledge.entity.*;
import com.enterprise.knowledge.exception.ResourceNotFoundException;
import com.enterprise.knowledge.repository.*;
import com.enterprise.knowledge.security.AccessLevelEvaluator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class FeedbackService {

    private static final Logger log =
            LoggerFactory.getLogger(FeedbackService.class);

    private final KnowledgeFeedbackRepository feedbackRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final AccessLevelEvaluator accessLevelEvaluator;

    // Explicit constructor replaces @RequiredArgsConstructor
    public FeedbackService(
            KnowledgeFeedbackRepository feedbackRepository,
            KnowledgeDocumentRepository documentRepository,
            AnswerRecordRepository answerRecordRepository,
            AccessLevelEvaluator accessLevelEvaluator) {

        this.feedbackRepository = feedbackRepository;
        this.documentRepository = documentRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.accessLevelEvaluator = accessLevelEvaluator;
    }

    @Transactional
    public FeedbackResponse submit(
            FeedbackRequest request, AppUser user) {

        KnowledgeDocument doc = documentRepository
                .findById(request.getDocumentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found: "
                                        + request.getDocumentId()));

        List<KnowledgeDocument.AccessLevel> allowedLevels =
                accessLevelEvaluator.getAllowedLevels(user.getRole());

        if (!allowedLevels.contains(doc.getAccessLevel())) {
            throw new AccessDeniedException(
                    "User cannot submit feedback for this document.");
        }

        AnswerRecord answerRecord = null;

        if (request.getAnswerRecordId() != null) {
            answerRecord = answerRecordRepository
                    .findById(request.getAnswerRecordId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Answer record not found: "
                                            + request.getAnswerRecordId()));

            if (user.getRole() != AppUser.Role.ADMIN
                    && !Objects.equals(
                            answerRecord.getUser().getId(),
                            user.getId())) {

                throw new AccessDeniedException(
                        "User cannot submit feedback for another user's answer.");
            }
        }

        KnowledgeFeedback feedback = KnowledgeFeedback.builder()
                .user(user)
                .question(request.getQuestion())
                .answerRecord(answerRecord)
                .document(doc)
                .rating((byte) request.getRating().intValue())
                .comment(request.getComment())
                .build();

        feedback = feedbackRepository.save(feedback);

        log.info(
                "Feedback saved: id={} rating={} user={}",
                feedback.getId(),
                feedback.getRating(),
                user.getUsername());

        return FeedbackResponse.builder()
                .id(feedback.getId())
                .documentId(doc.getId())
                .rating(request.getRating())
                .message("Thank you for your feedback!")
                .build();
    }
}
