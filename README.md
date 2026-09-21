# Internal Ticketing System - Backend Service

Spring Boot 3 REST API service for the Internal Ticketing System.

---

## 1. Backend Overview

The backend service provides RESTful APIs, data persistence, and authentication logic for the Internal Ticketing System. It connects to PostgreSQL and exposes stateless JWT-secured endpoints consumed by the React single-page frontend application.

---

## 2. Technology Stack & Specifications

- **Framework**: Spring Boot 3.4.3
- **JDK Version**: Java 17+
- **Security**: Spring Security 6 (Stateless JWT Authentication)
- **Password Hashing**: BCrypt
- **Database**: PostgreSQL 16 (via Spring Data JPA & Hibernate)
- **Database Migrations**: Flyway
- **Validation**: Jakarta Bean Validation
- **Build Tool**: Apache Maven 3.8+

---

## 3. Backend Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/ticket/system/
│   │   │   ├── config/           # SecurityConfig, WebConfig
│   │   │   ├── controller/       # AuthController, HealthController
│   │   │   ├── dto/              # Request & Response DTOs
│   │   │   ├── entity/           # User, PasswordResetToken, Role
│   │   │   ├── exception/        # AppException, GlobalExceptionHandler
│   │   │   ├── repository/       # UserRepository, PasswordResetTokenRepository
│   │   │   ├── security/         # JwtTokenProvider, JwtAuthenticationFilter, UserSecurityDetails
│   │   │   ├── service/          # AuthService, AdminSeeder
│   │   │   └── TicketingSystemApplication.java
│   │   └── resources/
│   │       ├── db/migration/     # Flyway V1__init_authentication_schema.sql
│   │       └── application.yml   # Environment properties
│   └── test/                     # Integration and Unit test suite
├── .gitignore                    # Backend-specific ignore rules
├── pom.xml                       # Maven POM
└── README.md                     # Backend documentation
```

---

## 4. PostgreSQL & Flyway Schema Migrations

Database schema modifications are managed exclusively via **Flyway migrations** (`src/main/resources/db/migration`). Hibernate DDL auto-generation is set to `ddl-auto: validate`.

### V1 Initial Migration (`V1__init_authentication_schema.sql`)
- `users`: Stores user identity, BCrypt `password_hash`, role enum (`APP_ADMIN`, `CLIENT_ADMIN`, `CLIENT_USER`), and active status flag.
- `password_reset_tokens`: Stores password reset request tokens with `token_hash` (SHA-256 hashed), expiration timestamps, and used status.

---

## 5. Environment Variables

| Variable | Description | Default / Example Value |
| :--- | :--- | :--- |
| `POSTGRES_HOST` | PostgreSQL hostname | `localhost` |
| `POSTGRES_PORT` | PostgreSQL port | `5432` |
| `POSTGRES_DB` | Database name | `ticketing_db` |
| `POSTGRES_USER` | Database username | `postgres` |
| `POSTGRES_PASSWORD` | Database password | `postgres` |
| `SERVER_PORT` | Embedded Tomcat HTTP port | `8080` |
| `FRONTEND_URL` | Allowed CORS origin | `http://localhost:5173` |
| `JWT_SECRET` | Secret key for signing JWTs | `v9y$B&E)H@MbQeThWmZq4t7w!z%C*F-JaNdRfUjXn2r5u8x/A?D(G+KbPeShVkYp` |
| `JWT_EXPIRATION_MS` | JWT validity in milliseconds | `86400000` (24 Hours) |
| `SEED_ADMIN_EMAIL` | Development admin seed email | `admin@example.com` |
| `SEED_ADMIN_PASSWORD` | Development admin seed password | `Admin@12345` |

---

## 6. Authentication & API Endpoints

### Public Endpoints (No Auth Required)
- `GET /api/health` - Basic health check.
- `POST /api/auth/login` - Authenticates user credentials and returns JWT `accessToken` & user profile.
- `POST /api/auth/forgot-password` - Generates password reset token and logs development email stub. Always returns a generic non-leaking message.
- `POST /api/auth/reset-password` - Validates token and resets user password.

### Protected Endpoints (Requires `Authorization: Bearer <token>`)
- `GET /api/auth/me` - Retrieves current authenticated user's profile.
- `PUT /api/auth/me` - Updates allowed profile fields (`firstName`, `lastName`, `mobile`, `designation`, `office`).
- `POST /api/auth/change-password` - Validates current password and sets new BCrypt password.

---

## 7. Local Testing Examples (cURL)

### 1. Login (Development Seed Admin)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "password": "Admin@12345"}'
```

### 2. Get Current Profile
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>"
```

### 3. Change Password
```bash
curl -X POST http://localhost:8080/api/auth/change-password \
  -H "Authorization: Bearer <YOUR_ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"currentPassword": "Admin@12345", "newPassword": "NewAdminPass@123"}'
```

### 4. Forgot Password
```bash
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com"}'
```
*Check backend server console output for the development email stub containing the raw token:*
```text
========== DEVELOPMENT EMAIL STUB ==========
Password reset requested for email: admin@example.com
Raw Reset Token (Dev Only): 3128e2f0-b342-4c26-833c-37112b680034-...
=============================================
```

### 5. Reset Password
```bash
curl -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token": "<RAW_RESET_TOKEN_FROM_LOGS>", "newPassword": "ResetAdminPass@123"}'
```

---

## 8. Security Rationale & Architectural Decisions

1. **BCrypt Hashing**: Passwords are never stored or logged in plaintext. Spring Security's `BCryptPasswordEncoder` is used for salted password hashing.
2. **Hashed Reset Tokens**: Raw password reset tokens are generated using cryptographically secure random UUIDs. Only SHA-256 hashes of reset tokens are stored in PostgreSQL. Raw tokens are logged ONLY in the development email stub.
3. **No Account Enumeration**: The `/forgot-password` endpoint returns the exact same generic message regardless of whether the email exists, preventing user account discovery attacks.
4. **Stateless JWT**: Sessions are stateless (`SessionCreationPolicy.STATELESS`). Identity and roles are extracted directly from verified server-side JWT signatures and user validation.
5. **Backend Authorization Ownership**: Backend enforces authorization on all non-public routes; client-side guards are strictly UI conveniences.

---

## 9. Running Tests

Run full test suite (unit and security integration tests):

```bash
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home"
mvn clean test
```
