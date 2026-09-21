# Internal Ticketing System - Backend Service

Spring Boot 3 REST API service for the Internal Ticketing System.

---

## 1. Backend Overview

The backend service provides RESTful APIs, data persistence, organization scoping, user management, project management, project memberships, module management, issue management, and multi-tenant Role-Based Access Control (RBAC) for the Internal Ticketing System. It connects to PostgreSQL and exposes stateless JWT-secured endpoints consumed by the React single-page frontend application.

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
│   │   │   ├── controller/       # AuthController, OrganizationController, UserController, ProjectController, ProjectMembershipController, ModuleController, IssueController, HealthController
│   │   │   ├── dto/              # Request & Response DTOs (Issue, Module, Project, Membership, Organization, User, Auth)
│   │   │   ├── entity/           # Issue, IssueType, IssuePriority, IssueStage, VerificationStatus, Module, Project, ProjectMembership, User, ClientOrganization, PasswordResetToken, Role
│   │   │   ├── exception/        # AppException, GlobalExceptionHandler
│   │   │   ├── repository/       # IssueRepository, IssueSpecification, ModuleRepository, ModuleSpecification, ProjectRepository, ProjectMembershipRepository, ProjectSpecification, UserRepository, ClientOrganizationRepository
│   │   │   ├── security/         # JwtTokenProvider, JwtAuthenticationFilter, UserSecurityDetails
│   │   │   ├── service/          # IssueService, ModuleService, ProjectService, ProjectMembershipService, AuthService, UserService, OrganizationService, AdminSeeder
│   │   │   └── TicketingSystemApplication.java
│   │   └── resources/
│   │       ├── db/migration/     # Flyway V1, V2, V3, V4 & V5 migration scripts
│   │       └── application.yml   # Environment properties
│   └── test/                     # Security, Auth, Org, User, Project, Module & Issue Integration Test suite
├── .gitignore                    # Backend-specific ignore rules
├── pom.xml                       # Maven POM
└── README.md                     # Backend documentation
```

---

## 4. PostgreSQL & Flyway Schema Migrations

Database schema modifications are managed exclusively via **Flyway migrations** (`src/main/resources/db/migration`). Hibernate DDL auto-generation is set to `ddl-auto: validate`.

### Schema Version History
- **V1 (`V1__init_authentication_schema.sql`)**: Initial schema for `users` and `password_reset_tokens`.
- **V2 (`V2__init_organization_and_user_management.sql`)**: Creates `client_organizations` table and adds `organization_id` foreign key to `users`.
- **V3 (`V3__init_projects_and_memberships.sql`)**: Creates `projects` and `project_memberships` tables with foreign keys, unique constraint `(project_id, user_id)`, and performance indexes.
- **V4 (`V4__init_modules.sql`)**: Creates `modules` table with `project_id` foreign key (`ON DELETE RESTRICT`), unique constraint `uk_modules_project_name` (`(project_id, name)`), and performance indexes.
- **V5 (`V5__init_issues.sql`)**: Creates `issues` table with foreign keys `project_id`, `module_id` (nullable), `reporter_id` (`ON DELETE RESTRICT`), enums `type`, `priority`, `stage`, `verification_status`, and performance indexes.

---

## 5. Domain Models Architecture

### Project Model
- `id` (UUID PRIMARY KEY)
- `organization_id` (UUID NOT NULL REFERENCES `client_organizations(id)` ON DELETE RESTRICT) - Immutable after creation.
- `name` (String, required)
- `short_code` (String, required, unique, normalized to uppercase)
- `description` (Text, optional)
- `isActive` (Boolean, default `true`)

### Project Membership Model
- `id` (UUID PRIMARY KEY)
- `project_id` (UUID NOT NULL REFERENCES `projects(id)` ON DELETE CASCADE)
- `user_id` (UUID NOT NULL REFERENCES `users(id)` ON DELETE CASCADE)
- Unique constraint on `(project_id, user_id)` prevents duplicate member assignments.

### Module Model
- `id` (UUID PRIMARY KEY)
- `project_id` (UUID NOT NULL REFERENCES `projects(id)` ON DELETE RESTRICT) - Immutable after creation.
- `name` (String, required) - Unique per project (case-insensitive).
- `description` (Text, optional)
- `isActive` (Boolean, default `true`)

### Issue Model
- `id` (UUID PRIMARY KEY)
- `project_id` (UUID NOT NULL REFERENCES `projects(id)` ON DELETE RESTRICT) - Immutable after creation.
- `module_id` (UUID NULL REFERENCES `modules(id)` ON DELETE RESTRICT) - Optional; must belong to the same project and be active.
- `reporter_id` (UUID NOT NULL REFERENCES `users(id)` ON DELETE RESTRICT) - Derived from authenticated security context; immutable.
- `title` (String, required)
- `description` (Text, required)
- `type` (Enum: `BUG`, `ENHANCEMENT`, `NEW_FEATURE`)
- `priority` (Enum: `VERY_LOW`, `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`, `URGENT`)
- `stage` (Enum: `SUBMITTED`, `RECEIVED`, `UNDER_DEVELOPMENT`, `TESTING`, `DEPLOYED`, `DECLINED`, `RESOLVED`) - Initial default: `SUBMITTED`.
- `verification_status` (Enum: `PENDING_VERIFICATION`, `VERIFIED`, `REJECTED`) - Initial default: `PENDING_VERIFICATION`.

### Cross-Tenant & Module Integrity Guards
- A user can ONLY create an issue for a project in their organization (or a project where they hold an active membership for `CLIENT_USER`).
- When `moduleId` is provided, backend validation strictly enforces that the module exists, is active, and belongs to the specified `projectId`.

---

## 6. Server-Side Role-Based Access Control (RBAC) Matrix

| Action | APP_ADMIN | CLIENT_ADMIN | CLIENT_USER |
|---|---|---|---|
| **List Projects** | All Organizations | Own Organization Only | Member Projects Only |
| **View Project Details** | Any Project | Own Organization Only | Member Projects Only |
| **Create Project** | Yes | Forbidden (403) | Forbidden (403) |
| **Edit Project** | Yes | Forbidden (403) | Forbidden (403) |
| **Activate/Deactivate Project** | Yes | Forbidden (403) | Forbidden (403) |
| **Delete Project** | Yes (Restricted by Modules/Issues FK) | Forbidden (403) | Forbidden (403) |
| **Add Project Member** | Yes (Same Org User) | Yes (Own Org Project & User) | Forbidden (403) |
| **Remove Project Member** | Yes | Yes (Own Org Project) | Forbidden (403) |
| **List Project Members** | Any Project | Own Org Projects Only | Member Projects Only |
| **Create Module** | Yes | Yes (Own Org Project) | Forbidden (403) |
| **Edit / Toggle Module** | Yes | Yes (Own Org Project) | Forbidden (403) |
| **Delete Module** | Yes | Yes (Own Org Project) | Forbidden (403) |
| **List / View Modules** | Any Project | Own Org Projects Only | Active Member Projects Only |
| **Create Issue** | Any Active Project | Own Org Active Projects | Active Member Projects Only |
| **List / View Issues** | Any Project | Own Org Projects Only | Member Projects Only |
| **Edit Issue** | Any Issue | Own Org Issues | Own Reported Issues Only (in Member Projects) |
| **Delete Issue** | Any Issue | Own Org Issues | Forbidden (403) |
| **User & Org Management** | Full | Own Organization | Forbidden (403) |

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
- `GET /api/users` - Paginated user search (`page`, `size`, `search`, `role`, `active`, `organizationId`).
- `GET /api/users/{id}` - Returns user details by ID.
- `POST /api/users` - Creates new user adhering to RBAC matrix.
- `PUT /api/users/{id}` - Updates user profile, role, active status, or organization assignment.
- `PATCH /api/users/{id}/deactivate` - Deactivates user account (`isActive = false`).
- `DELETE /api/users/{id}` - (`APP_ADMIN` only) Hard-deletes user account.
- `POST /api/users/{id}/force-password-reset` - Administrative force password reset with BCrypt hashing.

### Project Management Endpoints (`/api/projects`)
- `POST /api/projects` - (`APP_ADMIN`) Creates new project under a client organization.
- `GET /api/projects` - Paginated search (`page`, `size`, `search`, `active`, `organizationId`). Scoped automatically for `CLIENT_ADMIN` (own org) and `CLIENT_USER` (member projects only).
- `GET /api/projects/{id}` - Returns project details adhering to access matrix.
- `PUT /api/projects/{id}` - (`APP_ADMIN`) Updates project name, short code, description, and `isActive` status. (`organizationId` is immutable).
- `DELETE /api/projects/{id}` - (`APP_ADMIN` only) Hard-deletes project (blocked if modules/issues exist).

### Project Membership Endpoints (`/api/projects/{projectId}/members`)
- `POST /api/projects/{projectId}/members` - Adds user to project. Validates user active, project active, organization match, and uniqueness.
- `DELETE /api/projects/{projectId}/members/{userId}` - Removes user membership from project.
- `GET /api/projects/{projectId}/members` - Returns paginated list of project members.

### Module Management Endpoints (`/api/modules`)
- `POST /api/modules` - Creates a new module within a project (`APP_ADMIN` or `CLIENT_ADMIN` of the project's org).
- `GET /api/modules` - Paginated module search (`projectId`, `search`, `active`, `page`, `size`). Scoped automatically by RBAC.
- `GET /api/modules/{id}` - Retrieves module details by ID.
- `PUT /api/modules/{id}` - Updates module name and description (`projectId` immutable).
- `PATCH /api/modules/{id}/activate` - Activates module (`isActive = true`).
- `PATCH /api/modules/{id}/deactivate` - Deactivates module (`isActive = false`).
- `DELETE /api/modules/{id}` - Deletes module (`APP_ADMIN` or `CLIENT_ADMIN` of the project's org).

### Issue Management Endpoints (`/api/issues`)
- `POST /api/issues` - Creates a new issue. Project must be active. Reporter is set automatically. Default `stage = SUBMITTED`, `verificationStatus = PENDING_VERIFICATION`.
- `GET /api/issues` - Paginated issue search (`search`, `projectId`, `moduleId`, `type`, `priority`, `stage`, `verificationStatus`, `reporterId`, `page`, `size`). Scoped by RBAC.
- `GET /api/issues/{id}` - Retrieves issue details by ID (Scoped by RBAC).
- `PUT /api/issues/{id}` - Updates title, description, type, priority, and module (`projectId`, `reporter`, `stage`, `verificationStatus` immutable).
- `PATCH /api/issues/{id}/stage` - Transitions issue stage (`SUBMITTED` → `RECEIVED` → `UNDER_DEVELOPMENT` → `TESTING` → `DEPLOYED`, `SUBMITTED` → `DECLINED`, `TESTING` → `RESOLVED`) adhering to centralized state machine and role permissions.
- `DELETE /api/issues/{id}` - Deletes issue (`APP_ADMIN` or `CLIENT_ADMIN` of project's org; `CLIENT_USER` receives `403`).

---

## 8. Issue Stage State Machine Architecture

### Allowed Stage Transition Flow
- **Primary Linear Flow**: `SUBMITTED` → `RECEIVED` → `UNDER_DEVELOPMENT` → `TESTING` → `DEPLOYED`
- **Terminal Transitions**: `SUBMITTED` → `DECLINED` and `TESTING` → `RESOLVED`
- **Terminal States**: `DEPLOYED`, `DECLINED`, `RESOLVED` have no outgoing transitions.

### Role Permission Matrix for Stage Transitions
- **`APP_ADMIN` & `CLIENT_ADMIN`**: Can perform any valid state machine transition (scoped to own org for `CLIENT_ADMIN`).
- **`CLIENT_USER`**: Restricted strictly to transitioning their own reported issues for:
  - `SUBMITTED` → `DECLINED`
  - `TESTING` → `RESOLVED`
  - *Forbidden from developer workflow transitions (`SUBMITTED → RECEIVED`, `RECEIVED → UNDER_DEVELOPMENT`, `UNDER_DEVELOPMENT → TESTING`, `TESTING → DEPLOYED`).*

---

## 9. Delete Behavior & Dependencies

> [!IMPORTANT]
> **Data Integrity Safeguards**:
> - Foreign keys from `issues` to `projects`, `modules`, and `users` use `ON DELETE RESTRICT`. A project, module, or user cannot be deleted if referenced by existing issue records.
> - **Verification Workflows, Comments & Attachments**: Verification approval/rejection workflows, issue comments, file attachments, and audit logs are implemented in subsequent feature branches.

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

Run full unit and integration test suite:

```bash
mvn clean test -Dnet.bytebuddy.experimental=true
```


