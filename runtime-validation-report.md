# Runtime Validation Report

**Generated:** 2026-10-14  
**Target:** `C:\Riya\Projects\enterprise-knowledge-assistant`

## Final verification

| Step | Status | Result | Details |
|---|---|---:|---|
| Clean compile | PASS | Exit 0 | Re-run on 2026-10-14; `mvn clean compile`; 59 main Java files compiled with Java 21 |
| Full test suite | PASS | Exit 0; 56 passed, 0 failed, 0 skipped | Re-run on 2026-10-14; `mvn test` |
| Frontend syntax | PASS | Exit 0 | `node --check src\main\resources\static\js\app.js` |
| Session authentication | PASS | Verified | HTTP integration login using corrected seeded BCrypt hashes, JSESSIONID continuity, `/api/auth/me`, logout, CSRF, expired session, and role restrictions |
| HTTP Basic challenge removal | PASS | Verified | Anonymous protected GET returned JSON 401 with no `WWW-Authenticate` header |
| Standalone startup and browser rendering | PARTIAL | Health 200; H2 schema applied; landing page rendered | Started with Java 21 and the default `test` profile on port 8081; anonymous API request returned expected 401. No authenticated user was seeded for browser verification. |
| Document-management API | PASS | Covered by H2 integration tests | Seed demo documents read/content/preview/history; admin metadata updates persist without version/chunk changes; HR_ONLY routes deny employees; uploaded reads, versioning, deletion, and search remain covered |
| V4 seed-text migration | PASS | 2 H2 Flyway tests passed | Populates all five matching empty seed versions in section/chunk order; leaves populated text and uploaded content alone; rerunning Flyway creates no duplicate versions/chunks |
| Synthetic RAG evaluation | PASS | supported=1, citation-valid=2, abstained=2, conflicts=1 | Dynamically computed from the controlled test set |
| VPN RAG prompt smoke test | PASS | Supported VPN answer cited `[1]`; unrelated evidence led to abstention | Direct local Ollama `qwen2.5:3b` generation at configured temperature 0.1; not an application/database end-to-end test |

The latest clean compile emitted no javac warnings. The test output includes
third-party Byte Buddy dynamic-agent and Commons Logging discovery warnings;
neither causes a build or test failure.

## VS Code Problems triage

- The refreshed workspace Problems tool reported **112** entries (first 50
  returned). `mvn clean compile` passes with Java 21/release 21; the IDE report
  is not a list of javac compile failures. The returned entries include
  Spring Data null-annotation conversion diagnostics and configuration
  metadata/YAML inspection warnings.
- Red Hat Java 1.57 and Oracle Java 27 were both installed; Oracle Java activates
  for Java files. Workspace settings select Red Hat Java formatting and enable
  its Lombok support; a fresh VS Code window was previously launched with the
  Oracle Java extension disabled.
- Focused Problems checks of `KnowledgeChunkSearchRepositoryImpl.java`,
  `AccessPolicyRepository.java`, and `ApplicationSecurityIntegrationTest.java`
  return no diagnostics. `DocumentService.java` and `AskService.java` still
  show IDE null-annotation conversion diagnostics at Spring Data/AI boundaries.
  The previously flagged unused import and nullable test content-type access
  were corrected. The non-binding `spring.security.method-security` property
  was removed; `@EnableMethodSecurity` remains enabled on the application class.
- The current Problems excerpt does not contain the earlier "Flexible
  Constructor Bodies" message. Maven reports 0 compilation errors and the last
  clean compile emitted no javac warnings. YAML logger-key suggestions and
  unknown custom `app` metadata are editor inspections, not runtime
  configuration failures.
- The dependency inspection flags one HIGH OSV advisory for
  `spring-boot-starter-actuator@3.3.5`, identifies Spring Boot 3.3.13 as a newer
  patch, and reports the 3.3.x support window has ended. The advisory ID was not
  included in the Problems output and no dependency upgrade was attempted.
- No SQL parser diagnostic appeared in the inspected results; MySQL FULLTEXT
  syntax remains unverified against a live database.

## Mandatory scenario coverage

| Scenario | Result | Verification |
|---|---|---|
| TC-05-01 — Upload/search | PASS | Authenticated H2 HTTP upload, document/version/chunk persistence and search readback; short document produces one chunk |
| TC-05-02 — Invalid uploads | PASS | Valid PDF/TXT/Markdown extraction verified. Unsupported extension, empty/truncated/corrupt PDF, extension mismatch, and MIME mismatch return HTTP 400 with clear messages and no document/version/chunk persistence |
| TC-05-03 — Grounded answer/citations | PASS | Authenticated H2 HTTP ask verifies retrieved citation and persisted answer; five-chunk unit regression verifies the VPN passage reaches the prompt and maps `[1]` to its source document/version |
| TC-05-04 — Insufficient evidence | PASS | Empty retrieval, missing/out-of-range citations, and unrelated retrieved evidence abstain; safe content-only retrieval fallback is covered |
| TC-05-05 — Multiple-document citations | PASS | Mocked retrieval verifies citation chunk/document IDs and versions for two sources |
| TC-05-06 — Filters/access control | PASS | POST search verifies category filter and employee-vs-HR-only visibility |
| TC-05-07 — Authorization restriction | PASS | Employee denied HR-only document detail and archive; admin archive allowed; admin-only analytics checked |
| TC-05-08 — Prompt injection | PARTIAL | Tests verify system/user separation, explicit untrusted evidence instruction, and JSON/tag-delimiter escaping; live-model compliance is not proven |
| TC-05-09 — Reindex/stale content | PASS | H2 HTTP reindex verifies new current PUBLISHED version, old OBSOLETE version, and no stale search match |
| TC-05-10 — AI/retrieval timeout | PARTIAL | Simulated socket timeout and retrieval failure return structured unavailable responses; connect/read deadlines configured, but no delayed live network call tested |
| TC-05-11 — Feedback persistence | PASS | H2 HTTP feedback readback, rating validation, and cross-user answer ownership rejection |
| TC-05-12 — Synthetic RAG evaluation | PASS | Controlled cases compute supported answer, citation/source validity, abstentions, and conflict handling |

Additional document-management integration coverage verifies admin-only hard
deletion and related version/chunk cleanup, rejection of unauthorized deletion,
not-found behavior, metadata validation and access-level changes, HR_ONLY content
protection, version history, and new-version reindexing. Search tests cover
normalized queries, distinct document ranking, and metadata/text fallback.
The seed-document integration test covers all five demonstration passage sets
through content, preview, and version-history endpoints; metadata reload and
access control; Search and Ask citation retrieval; and no version/chunk changes
from metadata-only edits. A separate read assertion protects uploaded-document
behavior.

## Full-suite test breakdown

- `ApplicationSecurityIntegrationTest`: 2 passed
- `KnowledgeUploadSearchIntegrationTest`: 16 passed
- `SessionAuthenticationIntegrationTest`: 7 passed
- `SeedPasswordMigrationTest`: 1 passed
- `SeedDocumentTextMigrationTest`: 2 passed
- `SearchRequestValidationTest`: 1 passed
- `EntityBuilderDefaultsTest`: 3 passed
- `AskServiceTest`: 14 passed
- `DocumentServiceAuthorizationTest`: 3 passed
- `FeedbackServiceTest`: 4 passed
- `SearchServiceTest`: 3 passed

Automated RAG regression tests use deterministic mocked model responses; local
Ollama is not required to run `mvn test`.

## Seed sample document Read/Edit/version follow-up

- Root cause: V2 inserted the five seeded `document_version` rows without
  `extracted_text`; `getContent` depends on that value and returned a
  not-found response, while preview returned empty text. Existing administrator
  metadata-edit and version-history APIs/forms were already in place.
- Flyway V2 was left unchanged to avoid checksum validation failures on
  installations where it has already run. V4 is a Java migration discovered at
  `classpath:db/migration`; it matches only the five fixed seed IDs plus their
  original filename/checksum/document/version identity, and fills only a blank
  extracted-text field from the ordered `knowledge_chunk` rows. It prepends an
  explicit statement that the result is demonstration text, not an original
  source file. It inserts no documents, versions, or chunks.
- For an empty extracted-text field outside that migration case, the content
  and preview service paths reconstruct text from ordered chunks and label it
  as reconstructed, without claiming it is original source text. The normal
  document access-level check runs before this fallback.
- H2 Flyway migration tests verify all five fixture identities, passage order,
  preservation of pre-populated text and uploaded content, and unchanged
  version/chunk counts after a repeat migration attempt. H2 HTTP tests cover all
  five sample flows, HR_ONLY denial, metadata persistence without version
  changes, Search/Ask retrieval, and uploaded-document Read.
- MySQL credentials were unavailable, so Flyway V4 and MySQL-specific FULLTEXT
  search were not run against MySQL. The H2 tests do not establish MySQL
  FULLTEXT compatibility.

## Ask AI VPN failure follow-up

- Citation validation was the failure branch: a model response without a
  numeric `[N]` citation or with an out-of-range label becomes
  `INSUFFICIENT_EVIDENCE`, even when its prose is supported. Model request
  failures and empty responses remain `AI_UNAVAILABLE`; retrieval has no
  post-retrieval similarity threshold.
- The prompt now asks for synthesis from relevant supplied passages, evidence-
  based abstention, numeric citations matching supplied labels, and use of
  document title/version metadata. Existing injection defenses and
  citation-to-retrieved-chunk validation remain enabled.
- The regression supplies five chunks including the exact VPN policy passage,
  verifies that text and source/version metadata reach the prompt, and verifies
  a supported answer returns the expected citation. A separate test verifies
  that unsupported questions abstain.
- Direct local Ollama smoke tests with `qwen2.5:3b` at temperature `0.1`
  returned a supported VPN answer with `[1]`; unrelated evidence caused the
  model to abstain without a citation. This is not a full application/database
  integration test.
- The actual five persisted chunks and final runtime context could not be
  inspected: no application endpoint was running and no database credentials
  were configured. MySQL retrieval and the live app-to-model request remain
  unverified.

The full suite emitted no Spring warning that a global `AuthenticationProvider`
would bypass `UserDetailsService` username/password login. Session-authentication
integration tests passed with the explicitly configured DAO provider.

## PDF upload handling

- PDF files are parsed using PDFBox 3 `Loader.loadPDF(byte[])`; TXT and Markdown
  are decoded as UTF-8 through their text path.
- Upload validation rejects empty files, unsupported extensions, incompatible
  multipart content types, PDF bytes without a `.pdf` filename, and non-PDF
  bytes with a `.pdf` filename. PDF structural/parser failures are returned to
  clients as HTTP 400 with a stable message rather than a PDFBox stack trace.
- HTTP integration tests pass for valid PDF/TXT/Markdown files and invalid,
  truncated, empty, mislabeled, and MIME-mismatched PDF uploads. Rejection tests
  assert that document, version, and chunk counts do not change.
- Validation reads the multipart bytes once and shares them with extraction.
- MySQL runtime compatibility remains unverified; these upload tests use the
  project's H2 test profile.

## Runtime and integration boundaries

- **H2:** Embedded Tomcat and Flyway test schema start successfully. HTTP
  integration login obtains and reuses the session cookie across authenticated API
  calls. A separate Flyway test applies V3 against an H2 fixture and confirms
  changed seed passwords remain unchanged. The standalone test-profile app
  applies the H2 schema from `filesystem:src/test/resources/db/test-migration`
  without command-line overrides, serves the frontend, and rejects an
  unauthenticated documents request with 401. HTTP integration tests exercise
  authenticated upload, search, ask, feedback, analytics, reindex, and access
  control.
- **MySQL:** Live migration and application login against the configured database
  were not run. Port 3306 is reachable, but no DB username/password environment
  variables are set in this validation environment. A read-only connection using
  the configured default `root` user without a password failed with MySQL error
  1045 (access denied). No schema/data changes were made. V2's documented seed passwords did not match its BCrypt hashes;
  additive V3 conditionally repairs only rows whose hashes still equal the
  original V2 values, preserving changed passwords. The resulting hashes were
  checked with BCrypt and used in H2 login integration tests. Hibernate schema
  validation, `MATCH ... AGAINST`, and native count queries have not been executed
  against MySQL 8. H2 is not evidence of MySQL syntax or FULLTEXT behavior.
- **Ollama:** Direct local generation with `qwen2.5:3b` was verified for
  supported and unrelated-evidence scenarios. Automated RAG regressions use
  mocks; the full Spring application-to-Ollama/database flow and a delayed
  live-model response were not exercised. The configured
  `app.ai.timeout-seconds` applies connect/read timeouts to the auto-configured
  Ollama `RestClient`.
- **Database outage:** When retrieval itself fails, the service returns an
  `AI_UNAVAILABLE` response without attempting to persist an answer record,
  because the same unavailable database is the persistence store.
- **Citation semantics:** The service validates citation labels against the
  retrieved chunks and persists chunk/document/version identity. This is not a
  semantic entailment proof for arbitrary model claims.
- **Prompt injection:** Retrieved text is carried as escaped JSON user evidence,
  not in the system prompt; prompt policy explicitly treats it as untrusted.
  Model compliance still needs a live or adversarial model evaluation.
- **Frontend:** JavaScript syntax passes. Login now uses the JSON session endpoint,
  same-origin cookies, CSRF tokens, and server-side session restoration/logout.
  The Documents UI exposes read, metadata edit, version history/new-version, and
  delete actions. HTTP integration tests verify authenticated API calls retain
  the login session and cover document permissions and lifecycle behavior. The
  standalone H2 app on port 8081 served the landing page; its anonymous API call
  returned the expected 401. No authenticated browser document-management flow
  was run because the isolated database has no test user.
- **Docker:** Docker daemon unavailable.
- **Workspace VCS:** No `.git` metadata is present, so repository status and
  commit history cannot be reported.

## Runtime verdict

```text
environment:
  docker: UNAVAILABLE — daemon unavailable; H2 in-memory test schema used
  node: AVAILABLE — Node.js 24.11.0
  playwright: UNVERIFIED — CLI 1.64.0 was present, but browser listing failed because the local .links folder was missing; browser installation and Playwright E2E were not completed
  infra-tier: FALLBACK(embedded/in-memory) — Docker unavailable; H2-backed integration tests passed
  browser-tier: UNVERIFIED — landing page rendered in the integrated browser, but no authenticated browser journey or Playwright suite ran
startup: PASS — mvn spring-boot:run with the default test profile; Flyway applied the H2 test schema and /actuator/health returned 200
integration: PASS — exit_code: 0, passed: 56, failed: 0, skipped: 0; H2 HTTP/API and Flyway migration tests; MySQL SQL/FULLTEXT compatibility remains unverified
e2e: PARTIAL — exit_code: N/A, passed: 0, failed: 0; browser loaded the landing page, but authenticated document and RAG flows were not exercised
overall: NEEDS_SIGNOFF — build and automated test suite pass; Playwright/authenticated browser and live MySQL verification remain open
```

## Commands

```powershell
mvn clean compile
mvn test
node --check src\main\resources\static\js\app.js
```
