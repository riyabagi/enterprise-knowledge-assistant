
package com.enterprise.knowledge.service;

import com.enterprise.knowledge.dto.AnalyticsResponse;
import com.enterprise.knowledge.dto.AccessibleDocumentSummary;
import com.enterprise.knowledge.dto.RoleAccessReport;
import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.entity.KnowledgeDocument;
import com.enterprise.knowledge.repository.AnswerRecordRepository;
import com.enterprise.knowledge.repository.KnowledgeDocumentRepository;
import com.enterprise.knowledge.repository.KnowledgeFeedbackRepository;
import com.enterprise.knowledge.repository.SearchHistoryRepository;
import com.enterprise.knowledge.security.AccessLevelEvaluator;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final KnowledgeDocumentRepository documentRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final KnowledgeFeedbackRepository feedbackRepository;
    private final AccessLevelEvaluator accessLevelEvaluator;

    public AnalyticsService(
            KnowledgeDocumentRepository documentRepository,
            SearchHistoryRepository searchHistoryRepository,
            AnswerRecordRepository answerRecordRepository,
            KnowledgeFeedbackRepository feedbackRepository,
            AccessLevelEvaluator accessLevelEvaluator) {

        this.documentRepository = documentRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.feedbackRepository = feedbackRepository;
        this.accessLevelEvaluator = accessLevelEvaluator;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics() {

        long total = documentRepository.count();

        long indexed = documentRepository.countByStatus(
                KnowledgeDocument.Status.INDEXED);

        long searches = searchHistoryRepository.count();
        long questions = answerRecordRepository.count();
        long answered = answerRecordRepository.countAnswered();

        Double avgRating = feedbackRepository.averageRating();

        double averageRating = avgRating != null
                ? Math.round(avgRating * 100.0) / 100.0
                : 0.0;

        return AnalyticsResponse.builder()
                .totalDocuments(total)
                .indexedDocuments(indexed)
                .totalSearches(searches)
                .totalQuestions(questions)
                .answeredQuestions(answered)
                .averageFeedbackRating(averageRating)
                .documentCountsByCategoryAndStatus(
                        documentRepository.countDocumentsByCategoryAndStatus())
                .pendingReindexDocuments(
                        documentRepository.findPendingReindexDocuments())
                .mostFrequentlySearchedCategories(
                        searchHistoryRepository.countSearchesBySelectedCategory())
                .feedbackByDocument(feedbackRepository.summarizeRatingsByDocument())
                .build();
    }

    @Transactional(readOnly = true)
    public RoleAccessReport getAccessibleDocuments(AppUser.Role role) {
        List<KnowledgeDocument.AccessLevel> allowedLevels =
                accessLevelEvaluator.getAllowedLevels(role);
        List<AccessibleDocumentSummary> documents = documentRepository
                .findByStatusAndAccessLevelInOrderByTitleAsc(
                        KnowledgeDocument.Status.INDEXED, allowedLevels)
                .stream()
                .map(document -> new AccessibleDocumentSummary(
                        document.getId(),
                        document.getTitle(),
                        document.getCategory(),
                        document.getAccessLevel(),
                        document.getStatus()))
                .toList();
        return new RoleAccessReport(role, documents);
    }
}
