
package com.enterprise.knowledge.service;

import com.enterprise.knowledge.dto.*;
import com.enterprise.knowledge.entity.*;
import com.enterprise.knowledge.entity.KnowledgeDocument.*;
import com.enterprise.knowledge.exception.*;
import com.enterprise.knowledge.repository.*;
import com.enterprise.knowledge.security.AccessLevelEvaluator;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "txt", "md");

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;

    private static final String RECONSTRUCTED_TEXT_NOTICE =
            "Reconstructed text assembled from indexed passages. "
                    + "Original extracted text is unavailable.\n\n";

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final AccessLevelEvaluator accessLevelEvaluator;
    private final KnowledgeFeedbackRepository feedbackRepository;

    @Value("${app.retrieval.chunk-size:800}")
    private int chunkSize;

    @Value("${app.retrieval.chunk-overlap:100}")
    private int chunkOverlap;

    // Explicit constructor: no Lombok constructor required
    public DocumentService(
            KnowledgeDocumentRepository documentRepository,
            DocumentVersionRepository versionRepository,
            KnowledgeChunkRepository chunkRepository,
            AccessLevelEvaluator accessLevelEvaluator,
            KnowledgeFeedbackRepository feedbackRepository) {

        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.chunkRepository = chunkRepository;
        this.accessLevelEvaluator = accessLevelEvaluator;
        this.feedbackRepository = feedbackRepository;
    }

    // -------------------- Upload --------------------

    @Transactional
    public DocumentResponse uploadDocument(
            MultipartFile file,
            DocumentUploadRequest request,
            AppUser uploader) {

        if (!accessLevelEvaluator.getAllowedLevels(uploader.getRole())
                .contains(request.getAccessLevel())) {
            throw new AccessDeniedException(
                    "User cannot upload documents with the requested access level.");
        }

        byte[] fileContent = validateFile(file);
        String extractedText = extractText(file, fileContent);

        if (extractedText == null || extractedText.isBlank()) {
            throw new DocumentProcessingException(
                    "Extracted text is empty — document cannot be indexed.");
        }

        String checksum = sha256(extractedText);

        KnowledgeDocument doc = KnowledgeDocument.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .accessLevel(request.getAccessLevel())
                .owner(uploader)
                .status(Status.DRAFT)
                .reindexRequired(false)
                .build();

        doc = documentRepository.save(doc);

        DocumentVersion version = DocumentVersion.builder()
                .document(doc)
                .versionNumber(1)
                .originalFilename(
                        Objects.requireNonNullElse(
                                file.getOriginalFilename(), "unknown"))
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .checksum(checksum)
                .extractedText(extractedText)
                .status(DocumentVersion.Status.DRAFT)
                .createdBy(uploader)
                .build();

        version = versionRepository.save(version);

        persistChunks(version, extractedText);

        version.setStatus(DocumentVersion.Status.PUBLISHED);
        versionRepository.save(version);

        doc.setCurrentVersion(version);
        doc.setStatus(Status.INDEXED);
        doc = documentRepository.save(doc);

        log.info(
                "Document indexed: id={}, title={}, version={}",
                doc.getId(), doc.getTitle(), version.getVersionNumber());

        return toResponse(doc);
    }

    // -------------------- Reindex --------------------

    @Transactional
    public DocumentResponse reindex(
            Long docId, MultipartFile file, AppUser uploader) {

        if (uploader.getRole() != AppUser.Role.ADMIN) {
            throw new AccessDeniedException(
                    "Only ADMIN can add document versions.");
        }

        KnowledgeDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + docId));

        byte[] fileContent = validateFile(file);
        String extractedText = extractText(file, fileContent);

        if (extractedText == null || extractedText.isBlank()) {
            throw new DocumentProcessingException(
                    "Extracted text is empty.");
        }

        int nextVersion = versionRepository.countByDocument(doc) + 1;

        DocumentVersion newVersion = DocumentVersion.builder()
                .document(doc)
                .versionNumber(nextVersion)
                .originalFilename(
                        Objects.requireNonNullElse(
                                file.getOriginalFilename(), "unknown"))
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .checksum(sha256(extractedText))
                .extractedText(extractedText)
                .status(DocumentVersion.Status.DRAFT)
                .createdBy(uploader)
                .build();

        newVersion = versionRepository.save(newVersion);
        persistChunks(newVersion, extractedText);

        DocumentVersion oldVersion = doc.getCurrentVersion();

        newVersion.setStatus(DocumentVersion.Status.PUBLISHED);
        versionRepository.save(newVersion);

        if (oldVersion != null) {
            oldVersion.setStatus(DocumentVersion.Status.OBSOLETE);
            versionRepository.save(oldVersion);
        }

        doc.setCurrentVersion(newVersion);
        doc.setStatus(Status.INDEXED);
        doc.setReindexRequired(false);
        doc = documentRepository.save(doc);

        log.info(
                "Document reindexed: id={}, new version={}",
                doc.getId(), nextVersion);

        return toResponse(doc);
    }

    // -------------------- Archive --------------------

    @Transactional
    public DocumentResponse archive(Long docId, AppUser user) {

        if (user.getRole() != AppUser.Role.ADMIN) {
            throw new AccessDeniedException(
                    "Only ADMIN can archive documents.");
        }

        KnowledgeDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + docId));

        doc.setStatus(Status.ARCHIVED);
        doc = documentRepository.save(doc);

        log.info("Document archived: id={}", docId);

        return toResponse(doc);
    }

    @Transactional
    public DocumentResponse updateMetadata(
            Long docId,
            DocumentMetadataUpdateRequest request,
            AppUser user) {
        if (user.getRole() != AppUser.Role.ADMIN) {
            throw new AccessDeniedException(
                    "Only ADMIN can update document metadata.");
        }

        KnowledgeDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + docId));

        doc.setTitle(request.getTitle().strip());
        doc.setDescription(request.getDescription() == null
                ? null
                : request.getDescription().strip());
        doc.setCategory(request.getCategory());
        doc.setAccessLevel(request.getAccessLevel());
        return toResponse(documentRepository.save(doc));
    }

    @Transactional
    public void delete(Long docId, AppUser user) {
        if (user.getRole() != AppUser.Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can delete documents.");
        }

        KnowledgeDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + docId));

        feedbackRepository.clearDocumentReference(docId);

        List<DocumentVersion> versions =
                versionRepository.findByDocumentOrderByVersionNumberDesc(doc);
        List<Long> versionIds = versions.stream()
                .map(DocumentVersion::getId)
                .toList();

        doc.setCurrentVersion(null);
        documentRepository.saveAndFlush(doc);

        if (!versionIds.isEmpty()) {
            chunkRepository.deleteByDocumentVersionIds(versionIds);
            versionRepository.deleteAll(versions);
        }

        documentRepository.delete(doc);
        log.info("Document deleted: id={}", docId);
    }

    // -------------------- List and Get --------------------

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(
            Collection<KnowledgeDocument.AccessLevel> allowedLevels,
            KnowledgeDocument.Category category,
            Pageable pageable) {

        Page<KnowledgeDocument> page = (category != null)
                ? documentRepository
                        .findByStatusAndCategoryAndAccessLevelIn(
                                Status.INDEXED, category,
                                allowedLevels, pageable)
                : documentRepository
                        .findByStatusAndAccessLevelIn(
                                Status.INDEXED, allowedLevels, pageable);

        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(
            Long id,
            Collection<KnowledgeDocument.AccessLevel> allowedLevels,
            boolean includeArchived) {

        KnowledgeDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + id));

        verifyReadable(doc, allowedLevels, includeArchived);

        return toResponse(doc);
    }

    @Transactional(readOnly = true)
    public String getPreview(
            Long id,
            Collection<KnowledgeDocument.AccessLevel> allowedLevels,
            boolean includeArchived) {

        KnowledgeDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + id));

        verifyReadable(doc, allowedLevels, includeArchived);

        DocumentVersion version = doc.getCurrentVersion();

        if (version == null) {
            return "";
        }

        String text = getReadableVersionText(version);
        if (text == null) {
            return "";
        }

        return text.length() > 500
                ? text.substring(0, 500) + "..."
                : text;
    }

    @Transactional(readOnly = true)
    public DocumentContentResponse getContent(
            Long id,
            Collection<KnowledgeDocument.AccessLevel> allowedLevels,
            boolean includeArchived) {
        KnowledgeDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + id));

        verifyReadable(doc, allowedLevels, includeArchived);

        DocumentVersion version = doc.getCurrentVersion();
        if (version == null) {
            throw new ResourceNotFoundException(
                    "Document content is not available: " + id);
        }

        String text = getReadableVersionText(version);
        if (text == null) {
            throw new ResourceNotFoundException(
                    "Document content is not available: " + id);
        }

        return new DocumentContentResponse(
                doc.getId(),
                doc.getTitle(),
                version.getVersionNumber(),
                version.getOriginalFilename(),
                text);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> getVersionHistory(
            Long id,
            Collection<KnowledgeDocument.AccessLevel> allowedLevels,
            boolean includeArchived) {

        KnowledgeDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found: " + id));

        verifyReadable(doc, allowedLevels, includeArchived);

        return versionRepository
                .findByDocumentOrderByVersionNumberDesc(doc)
                .stream()
                .map(this::toVersionResponse)
                .collect(Collectors.toList());
    }

    private String getReadableVersionText(DocumentVersion version) {
        String extractedText = version.getExtractedText();
        if (extractedText != null && !extractedText.isBlank()) {
            return extractedText;
        }

        List<KnowledgeChunk> chunks = chunkRepository
                .findByDocumentVersionIdOrderByChunkIndex(version.getId());
        if (chunks.isEmpty()) {
            return null;
        }

        StringBuilder text = new StringBuilder(RECONSTRUCTED_TEXT_NOTICE);
        for (KnowledgeChunk chunk : chunks) {
            if (text.length() > RECONSTRUCTED_TEXT_NOTICE.length()) {
                text.append("\n\n");
            }
            if (chunk.getSectionReference() != null
                    && !chunk.getSectionReference().isBlank()) {
                text.append(chunk.getSectionReference()).append('\n');
            }
            text.append(chunk.getContent());
        }
        return text.toString();
    }

    private void verifyReadable(
            KnowledgeDocument doc,
            Collection<KnowledgeDocument.AccessLevel> allowedLevels,
            boolean includeArchived) {

        if (!allowedLevels.contains(doc.getAccessLevel())) {
            throw new AccessDeniedException(
                    "Access denied to document " + doc.getId());
        }

        if (doc.getStatus() == Status.ARCHIVED && !includeArchived) {
            throw new ResourceNotFoundException(
                    "Document not found: " + doc.getId());
        }
    }

    // -------------------- File Validation --------------------

    private byte[] validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new DocumentProcessingException(
                    "File must not be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new DocumentProcessingException(
                    "File exceeds 50 MB limit.");
        }

        String name = file.getOriginalFilename();

        if (name == null || name.isBlank()) {
            throw new DocumentProcessingException(
                    "Filename is missing.");
        }

        String ext = name.contains(".")
                ? name.substring(name.lastIndexOf('.') + 1)
                        .toLowerCase(Locale.ROOT)
                : "";

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new DocumentProcessingException(
                    "Unsupported file type: " + ext
                            + ". Allowed: pdf, txt, md");
        }

        String contentType = file.getContentType();

        if (contentType != null
                && !contentType.isBlank()
                && !isCompatibleContentType(ext, contentType)) {

            throw new DocumentProcessingException(
                    "File content type does not match the ."
                            + ext + " filename.");
        }

        try {
            byte[] bytes = file.getBytes();

            if ("pdf".equals(ext) && !hasPdfSignature(bytes)) {
                throw new DocumentProcessingException(
                        "File is not a valid PDF. "
                                + "Upload a complete PDF file.");
            }

            if (!"pdf".equals(ext) && hasPdfSignature(bytes)) {
                throw new DocumentProcessingException(
                        "PDF content must be uploaded "
                                + "with a .pdf filename.");
            }

            return bytes;

        } catch (IOException e) {
            throw new DocumentProcessingException(
                    "Unable to read uploaded file.", e);
        }
    }

    // -------------------- Text Extraction --------------------

    private String extractText(
            MultipartFile file, byte[] fileContent) {

        String name = Objects.requireNonNullElse(
                file.getOriginalFilename(), "");

        try {
            if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {

                try (PDDocument pdfDoc = Loader.loadPDF(fileContent)) {
                    String text = new PDFTextStripper().getText(pdfDoc);

                    if (text == null || text.isBlank()) {
                        throw new DocumentProcessingException(
                                "PDF contains no extractable text.");
                    }

                    return text;
                }

            } else {
                return new String(
                        fileContent, StandardCharsets.UTF_8);
            }

        } catch (IOException e) {
            throw new DocumentProcessingException(
                    "PDF is invalid, truncated, or unreadable. "
                            + "Upload a complete, readable PDF.",
                    e);
        }
    }

    private boolean isCompatibleContentType(
            String extension, String contentType) {

        String normalizedType = contentType
                .split(";", 2)[0]
                .trim()
                .toLowerCase(Locale.ROOT);

        return switch (extension) {
            case "pdf" ->
                normalizedType.equals("application/pdf")
                        || normalizedType.equals(
                                "application/octet-stream");

            case "txt" ->
                normalizedType.equals("text/plain")
                        || normalizedType.equals(
                                "application/octet-stream");

            case "md" ->
                normalizedType.equals("text/markdown")
                        || normalizedType.equals("text/x-markdown")
                        || normalizedType.equals("text/plain")
                        || normalizedType.equals(
                                "application/octet-stream");

            default -> false;
        };
    }

    private boolean hasPdfSignature(byte[] bytes) {

        int headerLength = Math.min(bytes.length, 1024);
        byte[] signature = "%PDF-".getBytes(StandardCharsets.US_ASCII);

        for (int offset = 0; offset <= headerLength - signature.length; offset++) {

            boolean matches = true;

            for (int i = 0; i < signature.length; i++) {
                if (bytes[offset + i] != signature[i]) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return true;
            }
        }

        return false;
    }

    // -------------------- Chunking --------------------

    private void persistChunks(
            DocumentVersion version, String text) {

        List<String> chunks = chunkText(text);
        List<KnowledgeChunk> chunkEntities = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            chunkEntities.add(
                    KnowledgeChunk.builder()
                            .documentVersion(version)
                            .chunkIndex(i)
                            .content(chunks.get(i))
                            .searchableText(chunks.get(i))
                            .build());
        }

        chunkRepository.saveAll(chunkEntities);
    }

    // Package-private so tests in the same package can access it
    List<String> chunkText(String text) {

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Prefer to break at a sentence boundary if possible
            if (end < text.length()) {
                int lastDot = text.lastIndexOf('.', end);

                if (lastDot > start + chunkSize / 2) {
                    end = lastDot + 1;
                }
            }

            String chunk = text.substring(start, end).strip();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end == text.length()) {
                start = end;
            } else {
                start = Math.max(start + 1, end - chunkOverlap);
            }
        }

        return chunks;
    }

    // -------------------- Checksum --------------------

    private String sha256(String text) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    text.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "SHA-256 unavailable", e);
        }
    }

    // -------------------- DTO Mapping --------------------

    public DocumentResponse toResponse(KnowledgeDocument doc) {

        return DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .category(doc.getCategory())
                .accessLevel(doc.getAccessLevel())
                .status(doc.getStatus())
                .ownerUsername(
                        doc.getOwner() != null
                                ? doc.getOwner().getUsername()
                                : null)
                .currentVersion(
                        doc.getCurrentVersion() != null
                                ? doc.getCurrentVersion().getVersionNumber()
                                : null)
                .originalFilename(
                        doc.getCurrentVersion() != null
                                ? doc.getCurrentVersion().getOriginalFilename()
                                : null)
                .uploadedAt(doc.getUploadedAt())
                .updatedAt(doc.getUpdatedAt())
                .reindexRequired(doc.getReindexRequired())
                .build();
    }

    private DocumentVersionResponse toVersionResponse(
            DocumentVersion version) {

        return DocumentVersionResponse.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .originalFilename(version.getOriginalFilename())
                .contentType(version.getContentType())
                .fileSizeBytes(version.getFileSizeBytes())
                .checksum(version.getChecksum())
                .status(version.getStatus())
                .createdAt(version.getCreatedAt())
                .createdByUsername(
                        version.getCreatedBy() != null
                                ? version.getCreatedBy().getUsername()
                                : null)
                .build();
    }
}
