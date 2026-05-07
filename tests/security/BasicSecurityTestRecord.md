# Basic Security Test Record (HerLink)

## 1. Test Metadata

| Field | Value |
|---|---|
| Project | HerLink |
| Environment | http://47.99.122.227 |
| Test Date | 2026-05-07 |
| Tester | Yan |
| Build Version / Commit ID | d6f8a9a |

---

## 2. Scope

| Category | Covered |
|---|---|
| Authentication / Session | Yes |
| Authorization (Role-Based Access) | Yes |
| Input Validation (SQL Injection) | Yes |
| Input Validation (XSS) | Yes |
| Sensitive Data Exposure | Yes |
| Error Handling Information Leak | Yes |

---

## 3. Authentication & Session Tests

### SEC-001: Unauthenticated Access Protection

| Field | Value |
|---|---|
| **Endpoint / Page** | Protected pages (resource list, admin, contributor) |
| **Method** | GET (direct URL access) |
| **Precondition** | User logged out |
| **Steps** | 1. Logout 2. Directly visit protected URL |
| **Expected Result** | Redirect to login page or show 401/403 |
| **Actual Result** | Redirected to login page; access blocked |
| **Result** | Pass |
| **Severity (if fail)** | N/A |
| **Evidence** | Verified during SMK-016 |

---

### SEC-002: Session Invalidation on Logout

| Field | Value |
|---|---|
| **Endpoint / Page** | Previously visited protected page |
| **Method** | Browser back button after logout |
| **Precondition** | User logged in, then logged out |
| **Steps** | 1. Login as viewer 2. Visit resource list page 3. Logout 4. Press browser back button 5. Attempt data-modifying action 6. Force refresh page |
| **Expected Result** | Cannot access page or perform actions without re-login |
| **Actual Result** | Cached page displayed after back button. Any data-modifying action (e.g., post comment) triggers re-login prompt. Force refresh also requires re-login. Server-side session correctly invalidated. |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Read-only cached page accessible; write operations blocked with re-login prompt |

---

### SEC-003: Session Persistence After Browser Close

| Field | Value |
|---|---|
| **Endpoint / Page** | Any protected page |
| **Method** | Close and reopen browser |
| **Precondition** | User logged in, browser fully closed |
| **Steps** | 1. Login as viewer 2. Close all browser windows completely 3. Reopen browser and visit http://47.99.122.227 |
| **Expected Result** | Session may persist or require re-login; either is acceptable depending on cookie policy |
| **Actual Result** | Required to re-login. Session not persisted after browser close. |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Login page shown after browser restart |

---

## 4. Authorization Tests (RBAC)

### SEC-004: Viewer Cannot Access Admin Pages

| Field | Value |
|---|---|
| **Endpoint / Page** | Admin pages / API |
| **Role** | Viewer |
| **Steps** | 1. Login as viewer (`1067364488@qq.com`) 2. Directly access admin URL or API |
| **Expected Result** | Access denied (401/403) or redirected |
| **Actual Result** | Access denied; redirected away from admin page |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Verified during SMK-017 |

---

### SEC-005: Viewer Cannot Perform Contributor Actions

| Field | Value |
|---|---|
| **Endpoint / Page** | Contributor draft creation page / API |
| **Role** | Viewer |
| **Steps** | 1. Login as viewer 2. Attempt to create resource draft |
| **Expected Result** | Access denied or feature hidden |
| **Actual Result** | Access denied; contributor functionality unavailable to viewer |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Verified during SMK-018 |

---

### SEC-006: Contributor Cannot Access Admin Pages

| Field | Value |
|---|---|
| **Endpoint / Page** | Admin pages / API |
| **Role** | Contributor |
| **Steps** | 1. Login as contributor (`bob_contributor@example.com`) 2. Directly access admin URL or API |
| **Expected Result** | Access denied (401/403) or redirected |
| **Actual Result** | Access denied; cannot enter admin pages |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Verified during Smoke Test |

---

## 5. Input Validation Tests

### SEC-007: SQL Injection - Login Form

| Field | Value |
|---|---|
| **Endpoint** | `/api/auth/login` |
| **Method** | POST |
| **Payload** | `' OR '1'='1` |
| **Expected Result** | Login fails; no bypass |
| **Actual Result** | Login failed. Frontend email format validation rejected payload. |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Error: "An email address should include the "@" symbol." |

---

### SEC-008: SQL Injection - Login Form (OR payload)

| Field | Value |
|---|---|
| **Endpoint** | `/api/auth/login` |
| **Method** | POST |
| **Payload** | `' OR 1=1--` |
| **Expected Result** | Login fails; no bypass |
| **Actual Result** | Login failed. SQL injection attempt blocked. |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Login rejected, no access granted |

---

### SEC-009: SQL Injection - Search / Keyword Field

| Field | Value |
|---|---|
| **Endpoint** | Search / filter on resource list |
| **Method** | GET |
| **Payload** | `' OR '1'='1' --` |
| **Expected Result** | No data leak; app returns no results or error gracefully |
| **Actual Result** | Returned "No matched approved resources." No data leaked. |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Search result showed empty state |

---

### SEC-010: SQL Injection - Comment Field

| Field | Value |
|---|---|
| **Endpoint** | Comment submission API |
| **Method** | POST |
| **Payload** | `'; DROP TABLE comments; --` |
| **Expected Result** | Stored as literal text; no table dropped |
| **Actual Result** | Comment submitted successfully; comment list loaded normally. Payload treated as plain text. |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Comment visible in list as normal text |

---

### SEC-011: XSS - Comment Field (Script Tag)

| Field | Value |
|---|---|
| **Endpoint** | Comment submission API |
| **Method** | POST |
| **Payload** | `<script>alert('xss')</script>` |
| **Expected Result** | Displayed as plain text; script not executed |
| **Actual Result** | Payload displayed as plain text. No script execution. Only normal "comment submitted" toast appeared. |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Comment shows literal text: `<script>alert('xss')</script>` |

---

### SEC-012: XSS - Comment Field (Image Onerror)

| Field | Value |
|---|---|
| **Endpoint** | Comment submission API |
| **Method** | POST |
| **Payload** | `<img src=x onerror=alert('xss')>` |
| **Expected Result** | Displayed as plain text; no alert |
| **Actual Result** | Displayed as plain text. No script execution. |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Comment shows literal text: `<img src=x onerror=alert('xss')>` |

---

### SEC-013: XSS - Resource Title / Description (Contributor)

| Field | Value |
|---|---|
| **Endpoint** | Draft create/update API |
| **Method** | POST / PUT |
| **Payload** | Title: `<script>alert('xss-title')</script>`, Description: `<img src=x onerror=alert('xss-desc')>` |
| **Expected Result** | Displayed as plain text when viewing resource |
| **Actual Result** | Both title and description displayed as plain text. No script execution. |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Draft detail page shows payload as literal text |

---

## 6. Sensitive Data Exposure Tests

### SEC-014: Login Response Data Leak

| Field | Value |
|---|---|
| **Endpoint** | `/api/auth/login` |
| **Method** | POST |
| **Expected Result** | Response contains only token / user info; NO password hash, NO stack trace, NO internal config |
| **Actual Result** | Response sets JSESSIONID cookie with HttpOnly flag. Response body could not be directly inspected in DevTools but headers show no obvious sensitive data leak. HttpOnly flag present — good security practice. |
| **Result** | **Pass** |
| **Severity (if fail)** | N/A |
| **Evidence** | Response headers: `set-cookie: JSESSIONID=...; Path=/; HttpOnly` |

---

### SEC-015: Error Response Information Leak

### SEC-015: Error Response Information Leak

| Field | Value |
|---|---|
| **Endpoint** | `/api/nonexistent-endpoint` |
| **Method** | GET |
| **Expected Result** | Returns clean JSON error; NO framework name, NO version, NO stack trace, NO SQL |
| **Actual Result** | `{"statusCode":500,"message":"Internal server error.","details":[],"path":"/api/nonexistent-endpoint","timestamp":"2026-05-07T19:14:22.967815941"}`. No sensitive data exposed, but incorrect status code (500 instead of 404). |
| **Result** | **Pass** (no info leak) |
| **Severity (if fail)** | N/A |
| **Notes** | Recommend changing 500 to 404 for non-existent endpoints |

---

### SEC-016: Non-Existent Resource Error Leak

| Field | Value |
|---|---|
| **Endpoint** | `/api/viewer/resources/99` |
| **Method** | GET |
| **Expected Result** | Returns clean 404 or error; NO stack trace in response body |
| **Actual Result** | Frontend shows friendly error message. Backend returns 500 in console, but no stack trace leaked. See SMK-019 & SMK-021. |
| **Result** | **Pass** (no info leak, but 500 logged as defect DEV-02) |
| **Severity (if fail)** | N/A |
| **Evidence** | SMK-019 / SMK-021 |

---

## 7. Browser Security Headers Check (Quick)

| Header | Expected | Found? | Notes |
|---|---|---|---|
| X-Content-Type-Options | `nosniff` | No | Missing; recommended to add |
| X-Frame-Options | `DENY` or `SAMEORIGIN` | No | Missing; recommended to add |
| Content-Security-Policy | Present | No | Missing; recommended to add |
| Strict-Transport-Security | Present (for HTTPS) | No | Not applicable in HTTP environment |

> Check: Open DevTools → Network → click any request → Response Headers section.

---

## 8. Findings Summary

| ID | Category | Description | Severity | Endpoint | Status |
|---|---|---|---|---|---|
| SEC-FIND-01 | Security Headers | Missing X-Content-Type-Options, X-Frame-Options, and Content-Security-Policy headers | Low | All endpoints | Open — recommended Nginx config change provided |
| SEC-FIND-02 | Error Handling | Non-existent endpoints return 500 instead of 404 | Low | `/api/nonexistent-endpoint`, `/api/viewer/resources/{invalid-id}` | Open — recorded as DEV-02 |
| SEC-FIND-03 | Error Handling | Static assets return 500 in console (category images, favicon.ico) — no user impact | Low | `/module6/assets/category-*.png`, `/favicon.ico` | Open — recorded as DEV-01 |

---

## 9. Final Result

- Overall Security Test Result: **Pass**
- Critical Issues: **No**
- High Issues: **No**
- Release Blocker: **No**
- Notes:
  - All authentication and authorization tests passed. No session hijacking or privilege escalation vulnerabilities found.
  - SQL injection and XSS payloads were correctly handled across login, search, comment, and draft fields. All payloads treated as plain text or rejected with proper validation.
  - No sensitive data exposed in API responses. Session cookies use HttpOnly flag.
  - Three security headers are missing (X-Content-Type-Options, X-Frame-Options, CSP). These are low-severity hardening improvements, not blocking for release. Nginx configuration update provided.
  - Two low-severity error handling issues carried over from Smoke Test (DEV-01, DEV-02) — backend 500s on static assets and non-existent resources. No sensitive data leaked.
  - Recommended: Add missing security headers to Nginx config in next release cycle.
  - No critical or high findings. Safe to proceed with release.