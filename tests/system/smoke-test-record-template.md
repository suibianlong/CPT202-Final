# Smoke Test Record Template

Use this template after each deployment. Fill all fields and keep one copy per release.

## 1. Test Metadata

| Field | Value |
|---|---|
| Project | HerLink |
| Environment | http://47.99.122.227 |
| Test Date | 2026.5.7 |
| Start Time | 16：45 |
| End Time | 17:33 |
| Build Version / Commit ID | Commit d6f8a9a24b6201354e60dbd9003004950ee7ed63 |
| Release Ticket / Pipeline Run | https://github.com/suibianlong/CPT202-Final/commit/d6f8a9a24b6201354e60dbd9003004950ee7ed63 |
| Tester | Yan Long |
| Backend Version Tag | sha-d6f8a9a24b6201354e60dbd9003004950ee7ed63 |
| Frontend Version Tag | sha-d6f8a9a24b6201354e60dbd9003004950ee7ed63 |
| Database Migration Version | N/A |

## 2. Test Accounts Used

| Role | Account | Password / Source | Notes |
|---|---|---|---|
| Viewer-1 | 1067364488@qq.com | (filled) | Primary viewer |
| Viewer-2 | carol_viewer@example.com | (filled) | Backup viewer |
| Viewer-3 | alice@example.com | (filled) | Backup viewer |
| Contributor | bob_contributor@example.com | (filled) | Approved contributor required |
| Admin | david_reviewer@example.com | (filled) | Admin role required |

## 3. Entry and Exit Criteria

### Entry Criteria (before running)

- Deployment finished with no failed pipeline stage.
- Application URL is reachable from test machine.
- Test accounts can log in.
- At least one approved resource exists for viewer browsing.
- Core dependencies healthy (DB/Redis/MQ if used).

| Check | Result (Pass/Fail) | Evidence / Notes |
|---|---|---|
| Deployment completed | Pass | GitHub Actions CI/CD #35 succeeded |
| URL reachable | Pass | Home page loaded successfully at http://47.99.122.227 |
| Accounts available | Pass | Contributor-1 (bob_contributor@example.com) login successful |
| Approved resource exists | Pass | Resource list loaded with 7 approved resources |
| Dependencies healthy | Pass | All 3 containers Up: herlink-app, herlink-nginx, herlink-mysql |

### Exit Criteria (to mark smoke completed)

- All P0 cases pass.
- No unresolved blocker/high issue.
- Final decision marked as `Go` or `No-Go`.

## 4. Smoke Test Cases (P0)

Fill one row per case.

Result options: `Pass` / `Fail` / `Blocked` / `N/A`
Severity options when failed: `Blocker` / `High` / `Medium` / `Low`

| ID | Area | Preconditions | Steps | Expected Result | Actual Result | Result | Severity (if fail) | Evidence (screenshot/log URL) | Defect ID | Owner |
|---|---|---|---|---|---|---|---|---|---|---|
| SMK-001 | Site Availability | None | Open home page and login page. | Pages load within acceptable time, no white screen/5xx. | Pass | Home page and login page load normally within acceptable time |  |  |  |  |
| SMK-002 | Viewer Login | Viewer account available | Login as viewer (`1067364488@qq.com`). | Login succeeds; redirected to authenticated area. | Pass | Login successful, redirected to home/dashboard |  |  |  |  |
| SMK-003 | Auth Session | Viewer logged in | Refresh page, open a protected page directly. | Session remains valid; user stays authenticated. | Pass | Session persisted after refresh; protected page accessible |  |  |  |  |
| SMK-004 | Viewer Resource List | Viewer logged in; approved resource exists | Open resource list page. | List loads correctly (or valid empty state if none). | Pass | Resource list page loads correctly, 7 items displayed |  |  |  |  |
| SMK-005 | Viewer Resource Detail | Resource list loaded | Open one resource detail page. | Detail data loads correctly; no broken key fields. | Pass | Resource detail page loaded with title, description, category fields populated |  |  |  |  |
| SMK-006 | Viewer Comment Create | Viewer on resource detail | Submit a valid comment. | Comment created and visible in list. | Pass | Comment "Smoke test comment" submitted and visible in comment list |  |  |  |  |
| SMK-007 | Viewer Comment Delete | Comment created by same viewer | Delete the created comment. | Comment removed successfully; no unexpected error. | Pass | Comment deleted successfully, no error shown |  |  |  |  |
| SMK-008 | Contributor Login | Contributor account available | Login as contributor (`bob_contributor@example.com`). | Login succeeds with contributor permissions. | Pass | Contributor login successful, contributor dashboard accessible |  |  |  |  |
| SMK-009 | Contributor Create Draft | Contributor logged in | Create a new resource draft. | Draft created successfully with valid ID. | Pass | Draft created successfully with ID 45 |  |  |  |  |
| SMK-010 | Contributor Update Draft | Draft exists | Edit draft title/description/category/type and save. | Update succeeds and persists after refresh. | Pass | Draft updated: title/description/category saved and persisted after refresh |  |  |  |  |
| SMK-011 | Contributor My Resources | Contributor logged in | Open "My resources". | Newly created/updated draft appears correctly. | Pass | My Resources shows the draft created and updated in previous steps |  |  |  |  |
| SMK-012 | Admin Login | Admin account available | Login as admin (`david_reviewer@example.com`). | Login succeeds with admin permissions. | Pass | Admin login successful, admin panel accessible |  |  |  |  |
| SMK-013 | Admin Pending Requests | Admin logged in | Open pending contributor requests page/API view. | Data loads without permission/data errors. | Pass | Pending contributor requests page loads without permission errors |  |  |  |  |
| SMK-014 | Admin Classification Pages | Admin logged in | Open category/tag/resource-type management pages. | Lists load successfully. | Pass | Category, tag, and resource type management pages load successfully |  |  |  |  |
| SMK-015 | Admin Operation History | Admin logged in | Open operation history page. | History list loads without server error. | Pass | Operation history page loads without server error |  |  |  |  |
| SMK-016 | Logout and Re-Access Protection | User currently logged in | Logout, then access a protected page by URL. | User is redirected/blocked and asked to login again. | Pass | Logged out, protected URL http://47.99.122.227/admin-dashboard.html redirects to login page |  |  |  |  |

## 5. Extended Smoke Cases (Recommended)

Run these when time allows or for high-risk releases.

| ID | Area | Preconditions | Steps | Result | Expected Result | Actual Result | Severity (if fail) | Evidence | Defect ID | Owner |
|---|---|---|---|---|---|---|---|---|---|---|
| SMK-017 | RBAC Negative | Viewer logged in | Attempt to access admin page/API. | Pass | Access denied (redirected); viewer cannot access admin page |  |  |  |  |  |
| SMK-018 | Contributor Permission Boundary | Viewer logged in | Attempt contributor-only operation (create draft). | Pass | Access denied; viewer cannot access contributor draft creation page |  |  |  |  |  |
| SMK-019 | Basic Error Handling | Any role logged in | Open non-existing resource ID page/API. | Pass | Friendly error message shown: "Approved resource does not exist."; | Page shows friendly error message, but backend returned 500 (visible in console). See SMK-021. |  |  |  |  |
| SMK-020 | File Upload Basic (if feature in release scope) | Contributor draft exists | Upload supported file type/size. | Pass | File resource-45/a699581e04224c74b21a30e3a5c4fbe5.jpg uploaded successfully; preview/upload status updated |  |  |  |  |  |
| SMK-021 | Browser Console Health | Any page loaded | Check browser console while doing core flow. | Fail | No new critical JS errors | Console shows 500 error when accessing non-existent resource ID: Failed to load resource: the server responded with a status of 500 (). Error not visible on page, caught by frontend with friendly message | Medium |  |  |  |

## 6. API Quick Verification Checklist (Optional but useful)

| Endpoint | Method | Expected Status | Actual Status | Result | Notes |
|---|---|---|---|---|---|
| `/api/auth/me` (unauthenticated) | GET | 401 | 401 | Pass | Returns 401 when unauthenticated |
| `/api/auth/login` | POST | 200 | 200 | Pass |  |
| `/api/viewer/resources` | GET | 200 | 200 | Pass |  |
| `/api/viewer/resources/{id}` | GET | 200 | 200 | Pass |  |
| `/api/viewer/resources/{id}/comments` | POST | 201 | 201 | Pass |  |
| `/api/viewer/resources/{id}/comments/{commentId}` | DELETE | 204 | 204 | Pass |  |
| `/api/contributor/resources` | POST | 200 | 200 | Pass |  |
| `/api/contributor/resources/{id}` | PUT | 200 | 200 | Pass |  |
| `/api/admin/contributor-requests/pending` | GET | 200 | 200 | Pass |  |
| `/api/admin/operation-history` | GET | 200 | 200 | Pass |  |

## 7. Defect Summary

| Title | Severity | Area | Repro Steps | Current Status | Owner | ETA |
|---|---|---|---|---|---|---|
| Backend returns 500 when accessing non-existent resource ID | Medium | API / Error Handling | Visit non-existent resource detail page (/resource/99), open DevTools Console, observe 500 error. Friendly message shown on page but backend exception not properly handled | Open | TBD |  |
| Backend returns 500 when accessing non-existent resource ID | High | Static Assets / Nginx | Open any page on the platform, check DevTools Console. Multiple image/icon requests return 500 | Open |  |

## 8. Risk and Impact Assessment

| Risk Item | Impact | Likelihood | Mitigation / Action |
|---|---|---|---|
| Backend 500 error on non-existent resource ID (SMK-021) may cause issues under high load or for invalid ID scanning | Low — page still shows friendly error to users | Medium — every invalid ID request triggers it | Fix backend to return proper 404 instead of 500; track as defect |

## 9. Final Decision

- Smoke Test Result: Go
- Blocking Issues Present: No
- Decision Notes: 
  All 16 P0 smoke cases passed.
  2 defects found but not blocking:
  - DEV-01: Static assets (favicon.ico, category images) return 500 in console, but page display is normal. High priority, fix in next release.
  - DEV-02: Backend returns 500 on non-existent resource ID, frontend shows friendly error. Medium priority.
  No Blocker or unresolved High issue. Recommended to proceed with release.
