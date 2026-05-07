# Acceptance Test Record

Project: HerLink Community Heritage Resource Sharing Platform

Use this template to record one acceptance test cycle after deployment.

## 1. Test Metadata

| Field | Value |
|---|---|
| Environment | http://47.99.122.227 |
| Test Date |  |
| Start Time |  |
| End Time |  |
| Build Version / Commit ID |  |
| Deployment Ticket / Pipeline Run |  |
| Tester |  |
| Test Goal | Acceptance test for release approval |

## 2. Accounts Used

| Role | Account | Password / Source | Notes |
|---|---|---|---|
| Viewer 1 | 1067364488@qq.com | provided by user | Primary viewer account |
| Viewer 2 | carol_viewer@example.com | provided by user | Backup viewer account |
| Viewer 3 | alice@example.com | provided by user | Backup viewer account |
| Contributor | bob_contributor@example.com | provided by user | Approved contributor account |
| Admin | david_reviewer@example.com | provided by user | Admin account |

## 3. Preconditions Checklist

| Check | Result (Pass/Fail) | Evidence / Notes |
|---|---|---|
| Site is reachable |  |  |
| Viewer login works |  |  |
| Contributor login works |  |  |
| Admin login works |  |  |
| At least one approved resource exists |  |  |
| Application logs show no critical startup errors |  |  |

## 4. Acceptance Test Cases

Fill one row per case.

Result options:
- `Pass`
- `Fail`
- `Blocked`
- `N/A`

Severity options if failed:
- `Blocker`
- `High`
- `Medium`
- `Low`

| ID | Scenario | Preconditions | Steps | Expected Result | Actual Result | Result | Severity | Evidence | Defect ID | Owner |
|---|---|---|---|---|---|---|---|---|---|---|
| ACT-001 | Viewer login and session | Viewer account available | Login as viewer and refresh the page. | Session remains valid and user stays authenticated. |  |  |  |  |  |  |
| ACT-002 | Viewer browse resources | Viewer logged in | Open resource list. | Approved resources display correctly. |  |  |  |  |  |  |
| ACT-003 | Viewer resource detail | Viewer logged in; resource exists | Open one approved resource detail. | Detail page loads with complete resource info. |  |  |  |  |  |  |
| ACT-004 | Viewer comment create/delete | Viewer logged in; resource detail open | Create a comment, then delete it. | Comment is created and removed successfully. |  |  |  |  |  |  |
| ACT-005 | Viewer feedback submit | Viewer logged in | Submit feedback and view own feedback list. | Feedback is stored and retrievable. |  |  |  |  |  |  |
| ACT-006 | Contributor login and draft creation | Contributor account available | Login as contributor and create a draft. | Draft is created successfully. |  |  |  |  |  |  |
| ACT-007 | Contributor edit draft | Draft exists | Update metadata and save. | Changes persist after refresh. |  |  |  |  |  |  |
| ACT-008 | Contributor file upload | Draft exists and files available | Upload preview/media files. | Files are linked to the draft. |  |  |  |  |  |  |
| ACT-009 | Contributor submit review | Draft is complete | Submit draft for review. | Resource enters review state and submission history is created. |  |  |  |  |  |  |
| ACT-010 | Admin login and pending requests | Admin account available | Login as admin and open pending contributor requests. | Admin can view pending requests. |  |  |  |  |  |  |
| ACT-011 | Admin resource lifecycle | Admin logged in | Archive and unarchive a resource. | Resource status changes correctly. |  |  |  |  |  |  |
| ACT-012 | Admin classification pages | Admin logged in | Open category, type, and tag management pages. | Pages load and show expected data. |  |  |  |  |  |  |
| ACT-013 | Role boundary check: viewer to admin | Viewer logged in | Try opening admin-only page/API. | Access denied or redirected. |  |  |  |  |  |  |
| ACT-014 | Role boundary check: viewer to contributor | Viewer logged in | Try contributor-only action. | Access denied. |  |  |  |  |  |  |
| ACT-015 | Logout and re-login | Any role logged in | Logout and try accessing a protected page. | Protected page requires login again. |  |  |  |  |  |  |
| ACT-016 | Data persistence after refresh | Any workflow completed | Refresh page and revisit the entity. | Data remains consistent after refresh. |  |  |  |  |  |  |

## 5. Issues Found

| Defect ID | Title | Severity | Scenario ID | Summary | Status | Owner | ETA |
|---|---|---|---|---|---|---|---|
|  |  |  |  |  |  |  |  |

## 6. Acceptance Summary

| Item | Value |
|---|---|
| Total Cases |  |
| Passed |  |
| Failed |  |
| Blocked |  |
| Not Applicable |  |
| Accepted / Rejected |  |

## 7. Sign-Off

| Role | Name | Decision | Time | Signature / Note |
|---|---|---|---|---|
| Tester |  |  |  |  |
| Developer |  |  |  |  |
| Product / Release Owner |  |  |  |  |

## 8. Notes

Use this section for any acceptance-specific observations, such as:

- Business rule mismatch
- Role permission gap
- Data inconsistency
- UI usability issue
- Missing localization or content issue

