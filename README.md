# Smart Library Management System

A comprehensive Java-based library management system with multi-role users, digital and physical book borrowing, and complete REST API documentation.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Project Structure](#project-structure)
- [Development](#development)
- [Testing](#testing)

## ✨ Features

### User Management
- Three-tier user roles: System Admin, Library Admin, Regular Member
- JWT-based authentication and authorization
- User registration and login with email verification
- Account status management (Active, Suspended, Deleted, Pending Verification)

### Library Management
- Create and manage multiple libraries
- Library-specific admin roles
- Membership approval workflow (automatic or manual)
- Auto-approve or manual-approve membership requests

### Book Management
- Add books with metadata (title, author, publisher, etc.)
- Manage physical book copies
- Support multiple digital book formats (PDF, EPUB, MOBI, AZW3)
- Book search by title and author
- Cover image support

### Borrowing System
- **Physical Books**: Request → Approve/Reject → Return workflow
- **Digital Books**: Auto-approve or manual approval based on book settings
- Support simultaneous borrowing of multiple digital formats
- One physical copy per user per book (at a time)
- Due date tracking and overdue detection
- Borrowing history and statistics

### Advanced Features
- Comprehensive audit logging
- Role-based access control (RBAC)
- Library-level permissions
- File upload with checksum verification
- Pagination and search capabilities
- Error handling with meaningful messages

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    REST API Layer                        │
│             (Controllers & OpenAPI/Swagger)              │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│                Service Layer (Business Logic)            │
│  (Authentication, Authorization, Workflow Management)    │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│         Repository Layer (Data Access - JPA)             │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│         PostgreSQL Database with Liquibase               │
│              (Schema Migrations)                         │
└─────────────────────────────────────────────────────────┘
```

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Framework** | Spring Boot | 3.2.1 |
| **Language** | Java | 21 |
| **Database** | PostgreSQL | 16 |
| **ORM** | Spring Data JPA + Hibernate | Latest |
| **Authentication** | JWT + Spring Security | 6.x |
| **API Documentation** | Springdoc-OpenAPI/Swagger UI | 2.1.0 |
| **Migrations** | Liquibase | Latest |
| **Build Tool** | Maven | 3.x |
| **Containerization** | Docker & Docker Compose | Latest |
| **Testing** | JUnit 5 + Mockito + TestContainers | Latest |

## 🚀 Getting Started

### Prerequisites

- Docker and Docker Compose installed
- Java 21 JDK (for local development)
- Maven 3.x (for local development)

### Quick Start with Docker

1. **Clone the repository:**
```bash
git clone https://github.com/mhhasani/Library.git
cd Library
```

2. **Build and run with Docker Compose:**
```bash
docker-compose up --build
```

The application will start on `http://localhost:8080`

3. **Access Swagger UI:**
```
http://localhost:8080/api/swagger-ui.html
```

### Local Development Setup

1. **Install dependencies:**
```bash
./mvnw clean install
```

2. **Start PostgreSQL:**
```bash
docker run -d \
  --name library_db \
  -e POSTGRES_DB=library_db \
  -e POSTGRES_USER=libraryuser \
  -e POSTGRES_PASSWORD=librarypass \
  -p 5432:5432 \
  postgres:16-alpine
```

3. **Run the application:**
```bash
./mvnw spring-boot:run
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication Endpoints (`/v1/auth`)

#### Register
```http
POST /v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890"
}
```

#### Login
```http
POST /v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "userId": 1,
    "email": "user@example.com",
    "systemRole": "USER",
    "tokenType": "Bearer"
  }
}
```

### Library Endpoints (`/v1/libraries`)

#### Create Library (Admin)
```http
POST /v1/libraries
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "City Public Library",
  "description": "A comprehensive public library",
  "autoMembershipApproval": false,
  "defaultBorrowDurationDays": 14
}
```

#### Get User Libraries
```http
GET /v1/libraries
Authorization: Bearer <token>
```

#### Request Membership
```http
POST /v1/libraries/{libraryId}/membership/request
Authorization: Bearer <token>
```

#### Approve Membership (Admin Only)
```http
POST /v1/libraries/{libraryId}/membership/{userId}/approve
Authorization: Bearer <token>
```

### Book Endpoints (`/v1/libraries/{libraryId}/books`)

#### Create Book (Admin)
```http
POST /v1/libraries/{libraryId}/books
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "publisher": "Prentice Hall",
  "publicationYear": 2008,
  "description": "A Handbook of Agile Software Craftsmanship",
  "autoDigitalBorrowEnabled": true
}
```

#### Get Books
```http
GET /v1/libraries/{libraryId}/books
Authorization: Bearer <token>
```

#### Search Books
```http
GET /v1/libraries/{libraryId}/books/search?query=clean
Authorization: Bearer <token>
```

#### Add Physical Copies (Admin)
```http
POST /v1/libraries/{libraryId}/books/{bookId}/copies
Authorization: Bearer <token>
Content-Type: application/json

{
  "numberOfCopies": 5
}
```

### Borrowing Endpoints (`/v1/libraries/{libraryId}/borrows`)

#### Request to Borrow
```http
POST /v1/libraries/{libraryId}/borrows/{bookId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "borrowType": "PHYSICAL",
  "bookCopyId": 1
}
```

Or for digital:
```json
{
  "borrowType": "DIGITAL",
  "digitalBookId": 1
}
```

#### Approve Borrow (Admin)
```http
POST /v1/libraries/{libraryId}/borrows/{borrowId}/approve
Authorization: Bearer <token>
```

#### Return Book
```http
POST /v1/libraries/{libraryId}/borrows/{borrowId}/return
Authorization: Bearer <token>
```

#### Get User Borrows
```http
GET /v1/libraries/{libraryId}/borrows
Authorization: Bearer <token>
```

#### Get Pending Requests (Admin)
```http
GET /v1/libraries/{libraryId}/borrows/pending
Authorization: Bearer <token>
```

## 💾 Database Schema

The system uses 10 main tables:

1. **users** - User accounts with roles and status
2. **libraries** - Library information and settings
3. **library_memberships** - User-library relationships
4. **books** - Book metadata
5. **book_copies** - Physical book copies
6. **digital_books** - Digital versions (multiple formats per book)
7. **file_resources** - File storage metadata
8. **borrows** - Borrowing records and workflow
9. **audit_logs** - Activity tracking (JSONB)
10. **Enums** - System role, membership status, book status, etc.

### Key Relationships

- Users own and manage Libraries
- Users request Membership in Libraries
- Libraries contain Books
- Books have multiple physical Copies and/or digital versions
- Users can Borrow either physical or digital versions
- All actions are logged in Audit Logs

## 📂 Project Structure

```
library-management-system/
├── pom.xml                                    # Maven dependencies
├── Dockerfile                                 # Application container
├── docker-compose.yml                         # Multi-container setup
├── .gitignore
├── README.md
│
├── src/main/
│   ├── java/com/library/
│   │   ├── LibraryManagementApplication.java  # Main entry point
│   │   ├── config/
│   │   │   └── SecurityConfig.java            # Spring Security configuration
│   │   ├── controller/
│   │   │   ├── AuthenticationController.java
│   │   │   ├── LibraryController.java
│   │   │   ├── BookController.java
│   │   │   └── BorrowController.java
│   │   ├── service/
│   │   │   ├── AuthenticationService.java
│   │   │   ├── LibraryService.java
│   │   │   ├── BookService.java
│   │   │   └── BorrowService.java
│   │   ├── repository/                        # Spring Data JPA repositories
│   │   │   ├── UserRepository.java
│   │   │   ├── LibraryRepository.java
│   │   │   ├── BookRepository.java
│   │   │   └── ... (more repositories)
│   │   ├── entity/                            # JPA entities
│   │   │   ├── User.java
│   │   │   ├── Library.java
│   │   │   ├── Book.java
│   │   │   └── ... (more entities)
│   │   ├── dto/                               # Data Transfer Objects
│   │   │   ├── UserDTO.java
│   │   │   ├── BookDTO.java
│   │   │   └── ... (more DTOs)
│   │   ├── security/
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── CustomUserDetailsService.java
│   │   ├── exception/
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── BadRequestException.java
│   │   │   ├── UnauthorizedException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   └── util/
│   │       └── SecurityUtils.java
│   │
│   └── resources/
│       ├── application.yml                    # Application configuration
│       └── db/changelog/                      # Liquibase migrations
│           ├── db.changelog-master.xml
│           └── v1/
│               ├── 01-create-enums.xml
│               ├── 02-create-users-table.xml
│               └── ... (more migration files)
│
└── src/test/
    └── java/com/library/
        ├── service/                           # Service tests
        ├── controller/                        # API tests
        └── repository/                        # Repository tests
```

## 🔧 Development

### Building the Project

```bash
# Clean and build
./mvnw clean package

# Build without tests
./mvnw clean package -DskipTests

# Build with coverage
./mvnw clean test jacoco:report
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UserServiceTest

# Run with coverage report
./mvnw clean test jacoco:report
```

### Code Style

The project uses:
- Lombok for reducing boilerplate
- Spring Code Style conventions
- Clean Architecture principles

### Database Migrations

Liquibase is used for database versioning:

```bash
# Check migration status
./mvnw liquibase:status

# Rollback last migration
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

## 🧪 Testing

Phase 4 includes comprehensive test coverage:

- **Unit Tests**: Service and utility layer logic
- **Integration Tests**: Database and API endpoint testing
- **E2E Tests**: Complete user workflow scenarios
- **Target Coverage**: 75%+ on service layer

Tests use:
- **JUnit 5**: Modern testing framework
- **Mockito**: Mocking dependencies
- **TestContainers**: Real PostgreSQL in tests
- **RestAssured**: API endpoint testing

### Run All Tests

```bash
./mvnw test
```

### Generate Test Report

```bash
./mvnw clean test jacoco:report
# Report available at: target/site/jacoco/index.html
```

## 🔐 Security Features

- **JWT Authentication**: Stateless token-based auth
- **Role-Based Access Control (RBAC)**: Three-tier user system
- **Spring Security**: Framework-level security
- **Password Hashing**: BCrypt encoding
- **Resource Authorization**: User can only access their own data
- **Library-Level Permissions**: Admins control their libraries
- **Audit Logging**: All actions tracked

## 📝 Environment Variables

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=library_db
DB_USER=libraryuser
DB_PASSWORD=librarypass

# JWT Configuration
JWT_SECRET=your-secret-key-change-this-in-production

# Application
SERVER_PORT=8080
```

## 🐳 Docker Commands

```bash
# Build containers
docker-compose build

# Start services
docker-compose up

# Start in background
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f app

# Access database
docker exec -it library_db psql -U libraryuser -d library_db
```

## 📊 Monitoring & Logs

Application logs are available at:
- Console output
- Docker logs: `docker logs -f library_app`
- File logs (if configured in application.yml)

## 🚀 Deployment

### Requirements for Production

1. Secure JWT secret (minimum 32 characters)
2. HTTPS/TLS configuration
3. Database backups
4. Reverse proxy (nginx/Traefik)
5. Monitoring and alerting
6. Log aggregation
7. Database pooling configuration

### Docker Production Build

```bash
docker build -t library-app:1.0.0 .
docker tag library-app:1.0.0 your-registry/library-app:1.0.0
docker push your-registry/library-app:1.0.0
```

## 📈 Future Enhancements

- [ ] Integration with AWS S3 for file storage
- [ ] Email notifications for approvals
- [ ] Fine calculation for overdue books
- [ ] Reservation system for popular books
- [ ] Analytics dashboard
- [ ] Mobile application
- [ ] Multi-language support
- [ ] Advanced search with filters
- [ ] Book ratings and reviews
- [ ] Notifications system

## 🤝 Contributing

1. Create a feature branch (`git checkout -b feature/amazing-feature`)
2. Commit changes (`git commit -m 'Add amazing feature'`)
3. Push to branch (`git push origin feature/amazing-feature`)
4. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 📞 Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Email: support@library-system.com

---

**Last Updated**: February 2026
**Version**: 1.0.0
