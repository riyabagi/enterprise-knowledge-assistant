# Enterprise Knowledge Assistant

Spring Boot application for document ingestion, MySQL full-text search, and
retrieval-augmented answers from a local Ollama model.

## Prerequisites

- Java 21
- Maven 3.9+
- MySQL 8 with permission to create/use the configured database
- Ollama running locally with the selected chat model available

The application configuration defaults to the locally verified `qwen2.5:3b`
model. Override it with `OLLAMA_MODEL` if using another installed chat model.
`OLLAMA_BASE_URL` defaults to `http://localhost:11434`.

The MySQL connection uses `DB_USERNAME` and `DB_PASSWORD`. Do not commit these
values. In PowerShell, set them in the current terminal before starting:

```powershell
$env:DB_USERNAME = "<mysql-user>"
$env:DB_PASSWORD = "<mysql-password>"
$env:OLLAMA_MODEL = "qwen2.5:3b"
mvn spring-boot:run
```

The development profile is active by default. Flyway applies migrations from
`src/main/resources/db/migration`; Hibernate validates, rather than alters, the
schema. The V2 seed migration contains synthetic demonstration content and
must not be treated as real company policy. Use a separate, secured MySQL
database for development.

## Tests

Run the automated unit and H2-backed embedded-server tests:

```powershell
mvn test
```

The H2 test schema intentionally omits MySQL FULLTEXT indexes and is not a
substitute for validating the production `MATCH ... AGAINST` queries on MySQL.
The suite includes upload/search and RAG/feedback/analytics HTTP integration
tests plus deterministic mocked-model tests for citation validation,
abstention, conflicting evidence, prompt isolation, and AI failures. See
`IMPLEMENTATION_STATUS.md` and `runtime-validation-report.md` for exact
coverage and remaining environment-dependent checks.

Ask AI cites retrieved passages using numeric labels such as `[1]`; the
response's citation metadata maps each label to the source document and version
shown in the UI. Retrieval/AI service failures return `AI_UNAVAILABLE`, while
answers without a valid evidence citation safely abstain as
`INSUFFICIENT_EVIDENCE`.

## Authentication and API

The frontend authenticates with a database-backed Spring Security session.
Successful login stores the authenticated security context server-side and the
browser sends its same-origin session cookie on subsequent requests. Mutating
requests require the session CSRF token; the frontend obtains it from
`GET /api/auth/csrf` and sends it in the `X-CSRF-TOKEN` header. Knowledge APIs
do not use HTTP Basic authentication, so unauthenticated API responses do not
trigger a browser-native credential dialog.

| Method | Route | Notes |
|---|---|---|
| `GET` | `/api/auth/csrf` | Returns the CSRF header name and token |
| `POST` | `/api/auth/login` | JSON body: `{"username":"...","password":"..."}`; returns the user and role |
| `GET` | `/api/auth/me` | Returns the current session user |
| `POST` | `/api/auth/logout` | Invalidates the session; requires CSRF |

The seed password hashes documented by the V2 migration did not match its
documented sample credentials. V3 conditionally repairs only those unchanged
seed hashes; it leaves user-modified hashes untouched. Configure user accounts
and role/access policies through the database, and do not hardcode credentials
in the frontend.

Implemented routes:

| Method | Route | Notes |
|---|---|---|
| `POST` | `/api/knowledge/documents` | Multipart upload (`file` plus document metadata) |
| `GET` | `/api/knowledge/documents` | Paginated document listing; optional `category`, `page`, `size` |
| `GET` | `/api/knowledge/documents/{id}` | Document details |
| `GET` | `/api/knowledge/documents/{id}/preview` | Extracted-text preview |
| `GET` | `/api/knowledge/documents/{id}/content` | Current extracted document text; access-level protected |
| `GET` | `/api/knowledge/documents/{id}/versions` | Version history |
| `PUT` | `/api/knowledge/documents/{id}` | JSON metadata update (`title`, `description`, `category`, `accessLevel`); admin-only |
| `PUT` | `/api/knowledge/documents/{id}/reindex` | Multipart new-version file; admin-only |
| `POST` | `/api/knowledge/documents/{id}/archive` | Admin-only |
| `DELETE` | `/api/knowledge/documents/{id}` | Admin-only hard delete; removes versions/chunks and detaches retained feedback |
| `GET` | `/api/knowledge/search?query=...` | Optional `category`, `page`, and `size` |
| `POST` | `/api/knowledge/search` | JSON body with `query` and optional `category`, `page`, and `size` |
| `POST` | `/api/knowledge/ask` | JSON body: `{"question":"..."}` |
| `POST` | `/api/knowledge/feedback` | JSON feedback tied to a document and optional answer |
| `GET` | `/api/knowledge/analytics` | Admin-only aggregate metrics |

OpenAPI documentation is served at `/swagger-ui.html`; its JSON document is at
`/api-docs`. The static frontend is served at `/` and `/index.html`.

## Current limitations

- MySQL full-text search and the updated count queries have not yet been run
  against the reachable local MySQL instance; the test profile uses H2, which
  does not implement MySQL full-text syntax.
- Prompt-injection and timeout tests are deterministic contract/failure-path
  tests. They do not demonstrate that a live model always follows the prompt
  or measure an actual network timeout against Ollama.
- Citation validation confirms that cited labels correspond to retrieved
  chunks; it cannot prove semantic entailment for arbitrary generated claims.
  The synthetic evaluation checks its controlled reference facts and source
  identifiers, not open-ended model quality.
- Direct document detail, content, preview, and version-history APIs enforce
  document access levels and hide archived documents from non-admin users;
  admins can explicitly view them. Upload also enforces the requesting role's
  configured access-level policy.
- The Documents UI provides Read, Edit, Versions, and Delete actions. Metadata
  changes and new-version uploads are Admin-only, matching the existing
  archive/reindex/delete policy. Search shows one best-ranked chunk per
  accessible document and falls back to safe keyword matching against document
  text and metadata when MySQL FULLTEXT has no hits.
- The repository has no Git metadata in the supplied workspace, so Git status
  and commit history cannot be reported.
