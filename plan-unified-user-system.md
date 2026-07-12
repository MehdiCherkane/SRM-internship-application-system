# Unified User System — Implementation Plan

**Approach A:** Merge `Admin` → `User` with `Role` enum, unified login, applicant dashboard + edit.

## Phase 1 — Model Changes

### New: `src/main/java/com/onep/internship/model/Role.java`
```java
public enum Role { ADMIN, APPLICANT }
```

### Replace: `model/Admin.java` → `model/User.java`
- Fields: `id`, `name`, `email` (unique), `username` (unique), `password`, `role`, `createdAt`
- `@Table(name = "admins")` — keeps existing DB table, zero migration

### Edit: `model/Application.java`
- Add `@ManyToOne(fetch = LAZY) @JoinColumn(name = "applicant_id") private User applicant`

### Rename: `repository/AdminRepository.java` → `UserRepository.java`
- Add `findByEmail(String)` and `existsByEmail(String)`

### DB migration (run once)
```sql
ALTER TABLE admins ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ADMIN';
ALTER TABLE admins ADD COLUMN created_at DATETIME(6);
UPDATE admins SET role = 'ADMIN' WHERE role IS NULL;
ALTER TABLE applications ADD COLUMN applicant_id BIGINT;
ALTER TABLE applications ADD FOREIGN KEY (applicant_id) REFERENCES admins(id);
```

## Phase 2 — Security

### Edit: `config/SecurityConfig.java`
- `UserDetailsService` sources from `UserRepository`, assigns `role`
- `AuthenticationSuccessHandler` routes by role: `ADMIN → /admin/dashboard`, `APPLICANT → /applicant/dashboard`
- Route rules:
  - `/apply`, `/success`, `/css/**`, `/js/**` → `permitAll`
  - `/register`, `/login` → `permitAll`
  - `/admin/**` → `hasRole("ADMIN")`
  - `/applicant/**` → `hasRole("APPLICANT")`
- `loginPage("/login")`, `loginProcessingUrl("/login")`

### Rename: `config/AdminSeeder.java` → `config/UserSeeder.java`
- Seeds one ADMIN and one APPLICANT for testing

## Phase 3 — New Controllers

### New: `AuthController.java`
| Endpoint | Method | Purpose |
|---|---|---|
| `/register` | GET | Registration form |
| `/register` | POST | Create user, auto-login, redirect to `/applicant/dashboard` |
| `/login` | GET | Unified login page |

### New: `ApplicantController.java`
| Endpoint | Method | Purpose |
|---|---|---|
| `/applicant/dashboard` | GET | List user's applications + statuses |
| `/applicant/apply` | GET | Pre-filled form (firstName, lastName, email from profile) |
| `/applicant/apply` | POST | Submit/update application, linked to logged-in user |
| `/applicant/application/{id}` | GET | View single application detail |
| `/applicant/cv/{id}` | GET | Stream CV |

## Phase 4 — Templates

| File | Action |
|---|---|
| `templates/login.html` | New — unified login |
| `templates/register.html` | New — registration form |
| `templates/applicant/dashboard.html` | New — application list with status badges, edit/view links |
| `templates/applicant/edit.html` | New — pre-filled application form, CV re-upload |
| `templates/admin/login.html` | Redirect to `/login` or remove |

## Phase 5 — Existing Controller Updates

### Edit: `ApplicationController.java`
- After saving, if user is logged in → `app.setApplicant(user)` before saving

### Edit: `AdminController.java`
- `/admin/login` GET → redirect to `/login`

## What stays unchanged

- `pom.xml` (no new dependencies)
- `application.properties`
- `EmailService.java`
- `GlobalExceptionHandler.java`
- `HomeController.java`

## Build order

```
 1. Role.java
 2. User.java          (depends on Role)
 3. UserRepository     (depends on User)
 4. Application.java   (adds User field)
 5. ApplicationRepository (add findByApplicant)
 6. UserSeeder.java    (depends on User, UserRepository)
 7. SecurityConfig     (depends on UserRepository)
 8. AuthController     (depends on UserRepository, Security)
 9. ApplicantController (depends on ApplicationRepository, Security)
10. Templates
11. ApplicationController (minor edit)
12. AdminController      (minor edit)
```
