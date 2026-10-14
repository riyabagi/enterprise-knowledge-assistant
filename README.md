# Enterprise Knowledge Assistant

An AI-assisted internal knowledge portal built with Java and Spring Boot. It enables employees to upload and search company documents, ask questions in natural language, and receive answers grounded in retrieved document content.

The application uses MySQL for document storage and full-text search, Spring Security for authentication and authorization, and Ollama for locally hosted AI answer generation.

## Features

- **Document management:** Upload documents, view extracted content, update metadata, archive documents, and manage document versions.
- **Full-text search:** Search document content using MySQL full-text search and the application's keyword fallback.
- **AI-powered Q&A:** Ask natural-language questions about available knowledge documents.
- **Retrieval-Augmented Generation (RAG):** Supply retrieved document content as evidence for AI-generated answers.
- **Source citations:** Return references to retrieved document passages so users can identify the supporting sources.
- **Evidence-based abstention:** Return an insufficient-evidence result when the answer cannot be supported by valid retrieved citations.
- **Role-based access control:** Protect document content according to user roles and document access levels.
- **Session-based authentication:** Authenticate users through Spring Security sessions with CSRF protection for mutating requests.
- **Document version history:** Maintain document versions and associated extracted text.
- **Feedback and analytics:** Collect feedback and expose aggregate application metrics through the knowledge APIs.
- **Database migrations:** Manage schema changes and demonstration data using Flyway.
- **Automated testing:** Include unit tests and integration tests using JUnit 5, Mockito, Spring Boot testing, and H2.

> **Retrieval note:** The current configuration uses MySQL full-text search. Ollama embeddings are disabled, so the application should not be described as using vector-based semantic search unless that capability is subsequently implemented and verified.

## Technology Stack

| Component | Technology |
|---|---|
| Programming language | Java 21 |
| Backend framework | Spring Boot 3.3.5 |
| REST APIs | Spring Web |
| Persistence | Spring Data JPA, Hibernate |
| Database | MySQL 8 |
| Database migrations | Flyway |
| Connection pooling | HikariCP |
| Authentication and authorization | Spring Security |
| AI integration | Spring AI |
| Local AI runtime | Ollama |
| Default chat model | Qwen2.5 3B |
| Frontend | HTML5, CSS3, JavaScript |
| Build tool | Maven |
| Testing | JUnit 5, Mockito, Spring Boot Test, H2 |
| API documentation | Springdoc OpenAPI |

## Architecture

The application follows a layered Spring Boot architecture.

```text
Employee / Browser
        |
        v
HTML, CSS and JavaScript
        |
        v
Spring Boot REST Controllers
        |
        v
Spring Security
(Authentication and authorization)
        |
        v
Service Layer
   |             |
   v             v
MySQL         Spring AI
   |             |
   |             v
   |           Ollama
   |          Qwen2.5 3B
   |             |
   +------<------+
        |
        v
Answer with source references
```

### Main components

- **Controllers:** Handle HTTP requests and return API responses.
- **Services:** Implement document management, retrieval, AI question answering, feedback, and analytics workflows.
- **Repositories:** Access application data through Spring Data JPA and custom MySQL queries.
- **Entities:** Represent users, documents, versions, chunks, search history, answers, and feedback.
- **Security layer:** Enforces authentication, roles, and document access policies.
- **Flyway:** Applies versioned SQL migrations during application startup.
- **Ollama:** Runs the configured language model locally and generates answers from the supplied prompt and evidence.

### How the RAG workflow works

1. An employee submits a question.
2. The backend authenticates the user and determines which documents the user may access.
3. The retrieval service searches permitted document content.
4. Relevant passages are supplied to the AI model as context.
5. The model generates a response based on the supplied evidence.
6. The backend validates citation references and handles insufficient evidence or AI failures.
7. The application returns the answer and its source metadata.

The reliability of this workflow depends on the actual retrieval, authorization, citation-validation, and AI-generation implementations. Citation validation alone does not prove that every generated statement is semantically supported by its source.

## Prerequisites

Install the following software:

- Java Development Kit (JDK) 21
- Apache Maven 3.9 or later
- MySQL 8
- Ollama
- Git

Check your installations in PowerShell:

```powershell
java -version
mvn -version
mysql --version
ollama --version
```

## Getting Started

### 1. Clone the repository

Replace the URL below with your actual GitHub repository URL.

```powershell
git clone https://github.com/YOUR_USERNAME/enterprise-knowledge-assistant.git
cd enterprise-knowledge-assistant
```

### 2. Prepare MySQL

Start the MySQL service and sign in:

```powershell
mysql -u root -p
```

Create the application database if it does not already exist:

```sql
CREATE DATABASE enterprise_kb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

EXIT;
```

If the database already exists, you do not need to recreate it.

Flyway manages the application schema through the migration scripts under `src/main/resources/db/migration/`.

### 3. Configure database credentials

In the same PowerShell terminal used to start the application, set the environment variables:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = Read-Host "Enter your MySQL password"
```

Enter your MySQL password when prompted.

These environment variables apply only to the current PowerShell session. Set them again in a new terminal when necessary.

For production, use a dedicated database account with appropriate privileges rather than the MySQL root account.

**Security:** Never commit `.env` files, passwords, API keys, or other secrets to Git. A standard Spring Boot launch does not automatically load a `.env` file.

### 4. Configure Ollama

Start Ollama and download the configured chat model:

```powershell
ollama pull qwen2.5:3b
```

Check the installed models:

```powershell
ollama list
```

The default configuration is:

```text
Ollama URL: http://localhost:11434
Chat model: qwen2.5:3b
```

The model and URL can be overridden through environment variables:

```powershell
$env:OLLAMA_MODEL = "qwen2.5:3b"
$env:OLLAMA_BASE_URL = "http://localhost:11434"
```

If Ollama is installed as a background application, ensure its local API is reachable before testing AI requests.

### 5. Review the application configuration

Check the following files:

```text
src/main/resources/application.yml
src/main/resources/application-dev.yml
src/main/resources/application-test.yml
```

The development configuration uses MySQL and environment variables for database credentials. Hibernate validates the schema, while Flyway manages database migrations.

The test profile uses H2 for the configured automated tests. H2 is not a substitute for testing MySQL-specific full-text queries against MySQL itself.

### 6. Start the application

From the project root:

```powershell
mvn spring-boot:run
```

After successful startup, open:

| Resource | URL |
|---|---|
| Web application | http://localhost:8080 |
| Health endpoint | http://localhost:8080/actuator/health |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api-docs |

Swagger and Actuator availability depends on the active configuration and security rules.

A healthy Actuator response typically includes:

```json
{
  "status": "UP"
}
```

This response does not establish that every application feature or external dependency is working correctly.

## Database Migrations

Migration files are stored in:

```text
src/main/resources/db/migration/
```

The project includes the following migrations:

| Migration | Purpose |
|---|---|
| `V1__schema.sql` | Creates the application database schema. |
| `V2__seed.sql` | Adds demonstration users, access policies, documents, versions, and sample chunks. |
| `V3__repair_seed_password_hashes.sql` | Conditionally repairs the designated unchanged demonstration password hashes. |

Inspect the migration history in MySQL:

```sql
USE enterprise_kb;

SHOW TABLES;

SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Do not modify an already-applied versioned migration in a shared environment. Create a new migration for subsequent schema changes.

The seed data is for local development and demonstration only. It must not be treated as real company policy or production user data.

## Authentication and Demo Accounts

The application uses database-backed Spring Security authentication and sessions. Mutating requests require CSRF protection.

The seed migration defines these development accounts:

| Role | Username | Demonstration password |
|---|---|---|
| Administrator | `admin` | `admin123` |
| Employee | `alice` | `alice123` |
| HR | `hrmanager` | `hrm123` |

These credentials are intended for local demonstration only. Verify that the accounts work in your database, and change or remove them before deployment. Never reuse these passwords in production.

### Authentication flow

1. Obtain the CSRF token.
2. Submit the login request.
3. Use the authenticated session cookie for subsequent requests.
4. Include the CSRF token in mutating requests.
5. Log out to invalidate the session.

The frontend obtains the CSRF token through the authentication API and sends it in the `X-CSRF-TOKEN` header where required.

## REST API Reference

The following routes are implemented in the project according to the current application documentation. Request validation, authorization, and exact response structures should be checked against the source code and OpenAPI specification.

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/auth/csrf` | Obtain the CSRF token. |
| POST | `/api/auth/login` | Authenticate a user. |
| GET | `/api/auth/me` | Retrieve the current session user. |
| POST | `/api/auth/logout` | Invalidate the session. |

### Documents

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/knowledge/documents` | Upload a document and its metadata. |
| GET | `/api/knowledge/documents` | List documents with pagination and optional category filtering. |
| GET | `/api/knowledge/documents/{id}` | Retrieve document details. |
| GET | `/api/knowledge/documents/{id}/preview` | Retrieve an extracted-text preview. |
| GET | `/api/knowledge/documents/{id}/content` | Retrieve the current extracted document text. |
| GET | `/api/knowledge/documents/{id}/versions` | Retrieve document version history. |
| PUT | `/api/knowledge/documents/{id}` | Update document metadata; admin-only. |
| PUT | `/api/knowledge/documents/{id}/reindex` | Upload a new document version; admin-only. |
| POST | `/api/knowledge/documents/{id}/archive` | Archive a document; admin-only. |
| DELETE | `/api/knowledge/documents/{id}` | Permanently delete a document; admin-only. |

### Search, AI, Feedback, and Analytics

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/knowledge/search?query=...` | Search documents, with optional category and pagination parameters. |
| POST | `/api/knowledge/search` | Search using a JSON request. |
| POST | `/api/knowledge/ask` | Ask a natural-language question. |
| POST | `/api/knowledge/feedback` | Submit feedback on an answer or document. |
| GET | `/api/knowledge/analytics` | Retrieve aggregate analytics; admin-only. |

For exact JSON schemas and available operations, consult the application's Swagger UI.

## Testing

Run the automated test suite:

```powershell
mvn test
```

The project contains unit and integration tests covering areas such as:

- Search request validation.
- Document upload and search workflows.
- Authentication and session security.
- Document authorization.
- AI question-answering behavior.
- Citation validation and insufficient-evidence handling.
- Feedback and analytics services.
- Migration-related password hash behavior.

Check the actual Maven test results before reporting that the suite passes.

### Important testing limitations

- H2 does not implement MySQL's `MATCH ... AGAINST` full-text syntax. MySQL-specific search queries must be tested against a real MySQL instance.
- Mocked-model tests verify controlled application behavior, not the quality or reliability of every live model response.
- Citation-label validation cannot guarantee semantic support for every generated claim.
- A simulated or deterministic timeout test does not by itself prove correct behavior during a real Ollama network timeout.

## Configuration Reference

| Environment variable | Purpose | Default |
|---|---|---|
| `DB_USERNAME` | MySQL username | `root` in the development configuration |
| `DB_PASSWORD` | MySQL password | Must be configured |
| `OLLAMA_BASE_URL` | Ollama API URL | `http://localhost:11434` |
| `OLLAMA_MODEL` | Chat model identifier | `qwen2.5:3b` |

Confirm the current configuration files before changing these values.

## Security Considerations

- Keep passwords, tokens, and environment-specific secrets out of Git.
- Replace demonstration accounts and credentials before deployment.
- Enforce authorization in the backend, not merely by hiding frontend controls.
- Apply access checks to document lists, search results, previews, document versions, citations, and AI retrieval.
- Treat retrieved document content as untrusted input and defend against prompt injection.
- Do not expose HR-only or restricted content through generated answers, search, or analytics.
- Use HTTPS, managed secrets, suitable database privileges, and secure production configuration.
- Validate upload types and sizes, and handle parsing errors safely.
- Apply appropriate timeouts, request limits, and monitoring before allowing concurrent production use.

## Current Limitations and Verification

- Ollama embeddings are disabled in the current configuration; vector-based semantic retrieval is not enabled by default.
- MySQL full-text search requires verification against the actual MySQL environment.
- AI answer quality, source grounding, and abstention behavior must be evaluated with representative documents and questions.
- Authentication, document access restrictions, uploads, reindexing, archiving, and deletion should be verified through real API requests.
- The application has a local development setup, but that does not by itself constitute a production deployment configuration.
- The project should not be described as production-ready until its security, reliability, performance, and deployment requirements have been tested.

## Project Structure

```text
enterprise-knowledge-assistant/
├── src/
│   ├── main/
│   │   ├── java/com/enterprise/knowledge/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── db/migration/
│   │       ├── static/
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/
│       ├── java/
│       └── resources/
├── .gitignore
├── pom.xml
├── README.md
├── IMPLEMENTATION_STATUS.md
└── runtime-validation-report.md
```

## Future Improvements

Potential improvements include:

- Enable semantic retrieval using embeddings and a vector store.
- Add asynchronous document ingestion and processing.
- Improve evaluation of answer quality, retrieval relevance, and citation correctness.
- Add load testing and production monitoring.
- Deploy multiple application instances behind a load balancer when required.
- Add request throttling and bounded AI-generation concurrency.
- Introduce caching only where it is safe for permission-sensitive results.

## License

No license has been selected yet. Add a license file if you intend to publish or distribute this project under specific reuse terms.