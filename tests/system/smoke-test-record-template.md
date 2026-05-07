# Smoke Test Record Template

Use this template after each deployment. Fill all fields and keep one copy per release.

## 1. Test Metadata

| Field | Value |
|---|---|
| Project | HerLink |
| Environment | http://47.99.122.227 |
| Test Date |  |
| Start Time |  |
| End Time |  |
| Build Version / Commit ID |  |
| Release Ticket / Pipeline Run |  |
| Tester |  |
| Backend Version Tag |  |
| Frontend Version Tag |  |
| Database Migration Version |  |

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
| Deployment completed |  |  |
| URL reachable |  |  |
| Accounts available |  |  |
| Approved resource exists |  |  |
| Dependencies healthy |  |  |

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
| SMK-001 | Site Availability | None | Open home page and login page. | Pages load within acceptable time, no white screen/5xx. |  |  |  |  |  |  |
| SMK-002 | Viewer Login | Viewer account available | Login as viewer (`1067364488@qq.com`). | Login succeeds; redirected to authenticated area. |  |  |  |  |  |  |
| SMK-003 | Auth Session | Viewer logged in | Refresh page, open a protected page directly. | Session remains valid; user stays authenticated. |  |  |  |  |  |  |
| SMK-004 | Viewer Resource List | Viewer logged in; approved resource exists | Open resource list page. | List loads correctly (or valid empty state if none). |  |  |  |  |  |  |
| SMK-005 | Viewer Resource Detail | Resource list loaded | Open one resource detail page. | Detail data loads correctly; no broken key fields. |  |  |  |  |  |  |
| SMK-006 | Viewer Comment Create | Viewer on resource detail | Submit a valid comment. | Comment created and visible in list. |  |  |  |  |  |  |
| SMK-007 | Viewer Comment Delete | Comment created by same viewer | Delete the created comment. | Comment removed successfully; no unexpected error. |  |  |  |  |  |  |
| SMK-008 | Contributor Login | Contributor account available | Login as contributor (`bob_contributor@example.com`). | Login succeeds with contributor permissions. |  |  |  |  |  |  |
| SMK-009 | Contributor Create Draft | Contributor logged in | Create a new resource draft. | Draft created successfully with valid ID. |  |  |  |  |  |  |
| SMK-010 | Contributor Update Draft | Draft exists | Edit draft title/description/category/type and save. | Update succeeds and persists after refresh. |  |  |  |  |  |  |
| SMK-011 | Contributor My Resources | Contributor logged in | Open ¡°My resources¡±. | Newly created/updated draft appears correctly. |  |  |  |  |  |  |
| SMK-012 | Admin Login | Admin account available | Login as admin (`david_reviewer@example.com`). | Login succeeds with admin permissions. |  |  |  |  |  |  |
| SMK-013 | Admin Pending Requests | Admin logged in | Open pending contributor requests page/API view. | Data loads without permission/data errors. |  |  |  |  |  |  |
| SMK-014 | Admin Classification Pages | Admin logged in | Open category/tag/resource-type management pages. | Lists load successfully. |  |  |  |  |  |  |
| SMK-015 | Admin Operation History | Admin logged in | Open operation history page. | History list loads without server error. |  |  |  |  |  |  |
| SMK-016 | Logout and Re-Access Protection | User currently logged in | Logout, then access a protected page by URL. | User is redirected/blocked and asked to login again. |  |  |  |  |  |  |

## 5. Extended Smoke Cases (Recommended)

Run these when time allows or for high-risk releases.

| ID | Area | Preconditions | Steps | Expected Result | Actual Result | Result | Severity (if fail) | Evidence | Defect ID | Owner |
|---|---|---|---|---|---|---|---|---|---|---|
| SMK-017 | RBAC Negative | Viewer logged in | Attempt to access admin page/API. | Access denied (401/403 or redirected). |  |  |  |  |  |  |
| SMK-018 | Contributor Permission Boundary | Viewer logged in | Attempt contributor-only operation (create draft). | Access denied correctly. |  |  |  |  |  |  |
| SMK-019 | Basic Error Handling | Any role logged in | Open non-existing resource ID page/API. | Friendly error; no stack trace leak. |  |  |  |  |  |  |
| SMK-020 | File Upload Basic (if feature in release scope) | Contributor draft exists | Upload supported file type/size. | Upload succeeds and preview/state updates. |  |  |  |  |  |  |
| SMK-021 | Browser Console Health | Any page loaded | Check browser console while doing core flow. | No new critical JS errors. |  |  |  |  |  |  |

## 6. API Quick Verification Checklist (Optional but useful)

| Endpoint | Method | Expected Status | Actual Status | Result | Notes |
|---|---|---|---|---|---|
| `/api/auth/me` (unauthenticated) | GET | 401 |  |  |  |
| `/api/auth/login` | POST | 200 |  |  |  |
| `/api/viewer/resources` | GET | 200 |  |  |  |
| `/api/viewer/resources/{id}` | GET | 200 |  |  |  |
| `/api/viewer/resources/{id}/comments` | POST | 201 |  |  |  |
| `/api/viewer/resources/{id}/comments/{commentId}` | DELETE | 204 |  |  |  |
| `/api/contributor/resources` | POST | 200 |  |  |  |
| `/api/contributor/resources/{id}` | PUT | 200 |  |  |  |
| `/api/admin/contributor-requests/pending` | GET | 200 |  |  |  |
| `/api/admin/operation-history` | GET | 200 |  |  |  |

## 7. Defect Summary

| Defect ID | Title | Severity | Area | Repro Steps | Current Status | Owner | ETA |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

## 8. Risk and Impact Assessment

| Risk Item | Impact | Likelihood | Mitigation / Action |
|---|---|---|---|
|  |  |  |  |

## 9. Final Decision

- Smoke Test Result: `Go` / `No-Go`
- Blocking Issues Present: `Yes` / `No`
- Decision Notes:

```

```

## 10. Sign-Off

| Role | Name | Decision | Time |
|---|---|---|---|
| Tester |  |  |  |
| Dev Owner |  |  |  |
| Release Owner |  |  |  |
