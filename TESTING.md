# Test Suite Documentation

## Overview

This document describes the comprehensive test suite for the Smart Library Management System. The test suite uses **TestContainers** to spin up real PostgreSQL database instances for integration testing, ensuring that tests run against actual database constraints and behaviors.

## Test Infrastructure

### BaseIntegrationTest

Located at: `src/test/java/com/library/BaseIntegrationTest.java`

**Purpose**: Base class for all integration tests that provides:
- PostgreSQL TestContainer configuration
- Spring Boot test context
- Dynamic property configuration for database connection
- Automatic container lifecycle management

**Configuration**:
```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
```

**Database**: PostgreSQL 16 container with automatic port mapping

---

## Test Categories

### 1. Repository Tests

#### UserRepositoryIntegrationTest

**File**: `src/test/java/com/library/repository/UserRepositoryIntegrationTest.java`

**Test Cases**:
- ✅ `testCreateUser` - Verify user creation with all required fields
- ✅ `testFindByEmail` - Test email-based user lookup
- ✅ `testEmailUniqueness` - Ensure email unique constraint is enforced
- ✅ `testUserExists` - Check existsByEmail repository method
- ✅ `testUpdateUser` - Verify user updates persist correctly

**Coverage**: Basic CRUD operations and unique constraints

---

### 2. Service Layer Tests

#### Authentication and session security

Login, registration, password policy, captcha and MFA are handled by Keycloak; the
application side is covered by:

- `security/SessionSecurityFilterTest` - session binding to IP/browser, idle timeout, security-notice gate, role/status reload
- `security/CsrfAndSessionEndpointTest` - CSRF on cookie-authenticated writes, `/v1/auth/session`, notice acknowledgement
- `security/RecentAuthenticationInterceptorTest` - re-authentication for sensitive operations
- `security/LibraryAuthorizationRequestResolverTest` - safe return paths, `prompt=login` / `max_age=0`
- `security/UserProvisioningServiceTest` - account linking on first login, bootstrap super admin
- `security/ClassificationAccessTest` - clearance-based access to classified books
- `session/EncryptingCookieSerializerTest` - AES-256-GCM session cookie
- `keycloak/KeycloakIntegrationLogicTest`, `keycloak/TemporaryPasswordTest` - settings sync, user migration, temporary passwords
- `audit/AuditServiceTest`, `audit/AuditReportTest` - hash chain, reports, labeled CSV export
- `settings/SecuritySettingsTest`, `validation/InputValidationTest`, `labeling/PdfLabelStamperTest`
- Keycloak extensions: `mvn -f keycloak/extensions test` (captcha, conditional MFA, strict password change, bcrypt)

---

#### LibraryServiceIntegrationTest

**File**: `src/test/java/com/library/service/LibraryServiceIntegrationTest.java`

**Test Cases**:
- ✅ `testCreateLibrary` - Library creation with owner auto-enrollment as ADMIN
- ✅ `testGetLibraryById` - Retrieve library by ID
- ✅ `testGetLibraryByIdNotFound` - Handle non-existent library lookup
- ✅ `testUpdateLibrary` - Update library settings
- ✅ `testUpdateLibraryUnauthorized` - Prevent unauthorized updates
- ✅ `testRequestMembership_AutoApprovalDisabled` - Manual membership approval workflow
- ✅ `testRequestMembership_AutoApprovalEnabled` - Auto-approval when enabled
- ✅ `testRequestMembershipDuplicate` - Prevent duplicate membership requests
- ✅ `testApproveMembership` - Admin approves pending membership
- ✅ `testRejectMembership` - Admin rejects membership with reason
- ✅ `testGetUserLibraries` - List all libraries for a user
- ✅ `testGetAllActiveLibraries` - List all active libraries
- ✅ `testDeleteLibrary` - Soft or hard delete library

**Coverage**: Library CRUD, membership workflows, authorization

---

#### BookServiceIntegrationTest

**File**: `src/test/java/com/library/service/BookServiceIntegrationTest.java`

**Test Cases**:
- ✅ `testCreateBook` - Create book with library membership check
- ✅ `testGetBookById` - Retrieve book by ID
- ✅ `testGetBookByIdNotFound` - Handle non-existent book
- ✅ `testGetLibraryBooks` - List all books in a library
- ✅ `testSearchBooksByTitle` - Search books by title (case-insensitive)
- ✅ `testSearchBooksByAuthor` - Search books by author
- ✅ `testSearchBooksNoResults` - Handle empty search results
- ✅ `testUpdateBook` - Update book details
- ✅ `testDeleteBook` - Delete book
- ✅ `testAddBookCopies` - Add physical copies to a book
- ✅ `testAddBookCopiesWithExistingCopies` - Sequential copy number assignment
- ✅ `testGetAvailableCopiesCount` - Count available vs total copies
- ✅ `testGetAvailableCopiesCountAfterBorrow` - Available count decreases after borrow

**Coverage**: Book CRUD, search, copy management, availability tracking

---

#### BorrowServiceIntegrationTest

**File**: `src/test/java/com/library/service/BorrowServiceIntegrationTest.java`

**Test Cases**:

**Physical Borrowing**:
- ✅ `testCreatePhysicalBorrowRequest` - Request physical book borrow
- ✅ `testCreatePhysicalBorrowRequestNoCopyAvailable` - Reject when no copies available
- ✅ `testCreatePhysicalBorrowRequestDuplicate` - Prevent duplicate active borrows

**Digital Borrowing**:
- ✅ `testCreateDigitalBorrowRequestAutoApprove` - Auto-approve digital borrow when enabled
- ✅ `testCreateDigitalBorrowRequestNoAutoApprove` - Require approval when auto-approve disabled
- ✅ `testCreateMultipleDigitalBorrows` - Allow multiple format borrows simultaneously

**Borrow Workflow**:
- ✅ `testApproveBorrowRequest` - Admin approves borrow, copy status → BORROWED
- ✅ `testRejectBorrowRequest` - Admin rejects with reason, copy stays AVAILABLE
- ✅ `testReturnBook` - Return book, copy status → AVAILABLE

**Query Operations**:
- ✅ `testCalculateOverdue` - Overdue calculation based on dueDate
- ✅ `testGetUserBorrows` - List all borrows for a user
- ✅ `testGetPendingBorrowsAdminOnly` - Admins can view pending requests
- ✅ `testBorrowNotFoundThrowsException` - Handle non-existent borrow

**Coverage**: Complete borrow lifecycle, state transitions, business rules

---

### 3. Controller Integration Tests

#### LibraryControllerIntegrationTest

**File**: `src/test/java/com/library/controller/LibraryControllerIntegrationTest.java`

**Test Cases**:
- ✅ `testCreateLibrary` - POST /v1/libraries
- ✅ `testGetLibraryById` - GET /v1/libraries/{id}
- ✅ `testGetLibraryNotFound` - 404 Not Found handling
- ✅ `testUpdateLibrary` - PUT /v1/libraries/{id}
- ✅ `testDeleteLibrary` - DELETE /v1/libraries/{id}
- ✅ `testGetUserLibraries` - GET /v1/libraries/user
- ✅ `testGetAllActiveLibraries` - GET /v1/libraries
- ✅ `testRequestMembership` - POST /v1/libraries/{id}/membership
- ✅ `testRequestMembershipDuplicate` - 400 Bad Request for duplicate
- ✅ `testApproveMembership` - POST /v1/libraries/{id}/membership/{userId}/approve
- ✅ `testRejectMembership` - POST /v1/libraries/{id}/membership/{userId}/reject

**Coverage**: Library API endpoints, REST conventions, error responses

---

#### BookControllerIntegrationTest

**File**: `src/test/java/com/library/controller/BookControllerIntegrationTest.java`

**Test Cases**:
- ✅ `testCreateBook` - POST /v1/libraries/{libraryId}/books
- ✅ `testGetBookById` - GET /v1/libraries/{libraryId}/books/{bookId}
- ✅ `testGetBookNotFound` - 404 Not Found handling
- ✅ `testGetLibraryBooks` - GET /v1/libraries/{libraryId}/books
- ✅ `testSearchBooksByTitle` - GET /v1/libraries/{libraryId}/books/search?title=...
- ✅ `testSearchBooksByAuthor` - GET /v1/libraries/{libraryId}/books/search?author=...
- ✅ `testUpdateBook` - PUT /v1/libraries/{libraryId}/books/{bookId}
- ✅ `testDeleteBook` - DELETE /v1/libraries/{libraryId}/books/{bookId}
- ✅ `testAddBookCopies` - POST /v1/libraries/{libraryId}/books/{bookId}/copies

**Coverage**: Book API endpoints, search functionality, copy management

---

#### BorrowControllerIntegrationTest

**File**: `src/test/java/com/library/controller/BorrowControllerIntegrationTest.java`

**Test Cases**:
- ✅ `testCreatePhysicalBorrowRequest` - POST /v1/libraries/{libraryId}/borrows/{bookId}
- ✅ `testCreateDigitalBorrowRequest` - Digital borrow with auto-approval
- ✅ `testGetUserBorrows` - GET /v1/libraries/{libraryId}/borrows
- ✅ `testGetPendingBorrows` - GET /v1/libraries/{libraryId}/borrows/pending
- ✅ `testApproveBorrowRequest` - POST /v1/libraries/{libraryId}/borrows/{borrowId}/approve
- ✅ `testRejectBorrowRequest` - POST /v1/libraries/{libraryId}/borrows/{borrowId}/reject
- ✅ `testReturnBook` - POST /v1/libraries/{libraryId}/borrows/{borrowId}/return

**Coverage**: Borrowing workflow endpoints, state transitions, user authentication

---

## Running Tests

### Prerequisites

- Java 21
- Maven 3.6+
- Docker (for TestContainers)

### Run All Tests

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=BorrowServiceIntegrationTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=BorrowServiceIntegrationTest#testCreatePhysicalBorrowRequest
```

### Run with Coverage Report

```bash
mvn clean test jacoco:report
```

Coverage report will be generated at: `target/site/jacoco/index.html`

---

## Test Configuration

### application-test.yml

Tests use the `test` profile with TestContainers-managed PostgreSQL:

```yaml
spring:
  datasource:
    # Dynamically configured by TestContainers
    url: ${TESTCONTAINER_POSTGRESQL_URL}
    username: ${TESTCONTAINER_POSTGRESQL_USERNAME}
    password: ${TESTCONTAINER_POSTGRESQL_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
```

---

## Test Data Management

### Database State

- Each test class uses `@BeforeEach` to set up fresh test data
- Repositories cleared before each test to ensure isolation
- TestContainers provides clean database per test run

### Test Users

- **Admin User**: `admin@example.com` - System admin with all permissions
- **Member User**: `member@example.com` - Regular library member
- **Passwords**: Hashed using BCrypt (test values use mock hashes)

---

## Coverage Goals

### Current Coverage

| Layer      | Test Count    | Coverage Goal |
| ---------- | ------------- | ------------- |
| Repository | 5 tests       | 100%          |
| Service    | 57 tests      | 80%           |
| Controller | 38 tests      | 75%           |
| **Total**  | **100 tests** | **75%+**      |

### Key Business Logic Covered

✅ Session security, CSRF, re-authentication and classification access  
✅ Library ownership and membership workflows  
✅ Book search and copy availability tracking  
✅ Physical vs digital borrowing rules  
✅ Borrow approval/rejection workflows  
✅ Overdue calculation  
✅ Status transitions (REQUESTED → APPROVED → RETURNED)  
✅ Duplicate prevention (email, membership, active borrows)  
✅ Authorization checks (admin-only operations)  

---

## Test Patterns

### Integration Test Pattern

```java
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
public class MyControllerIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/v1/endpoint"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)));
    }
}
```

### Service Test Pattern

```java
@SpringBootTest
public class MyServiceIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private MyService myService;
    
    @Autowired
    private MyRepository myRepository;
    
    @BeforeEach
    void setUp() {
        myRepository.deleteAll();
        // Setup test data
    }
    
    @Test
    void testServiceMethod() {
        // Arrange
        // Act
        // Assert
    }
}
```

---

## Continuous Integration

### GitHub Actions (Recommended)

```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - run: mvn clean test
```

---

## Future Test Enhancements

### Planned Additions

- [ ] Unit tests for utility classes (SecurityUtils, etc.)
- [ ] End-to-end tests for complete user workflows
- [ ] Performance tests for search operations
- [ ] Security tests for authorization edge cases
- [ ] Mutation testing with PIT
- [ ] Contract testing for API endpoints

### Test Data Builders

Consider adding test data builders for complex entities:

```java
public class BookTestBuilder {
    public static Book.BookBuilder aBook() {
        return Book.builder()
            .title("Test Book")
            .author("Test Author")
            // ... default values
    }
}
```

---

## Troubleshooting

### Docker Not Running

**Error**: `Could not start container`

**Solution**: Ensure Docker daemon is running
```bash
sudo systemctl start docker
```

### Port Conflicts

**Error**: `Port already in use`

**Solution**: TestContainers uses dynamic ports, check for conflicting containers
```bash
docker ps
docker stop $(docker ps -q)
```

### Out of Memory

**Error**: `Java heap space`

**Solution**: Increase Maven memory
```bash
export MAVEN_OPTS="-Xmx2g"
```

---

## Summary

This test suite provides **comprehensive coverage** of the Smart Library Management System's critical functionality. All tests use **real database instances** via TestContainers, ensuring that:

1. Database constraints are properly enforced
2. JPA mappings work correctly
3. Transactions behave as expected
4. Integration between layers functions properly

The suite targets **75%+ coverage** on the service layer where most business logic resides, with additional coverage on controllers and repositories to ensure end-to-end correctness.
