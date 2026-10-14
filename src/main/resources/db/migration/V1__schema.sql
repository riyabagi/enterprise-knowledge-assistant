-- ============================================================
-- V1__schema.sql  Enterprise Knowledge Assistant – Core Schema
-- ============================================================

-- ─── Users ───────────────────────────────────────────────────
CREATE TABLE app_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(200),
    email         VARCHAR(255) UNIQUE,
    role          ENUM('ADMIN','EMPLOYEE','HR') NOT NULL DEFAULT 'EMPLOYEE',
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ─── Access Policies ─────────────────────────────────────────
CREATE TABLE access_policy (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    role         ENUM('ADMIN','EMPLOYEE','HR') NOT NULL,
    access_level ENUM('PUBLIC','INTERNAL','HR_ONLY','RESTRICTED') NOT NULL,
    UNIQUE KEY uq_role_access (role, access_level)
);

-- ─── Knowledge Documents ─────────────────────────────────────
CREATE TABLE knowledge_document (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    title              VARCHAR(500) NOT NULL,
    description        TEXT,
    category           ENUM('GENERAL','HR','IT','FINANCE','LEGAL','OPERATIONS') NOT NULL DEFAULT 'GENERAL',
    access_level       ENUM('PUBLIC','INTERNAL','HR_ONLY','RESTRICTED') NOT NULL DEFAULT 'INTERNAL',
    owner_id           BIGINT NOT NULL,
    current_version_id BIGINT,
    status             ENUM('DRAFT','INDEXED','ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    reindex_required   BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_owner FOREIGN KEY (owner_id) REFERENCES app_user(id),
    FULLTEXT INDEX ft_doc_title_desc (title, description)
);

-- ─── Document Versions ───────────────────────────────────────
CREATE TABLE document_version (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id       BIGINT NOT NULL,
    version_number    INT NOT NULL DEFAULT 1,
    original_filename VARCHAR(500) NOT NULL,
    content_type      VARCHAR(100),
    file_size_bytes   BIGINT,
    checksum          VARCHAR(64) NOT NULL,
    extracted_text    LONGTEXT,
    status            ENUM('DRAFT','PUBLISHED','OBSOLETE') NOT NULL DEFAULT 'DRAFT',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id     BIGINT NOT NULL,
    CONSTRAINT fk_version_doc  FOREIGN KEY (document_id)   REFERENCES knowledge_document(id),
    CONSTRAINT fk_version_user FOREIGN KEY (created_by_id) REFERENCES app_user(id),
    UNIQUE KEY uq_doc_version (document_id, version_number)
);

-- Add the deferred FK from knowledge_document to document_version
ALTER TABLE knowledge_document
    ADD CONSTRAINT fk_doc_current_version
        FOREIGN KEY (current_version_id) REFERENCES document_version(id);

-- ─── Knowledge Chunks ────────────────────────────────────────
CREATE TABLE knowledge_chunk (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_version_id  BIGINT NOT NULL,
    chunk_index          INT NOT NULL,
    section_reference    VARCHAR(300),
    content              TEXT NOT NULL,
    searchable_text      TEXT,
    CONSTRAINT fk_chunk_version FOREIGN KEY (document_version_id) REFERENCES document_version(id),
    UNIQUE KEY uq_chunk_version_idx (document_version_id, chunk_index),
    FULLTEXT INDEX ft_chunk_content (content, searchable_text)
);

-- ─── Search History ──────────────────────────────────────────
CREATE TABLE search_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    query           VARCHAR(1000) NOT NULL,
    category_filter VARCHAR(100),
    result_count    INT NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_search_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

-- ─── Answer Records ──────────────────────────────────────────
CREATE TABLE answer_record (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    question       VARCHAR(600) NOT NULL,
    answer_text    LONGTEXT,
    status         ENUM('ANSWERED','INSUFFICIENT_EVIDENCE','CONFLICTING_EVIDENCE','AI_UNAVAILABLE') NOT NULL,
    citation_json  TEXT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_answer_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

-- ─── Knowledge Feedback ──────────────────────────────────────
CREATE TABLE knowledge_feedback (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    question         VARCHAR(600),
    answer_record_id BIGINT,
    document_id      BIGINT,
    rating           TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment          TEXT,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_user   FOREIGN KEY (user_id)          REFERENCES app_user(id),
    CONSTRAINT fk_feedback_answer FOREIGN KEY (answer_record_id) REFERENCES answer_record(id),
    CONSTRAINT fk_feedback_doc    FOREIGN KEY (document_id)      REFERENCES knowledge_document(id)
);
