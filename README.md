# Internal Ticketing System - Backend Service

Spring Boot 3 REST API service for the Internal Ticketing System.

---

## 1. Backend Overview

The backend service provides RESTful APIs, data persistence, organization scoping, user management, and multi-tenant Role-Based Access Control (RBAC) for the Internal Ticketing System. It connects to PostgreSQL and exposes stateless JWT-secured endpoints consumed by the React single-page frontend application.

---

## 2. Technology Stack & Specifications

- **Framework**: Spring Boot 3.4.3
- **JDK Version**: Java 17+
- **Security**: Spring Security 6 (Stateless JWT Authentication & Method Security)
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
│   │   │   ├── controller/       # AuthController, OrganizationController, UserController, HealthController
│   │   │   ├── dto/              # Request & Response DTOs (Organization, User, Auth)
│   │   │   ├── entity/           # User, ClientOrganization, PasswordResetToken, Role
│   │   │   ├── exception/        # AppException, GlobalExceptionHandler
│   │   │   ├── repository/       # UserRepository, ClientOrganizationRepository, UserSpecification
│   │   │   ├── security/         # JwtTokenProvider, JwtAuthenticationFilter, UserSecurityDetails
│   │   │   ├── service/          # AuthService, UserService, OrganizationService, AdminSeeder
│   │   │   └── TicketingSystemApplication.java
│   │   └── resources/
│   │       ├── db/migration/     # Flyway V1 & V2 migration scripts
│   │       └── application.yml   # Environment properties
│   └── test/                     # Security, Auth, Org & User Management Integration Test suite
├── .gitignore                    # Backend-specific ignore rules
├── pom.xml                       # Maven POM
└── README.md                     # Backend documentation
```

---

## 4. PostgreSQL & Flyway Schema Migrations

Database schema modifications are managed exclusively via **Flyway migrations** (`src/main/resources/db/migration`). Hibernate DDL auto-generation is set to `ddl-auto: validate`.

### Schema Version History
- **V1 (`V1__init_authentication_schema.sql`)**: Initial schema for `users` and `password_reset_tokens`.
- **V2 (`V2__init_organization_and_user_management.sql`)**: Creates `client_organizations` table and adds `organization_id` foreign key with indexes on `users(organization_id)`, `users(role)`, `users(is_active)`, and `client_organizations(code)`.

---

## 5. Organization Model & Organization-Scoped Users

### Client Organization Entity
`ClientOrganization` contains:
- `id` (UUID PRIMARY KEY)
- `name` (String, required)
- `code` (String, required, unique, normalized to uppercase)
- `description` (Text, optional)
- `isActive` (Boolean, default `true`)

### Role & Organization Relationship Rules
- **`APP_ADMIN`**: Application-level super administrator. `organization` MUST be `null`. Operates across all client organizations.
- **`CLIENT_ADMIN`**: Administrator for a single client tenant. MUST belong to exactly one active `ClientOrganization`.
- **`CLIENT_USER`**: Standard end-user. MUST belong to exactly one active `ClientOrganization`.

### Organization Deactivation Policy
When an organization is deactivated (`isActive = false`):
- All belonging client users (`CLIENT_ADMIN` and `CLIENT_USER`) are immediately blocked from authenticating or executing protected API requests.
- No user records are deleted, maintaining full database referential integrity.

---

## 6. Server-Side Role-Based Access Control (RBAC) Matrix

| Action | APP_ADMIN | CLIENT_ADMIN | CLIENT_USER |
|---|---|---|---|
| **List / Search Users** | All Organizations | Own Organization Only | Forbidden (403) |
| **View User Details** | Any User | Own Organization Only | Forbidden (403) |
| **Create APP_ADMIN** | Yes (Org must be null) | Forbidden (403) | Forbidden (403) |
| **Create CLIENT_ADMIN / USER** | Yes (Must select Org) | Yes (Forced to own Org) | Forbidden (403) |
| **Edit APP_ADMIN** | Yes | Forbidden (403) | Forbidden (403) |
| **Edit CLIENT_ADMIN / USER** | Yes | Yes (Own Org only) | Forbidden (403) |
| **Deactivate User** | Yes | Yes (Own Org only, not APP_ADMIN) | Forbidden (403) |
| **Hard Delete User** | Yes | Forbidden (403) | Forbidden (403) |
| **Force Password Reset** | Yes (Any User) | Yes (Own Org, not APP_ADMIN) | Forbidden (403) |
| **Manage Organizations** | Full CRUD | View Own Org Only | Forbidden (403) |

---

## 7. API Endpoint Summary

### Public Endpoints
- `GET /api/health` - Basic health check status.
- `POST /api/auth/login` - Authenticates credentials, returns JWT & user profile.
- `POST /api/auth/forgot-password` - Requests password recovery email token.
- `POST /api/auth/reset-password` - Resets password using raw reset token.

### Authentication Profile Endpoints (`Authorization: Bearer <token>`)
- `GET /api/auth/me` - Retrieves authenticated user profile.
- `PUT /api/auth/me` - Updates profile details (`firstName`, `lastName`, `mobile`, `designation`, `office`).
- `POST /api/auth/change-password` - Changes password after validating current password.

### Organization Management Endpoints (`/api/organizations`)
- `POST /api/organizations` - (`APP_ADMIN`) Creates new client organization.
- `GET /api/organizations` - (`APP_ADMIN`) Returns paginated list of organizations.
- `GET /api/organizations/{id}` - Returns organization details (Scoped for `CLIENT_ADMIN` / `CLIENT_USER`).
- `PUT /api/organizations/{id}` - (`APP_ADMIN`) Updates organization metadata and `isActive` status.

### User Management Endpoints (`/api/users`)
- `GET /api/users` - Paginated user search (`page`, `size`, `search`, `role`, `active`, `organizationId`). Database-level query filtering & pagination.
- `GET /api/users/{id}` - Returns user details by ID.
- `POST /api/users` - Creates new user adhering to RBAC matrix.
- `PUT /api/users/{id}` - Updates user profile, role, active status, or organization assignment.
- `PATCH /api/users/{id}/deactivate` - Deactivates user account (`isActive = false`).
- `DELETE /api/users/{id}` - (`APP_ADMIN` only) Hard-deletes user account.
- `POST /api/users/{id}/force-password-reset` - Administrative force password reset with BCrypt hashing.

---

## 8. Hard-Delete Protection & Future Issue Dependency

> [!IMPORTANT]
> **Hard Delete Safeguard**: Only `APP_ADMIN` can execute `DELETE /api/users/{id}`.
> Users referenced by system entities cannot be hard deleted. Database referential integrity (`ON DELETE RESTRICT`) and service guards are structured to prevent hard deletion of users who own or are assigned to tickets once the `Issue` entity is added in upcoming steps.

---

## 9. Seed & Demo Accounts (Development Profile)

On application startup, `AdminSeeder` automatically initializes standard demo entities if missing:

| Email | Password | Role | Organization |
| :--- | :--- | :--- | :--- |
| `admin@example.com` | `Admin@12345` | `APP_ADMIN` | *None* (`null`) |
| `clientadmin@acme.com` | `ClientAdmin@12345` | `CLIENT_ADMIN` | `Acme Corporation` (`ACME`) |
| `clientuser@acme.com` | `ClientUser@12345` | `CLIENT_USER` | `Acme Corporation` (`ACME`) |

---

## 10. Running Tests

Run full unit and security integration test suite:

```bash
mvn clean test -Dnet.bytebuddy.experimental=true
```
