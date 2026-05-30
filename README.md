# Library Management System

A Java-based library management system with multi-role users, digital and physical book borrowing, and a React frontend.

## Technology Stack

| Component          | Technology                   | Version |
| ------------------ | ---------------------------- | ------- |
| Framework          | Spring Boot                  | 3.2.1   |
| Language           | Java                         | 21      |
| Database           | PostgreSQL                   | 16      |
| ORM                | Spring Data JPA + Hibernate  | —       |
| Authentication     | JWT + Spring Security        | 6.x     |
| API Documentation  | Springdoc OpenAPI/Swagger UI | 2.1.0   |
| Migrations         | Liquibase                    | —       |
| Frontend           | React                        | 18      |
| Web Server         | nginx                        | —       |
| Containerization   | Docker & Docker Compose      | —       |

## Architecture

```
Browser
  └── nginx (port 3000)
        ├── /          → React SPA (static files)
        └── /api/*     → Spring Boot (port 8080)
                            └── PostgreSQL (port 5432)
```

## Getting Started

**Prerequisite:** Docker

```bash
docker compose up --build
```

| Service  | URL                                        |
| -------- | ------------------------------------------ |
| Frontend | http://localhost:3000                      |
| API      | http://localhost:8080/api                  |
| Swagger  | http://localhost:8080/api/swagger-ui.html  |

## Offline Deployment

For environments without internet access, use the export/import scripts.

**On a machine with internet (build once):**
```bash
./scripts/export-bundle.sh
tar -czf library-bundle.tar.gz bundle/
```

**On the target machine (Docker only required):**
```bash
tar -xzf library-bundle.tar.gz
cd bundle/
./run.sh
```

## Configuration

Copy `.env.example` to `.env` and set your values:

```bash
cp .env.example .env
```

| Variable       | Default                              | Description             |
| -------------- | ------------------------------------ | ----------------------- |
| DB_NAME        | library_db                           | PostgreSQL database name |
| DB_USER        | libraryuser                          | PostgreSQL user         |
| DB_PASSWORD    | —                                    | PostgreSQL password     |
| JWT_SECRET     | —                                    | Min 32 characters       |
| STORAGE_PATH   | /data/library-files                  | File upload storage     |
| APP_PORT       | 8080                                 | Backend port            |
| FRONTEND_PORT  | 3000                                 | Frontend port           |

Without a `.env` file, insecure defaults are used — only suitable for local development.

## Docker Commands

```bash
# Start all services (with build)
docker compose up --build

# Start in background
docker compose up -d

# Stop services
docker compose down

# Stop and remove volumes (wipes database)
docker compose down -v

# View logs
docker compose logs -f app
docker compose logs -f frontend

# Access database
docker exec -it library_db psql -U libraryuser -d library_db
```

## Local Development (without Docker)

**Prerequisites:** Java 21, Maven 3.x, Node 20

Start PostgreSQL:
```bash
docker run -d --name library_db \
  -e POSTGRES_DB=library_db \
  -e POSTGRES_USER=libraryuser \
  -e POSTGRES_PASSWORD=librarypass \
  -p 5432:5432 postgres:16-alpine
```

Run backend:
```bash
./mvnw spring-boot:run
```

Run frontend:
```bash
cd frontend && npm install && npm start
```

## Features

### User Management
- Three-tier roles: System Admin, Library Admin, Regular Member
- JWT-based stateless authentication
- Account status management (Active, Suspended, Deleted, Pending Verification)

### Library Management
- Multiple libraries with independent admins
- Membership approval workflow (automatic or manual)

### Book Management
- Physical and digital books (PDF, EPUB, MOBI, AZW3)
- Multiple physical copies per book
- Search by title and author

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
| audit_logs          | Activity log (JSONB)               |

## Testing

```bash
# Run all tests
./mvnw test

# Run with coverage report (output: target/site/jacoco/index.html)
./mvnw clean test jacoco:report
```

Tests use JUnit 5, Mockito, TestContainers (real PostgreSQL), and RestAssured.

## Project Structure

```
├── Dockerfile                        # Backend image (2-stage)
├── docker-compose.yml                # All services
├── docker-compose.release.yml        # Offline deployment (pre-built images)
├── .env.example                      # Environment variable template
├── scripts/
│   ├── export-bundle.sh              # Package for offline deployment
│   └── import-and-run.sh             # Run on target machine
├── frontend/
│   ├── Dockerfile                    # Frontend image (Node build + nginx)
│   ├── nginx.conf                    # Proxy /api to backend, SPA routing
│   └── src/
├── src/main/java/com/library/
│   ├── config/SecurityConfig.java
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   ├── security/
│   └── exception/
└── src/main/resources/
    ├── application.yml
    └── db/changelog/                 # Liquibase migrations
```
