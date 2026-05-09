# Frontend Tests Explained

## 1. What This Frontend Test Suite Does

The frontend tests in this project use `Jest + jsdom`, with the goal of verifying:

* Whether page scripts work as expected under normal flows.
* Whether boundary inputs (empty values, missing fields, missing DOM, cancelled operations) are handled correctly.
* Whether exceptional flows (API failure, permission failure) provide correct prompts or redirects.
* Whether output content is safe (such as `escapeHtml` preventing injection).
* Whether key business flows on the admin side and user side can be regression-tested.

---

## 2. Current Test File Structure

```text
frontend/tests/
  module1/module1.test.js
  module2/admin-approval.test.js
  module3/module3.test.js
  module5/review-approval.test.js
  module6/module6.test.js
  module7/admin-dashboard.test.js
  module7/admin-resources.test.js
  module7/classification-management.test.js
  module7/module7.test.js
  module7/tag-management.test.js
  shared/shared.test.js
  test-utils/eval-with-coverage.js
```

---

## 3. Specific Verification Content of Each Test File

### `tests/shared/shared.test.js`

* Query parameter reading: `getQueryMessage`
* Network request wrapper: `requestJson` (success, non-JSON, network exception, business exception)
* Unified prompts: `showToast` / `showMessageFromQuery`
* Logout event binding: `bindLogoutButtons`
* Common utility functions: `escapeHtml`, `formatDateTime`, `setText`, `setValue`

### `tests/module1/module1.test.js` (Login / Registration / Account)

* Post-login redirect path sanitisation and whitelist validation
* Enum status text and account status text generation
* Registration verification code cooldown countdown
* Admin pending review list loading and rendering
* Home page login status card rendering
* Account page contributor status area rendering
* Key initialisation flows for the registration page and login page (including validation and exception prompts)

### `tests/module2/admin-approval.test.js` (Admin Contributor Approval)

* Approval page tab switching and event binding
* Pending approval list and approved list loading
* Detail area rendering
* Approval/rejection submission flow
* Contributor role revocation flow
* Common exceptional branches (cancellation, API failure, empty data)

### `tests/module3/module3.test.js` (Resource Editing Workspace)

* Tag, category, and resource type normalisation and deduplication
* Dropdown option rendering and fallback logic
* Category/resource type loading flow
* Metadata saving, auto-saving, and review submission flow
* File upload trigger chain
* Page session information and query parameter parsing

### `tests/module5/review-approval.test.js` (Resource Review)

* Pending resource list loading and card rendering
* Resource detail, media area, and review history rendering
* Approval/rejection submission
* Normal and exceptional branches (empty list, API failure, cancellation)

### `tests/module6/module6.test.js` (Viewer Browsing and Feedback)

* Viewer-side filter option loading and filter reset
* Approved resource list and detail rendering
* Comment submission, comment deletion, and comment error display
* Feedback history loading and rendering
* Resource tag/media preview rendering
* Authentication and unified error handling branches

### `tests/module7/module7.test.js` (Admin Common Layer)

* `bindAdminBasics` logout binding configuration
* `requireAdmin` authentication logic
* 401 scenario redirecting to login with `next`
* Access prompt rendering for non-admin/exception scenarios
* Status labels, empty-row templates, and error message utility functions
* `jsonRequest` request header assembly logic

### `tests/module7/admin-dashboard.test.js`

* `DOMContentLoaded` initialisation flow
* Admin welcome text rendering
* Early return when there is no user
* No error thrown when target DOM nodes are missing

### `tests/module7/admin-resources.test.js`

* Resource status normalisation and filtering
* Resource list loading/rendering/empty state/error state
* Archive/unarchive actions
* Exceptional branches such as cancelled confirmation and API failure

### `tests/module7/classification-management.test.js`

* Resource type and category data loading
* Overview, usage history, and operation history rendering
* Add, edit, enable/disable flows
* Empty state, cancellation, and failure branch handling

### `tests/module7/tag-management.test.js`

* Tag data and history data loading
* List/overview/history rendering
* Add, edit, enable/disable flows
* Empty state and exceptional branch handling

---

## 4. Role of `test-utils/eval-with-coverage.js`

Many page scripts are not modularly exported, but run directly in the browser environment.
This utility performs coverage instrumentation before `window.eval`, so that the execution paths of these scripts can be correctly counted by Jest coverage.

---

## 5. How Frontend Tests Run in CI

In the `frontend-check` job of `.github/workflows/ci-cd.yml`, the frontend process includes:

* `npm ci`
* `npm run lint`
* `npm run format:check`
* `npm run test:coverage -- --runInBand --json --outputFile=jest-results.json`
* Summarising and outputting:

  * Statement coverage (Statements)
  * Branch coverage (Branches)
  * Average execution time per test case
  * Test pass rate
* Uploading the coverage report and test log artifact

---

## 6. How to Run Locally

```bash
cd frontend
npm ci
npm run lint
npm run format:check
npm run test -- --runInBand
npm run test:coverage -- --runInBand
```

---

## 7. Suggested Priority If Continuing to Add Tests

* First add tests for low-coverage branches in `module3.js` (long flows, many branches)
* Then add tests for exceptional paths and permission branches in `module6.js`
* For each new feature, add at the same time:

  * 1 normal path
  * 1 boundary path
  * 1 exceptional path
