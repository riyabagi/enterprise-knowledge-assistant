package com.enterprise.knowledge.controller;

import com.enterprise.knowledge.entity.AccessPolicy;
import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.entity.DocumentVersion;
import com.enterprise.knowledge.entity.KnowledgeDocument;
import com.enterprise.knowledge.entity.AnswerRecord;
import com.enterprise.knowledge.entity.KnowledgeFeedback;
import com.enterprise.knowledge.repository.AccessPolicyRepository;
import com.enterprise.knowledge.repository.AnswerRecordRepository;
import com.enterprise.knowledge.repository.KnowledgeChunkRepository;
import com.enterprise.knowledge.repository.KnowledgeDocumentRepository;
import com.enterprise.knowledge.repository.KnowledgeFeedbackRepository;
import com.enterprise.knowledge.repository.DocumentVersionRepository;
import com.enterprise.knowledge.repository.AppUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class KnowledgeUploadSearchIntegrationTest {

    private static final String PASSWORD = "integration-test-password";

    @Autowired
    private TestRestTemplate http;
    private final Map<String, SessionRestTemplate> sessions = new HashMap<>();
    private SessionRestTemplate restTemplate;
    @LocalServerPort
    private int port;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private AccessPolicyRepository accessPolicyRepository;
    @Autowired
    private KnowledgeDocumentRepository documentRepository;
    @Autowired
    private DocumentVersionRepository versionRepository;
    @Autowired
    private KnowledgeChunkRepository chunkRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AnswerRecordRepository answerRecordRepository;
    @Autowired
    private KnowledgeFeedbackRepository feedbackRepository;
    @MockBean
    private ChatClient chatClient;

    @BeforeEach
    void initializeSessionClient() {
        restTemplate = new SessionRestTemplate(null, null);
        sessions.clear();
    }

    @Test
    void uploadsPersistsChunksAndReturnsDocumentFromSearch() throws Exception {
        String username = createEmployee();

        String content = "Helios retirement eligibility requires manager approval and documented service.";
        HttpEntity<MultiValueMap<String, Object>> uploadRequest =
                uploadRequest("tc-05-01.txt", content.getBytes(StandardCharsets.UTF_8));

        ResponseEntity<String> unauthenticated =
                restTemplate.postForEntity("/api/knowledge/documents", uploadRequest, String.class);
        assertEquals(403, unauthenticated.getStatusCode().value(),
                "CSRF rejects unsafe anonymous requests before authentication");

        ResponseEntity<String> uploadResponse = restTemplate.withSessionLogin(username, PASSWORD)
                .postForEntity("/api/knowledge/documents", uploadRequest, String.class);
        assertEquals(201, uploadResponse.getStatusCode().value(), uploadResponse.getBody());
        JsonNode uploaded = objectMapper.readTree(uploadResponse.getBody());
        long documentId = uploaded.path("id").asLong();
        assertTrue(documentId > 0);

        KnowledgeDocument persistedDocument = documentRepository.findById(documentId).orElseThrow();
        assertEquals(KnowledgeDocument.Status.INDEXED, persistedDocument.getStatus());
        assertEquals(1, versionRepository.countByDocument(persistedDocument));
        assertEquals(1, chunkRepository.findByDocumentVersionId(
                persistedDocument.getCurrentVersion().getId()).size(),
                "short documents should produce one chunk rather than duplicated suffixes");

        java.net.URI searchUri = UriComponentsBuilder
                .fromUriString("http://localhost:" + port + "/api/knowledge/search")
                .queryParam("query", "Helios retirement eligibility")
                .build()
                .encode()
                .toUri();
        ResponseEntity<String> searchResponse = restTemplate.withSessionLogin(username, PASSWORD)
                .getForEntity(searchUri, String.class);
        assertEquals(200, searchResponse.getStatusCode().value(), searchResponse.getBody());
        JsonNode results = objectMapper.readTree(searchResponse.getBody()).path("results");
        assertTrue(results.isArray());
        assertTrue(containsDocument(results, documentId),
                "uploaded document was not returned by search: " + searchResponse.getBody());
    }

    @Test
    void adminDeletesDocumentAndCleansUpRelatedRecords() throws Exception {
        String admin = createUser(AppUser.Role.ADMIN);
        ensureAccessPolicy(AppUser.Role.ADMIN, KnowledgeDocument.AccessLevel.INTERNAL);
        long documentId = uploadDocument(
                admin, "delete-me.txt", "Document content to remove.", "GENERAL", "INTERNAL");
        KnowledgeDocument document = documentRepository.findById(documentId).orElseThrow();
        List<DocumentVersion> versions =
                versionRepository.findByDocumentOrderByVersionNumberDesc(document);
        List<Long> versionIds = versions.stream().map(DocumentVersion::getId).toList();
        AppUser user = userRepository.findByUsername(admin).orElseThrow();
        AnswerRecord answer = answerRecordRepository.save(AnswerRecord.builder()
                .user(user)
                .question("Test answer to preserve.")
                .answerText("This answer remains available.")
                .status(AnswerRecord.Status.ANSWERED)
                .build());
        KnowledgeFeedback feedback = feedbackRepository.save(KnowledgeFeedback.builder()
                .user(user)
                .answerRecord(answer)
                .document(document)
                .rating((byte) 5)
                .comment("Preserve this feedback.")
                .build());

        ResponseEntity<String> response = restTemplate.withSessionLogin(admin, PASSWORD)
                .exchange("/api/knowledge/documents/" + documentId, HttpMethod.DELETE, null, String.class);

        assertEquals(204, response.getStatusCode().value());
        assertTrue(documentRepository.findById(documentId).isEmpty());
        assertTrue(versionIds.stream().noneMatch(versionRepository::existsById));
        assertEquals(0, chunkRepository.countByDocumentVersionIdIn(versionIds));
        KnowledgeFeedback retainedFeedback = feedbackRepository.findById(feedback.getId()).orElseThrow();
        assertNull(retainedFeedback.getDocument(),
                "feedback history is retained with its deleted document reference cleared");
    }

    @Test
    void rejectsDeletionOfHrOnlyDocumentByHrUser() throws Exception {
        String admin = createUser(AppUser.Role.ADMIN);
        ensureAccessPolicy(AppUser.Role.ADMIN, KnowledgeDocument.AccessLevel.HR_ONLY);
        long documentId = uploadDocument(
                admin, "hr-confidential.txt", "Confidential HR content.", "HR", "HR_ONLY");
        String hrUser = createUser(AppUser.Role.HR);
        KnowledgeDocument document = documentRepository.findById(documentId).orElseThrow();
        int versionCount = versionRepository.countByDocument(document);

        ResponseEntity<String> response = restTemplate.withSessionLogin(hrUser, PASSWORD)
                .exchange("/api/knowledge/documents/" + documentId, HttpMethod.DELETE, null, String.class);

        assertEquals(403, response.getStatusCode().value(), response.getBody());
        assertTrue(documentRepository.existsById(documentId));
        assertEquals(versionCount, versionRepository.countByDocument(document));
    }

    @Test
    void reportsNotFoundWhenAdminDeletesMissingDocument() {
        String admin = createUser(AppUser.Role.ADMIN);

        ResponseEntity<String> response = restTemplate.withSessionLogin(admin, PASSWORD)
                .exchange("/api/knowledge/documents/9223372036854775807",
                        HttpMethod.DELETE, null, String.class);

        assertEquals(404, response.getStatusCode().value(), response.getBody());
        assertTrue(response.getBody().contains("Document not found"));
    }

    @Test
    void rejectsUnsupportedAndCorruptFilesWithoutPersistingDocuments() {
        String username = createEmployee();
        long documentsBefore = documentRepository.count();
        long versionsBefore = versionRepository.count();
        long chunksBefore = chunkRepository.count();

        ResponseEntity<String> unsupportedResponse = restTemplate.withSessionLogin(username, PASSWORD)
                .postForEntity("/api/knowledge/documents",
                        uploadRequest("malware.exe", "not a document".getBytes(StandardCharsets.UTF_8)),
                        String.class);
        ResponseEntity<String> corruptPdfResponse = restTemplate.withSessionLogin(username, PASSWORD)
                .postForEntity("/api/knowledge/documents",
                        uploadRequest("corrupt.pdf", "not a PDF".getBytes(StandardCharsets.UTF_8)),
                        String.class);

        assertEquals(400, unsupportedResponse.getStatusCode().value(), unsupportedResponse.getBody());
        assertEquals(400, corruptPdfResponse.getStatusCode().value(), corruptPdfResponse.getBody());
        assertTrue(unsupportedResponse.getBody().contains("Unsupported file type"));
        assertTrue(corruptPdfResponse.getBody().contains("valid PDF"));
        assertEquals(documentsBefore, documentRepository.count(),
                "invalid uploads must not create document records");
        assertEquals(versionsBefore, versionRepository.count(),
                "invalid uploads must not create document versions");
        assertEquals(chunksBefore, chunkRepository.count(),
                "invalid uploads must not create document chunks");
    }

    @Test
    void extractsValidPdfTxtAndMarkdownContent() throws Exception {
        String username = createEmployee();
        SessionRestTemplate session = restTemplate.withSessionLogin(username, PASSWORD);
        long versionsBefore = versionRepository.count();
        long chunksBefore = chunkRepository.count();

        long pdfId = uploadAndReadDocumentId(session,
                uploadRequest("valid.pdf", validPdf("PDF extraction works.").toByteArray()));
        long txtId = uploadAndReadDocumentId(session,
                uploadRequest("valid.txt", "Plain text extraction works.".getBytes(StandardCharsets.UTF_8)));
        long markdownId = uploadAndReadDocumentId(session,
                uploadRequest("valid.md", "# Markdown\n\nMarkdown extraction works."
                        .getBytes(StandardCharsets.UTF_8), "GENERAL", "INTERNAL", "text/markdown"));

        assertTrue(extractedTextContains(pdfId, "PDF extraction works."));
        assertTrue(extractedTextContains(txtId, "Plain text extraction works."));
        assertTrue(extractedTextContains(markdownId, "Markdown extraction works."));
        assertEquals(versionsBefore + 3, versionRepository.count());
        assertTrue(chunkRepository.count() >= chunksBefore + 3);
    }

    @Test
    void rejectsEmptyTruncatedAndMislabeledPdfsAsBadRequestWithoutPartialRecords() throws Exception {
        String username = createEmployee();
        SessionRestTemplate session = restTemplate.withSessionLogin(username, PASSWORD);
        long documentsBefore = documentRepository.count();
        long versionsBefore = versionRepository.count();
        long chunksBefore = chunkRepository.count();

        ResponseEntity<String> emptyPdf = session.postForEntity("/api/knowledge/documents",
                uploadRequest("empty.pdf", new byte[0]), String.class);
        ResponseEntity<String> truncatedPdf = session.postForEntity("/api/knowledge/documents",
                uploadRequest("truncated.pdf", "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII)), String.class);
        ResponseEntity<String> pdfNamedAsText = session.postForEntity("/api/knowledge/documents",
                uploadRequest("mislabeled.txt", validPdf("This is PDF bytes.")
                        .toByteArray()), String.class);
        ResponseEntity<String> pdfWithWrongContentType = session.postForEntity("/api/knowledge/documents",
                uploadRequest("wrong-content-type.pdf", validPdf("PDF content type check.")
                        .toByteArray(), "GENERAL", "INTERNAL", "text/plain"), String.class);
        ResponseEntity<String> textNamedAsPdf = session.postForEntity("/api/knowledge/documents",
                uploadRequest("text-is-not-pdf.pdf", "This is plain text."
                        .getBytes(StandardCharsets.UTF_8)), String.class);

        for (ResponseEntity<String> response : List.of(
                emptyPdf, truncatedPdf, pdfNamedAsText, pdfWithWrongContentType, textNamedAsPdf)) {
            assertEquals(400, response.getStatusCode().value(), response.getBody());
            assertTrue(response.getBody().contains("\"message\""), response.getBody());
        }
        assertTrue(emptyPdf.getBody().contains("must not be empty"));
        assertTrue(truncatedPdf.getBody().contains("invalid, truncated, or unreadable"));
        assertTrue(pdfNamedAsText.getBody().contains("PDF content must be uploaded with a .pdf filename"));
        assertTrue(pdfWithWrongContentType.getBody().contains("content type does not match"));
        assertTrue(textNamedAsPdf.getBody().contains("not a valid PDF"));
        assertEquals(documentsBefore, documentRepository.count());
        assertEquals(versionsBefore, versionRepository.count());
        assertEquals(chunksBefore, chunkRepository.count());
    }

    @Test
    void authenticatedAskRetrievesEvidenceAndPersistsGroundedAnswer() throws Exception {
        String username = createEmployee();
        String content = "Helios retirement eligibility requires manager approval and documented service.";
        ResponseEntity<String> uploadResponse = restTemplate.withSessionLogin(username, PASSWORD)
                .postForEntity("/api/knowledge/documents",
                        uploadRequest("tc-05-03.txt", content.getBytes(StandardCharsets.UTF_8)),
                        String.class);
        assertEquals(201, uploadResponse.getStatusCode().value(), uploadResponse.getBody());

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(
                "Helios retirement eligibility requires manager approval. [1]");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        long answersBefore = answerRecordRepository.count();
        ResponseEntity<String> askResponse = restTemplate.withSessionLogin(username, PASSWORD)
                .postForEntity("/api/knowledge/ask",
                        new HttpEntity<>("{\"question\":\"What does Helios retirement eligibility require?\"}", headers),
                        String.class);

        assertEquals(200, askResponse.getStatusCode().value(), askResponse.getBody());
        JsonNode response = objectMapper.readTree(askResponse.getBody());
        assertEquals("ANSWERED", response.path("status").asText());
        assertTrue(response.path("answer").asText().contains("[1]"));
        assertEquals(1, response.path("citations").size());
        assertTrue(response.path("citations").get(0).path("chunkId").asLong() > 0);
        assertEquals(1, response.path("citations").get(0).path("citationIndex").asInt());
        assertEquals(answersBefore + 1, answerRecordRepository.count());
        AnswerRecord persisted = answerRecordRepository.findAll().stream()
                .filter(record -> record.getQuestion().contains("Helios retirement eligibility"))
                .findFirst().orElseThrow();
        assertEquals(AnswerRecord.Status.ANSWERED, persisted.getStatus());
        assertTrue(persisted.getCitationJson().contains("\"chunkId\""));
    }

    @Test
    void postSearchAppliesCategoryFilterAndRoleAccessLevels() throws Exception {
        String employee = createEmployee();
        String hrUser = createUser(AppUser.Role.HR);
        ensureAccessPolicy(AppUser.Role.HR, KnowledgeDocument.AccessLevel.INTERNAL);
        ensureAccessPolicy(AppUser.Role.HR, KnowledgeDocument.AccessLevel.HR_ONLY);
        String searchableText = "Asterion ledger controls require quarterly review.";

        long financeId = uploadDocument(employee, "tc-05-06-finance.txt", searchableText,
                "FINANCE", "INTERNAL");
        uploadDocument(employee, "tc-05-06-it.txt", searchableText, "IT", "INTERNAL");
        long hrOnlyId = uploadDocument(hrUser, "tc-05-06-hr.txt", searchableText,
                "HR", "HR_ONLY");

        ResponseEntity<String> filteredResponse = restTemplate.withSessionLogin(employee, PASSWORD)
                .postForEntity("/api/knowledge/search",
                        jsonRequest("{\"query\":\"Asterion ledger controls\",\"category\":\"FINANCE\"}"),
                        String.class);
        assertEquals(200, filteredResponse.getStatusCode().value(), filteredResponse.getBody());
        JsonNode filteredResults = objectMapper.readTree(filteredResponse.getBody()).path("results");
        assertEquals(1, filteredResults.size(), filteredResponse.getBody());
        assertEquals(financeId, filteredResults.get(0).path("documentId").asLong());

        ResponseEntity<String> employeeResponse = restTemplate.withSessionLogin(employee, PASSWORD)
                .postForEntity("/api/knowledge/search",
                        jsonRequest("{\"query\":\"Asterion ledger controls\"}"), String.class);
        assertEquals(200, employeeResponse.getStatusCode().value(), employeeResponse.getBody());
        JsonNode employeeResults = objectMapper.readTree(employeeResponse.getBody()).path("results");
        assertFalse(containsDocument(employeeResults, hrOnlyId),
                "employee search must not include HR_ONLY documents");

        ResponseEntity<String> hrResponse = restTemplate.withSessionLogin(hrUser, PASSWORD)
                .postForEntity("/api/knowledge/search",
                        jsonRequest("{\"query\":\"Asterion ledger controls\"}"), String.class);
        assertEquals(200, hrResponse.getStatusCode().value(), hrResponse.getBody());
        JsonNode hrResults = objectMapper.readTree(hrResponse.getBody()).path("results");
        assertTrue(containsDocument(hrResults, hrOnlyId),
                "HR search should include HR_ONLY documents");
        ResponseEntity<String> employeeDetails = restTemplate.withSessionLogin(employee, PASSWORD)
                .getForEntity("/api/knowledge/documents/" + hrOnlyId, String.class);
        ResponseEntity<String> employeeContent = restTemplate.withSessionLogin(employee, PASSWORD)
                .getForEntity("/api/knowledge/documents/" + hrOnlyId + "/content", String.class);
        ResponseEntity<String> hrDetails = restTemplate.withSessionLogin(hrUser, PASSWORD)
                .getForEntity("/api/knowledge/documents/" + hrOnlyId, String.class);
        ResponseEntity<String> hrContent = restTemplate.withSessionLogin(hrUser, PASSWORD)
                .getForEntity("/api/knowledge/documents/" + hrOnlyId + "/content", String.class);
        assertEquals(403, employeeDetails.getStatusCode().value());
        assertEquals(403, employeeContent.getStatusCode().value());
        assertEquals(200, hrDetails.getStatusCode().value());
        assertEquals(200, hrContent.getStatusCode().value());
        assertTrue(hrContent.getBody().contains(searchableText));
    }

    @Test
    void searchReturnsOneBestChunkPerDocumentAndFallsBackToDocumentMetadata() throws Exception {
        String employee = createEmployee();
        String repeatedText = ("Company VPN access requires approved multi-factor authentication. ")
                .repeat(30);
        long bodyMatchId = uploadDocument(employee, "vpn-content.txt", repeatedText,
                "IT", "INTERNAL");
        long metadataMatchId = uploadDocument(employee, "vpn-title.txt",
                "This content is unrelated to networking.", "IT", "INTERNAL");
        KnowledgeDocument metadataDocument = documentRepository.findById(metadataMatchId).orElseThrow();
        metadataDocument.setTitle("Remote Work policy");
        documentRepository.save(metadataDocument);

        ResponseEntity<String> bodyResponse = restTemplate.withSessionLogin(employee, PASSWORD)
                .getForEntity(UriComponentsBuilder
                        .fromUriString("http://localhost:" + port + "/api/knowledge/search")
                        .queryParam("query", "company VPN")
                        .build().encode().toUri(), String.class);
        assertEquals(200, bodyResponse.getStatusCode().value(), bodyResponse.getBody());
        JsonNode bodyResults = objectMapper.readTree(bodyResponse.getBody()).path("results");
        assertEquals(1, bodyResults.size(),
                "chunks from one document should not crowd out distinct documents");
        assertEquals(bodyMatchId, bodyResults.get(0).path("documentId").asLong());

        ResponseEntity<String> metadataResponse = restTemplate.withSessionLogin(employee, PASSWORD)
                .getForEntity(UriComponentsBuilder
                        .fromUriString("http://localhost:" + port + "/api/knowledge/search")
                        .queryParam("query", "remote work")
                        .queryParam("category", "IT")
                        .build().encode().toUri(), String.class);
        assertEquals(200, metadataResponse.getStatusCode().value(), metadataResponse.getBody());
        assertTrue(containsDocument(
                objectMapper.readTree(metadataResponse.getBody()).path("results"), metadataMatchId));
    }

    @Test
    void metadataUpdatesAreAdminOnlyValidatedAndEnforceNewReadAccess() throws Exception {
        String employee = createEmployee();
        String admin = createUser(AppUser.Role.ADMIN);
        String hrUser = createUser(AppUser.Role.HR);
        ensureAccessPolicy(AppUser.Role.HR, KnowledgeDocument.AccessLevel.HR_ONLY);
        long documentId = uploadDocument(employee, "metadata.txt",
                "Company VPN requires multi-factor authentication.", "GENERAL", "INTERNAL");
        String route = "/api/knowledge/documents/" + documentId;

        ResponseEntity<String> denied = restTemplate.withSessionLogin(employee, PASSWORD)
                .exchange(route, HttpMethod.PUT, jsonRequest(
                        "{\"title\":\"Changed\",\"description\":\"Changed\","
                                + "\"category\":\"IT\",\"accessLevel\":\"HR_ONLY\"}"), String.class);
        assertEquals(403, denied.getStatusCode().value());

        ResponseEntity<String> updated = restTemplate.withSessionLogin(admin, PASSWORD)
                .exchange(route, HttpMethod.PUT, jsonRequest(
                        "{\"title\":\"Company VPN Guidance\",\"description\":\"Current guidance\","
                                + "\"category\":\"IT\",\"accessLevel\":\"HR_ONLY\"}"), String.class);
        assertEquals(200, updated.getStatusCode().value(), updated.getBody());
        JsonNode updatedDoc = objectMapper.readTree(updated.getBody());
        assertEquals("Company VPN Guidance", updatedDoc.path("title").asText());
        assertEquals("IT", updatedDoc.path("category").asText());
        assertEquals("HR_ONLY", updatedDoc.path("accessLevel").asText());

        ResponseEntity<String> invalid = restTemplate.withSessionLogin(admin, PASSWORD)
                .exchange(route, HttpMethod.PUT, jsonRequest(
                        "{\"title\":\" \",\"description\":\"\",\"category\":\"IT\","
                                + "\"accessLevel\":\"INTERNAL\"}"), String.class);
        assertEquals(400, invalid.getStatusCode().value(), invalid.getBody());
        assertEquals("Company VPN Guidance",
                documentRepository.findById(documentId).orElseThrow().getTitle());

        ResponseEntity<String> deniedContent = restTemplate.withSessionLogin(employee, PASSWORD)
                .getForEntity(route + "/content", String.class);
        ResponseEntity<String> permittedContent = restTemplate.withSessionLogin(hrUser, PASSWORD)
                .getForEntity(route + "/content", String.class);
        assertEquals(403, deniedContent.getStatusCode().value());
        assertEquals(200, permittedContent.getStatusCode().value(), permittedContent.getBody());
        assertTrue(permittedContent.getBody().contains("multi-factor authentication"));
    }

    @Test
    void nonAdminCannotCallDocumentManagementEndpoint() throws Exception {
        String employee = createEmployee();
        String admin = createUser(AppUser.Role.ADMIN);
        long documentId = uploadDocument(employee, "tc-05-07.txt",
                "This document is available to internal employees.", "GENERAL", "INTERNAL");

        ResponseEntity<String> denied = restTemplate.withSessionLogin(employee, PASSWORD)
                .postForEntity("/api/knowledge/documents/" + documentId + "/archive", null, String.class);
        assertEquals(403, denied.getStatusCode().value(), denied.getBody());
        assertEquals(KnowledgeDocument.Status.INDEXED,
                documentRepository.findById(documentId).orElseThrow().getStatus());

        ResponseEntity<String> allowed = restTemplate.withSessionLogin(admin, PASSWORD)
                .postForEntity("/api/knowledge/documents/" + documentId + "/archive", null, String.class);
        assertEquals(200, allowed.getStatusCode().value(), allowed.getBody());
        assertEquals(KnowledgeDocument.Status.ARCHIVED,
                documentRepository.findById(documentId).orElseThrow().getStatus());
    }

    @Test
    void reindexReplacesCurrentEvidenceAndHidesObsoleteChunks() throws Exception {
        String employee = createEmployee();
        String admin = createUser(AppUser.Role.ADMIN);
        String unique = UUID.randomUUID().toString().replace("-", "");
        String oldMarker = "Legacy" + unique;
        String newMarker = "Current" + unique;
        long documentId = uploadDocument(employee, "tc-05-09-original.txt",
                "The " + oldMarker + " procedure is archived.", "GENERAL", "INTERNAL");

        ResponseEntity<String> reindexResponse = restTemplate.withSessionLogin(admin, PASSWORD)
                .exchange("/api/knowledge/documents/" + documentId + "/reindex", HttpMethod.PUT,
                        uploadRequest("tc-05-09-revised.txt",
                                ("The " + newMarker + " procedure is active.").getBytes(StandardCharsets.UTF_8),
                                "GENERAL", "INTERNAL"),
                        String.class);
        assertEquals(200, reindexResponse.getStatusCode().value(), reindexResponse.getBody());

        ResponseEntity<String> oldSearch = restTemplate.withSessionLogin(employee, PASSWORD)
                .postForEntity("/api/knowledge/search",
                        jsonRequest("{\"query\":\"" + oldMarker + "\"}"), String.class);
        ResponseEntity<String> newSearch = restTemplate.withSessionLogin(employee, PASSWORD)
                .postForEntity("/api/knowledge/search",
                        jsonRequest("{\"query\":\"" + newMarker + "\"}"), String.class);
        assertEquals(200, oldSearch.getStatusCode().value(), oldSearch.getBody());
        assertEquals(200, newSearch.getStatusCode().value(), newSearch.getBody());
        assertFalse(containsDocument(objectMapper.readTree(oldSearch.getBody()).path("results"), documentId),
                "search must not return obsolete-version chunks");
        assertTrue(containsDocument(objectMapper.readTree(newSearch.getBody()).path("results"), documentId),
                "search should return the newly published version");

        KnowledgeDocument document = documentRepository.findById(documentId).orElseThrow();
        var versions = versionRepository.findByDocumentOrderByVersionNumberDesc(document);
        assertEquals(2, versions.size());
        assertEquals(document.getCurrentVersion().getId(), versions.get(0).getId());
        assertEquals(DocumentVersion.Status.PUBLISHED, versions.get(0).getStatus());
        assertEquals(DocumentVersion.Status.OBSOLETE, versions.get(1).getStatus());
    }

    @Test
    void feedbackPersistsAndEnforcesAnswerOwnershipAndRatingValidation() throws Exception {
        String owner = createEmployee();
        String otherEmployee = createEmployee();
        long documentId = uploadDocument(owner, "tc-05-11.txt",
                "The Helios review occurs every quarter.", "GENERAL", "INTERNAL");
        stubAnswer("The Helios review occurs every quarter. [1]");
        HttpEntity<String> askRequest = jsonRequest("{\"question\":\"When is the Helios review?\"}");
        ResponseEntity<String> askResponse = restTemplate.withSessionLogin(owner, PASSWORD)
                .postForEntity("/api/knowledge/ask", askRequest, String.class);
        assertEquals(200, askResponse.getStatusCode().value(), askResponse.getBody());
        long answerId = objectMapper.readTree(askResponse.getBody()).path("answerRecordId").asLong();
        assertTrue(answerId > 0);

        long feedbackBefore = feedbackRepository.count();
        String feedbackJson = "{\"documentId\":" + documentId
                + ",\"answerRecordId\":" + answerId
                + ",\"question\":\"When is the Helios review?\",\"rating\":5,\"comment\":\"Helpful\"}";
        ResponseEntity<String> accepted = restTemplate.withSessionLogin(owner, PASSWORD)
                .postForEntity("/api/knowledge/feedback", jsonRequest(feedbackJson), String.class);
        assertEquals(200, accepted.getStatusCode().value(), accepted.getBody());
        assertEquals(feedbackBefore + 1, feedbackRepository.count());
        KnowledgeFeedback savedFeedback = feedbackRepository.findAll().stream()
                .filter(feedback -> feedback.getAnswerRecord().getId().equals(answerId))
                .findFirst().orElseThrow();
        assertEquals(5, savedFeedback.getRating().intValue());
        assertEquals(documentId, savedFeedback.getDocument().getId());
        assertEquals(userRepository.findByUsername(owner).orElseThrow().getId(),
                savedFeedback.getUser().getId());

        ResponseEntity<String> denied = restTemplate.withSessionLogin(otherEmployee, PASSWORD)
                .postForEntity("/api/knowledge/feedback", jsonRequest(feedbackJson), String.class);
        assertEquals(403, denied.getStatusCode().value(), denied.getBody());
        assertEquals(feedbackBefore + 1, feedbackRepository.count());

        String invalidRating = feedbackJson.replace("\"rating\":5", "\"rating\":6");
        ResponseEntity<String> invalid = restTemplate.withSessionLogin(owner, PASSWORD)
                .postForEntity("/api/knowledge/feedback", jsonRequest(invalidRating), String.class);
        assertEquals(400, invalid.getStatusCode().value(), invalid.getBody());
        assertEquals(feedbackBefore + 1, feedbackRepository.count());
    }

    @Test
    void analyticsIsAdminOnlyAndReturnsDatabaseBackedMetrics() throws Exception {
        String employee = createEmployee();
        String admin = createUser(AppUser.Role.ADMIN);
        uploadDocument(employee, "tc-analytics.txt",
                "Athena onboarding guide requires manager approval.", "GENERAL", "INTERNAL");
        ResponseEntity<String> search = restTemplate.withSessionLogin(employee, PASSWORD)
                .postForEntity("/api/knowledge/search",
                        jsonRequest("{\"query\":\"Athena onboarding guide\"}"), String.class);
        assertEquals(200, search.getStatusCode().value(), search.getBody());
        stubAnswer("The Athena onboarding guide requires manager approval. [1]");
        ResponseEntity<String> ask = restTemplate.withSessionLogin(employee, PASSWORD)
                .postForEntity("/api/knowledge/ask",
                        jsonRequest("{\"question\":\"What does the Athena onboarding guide require?\"}"),
                        String.class);
        assertEquals(200, ask.getStatusCode().value(), ask.getBody());

        ResponseEntity<String> denied = restTemplate.withSessionLogin(employee, PASSWORD)
                .getForEntity("/api/knowledge/analytics", String.class);
        ResponseEntity<String> adminMetrics = restTemplate.withSessionLogin(admin, PASSWORD)
                .getForEntity("/api/knowledge/analytics", String.class);
        assertEquals(403, denied.getStatusCode().value());
        assertEquals(200, adminMetrics.getStatusCode().value(), adminMetrics.getBody());
        JsonNode metrics = objectMapper.readTree(adminMetrics.getBody());
        assertTrue(metrics.path("totalDocuments").asLong() > 0);
        assertTrue(metrics.path("indexedDocuments").asLong() > 0);
        assertTrue(metrics.path("totalSearches").asLong() > 0);
        assertTrue(metrics.path("totalQuestions").asLong() > 0);
        assertTrue(metrics.path("answeredQuestions").asLong() > 0);
        assertTrue(metrics.has("averageFeedbackRating"));
    }

    private void stubAnswer(String answer) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(answer);
    }

    private String createEmployee() {
        String username = createUser(AppUser.Role.EMPLOYEE);
        ensureAccessPolicy(AppUser.Role.EMPLOYEE, KnowledgeDocument.AccessLevel.INTERNAL);
        return username;
    }

    private String createUser(AppUser.Role role) {
        String username = "tc-user-" + UUID.randomUUID();
        userRepository.save(AppUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(role)
                .enabled(true)
                .build());
        return username;
    }

    private void ensureAccessPolicy(AppUser.Role role, KnowledgeDocument.AccessLevel accessLevel) {
        if (accessPolicyRepository.findByRole(role).stream()
                .noneMatch(policy -> policy.getAccessLevel() == accessLevel)) {
            accessPolicyRepository.save(AccessPolicy.builder()
                    .role(role)
                    .accessLevel(accessLevel)
                    .build());
        }
    }

    private long uploadDocument(String username, String filename, String content,
            String category, String accessLevel) throws Exception {
        ResponseEntity<String> uploadResponse = restTemplate.withSessionLogin(username, PASSWORD)
                .postForEntity("/api/knowledge/documents",
                        uploadRequest(filename, content.getBytes(StandardCharsets.UTF_8), category, accessLevel),
                        String.class);
        assertEquals(201, uploadResponse.getStatusCode().value(), uploadResponse.getBody());
        return objectMapper.readTree(uploadResponse.getBody()).path("id").asLong();
    }

    private final class SessionRestTemplate {
        private final String cookie;
        private final String csrfToken;

        private SessionRestTemplate(String cookie, String csrfToken) {
            this.cookie = cookie;
            this.csrfToken = csrfToken;
        }

        private SessionRestTemplate withSessionLogin(String username, String password) {
            return sessions.computeIfAbsent(username, ignored -> authenticate(username, password));
        }

        private <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType) {
            return http.exchange(url, HttpMethod.GET, request(null, HttpMethod.GET), responseType);
        }

        private <T> ResponseEntity<T> getForEntity(java.net.URI url, Class<T> responseType) {
            return http.exchange(url, HttpMethod.GET, request(null, HttpMethod.GET), responseType);
        }

        private <T> ResponseEntity<T> postForEntity(String url, Object body, Class<T> responseType) {
            return http.exchange(url, HttpMethod.POST, request(body, HttpMethod.POST), responseType);
        }

        private <T> ResponseEntity<T> exchange(
                String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType) {
            return http.exchange(url, method, request(entity, method), responseType);
        }

        private HttpEntity<?> request(Object requestBody, HttpMethod method) {
            if (cookie == null) {
                return requestBody instanceof HttpEntity<?> entity ? entity : new HttpEntity<>(requestBody);
            }
            HttpHeaders headers = new HttpHeaders();
            if (requestBody instanceof HttpEntity<?> entity) {
                headers.putAll(entity.getHeaders());
                requestBody = entity.getBody();
            }
            headers.set(HttpHeaders.COOKIE, cookie);
            if (method != HttpMethod.GET && method != HttpMethod.HEAD && method != HttpMethod.OPTIONS) {
                headers.set("X-CSRF-TOKEN", csrfToken);
            }
            return new HttpEntity<>(requestBody, headers);
        }
    }

    private SessionRestTemplate authenticate(String username, String password) {
        ResponseEntity<String> csrfResponse = http.getForEntity("/api/auth/csrf", String.class);
        String cookie = sessionCookie(csrfResponse.getHeaders());
        try {
            String token = objectMapper.readTree(csrfResponse.getBody()).path("token").asText();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.COOKIE, cookie);
            headers.set("X-CSRF-TOKEN", token);
            ResponseEntity<String> loginResponse = http.postForEntity("/api/auth/login",
                    new HttpEntity<>(objectMapper.writeValueAsString(
                            Map.of("username", username, "password", password)), headers), String.class);
            assertEquals(200, loginResponse.getStatusCode().value(), loginResponse.getBody());
            cookie = sessionCookie(loginResponse.getHeaders());
            ResponseEntity<String> refreshedCsrf = http.exchange("/api/auth/csrf", HttpMethod.GET,
                    new HttpEntity<>(cookieHeaders(cookie)), String.class);
            String refreshedToken = objectMapper.readTree(refreshedCsrf.getBody()).path("token").asText();
            cookie = sessionCookieOr(cookie, refreshedCsrf.getHeaders());
            SessionRestTemplate authenticatedSession = new SessionRestTemplate(cookie, refreshedToken);
            ResponseEntity<String> currentUser =
                    authenticatedSession.getForEntity("/api/auth/me", String.class);
            assertEquals(200, currentUser.getStatusCode().value(), currentUser.getBody());
            assertEquals(username, objectMapper.readTree(currentUser.getBody()).path("username").asText());
            return authenticatedSession;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not establish test session for " + username, exception);
        }
    }

    private HttpHeaders cookieHeaders(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, cookie);
        return headers;
    }

    private String sessionCookie(HttpHeaders headers) {
        return headers.getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> value.startsWith("JSESSIONID="))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Response did not set a session cookie."));
    }

    private String sessionCookieOr(String current, HttpHeaders headers) {
        return headers.getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .map(value -> value.split(";", 2)[0])
                .filter(value -> value.startsWith("JSESSIONID="))
                .findFirst()
                .orElse(current);
    }

    private HttpEntity<String> jsonRequest(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    private long uploadAndReadDocumentId(SessionRestTemplate session,
            HttpEntity<MultiValueMap<String, Object>> request) throws Exception {
        ResponseEntity<String> response =
                session.postForEntity("/api/knowledge/documents", request, String.class);
        assertEquals(201, response.getStatusCode().value(), response.getBody());
        return objectMapper.readTree(response.getBody()).path("id").asLong();
    }

    private boolean extractedTextContains(long documentId, String expectedText) {
        KnowledgeDocument document = documentRepository.findById(documentId).orElseThrow();
        DocumentVersion version = versionRepository.findByDocumentOrderByVersionNumberDesc(document)
                .get(0);
        return version.getExtractedText().contains(expectedText);
    }

    private ByteArrayOutputStream validPdf(String text) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            document.save(output);
        }
        return output;
    }

    private HttpEntity<MultiValueMap<String, Object>> uploadRequest(String filename, byte[] bytes) {
        return uploadRequest(filename, bytes, "GENERAL", "INTERNAL");
    }

    private HttpEntity<MultiValueMap<String, Object>> uploadRequest(
            String filename, byte[] bytes, String category, String accessLevel) {
        return uploadRequest(filename, bytes, category, accessLevel, "application/octet-stream");
    }

    private HttpEntity<MultiValueMap<String, Object>> uploadRequest(
            String filename, byte[] bytes, String category, String accessLevel, String fileContentType) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("title", "Synthetic " + UUID.randomUUID());
        form.add("description", "Synthetic integration test document");
        form.add("category", category);
        form.add("accessLevel", accessLevel);
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(fileContentType));
        form.add("file", new HttpEntity<>(resource, fileHeaders));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(form, headers);
    }

    private boolean containsDocument(JsonNode results, long documentId) {
        for (JsonNode result : results) {
            if (result.path("documentId").asLong() == documentId) {
                return true;
            }
        }
        return false;
    }
}
