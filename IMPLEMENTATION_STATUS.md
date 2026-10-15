# Implementation Status

This checklist records only verified state. `DONE — VERIFIED` requires a passing
test or a direct runtime check; file presence alone is not sufficient.

## Initial inspection

| Requirement | Status | Evidence |
|---|---|---|
| Existing project features and API contracts inventoried | DONE — VERIFIED | Source audit completed; see requirements table below |
| Assignment/problem statement checked | DONE — VERIFIED | Original supplied assignment attachment reviewed, including TC-05-01 through TC-05-12; no copy is stored in the project root |
| Java/Maven versions checked | DONE — VERIFIED | Java 21.0.12.1, Maven 3.9.11 |
| Current `mvn clean compile` | DONE — VERIFIED | Exit code 0; 59 main Java files compiled with Java 21; Lombok builder and annotation-processor warnings remain |
| Current `mvn test` | DONE — VERIFIED | Exit code 0; 56 passed, 0 failed, 0 skipped |
| Runtime dependencies checked | DONE — VERIFIED | Docker daemon unavailable; localhost:3306 and localhost:11434 reachable; Ollama models enumerated |
| Repository status checked | DONE — VERIFIED | No `.git` directory; `git status` is unavailable |

## Requirements

| Requirement | Status | Evidence / follow-up |
|---|---|---|
| Document lifecycle | PARTIALLY VERIFIED | H2 HTTP tests cover upload/read, all five seed-demo content/preview/version endpoints, admin-only metadata edits and authorization, version/chunk stability, reindex/current-version retrieval, deletion cleanup, and protected content. V4 populates only blank, identity-matched seed text from ordered chunks. Production MySQL persistence remains unverified. |
| Search API | PARTIALLY VERIFIED | H2 HTTP tests cover normalized GET/POST search, category and access filters, distinct-document results, content/metadata fallback, and readback. Production MySQL FULLTEXT and count SQL have not been run. |
| Grounded RAG and citation validation | PARTIALLY VERIFIED | Tests verify authorized H2 retrieval, exact VPN evidence in a five-chunk model prompt, numeric citation validation and document/version mapping, unsupported-evidence abstention, conflict status, and privacy-safe retrieval diagnostics. Local Ollama prompt smoke tests passed; actual persisted user chunks and full app-to-Ollama flow were not available. Labels are checked against retrieved chunks, not semantic entailment for arbitrary claims. |
| REST API contracts and errors | PARTIALLY VERIFIED | Upload/search/ask/feedback/analytics and auth login/logout contracts have HTTP/security integration coverage; invalid document category error shape and all endpoints are not exhaustively checked. |
| Role/document authorization | DONE — VERIFIED | Session-authenticated H2 HTTP tests verify employee/HR visibility, protected document reads, admin-only archive/analytics, unauthenticated JSON 401 without `WWW-Authenticate`, and 403 for authenticated non-admins. |
| Session login/logout and CSRF | DONE — VERIFIED | Exact corrected seed BCrypt hashes authenticate ADMIN, EMPLOYEE, and HR accounts; bad password returns JSON 401, session persists across real HTTP requests, logout/expired session lose access, unsafe requests require CSRF, and no HTTP Basic challenge is returned. |
| Feedback persistence/ownership | DONE — VERIFIED | H2 HTTP test persists and reads back feedback; verifies answer ownership rejection and invalid rating rejection; existing service tests also cover denied document access and missing answer records. |
| Database-backed analytics | DONE — VERIFIED | H2 HTTP test creates indexed document, search history, and answered question; employee is denied and admin receives populated persisted totals. |
| Frontend/API/auth integration | PARTIALLY VERIFIED | JavaScript parses; frontend renders in a browser and now uses `/api/auth/login`, same-origin session cookies, CSRF headers, `/api/auth/me`, and server logout. HTTP integration tests verify login/session persistence; document-management actions were not exercised in an authenticated browser session. |
| Flyway/MySQL/H2 schema parity | PARTIALLY VERIFIED | V4 Java migration passes H2 Flyway tests for seeded passage order, existing text preservation, uploaded-version safety, and retry counts. V1-V3 and V4 have not been run against configured MySQL credentials; production MySQL FULLTEXT and native count/search queries remain unverified. |
| Seeded demonstration document text | DONE — VERIFIED | Migration test confirms V4 builds extracted text from ordered chunks for all five known seed identities, does not replace populated text or uploaded content, and creates no duplicate versions/chunks. HTTP tests verify Read/content, preview, history, metadata-edit persistence, HR_ONLY denial, Search, and Ask citations for all five fixtures. |
| TC-05-01 — Upload and search | DONE — VERIFIED | H2 HTTP test verifies auth, document/version persistence, exactly one chunk for short text, and search readback. |
| TC-05-02 — Invalid upload rejection | DONE — VERIFIED | Unsupported extension and invalid PDF return HTTP 400; document, version, and chunk counts remain unchanged. Valid PDF/TXT/Markdown extraction and empty, truncated, mislabeled, or MIME-mismatched PDF rejection are covered by HTTP integration tests. |
| TC-05-03 — Grounded answer/citations | DONE — VERIFIED | Authenticated H2 HTTP ask verifies current authorized retrieval, valid citation index/chunk, persisted answer/status/citation JSON. A five-chunk regression verifies the exact VPN policy passage reaches the model prompt and returns a citation mapped to the source document and version. |
| TC-05-04 — Insufficient evidence | DONE — VERIFIED | Unit tests verify empty retrieval abstains without calling AI, missing/out-of-range citations abstain, and unrelated retrieved evidence does not support the VPN answer. |
| TC-05-05 — Multiple-document citations | DONE — VERIFIED | Unit test verifies two distinct chunk IDs, document IDs, version numbers, and citation labels from two source documents. |
| TC-05-06 — Filters and access control | DONE — VERIFIED | H2 HTTP POST test verifies category filter and that employee search excludes HR_ONLY content while HR search includes it. |
| TC-05-07 — Authorization restriction | DONE — VERIFIED | H2 HTTP tests verify employee cannot read an HR_ONLY document or archive a document; admin archive succeeds; analytics is also admin-only. |
| TC-05-08 — Prompt-injection resistance | PARTIALLY VERIFIED | Deterministic prompt test verifies retrieved text is kept out of the system instruction, evidence is JSON-encoded with escaped tag delimiters, and the system prompt explicitly treats it as untrusted. Live-model compliance is unverified. |
| TC-05-09 — Reindexing/stale content | DONE — VERIFIED | H2 HTTP test reindexes a document, verifies old version is OBSOLETE, new version is PUBLISHED/current, and old-only search terms no longer return it. |
| TC-05-10 — AI/retrieval timeout | PARTIALLY VERIFIED | Unit tests map simulated socket timeout/AI exceptions and retrieval failure to structured unavailable responses. Ollama RestClient connect/read timeouts use `app.ai.timeout-seconds`; no delayed live-network timeout was exercised. Retrieval DB outage cannot persist an answer record. |
| TC-05-11 — Feedback persistence | DONE — VERIFIED | H2 HTTP test verifies persisted feedback readback, rating validation, and rejection of another user's answer; service tests cover document access restrictions. |
| TC-05-12 — Synthetic RAG evaluation | DONE — VERIFIED | Deterministic evaluation computed and passed: 1 supported answer, 2 citation-valid cases, 2 expected abstentions, 1 conflict handled. The test checks controlled reference facts and source IDs. |
| Mandatory test scenarios overall | PARTIALLY VERIFIED | All 12 scenarios have test coverage; TC-05-08 and TC-05-10 remain partially verified as described above. |
| README, API/setup docs, test evidence, Postman, synthetic sample, demo | IN PROGRESS | README and runtime/status reports updated. Postman collection, separate synthetic sample artifact, architecture overview, and demo guide remain. |
| Application startup and runtime workflows | PARTIALLY VERIFIED | `mvn clean compile` succeeds under Java 21; H2 HTTP integration tests verify auth and protected API workflows using an embedded server and session cookies. Standalone H2 startup applies the test schema from `filesystem:src/test/resources/db/test-migration`; the landing page renders and anonymous API calls return 401. Authenticated browser document-management flows, live-model prompt compliance/timeout, and production MySQL workflows remain unverified. |

## Validation evidence

- Project sources include 59 main Java files and a static HTML/CSS/JavaScript frontend.
- Reverified on 2026-10-14: `mvn clean compile` exit 0 with 59 main Java files
  compiled under Java 21; `mvn test` exit 0 with 56 passed, 0 failed, 0 skipped.
- The refreshed workspace Problems query reports 112 entries (first 50 returned),
  while `mvn clean compile` succeeds using `javac --release 21`. Returned IDE
  diagnostics include Spring Data null-annotation conversion warnings and
  configuration metadata/YAML inspection warnings; focused checks of the
  touched repository and security test files report no diagnostics.
- The current Problems excerpt does not show the earlier "Flexible Constructor
  Bodies" message. Maven compiles all 59 main Java files at release 21, so no
  Java 25 source level is required.
- The latest `mvn clean compile` emitted no javac warnings. `mvn test` still
  prints third-party Byte Buddy dynamic-agent and Commons Logging discovery
  warnings; neither caused test failures.
- The Problems excerpt flags one HIGH OSV advisory against
  `spring-boot-starter-actuator@3.3.5`, a newer Spring Boot 3.3 patch, and the
  end of 3.3.x support. No dependency upgrade was made because it is a separate
  compatibility/security change; resolve it in a dedicated Spring Boot
  maintenance upgrade.
- Removed the invalid `spring.security.method-security` YAML setting
  (method security is enabled by `@EnableMethodSecurity`) and an unused import.
- Java 21 and Maven 3.9.11 are available.
- Original assignment attachment was inspected; it is external to the project root.
- The 12 mandatory assignment tests are mapped individually above.
- `mvn test`: exit code 0; 56 passed, 0 failed, 0 skipped. The latest run emitted
  no `AuthenticationProvider`/`UserDetailsService` configuration warning; the
  tested DAO provider authenticates through the configured `UserDetailsService`.
- MySQL port 3306 responds, but `DB_USERNAME`, `DB_PASSWORD`, and their Spring
  datasource equivalents are unset in the current environment. No authenticated
  MySQL schema/query validation was attempted; do not infer MySQL compatibility
  from the H2 suite.
- Follow-up read-only MySQL connection using the configured default `root` user
  and no password failed with MySQL error 1045 (access denied). The database
  service is reachable, but live MySQL verification remains blocked until the
  development process is configured with valid credentials.
- The isolated H2-profile server was started on port 8081 with the default
  `test` profile; Flyway applied the H2 test schema from
  `filesystem:src/test/resources/db/test-migration`. `/actuator/health`
  returned HTTP 200 and the browser rendered the landing page. The page's
  anonymous API request returned HTTP 401, as expected. No authenticated
  browser document-management flow was run because the isolated database has
  no test user.
- `node --check src/main/resources/static/js/app.js`: exit code 0.
- Deterministic synthetic RAG evaluation output: `supported=1, citation-valid=2, abstained=2, conflicts=1`.
- Local Ollama `POST /api/generate` with `qwen2.5:3b`: returned `READY`; application default model updated to this locally verified model.
- Local prompt smoke test with `qwen2.5:3b` at the configured temperature 0.1 produced a VPN-policy answer with numeric citation `[1]`; unrelated evidence led the model to abstain without a citation. This exercised direct Ollama generation, not the full application/database flow.
- RAG abstention diagnosis: retrieval count alone is not proof of a usable answer. The service rejects model output without a valid numeric `[N]` citation as `INSUFFICIENT_EVIDENCE`; request exceptions and empty output remain `AI_UNAVAILABLE`. The prompt now explicitly requests synthesis from relevant passages and numeric citations, and carries document version metadata alongside title.
- H2 integration tests verified the embedded server starts, `/` serves the frontend, and unauthenticated `/api/knowledge/documents` returns 401.
- Session security tests verify all three V2-documented passwords against corrected V3 BCrypt hashes, session continuity, JSON 401 without `WWW-Authenticate`, incorrect-password rejection, CSRF enforcement, logout invalidation, expired sessions, and role restrictions.
- Standalone H2 test-profile server was started without a Flyway command-line
  override; Flyway applied the H2 test schema, `/actuator/health` returned 200,
  and `/` rendered the expected page title. An anonymous API request returned
  401. The standalone H2 database has no test users, so no authenticated
  browser journey was run.
- Local Ollama endpoint and `qwen2.5:3b` direct generation were verified,
  including the VPN prompt smoke test; Spring application's complete
  app-to-model/database flow remains unverified. MySQL is only TCP-reachable
  here, without credentials for application-level validation.
- `DB_USERNAME` and `DB_PASSWORD` are not configured in the environment; MySQL
  TCP reachability alone does not verify MySQL migrations or full-text queries.
- Timeout behavior is tested via deterministic exceptions, and Ollama's HTTP
  client has configured connect/read deadlines; no live delayed Ollama response
  or MySQL outage has been induced.
