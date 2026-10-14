-- H2-compatible test schema (no FULLTEXT, ENUM→VARCHAR)
CREATE TABLE app_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(200),
    email         VARCHAR(255),
    role          VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_username UNIQUE (username)
);

CREATE TABLE access_policy (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    role         VARCHAR(20) NOT NULL,
    access_level VARCHAR(20) NOT NULL,
    CONSTRAINT uq_role_access UNIQUE (role, access_level)
);

CREATE TABLE knowledge_document (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    title              VARCHAR(500) NOT NULL,
    description        CLOB,
    category           VARCHAR(30)  NOT NULL DEFAULT 'GENERAL',
    access_level       VARCHAR(20)  NOT NULL DEFAULT 'INTERNAL',
    owner_id           BIGINT       NOT NULL,
    current_version_id BIGINT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    reindex_required   BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doc_owner FOREIGN KEY (owner_id) REFERENCES app_user(id)
);

CREATE TABLE document_version (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id       BIGINT       NOT NULL,
    version_number    INT          NOT NULL DEFAULT 1,
    original_filename VARCHAR(500) NOT NULL,
    content_type      VARCHAR(100),
    file_size_bytes   BIGINT,
    checksum          VARCHAR(64)  NOT NULL,
    extracted_text    CLOB,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_id     BIGINT       NOT NULL,
    CONSTRAINT fk_version_doc  FOREIGN KEY (document_id)   REFERENCES knowledge_document(id),
    CONSTRAINT fk_version_user FOREIGN KEY (created_by_id) REFERENCES app_user(id),
    CONSTRAINT uq_doc_version  UNIQUE (document_id, version_number)
);

ALTER TABLE knowledge_document
    ADD CONSTRAINT fk_doc_current_version
        FOREIGN KEY (current_version_id) REFERENCES document_version(id);

CREATE TABLE knowledge_chunk (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_version_id  BIGINT NOT NULL,
    chunk_index          INT    NOT NULL,
    section_reference    VARCHAR(300),
    content              CLOB   NOT NULL,
    searchable_text      CLOB,
    CONSTRAINT fk_chunk_version FOREIGN KEY (document_version_id) REFERENCES document_version(id),
    CONSTRAINT uq_chunk_version_idx UNIQUE (document_version_id, chunk_index)
);

CREATE TABLE search_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    query           VARCHAR(1000) NOT NULL,
    category_filter VARCHAR(100),
    result_count    INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_search_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE answer_record (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT        NOT NULL,
    question       VARCHAR(600)  NOT NULL,
    answer_text    CLOB,
    status         VARCHAR(30)   NOT NULL,
    citation_json  CLOB,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_answer_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE knowledge_feedback (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    question         VARCHAR(600),
    answer_record_id BIGINT,
    document_id      BIGINT,
    rating           TINYINT NOT NULL,
    comment          CLOB,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedback_user   FOREIGN KEY (user_id)          REFERENCES app_user(id),
    CONSTRAINT fk_feedback_answer FOREIGN KEY (answer_record_id) REFERENCES answer_record(id),
    CONSTRAINT fk_feedback_doc    FOREIGN KEY (document_id)      REFERENCES knowledge_document(id)
);
