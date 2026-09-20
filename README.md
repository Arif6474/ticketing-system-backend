# Internal Ticketing System - Backend Service

Spring Boot 3 REST API service for the Internal Ticketing System.

---

## 1. Backend Overview

The backend service provides the core RESTful APIs, data persistence layer, and business logic for the Internal Ticketing System. It connects to PostgreSQL and exposes endpoints consumed by the React single-page frontend application.

---

## 2. Technology Stack & Specifications

- **Framework**: Spring Boot 3.4.3
- **JDK Version**: Java 17+
- **Build & Dependency Management**: Apache Maven 3.8+
- **Database**: PostgreSQL 16 (via Spring Data JPA & Hibernate)
- **Validation**: Jakarta Bean Validation

---

## 3. Backend Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/ticket/system/
│   │   │   ├── config/           # Application & Web configuration
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/       # REST API Endpoints
│   │   │   │   └── HealthController.java
│   │   │   └── TicketingSystemApplication.java
│   │   └── resources/
│   │       └── application.yml   # Spring Boot environment properties
│   └── test/                     # Unit & Integration test suite
│       └── java/com/ticket/system/controller/HealthControllerTest.java
├── .gitignore                    # Backend-specific ignore rules
├── pom.xml                       # Maven POM
└── README.md                     # Backend documentation
```

> **Note on Packages**: In accordance with project architecture guidelines, no empty placeholder packages (`entity`, `repository`, `service`, `dto`, `mapper`, `exception`) are created until real domain feature requirements mandate them.

---

## 4. PostgreSQL Configuration

The backend connects to PostgreSQL using HikariCP datasource pooling. All database parameters are read dynamically from environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:ticketing_db}
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
```

---

## 5. Environment Variables Used by Backend

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `POSTGRES_HOST` | PostgreSQL hostname | `localhost` |
| `POSTGRES_PORT` | PostgreSQL port | `5432` |
| `POSTGRES_DB` | Database name | `ticketing_db` |
| `POSTGRES_USER` | Database username | `postgres` |
| `POSTGRES_PASSWORD` | Database password | `postgres` |
| `SERVER_PORT` | Embedded Tomcat HTTP port | `8080` |
| `FRONTEND_URL` | Allowed CORS origin | `http://localhost:5173` |

---

## 6. JPA / Hibernate Configuration

Hibernate auto DDL schema generation (`ddl-auto: update`) is **strictly disabled** to prevent unmanaged automatic database mutations:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
```

---

## 7. API Information & Health Check

### GET `/api/health`
Basic application and server health check endpoint.

- **URL**: `/api/health`
- **Method**: `GET`
- **Auth**: None (Public)
- **Response Headers**: `Content-Type: application/json`
- **Success Response (HTTP 200)**:
```json
{
  "status": "ok"
}
```

---

## 8. How to Run Backend

Ensure PostgreSQL is running locally via Docker Compose from the root workspace (`docker compose up -d`).

### Start Backend Application
```bash
export JAVA_HOME="/path/to/jdk-17" # Adjust path to local JDK 17+
mvn spring-boot:run
```
The server will start on `http://localhost:8080`.

---

## 9. How to Run Backend Tests

Execute the Maven unit and integration test suite:

```bash
mvn clean test
```

---

## 10. Future Flyway Migration Strategy

- Flyway is designated as the database schema migration tool for the project.
- Flyway migrations will be introduced when the first domain entities (Users, Organizations, Projects, Issues) are implemented in subsequent feature branches.
- No Flyway migration scripts are included in this foundation step because there are currently no database tables.

---

## 11. Backend Architecture Conventions

1. **CORS Security**: CORS allowed origins are resolved dynamically via `WebConfig.java` using the `cors.allowed-origins` property mapped to `FRONTEND_URL`.
2. **Minimal Layering**: Features will introduce layered packages (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `exception`) strictly on-demand.
3. **Environment Security**: No credentials or database secrets are hardcoded in source code or `application.yml`.

---

## 12. Implementation Status & Deferred Features

The backend currently implements **ONLY** the project foundation and the `GET /api/health` check endpoint.

The following features are **NOT** implemented in this foundation step:
- Authentication & Spring Security
- JWT Token verification
- User Management & RBAC
- Organization & Project management
- Modules, Issues, Kanban endpoints, Comments, and Workflows
- Cloudflare R2 file attachment storage logic
- Audit logging & verification workflows
