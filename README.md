# Library Management System

A Java-based library management system with multi-role users, digital and physical book borrowing, and a React frontend.

## Technology Stack

| Component          | Technology                   | Version |
| ------------------ | ---------------------------- | ------- |
| Framework          | Spring Boot                  | 3.2.1   |
| Language           | Java                         | 21      |
| Database           | PostgreSQL                   | 16      |
| ORM                | Spring Data JPA + Hibernate  | —       |
| Authentication     | Keycloak (OIDC, built in) + Spring Security | 26.x / 6.x |
| Sessions           | Spring Session JDBC (encrypted cookie) | — |
| API Documentation  | Springdoc OpenAPI/Swagger UI | 2.1.0   |
| Migrations         | Liquibase                    | —       |
| Frontend           | React                        | 18      |
| Web Server         | nginx                        | —       |
| Containerization   | Docker & Docker Compose      | —       |

## Architecture

```
Browser ──HTTPS──► nginx (the only published port)
                    ├── /        → React SPA (static files)
                    ├── /api/*   → Spring Boot ──┐
                    └── /auth/*  → Keycloak    ──┴──► PostgreSQL (internal network only)
```

The app is a backend-for-frontend OIDC client: the browser only holds an encrypted,
HttpOnly session cookie; tokens never reach it. Keycloak provides login with captcha,
MFA (TOTP, required for everyone by default), password policy and account lockout.
See [SECURITY_COMPLIANCE.md](SECURITY_COMPLIANCE.md) for the full security design.

## Getting Started

**Prerequisite:** Docker

```bash
./scripts/generate-env.sh      # creates .env with random secrets (no defaults exist)
docker compose up -d --build
```

The system is then served at `APP_PUBLIC_URL` (e.g. `https://library.local:3000`).
Register with the `BOOTSTRAP_SUPER_ADMIN_EMAIL` address to get the first super admin.
Full installation, TLS, upgrade and backup instructions: [DEPLOYMENT.md](DEPLOYMENT.md).

## Offline Deployment

For environments without internet access, use the export/import scripts.

**On a machine with internet (build once):**
```bash
./scripts/export-bundle.sh
tar -czf library-bundle.tar.gz bundle/
sha256sum library-bundle.tar.gz
```

**On the target machine (Docker only required):**
```bash
tar -xzf library-bundle.tar.gz
cd bundle/
./run.sh        # verifies SHA256SUMS (and the GPG signature, if any) before starting
```

## Configuration

All configuration comes from `.env`, created by `./scripts/generate-env.sh` (see `.env.example`).
Services refuse to start when a required secret is missing.

| Variable                      | Description                                             |
| ----------------------------- | ------------------------------------------------------- |
| APP_PUBLIC_URL                | Public HTTPS URL of the system                          |
| DB_USER / DB_PASSWORD         | Schema owner (migrations only)                          |
| DB_APP_USER / DB_APP_PASSWORD | Least-privilege runtime role (DML only)                 |
| DB_SSLMODE                    | PostgreSQL TLS mode (`verify-full` for a remote DB)     |
| KC_DB_USER / KC_DB_PASSWORD   | Keycloak's own database account                         |
| OIDC_CLIENT_SECRET            | Secret of the app's Keycloak client                     |
| SESSION_COOKIE_KEY            | AES-256 key for the session cookie (base64)             |
| BOOTSTRAP_SUPER_ADMIN_EMAIL   | Account that becomes super admin on first login         |
| ORGANIZATION_NAME             | Shown in the security notice and output labels          |
| FRONTEND_PORT                 | Published HTTPS port                                    |

Security settings (lockout, idle timeout, password history/expiry, MFA, re-authentication)
are changed at runtime in the system admin panel and synced to Keycloak.

## Docker Commands

```bash
# Start all services (with build)
docker compose up --build

# Start in background
docker compose up -d

# Stop services
docker compose down

# Encrypted backup / restore (databases + uploaded files)
./scripts/backup.sh /mnt/backup
./scripts/restore.sh /mnt/backup/library-backup-<timestamp>.tar.enc

# View logs
docker compose logs -f app
docker compose logs -f frontend

# Access database
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

## Local Development

**Prerequisites:** Java 21, Maven 3.x, Node 20

The easiest setup is the full stack via Docker Compose (above). To run the backend or
frontend outside Docker, point them at a running PostgreSQL and Keycloak:

```bash
# backend: needs DB_*, OIDC_ISSUER_URI, OIDC_CLIENT_SECRET, SESSION_COOKIE_KEY, APP_PUBLIC_URL
mvn spring-boot:run

# frontend dev server (proxies /api and /auth)
cd frontend && npm ci && npm run dev
```

## Features

### User Management
- Roles: Super Admin, System Admin, Library Admin, Regular Member
- Single sign-on through Keycloak with MFA, captcha and account lockout
- Clearance levels (unclassified / confidential / highly confidential) per user
- Security notice with last-login information, idle lock, single session per user
- Account status management (Active, Suspended, Deleted, Pending Verification)

### Library Management
- Multiple libraries with independent admins
- Membership approval workflow (automatic or manual)

### Book Management
- Physical and digital books (PDF, EPUB, MOBI, AZW3)
- Multiple physical copies per book
- Search by title and author
- Classification level per book; downloaded PDFs and printouts carry a classification label

### Borrowing
- Physical: Request → Approve/Reject → Return
- Digital: Auto-approve or manual approval
- Due date tracking and overdue detection

## Database Schema

| Table               | Description                        |
| ------------------- | ---------------------------------- |
| users               | Accounts, roles, status            |
| libraries           | Library info and settings          |
| library_memberships | User–library relationships         |
| books               | Book metadata                      |
| book_copies         | Physical copies                    |
| digital_books       | Digital formats per book           |
| file_resources      | Uploaded file metadata             |
| borrows             | Borrow records and workflow state  |
| audit_logs          | Append-only, hash-chained security audit trail |
| security_settings   | Runtime security policy            |
| spring_session      | Server-side sessions               |

## Testing

```bash
# Run all tests
mvn test

# Keycloak extensions (captcha, conditional MFA, password change, bcrypt migration)
mvn -f keycloak/extensions test

# Coverage report (output: target/site/jacoco/index.html)
mvn clean test jacoco:report

# Dependency vulnerability scan (OWASP Dependency-Check, fails on CVSS >= 7)
mvn -Psecurity-scan verify
```

## Project Structure

```
├── Dockerfile                        # Backend image (2-stage)
├── docker-compose.yml                # All services
├── docker-compose.release.yml        # Offline deployment (pre-built images)
├── .env.example                      # Environment variable template
├── deploy/postgres/                  # DB roles and Keycloak database (init + upgrade)
├── keycloak/
│   ├── Dockerfile                    # Keycloak with extensions, built for offline start
│   ├── realm/library-realm.json      # Realm: client, password policy, brute-force, events
│   └── extensions/                   # Captcha, conditional MFA, strict password change, theme
├── scripts/
│   ├── generate-env.sh               # Create .env with random secrets
│   ├── db-upgrade.sh                 # Upgrade an existing database volume
│   ├── backup.sh / restore.sh        # Encrypted backup and restore
│   ├── export-bundle.sh              # Package for offline deployment
│   └── import-and-run.sh             # Verify and run on the target machine
├── frontend/
│   ├── Dockerfile                    # Frontend image (Node build + nginx)
│   ├── nginx/                        # TLS, security headers, rate limits, proxy
│   └── src/
├── src/main/java/com/library/
│   ├── config/                       # Security, sessions, OIDC client, validation
│   ├── security/                     # Login flow, session guard, classification guard
│   ├── keycloak/                     # Admin API client, settings sync, user migration
│   ├── audit/                        # Audit trail and reports
│   ├── labeling/                     # Output classification labels
│   ├── controller/ service/ repository/ entity/ dto/ exception/
└── src/main/resources/
    ├── application.yml
    ├── logback-spring.xml
    └── db/changelog/                 # Liquibase migrations
```
