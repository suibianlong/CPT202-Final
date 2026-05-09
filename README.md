# HerLink: Community Heritage Resource Sharing and Curation Platform

HerLink is a full-stack community heritage resource sharing and curation platform. It allows registered users to browse approved heritage resources, apply to become contributors, submit heritage resources for review, and interact through comments and feedback. Reviewers can evaluate submitted resources, while administrators can manage contributors, classifications, resource types, tags, resource lifecycle status, and operation history.

The system is implemented with a Spring Boot backend, a static HTML/CSS/Vanilla JavaScript frontend, a MySQL database, Docker-based deployment support, Nginx reverse proxy configuration, and GitHub Actions CI/CD automation.

---

## Table of Contents

- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Core User Roles](#core-user-roles)
- [Backend Architecture](#backend-architecture)
- [Frontend Modules](#frontend-modules)
- [Database Overview](#database-overview)
- [Prerequisites](#prerequisites)
- [Local Development Setup](#local-development-setup)
- [Docker Deployment](#docker-deployment)
- [Testing](#testing)
- [CI/CD Pipeline](#cicd-pipeline)
- [Important API Groups](#important-api-groups)
- [Configuration](#configuration)
- [Security Notes](#security-notes)
- [Known Implementation Notes](#known-implementation-notes)
- [License](#license)

---

## Key Features

### Authentication and User Account Management

- Email verification code registration flow.
- Session-based login and logout.
- Current-user session lookup through `/api/auth/me`.
- Account profile update support.
- PBKDF2-based password hashing.

### Contributor Application Workflow

- Registered users can submit contributor applications.
- Administrators/reviewers can view pending contributor requests.
- Contributor requests can be approved or rejected.
- Approved contributors gain resource creation and submission permissions.
- Contributor status can be revoked by administrators.

### Contributor Resource Management

- Contributors can create resource drafts.
- Contributors can edit metadata including title, description, copyright, place, category, resource type, and tags.
- Resource files and preview images can be uploaded and managed.
- Draft resources can be submitted for review.
- Rejected resources can be revised and resubmitted.
- Resource submission history and version history are available.
- Version comparison and rollback are supported for editable resources.

### Reviewer Workflow

- Reviewers can view pending resource submissions.
- Reviewers can inspect resource details, contributor information, metadata, files, tags, versions, and review history.
- Reviewers can approve or reject resource submissions.
- Rejection requires review feedback.
- Review decisions are recorded in review history.

### Public Viewer Workflow

- Users can browse approved heritage resources.
- Resource list supports keyword search, category filtering, resource type filtering, and sorting.
- Users can open resource detail pages.
- Users can post, view, and delete their own comments.
- Users can submit feedback with optional attachments.

### Administrator Management

- Manage categories.
- Manage resource types.
- Manage tags.
- View classification usage history.
- View administrator operation history.
- Archive and unarchive approved resources.
- Manage contributor applications and approved contributors.

### Testing and Deployment Support

- Frontend ESLint and Prettier configuration.
- Frontend Jest test suites with coverage configuration.
- Backend unit, controller, web-slice, and integration tests.
- JaCoCo backend coverage configuration.
- k6 regression and performance test scripts.
- Dockerfile for application image build.
- Docker Compose configuration for MySQL, Spring Boot application, and Nginx.
- GitHub Actions workflow for frontend checks, backend tests, build, Docker image publishing, deployment, and health check.

---

## Technology Stack

### Backend

- Java 17
- Spring Boot 3.3.5
- Spring MVC
- Spring Boot Mail
- Spring Boot Actuator
- MyBatis 3.0.3 Spring Boot Starter
- MySQL Connector/J
- H2 Database for tests
- Springdoc OpenAPI
- Maven Wrapper
- JUnit 5
- Testcontainers
- JaCoCo

### Frontend

- HTML5
- CSS3
- Vanilla JavaScript
- Fetch API
- Jest
- jsdom
- ESLint
- Prettier

### Database and Infrastructure

- MySQL 8.4
- Docker
- Docker Compose
- Nginx stable-alpine
- GitHub Actions
- k6 for regression and performance testing

---

## Project Structure

```text
.
├── backend
│   ├── src/main/java/com/cpt202/HerLink
│   │   ├── config                 # Application configuration and demo data initializer
│   │   ├── controller             # REST-style API controllers
│   │   ├── dto                    # Request and response transfer objects
│   │   ├── entity                 # Database entity models
│   │   ├── enums                  # User, resource, and application status enums
│   │   ├── exception              # Application exception and global exception handling
│   │   ├── mapper                 # MyBatis mapper interfaces
│   │   ├── service                # Business service interfaces and implementations
│   │   ├── util                   # Permission, password, file, status, and tag utilities
│   │   └── vo                     # View objects returned to frontend pages
│   ├── src/main/resources
│   │   ├── mapper                 # MyBatis XML SQL mappings
│   │   ├── application.yml        # Shared application configuration
│   │   ├── application-local.yml  # Local profile configuration
│   │   └── application-prod.yml   # Production profile configuration
│   ├── src/test/java              # Backend unit, controller, and integration tests
│   ├── sql/init_database.sql      # MySQL schema initialization script
│   └── pom.xml                    # Backend Maven configuration
│
├── frontend
│   ├── module1                    # Authentication, registration, account, and entry pages
│   ├── module2                    # Contributor approval frontend logic
│   ├── module3                    # Contributor resource workspace and editor
│   ├── module5                    # Reviewer approval interface
│   ├── module6                    # Viewer resource browsing, detail, comments, and feedback
│   ├── module7                    # Administrator dashboard and management pages
│   ├── shared                     # Shared JavaScript and CSS utilities
│   ├── tests                      # Frontend Jest test suites
│   ├── package.json               # Frontend tooling scripts and dependencies
│   └── jest.config.cjs            # Jest configuration
│
├── tests
│   ├── acceptance                 # Acceptance test documents
│   ├── regression/k6              # k6 critical regression test script
│   ├── performance/k6             # k6 load, spike, ramp, and soak test script
│   ├── security                   # Manual security test record
│   ├── stability                  # Stability and recovery test record
│   ├── system                     # System test outputs and records
│   └── system-test-guide.md       # System testing guide
│
├── .github/workflows/ci-cd.yml    # GitHub Actions CI/CD workflow
├── Dockerfile                     # Multi-stage application Docker build
├── docker-compose.yml             # MySQL + app + Nginx deployment configuration
├── nginx/default.conf             # Nginx reverse proxy configuration
└── .gitignore
```

---

## Core User Roles

| Role | Representation | Main Permissions |
| --- | --- | --- |
| Registered user | `user.role = user`, `isContributor = false` | Register, log in, browse approved resources, comment, submit feedback, apply to become contributor |
| Contributor | `user.role = user`, `isContributor = true` | Create drafts, edit resources, upload files, submit resources for review, view submission/version history |
| Reviewer / Administrator | `user.role = reviewer` | Review contributor applications, approve/reject resource submissions, manage classifications, tags, resource types, resources, and operation history |

The project uses session-based authentication. After login, the authenticated user ID is stored in the HTTP session and checked on protected API calls.

---

## Backend Architecture

The backend follows a layered structure:

```text
Controller Layer
    ↓
Service Layer
    ↓
Mapper / MyBatis XML Layer
    ↓
MySQL Database
```

### Main Backend Responsibilities

- Controllers receive HTTP requests and expose REST-style endpoints.
- Services enforce business rules, permissions, resource lifecycle transitions, review logic, and notification logic.
- Mappers and MyBatis XML files execute explicit SQL operations.
- Entities map database records.
- DTOs represent request payloads and internal transfer models.
- VOs represent frontend-facing response objects.
- Utility classes provide permission checking, password hashing, file validation, file storage, status validation, and tag normalization.
- Global exception handling converts application errors into consistent HTTP responses.

---

## Frontend Modules

| Module | Main Files | Responsibility |
| --- | --- | --- |
| Module 1 | `index.html`, `login.html`, `register.html`, `account.html`, `module1/*` | Home page, login, registration, account management, contributor application entry |
| Module 2 | `module2/admin-approval.*` | Contributor application approval UI logic |
| Module 3 | `my-resources.html`, `resource-edit.html`, `module3/*` | Contributor resource list, draft creation, resource editing, file upload, submission, versions |
| Module 5 | `module5/review-approval.*` | Reviewer resource approval workflow UI |
| Module 6 | `module6/heritage-viewer.html`, `module6/viewer-detail.html`, `module6/viewer-feedback.html` | Approved resource browsing, resource detail, comments, feedback |
| Module 7 | `admin-dashboard.html`, `classification-management.html`, `tag-management.html`, `admin-resources.html`, `module7/*` | Admin dashboard, classifications, tags, resource lifecycle, operation history |
| Shared | `shared/shared.js`, `shared/shared.css` | Shared request, logout, toast, date formatting, and UI helpers |

---

## Database Overview

The database initialization script is located at:

```text
backend/sql/init_database.sql
```

It creates the database:

```text
heritageResourcePlatform
```

Main tables include:

| Table | Purpose |
| --- | --- |
| `user` | User account, role, contributor status, profile information |
| `category` | Resource category definitions |
| `resourceType` | Resource type definitions |
| `tag` | Tag definitions |
| `resource` | Main heritage resource records |
| `resourceTag` | Many-to-many relationship between resources and tags |
| `resourceSubmission` | Submitted resource versions awaiting or processed by review |
| `reviewRecord` | Reviewer decisions and feedback |
| `contributorApplication` | Contributor application workflow records |
| `comment` | Viewer comments on approved resources |
| `feedback` | Viewer feedback records |
| `attachedFile` | Feedback attachment records |
| `resourceVersion` | Version snapshots for resource history, comparison, and rollback |
| `resourceFile` | Resource file metadata |
| `adminOperationHistory` | Administrator operation audit history |
| `resourceArchive` | Archive table defined in schema |
| `contributorApplicationArchive` | Contributor application archive table defined in schema |

---

## Prerequisites

### Required for local backend development

- JDK 17
- MySQL 8.x
- Maven Wrapper included in `backend/mvnw`

### Required for frontend checks and tests

- Node.js 20 or compatible modern Node.js version
- npm

### Required for containerized deployment

- Docker
- Docker Compose

### Optional for system tests

- k6

---

## Local Development Setup

### 1. Clone or extract the project

```bash
git clone <repository-url>
cd CPT202-Final-main
```

If using the submitted archive, extract it first and enter the extracted root directory.

### 2. Initialize MySQL database

Make sure MySQL is running, then execute:

```bash
mysql -u root -p < backend/sql/init_database.sql
```

The script creates and uses the database `heritageResourcePlatform`.

### 3. Configure backend environment variables

For local development, the application uses the `local` profile by default. It is recommended to override database and mail settings through environment variables instead of hard-coding credentials.

macOS / Linux example:

```bash
export SPRING_PROFILES_ACTIVE=local
export DB_URL='jdbc:mysql://127.0.0.1:3306/heritageResourcePlatform?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true'
export DB_USERNAME='root'
export DB_PASSWORD='<your-mysql-password>'

# Optional: required only when testing email verification/notification flows
export MAIL_HOST='smtp.example.com'
export MAIL_PORT='587'
export MAIL_USERNAME='<your-mail-username>'
export MAIL_PASSWORD='<your-mail-password>'
export MAIL_FROM='<your-sender-email>'
export MAIL_SMTP_AUTH='true'
export MAIL_SMTP_STARTTLS_ENABLE='true'
```

Windows PowerShell example:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:DB_URL="jdbc:mysql://127.0.0.1:3306/heritageResourcePlatform?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="<your-mysql-password>"
```

### 4. Run the backend application

```bash
cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

The backend starts on:

```text
http://localhost:8080
```

The frontend is served as static resources through the Spring Boot application. Example pages:

```text
http://localhost:8080/index.html
http://localhost:8080/login.html
http://localhost:8080/register.html
http://localhost:8080/module6/heritage-viewer.html
```

### 5. Optional demo data

Demo data is disabled by default.

To enable demo data during local testing:

```bash
export DEMO_DATA_ENABLED=true
```

When enabled and the database is empty, the application can seed demo users and sample classification/resource data.

Default demo accounts used by the prepared k6 scripts are:

| Account | Email | Password |
| --- | --- | --- |
| Admin / Reviewer | `admin@heritage.local` | `Admin123!` |
| Contributor | `contributor@heritage.local` | `Contributor123!` |
| Viewer | `viewer@heritage.local` | `Viewer123!` |
| Pending applicant | `pending@heritage.local` | `Pending123!` |

---

## Docker Deployment

The repository provides:

- `Dockerfile`
- `docker-compose.yml`
- `nginx/default.conf`

The Docker Compose setup contains three services:

| Service | Image / Build | Purpose |
| --- | --- | --- |
| `db` | `mysql:8.4` | MySQL database with initialization script |
| `app` | Built from `Dockerfile` or pulled through `APP_IMAGE` | Spring Boot application and static frontend |
| `nginx` | `nginx:stable-alpine` | Reverse proxy from port 80 to the app container |

### 1. Create `.env`

Create a `.env` file in the project root:

```env
MYSQL_ROOT_PASSWORD=<your-strong-mysql-root-password>

MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=<your-mail-username>
MAIL_PASSWORD=<your-mail-password>
MAIL_FROM=<your-sender-email>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true

JAVA_OPTS=
APP_IMAGE=herlink-app:local
```

### 2. Start services

```bash
docker compose up --build -d
```

### 3. Check running containers

```bash
docker compose ps
```

Expected containers:

```text
herlink-mysql
herlink-app
herlink-nginx
```

### 4. Open the application

```text
http://localhost/
```

### 5. Health check

```bash
curl http://localhost/actuator/health
```

Expected response includes:

```json
{"status":"UP"}
```

### 6. Reinitialize database volume when needed

The SQL initialization script runs only when the MySQL volume is first created. To fully reset the database:

```bash
docker compose down -v
docker compose up --build -d
```

Warning: `docker compose down -v` removes persistent database and upload volumes.

---

## Testing

## Frontend checks

From the `frontend` directory:

```bash
cd frontend
npm ci
npm run lint
npm run format:check
npm run test:coverage -- --runInBand
```

Available frontend scripts:

| Command | Purpose |
| --- | --- |
| `npm run lint` | Run ESLint checks |
| `npm run lint:fix` | Auto-fix ESLint issues where possible |
| `npm run format` | Format supported files with Prettier |
| `npm run format:check` | Check Prettier formatting for configured project files |
| `npm test` | Run Jest tests |
| `npm run test:coverage` | Run Jest tests with coverage |

## Backend unit tests

From the `backend` directory:

```bash
cd backend
chmod +x mvnw
./mvnw clean test jacoco:report
```

## Backend web-slice and integration tests

```bash
cd backend
./mvnw verify -Dskip.unit.tests=true
```

## Backend package build

```bash
cd backend
./mvnw -DskipTests package
```

Generated JAR files are placed under:

```text
backend/target/
```

## k6 system tests

Prepared k6 scripts are available under:

```text
tests/regression/k6/critical-regression.js
tests/performance/k6/load-test.js
```

Example regression command:

```bash
BASE_URL="http://localhost" k6 run tests/regression/k6/critical-regression.js
```

Example performance command:

```bash
BASE_URL="http://localhost" LOAD_PROFILE="baseline" k6 run tests/performance/k6/load-test.js
```

More details are documented in:

```text
tests/system-test-guide.md
```

---

## CI/CD Pipeline

The GitHub Actions workflow is defined in:

```text
.github/workflows/ci-cd.yml
```

It runs on:

- Pushes to `main` or `master`
- Pull requests targeting `main` or `master`
- Manual workflow dispatch

Main jobs:

| Job | Purpose |
| --- | --- |
| `frontend-check` | Install frontend dependencies, run ESLint, check Prettier formatting, run Jest coverage tests, package frontend static resources |
| `backend-test` | Run backend unit tests and generate JaCoCo reports |
| `backend-slice-test` | Run backend web-slice and integration tests |
| `backend-build` | Build the backend JAR package |
| `docker-build-push` | Build Docker image and push to private registry on `main`/`master` push |
| `deploy` | Deploy the registry image to the server through SSH and Docker Compose, then run Actuator health check |

Required GitHub secrets for image publishing and deployment include:

```text
REGISTRY_HOST
REGISTRY_NAMESPACE
REGISTRY_USERNAME
REGISTRY_PASSWORD
SERVER_HOST
SERVER_USER
SERVER_SSH_PORT
SERVER_SSH_KEY
SERVER_APP_DIR
DEPLOY_HEALTHCHECK_URL
```

---

## Important API Groups

| API Base Path | Controller | Responsibility |
| --- | --- | --- |
| `/api/auth` | `AuthController` | Registration verification, register, login, logout, current user, account update |
| `/api/contributor-requests` | `ContributorRequestController` | User contributor application submission and latest application lookup |
| `/api/admin/contributor-requests` | `AdminContributorRequestController` | Contributor request approval, rejection, approved contributor list, contributor revoke |
| `/api/contributor/resources` | `ContributorResourceController`, `ContributorResourceHistoryController` | Draft creation, update, upload, submit, list own resources, versions, comparison, rollback |
| `/api/reviewer/reviews` | `ReviewWorkflowController` | Pending reviews, review details, review history, approve, reject, submit decision |
| `/api/viewer/resources` | `ViewerResourceController` | Approved resource browsing and resource detail |
| `/api/viewer/resources/{resourceId}/comments` | `ViewerCommentController` | Resource comments |
| `/api/viewer/feedback` | `ViewerFeedbackController` | Feedback submission and feedback listing |
| `/api/admin/categories` | `AdminCategoryController` | Category management |
| `/api/admin/resource-types` | `AdminResourceTypeController` | Resource type management |
| `/api/admin/tags` | `AdminTagController` | Tag management and tag usage history |
| `/api/admin/resources` | `AdminResourceController` | Resource lifecycle management, archive, unarchive |
| `/api/admin/classifications` | `AdminClassificationController` | Classification usage history |
| `/api/admin/operation-history` | `AdminOperationHistoryController` | Administrator operation history |

---

## Configuration

### Shared configuration

`backend/src/main/resources/application.yml` defines:

- Application name
- Active profile selection
- MySQL driver
- Multipart upload size limit
- MyBatis mapper location
- Upload directory
- Frontend directory
- Demo data toggle
- Email verification settings
- Notification sender settings

### Local profile

`backend/src/main/resources/application-local.yml` is used for local development.

Recommended practice:

- Do not commit real local database passwords or mail authorization codes.
- Override local settings through environment variables.
- Keep local-only credentials outside the public repository.

### Production profile

`backend/src/main/resources/application-prod.yml` reads database settings from environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Docker Compose also provides mail, upload, frontend, and Java runtime settings through environment variables.

---

## Security Notes

- The application uses session-based authentication.
- Passwords are stored with PBKDF2 hashing rather than plain text.
- Protected backend operations are checked through server-side permission validation.
- Resource editing and submission are restricted by contributor status and resource ownership.
- Reviewer/admin operations require reviewer-level permission.
- Nginx adds basic security response headers, including `X-Content-Type-Options`, `X-Frame-Options`, and a Content Security Policy.
- Uploaded files are stored in a configured upload directory and served through application configuration.
- Local configuration files must not contain real production secrets before public release.

---

## Known Implementation Notes

- Contributor upload-backed resource types are `photo`, `video`, `audio`, and `document`.
- The database schema defines archive tables, but the current application lifecycle primarily archives resources by updating resource status instead of moving rows into archive tables.
- The frontend is a static multi-page application served by the Spring Boot backend, not a React/Vue/Angular single-page application.
- The Maven artifact ID in `backend/pom.xml` is currently `module3`.
- Docker deployment uses standard rolling container replacement through Docker Compose. It does not implement blue-green deployment or automatic rollback.

---

## License

A backend `LICENSE` file is included in the repository. Check the project license file before redistribution or reuse.

---

## Acknowledgements

This project was developed as part of the CPT202 coursework project for a community heritage resource sharing and curation platform.
