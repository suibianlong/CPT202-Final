# Acceptance Test Plan

Project: HerLink Community Heritage Resource Sharing Platform

Environment:
- Production-like deployed site: `http://47.99.122.227`

Purpose:
- Verify the system satisfies the core business goals expected by users and stakeholders.
- Confirm the end-to-end workflows are usable after release.
- Validate role-based access, content lifecycle, and admin management flows.

## 1. Acceptance Scope

This acceptance test focuses on the business flows that matter to real users:

1. Viewer can browse approved resources and inspect resource details.
2. Viewer can submit feedback and comments on approved resources.
3. Contributor can create and maintain draft resources, upload files, and submit for review.
4. Admin can review contributor applications, manage resources, and maintain classification data.
5. Role boundaries are enforced correctly.
6. Data persists correctly across refresh, logout, and relogin.

## 2. Roles And Accounts

Use the real accounts you already prepared:

- Viewer 1: `1067364488@qq.com`
- Viewer 2: `carol_viewer@example.com`
- Viewer 3: `alice@example.com`
- Contributor: `bob_contributor@example.com`
- Admin: `david_reviewer@example.com`

## 3. Entry Criteria

Do not start acceptance testing until:

1. The latest deployment has completed successfully.
2. The site opens from the test machine.
3. All required test accounts can log in.
4. At least one approved resource exists for viewer verification.
5. The database is reachable and application errors are not present in logs.

## 4. Exit Criteria

Acceptance testing is complete only when:

1. All critical acceptance cases pass.
2. No blocker or high-severity defect remains unresolved.
3. The expected role-based permissions are enforced.
4. The final sign-off decision is recorded.

## 5. Test Strategy

The test should be executed in this order:

1. Login and session checks.
2. Viewer browse and interaction flow.
3. Contributor content creation and submission flow.
4. Admin management and moderation flow.
5. Negative permission checks.
6. Data persistence and recovery checks.

## 6. Acceptance Test Cases

### A1. Viewer access and browse

1. Open the site.
2. Log in as a viewer.
3. Open resource list.
4. Open one approved resource detail page.

Expected:
- Login succeeds.
- Resource list loads.
- Resource detail loads with title, description, category, type, and media references.

### A2. Viewer interaction

1. On an approved resource, submit a valid comment.
2. Verify the comment appears in the list.
3. Delete the comment.

Expected:
- Comment creation succeeds.
- Comment deletion succeeds.
- The comment no longer appears after refresh.

### A3. Viewer feedback submission

1. Submit a feedback message from the viewer account.
2. Verify the feedback is stored and visible in the viewer's own feedback list.

Expected:
- Feedback submission succeeds.
- Feedback is persisted correctly.

### A4. Contributor draft creation

1. Log in as contributor.
2. Create a new draft resource.
3. Update title, description, copyright, category, place, and resource type.

Expected:
- Draft is created.
- Draft metadata updates correctly.
- Draft remains owned by the contributor.

### A5. Contributor file upload

1. Upload supported preview/media files to the draft.
2. Refresh the page.
3. Verify uploaded files are still linked to the resource.

Expected:
- File upload succeeds.
- File metadata persists.

### A6. Contributor submission for review

1. Submit the draft for review.
2. Check submission status in the resource history or detail view.

Expected:
- Resource moves to review state.
- Submission note is stored.
- History records are created.

### A7. Admin contributor request handling

1. Log in as admin.
2. Open contributor request pending list.
3. Review an application or inspect approved contributor list.

Expected:
- Admin can view contributor requests.
- Admin actions are restricted to admin users only.

### A8. Admin resource lifecycle

1. Open the admin resource list.
2. Archive an approved resource.
3. Unarchive the same resource.

Expected:
- Archive/unarchive actions succeed.
- Resource status changes are reflected correctly.

### A9. Admin classification management

1. Open categories, resource types, and tags management pages.
2. Verify active items are listed.
3. Perform one safe update if your release scope includes it.

Expected:
- Classification data loads.
- Admin can manage classifications.

### A10. Negative authorization checks

1. Try to open admin functions with a viewer account.
2. Try contributor functions with a viewer account.
3. Try admin-only data with a contributor account.

Expected:
- Access is denied.
- No privileged data is exposed.

### A11. Logout and re-login

1. Logout from each role.
2. Open a protected page directly.
3. Login again.

Expected:
- Logout invalidates the session.
- Protected pages require authentication again.

## 7. Acceptance Result Rule

Mark each case as:

- `Pass`
- `Fail`
- `Blocked`
- `N/A`

Recommended decision:

- `Accepted` if all critical cases pass and no blocker/high issues remain.
- `Rejected` if any critical case fails.

## 8. Evidence To Collect

For each case, capture:

1. Screenshot of the key UI state or API response.
2. Request/response details if the case is API-driven.
3. Error message if the case fails.
4. Defect ID if one is created.

## 9. Final Deliverables

At the end of acceptance testing, prepare:

1. Completed acceptance record.
2. Defect list with severity and owner.
3. Final sign-off decision.
4. Short summary of what passed and what blocked release.
