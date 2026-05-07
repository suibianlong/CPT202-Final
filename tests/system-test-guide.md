# System Test Guide (HerLink)

This guide is for your deployed environment on Alibaba Cloud and focuses on the most critical system tests:

1. Smoke test (manual)
2. Critical API regression (automated)
3. Performance and stress test (automated)
4. Stability and recovery test (manual)
5. Basic security test (manual + tools)

The automated scripts are already prepared in this repo:

- `tests/regression/k6/critical-regression.js`
- `tests/performance/k6/load-test.js`

## 0. Tooling

Install k6 locally (on your own machine or CI runner):

- Windows (choco): `choco install k6`
- Verify: `k6 version`

If your API uses HTTPS with an internal/self-signed cert, add env var `INSECURE_TLS=true` when running tests.

---

## 1. Critical API regression (automated)

### What this tests

`tests/regression/k6/critical-regression.js` validates the most important end-to-end API paths:

- Auth boundary:
  - `GET /api/auth/me` unauthenticated returns `401`
  - Viewer login/logout and session check
- Viewer critical flow:
  - list approved resources
  - resource detail
  - create/list/delete comments
- Contributor critical flow:
  - login with contributor account
  - create draft
  - get category/type/tag options
  - update draft metadata
  - list my resources
  - get draft detail
- Admin critical flow:
  - login as admin
  - pending contributor requests
  - categories/resource-types/tags list
  - operation history list

The script fails immediately if any critical check fails.

### Required accounts/data

By default the script expects demo accounts from `DemoDataInitializer`:

- Admin: `admin@heritage.local / Admin123!`
- Contributor: `contributor@heritage.local / Contributor123!`
- Viewer: `viewer@heritage.local / Viewer123!`

And at least one approved resource (for viewer detail/comment flow).

### Run command

From repo root:

```powershell
$env:BASE_URL="https://<your-domain-or-ip>"
$env:VIEWER_EMAIL="viewer@heritage.local"
$env:VIEWER_PASSWORD="Viewer123!"
$env:CONTRIBUTOR_EMAIL="contributor@heritage.local"
$env:CONTRIBUTOR_PASSWORD="Contributor123!"
$env:ADMIN_EMAIL="admin@heritage.local"
$env:ADMIN_PASSWORD="Admin123!"
k6 run tests/regression/k6/critical-regression.js
```

Optional TLS skip:

```powershell
$env:INSECURE_TLS="true"
k6 run tests/regression/k6/critical-regression.js
```

### Pass/Fail rule

- Pass: all checks pass, `http_req_failed` near zero, script exits successfully.
- Fail: any core endpoint breaks or returns wrong status/structure.

---

## 2. Performance + stress test (automated)

### What this tests

`tests/performance/k6/load-test.js` runs high-value traffic against:

- login + `/api/auth/me`
- viewer resources list
- resource detail
- comments list
- optional write path (create/delete comment) when `ENABLE_WRITE_FLOW=true`

It supports 4 load profiles:

- `baseline`: constant normal load
- `ramp`: gradually increase load to find bottleneck
- `spike`: sudden traffic burst
- `soak`: long-duration stability (memory/connection leak detection)

### Fast path when your system blocks multi-login for same account

If your platform kicks previous sessions for the same account, do this first:

1. Log in once in browser as viewer.
2. Copy the `JSESSIONID` cookie value from DevTools.
3. Run k6 with `SHARED_SESSION_ID` so all VUs reuse one fixed session cookie.

Example:

```powershell
$env:BASE_URL="http://47.99.122.227"
$env:SHARED_SESSION_ID="<copied-jsessionid>"
$env:LOAD_PROFILE="baseline"
$env:BASELINE_VUS="30"
$env:BASELINE_DURATION="10m"
k6 run tests/performance/k6/load-test.js
```

Notes:

- In `SHARED_SESSION_ID` mode, script will skip login and directly use the shared session.
- This is the fastest way to do read-path stress without preparing hundreds of accounts.
- Prefer `ENABLE_WRITE_FLOW=false` in this mode to avoid creating too many comments.

### Common commands

#### Baseline (recommended first)

```powershell
$env:BASE_URL="https://<your-domain-or-ip>"
$env:LOAD_PROFILE="baseline"
$env:BASELINE_VUS="30"
$env:BASELINE_DURATION="10m"
$env:VIEWER_EMAIL="viewer@heritage.local"
$env:VIEWER_PASSWORD="Viewer123!"
k6 run tests/performance/k6/load-test.js
```

#### Ramp-up

```powershell
$env:BASE_URL="https://<your-domain-or-ip>"
$env:LOAD_PROFILE="ramp"
$env:RAMP_STAGE_1_TARGET="20"
$env:RAMP_STAGE_2_TARGET="80"
$env:RAMP_STAGE_3_TARGET="120"
k6 run tests/performance/k6/load-test.js
```

#### Spike

```powershell
$env:BASE_URL="https://<your-domain-or-ip>"
$env:LOAD_PROFILE="spike"
$env:SPIKE_STAGE_2_TARGET="180"
k6 run tests/performance/k6/load-test.js
```

#### Soak

```powershell
$env:BASE_URL="https://<your-domain-or-ip>"
$env:LOAD_PROFILE="soak"
$env:SOAK_VUS="50"
$env:SOAK_DURATION="2h"
k6 run tests/performance/k6/load-test.js
```

### Thresholds and gating

Default thresholds in script:

- `http_req_failed < 1%`
- global `p95(http_req_duration) < 800ms`
- checks pass rate `> 99%`

You can override by env vars:

- `MAX_HTTP_ERROR_RATE`
- `MAX_P95_MS`
- `MAX_LOGIN_P95_MS`
- `MAX_LIST_P95_MS`

Recommended release gate:

- P95 < 500ms for core read APIs
- Error rate < 1%
- no sustained CPU saturation

---

## 3. Three manual tests you still need to do

## 3.1 Smoke test (manual, each release)

### Objective

Validate the build is usable in 10-15 minutes.

### Steps

1. Open website home page and login page.
2. Login as viewer, verify profile (`/api/auth/me` in UI behavior).
3. Browse resource list, open one resource detail.
4. Post one comment and delete it.
5. Login as contributor, create draft, edit required metadata, attempt submit.
6. Login as admin, open contributor request list and operation history.
7. Logout and login again to verify session lifecycle.

### Record these fields

- Build version / commit id
- Test time window (start/end)
- Environment URL
- Test accounts used
- Each step: pass/fail + screenshot/short evidence
- Blocking issue id (if any)

---

## 3.2 Stability and recovery test (manual)

### Objective

Verify the system recovers from faults and does not corrupt state.

### Suggested scenarios

1. Restart backend container/service during active user traffic.
2. Restart database replica/read service (if you have one).
3. Restart Redis/message queue (if present).
4. Simulate short network interruption between app and DB/cache (30-60s).

### Steps (example: backend restart)

1. Start a small constant load (`baseline`, low VUs).
2. Record baseline latency/error for 5 minutes.
3. Restart backend service.
4. Observe for 10 minutes after restart.
5. Verify users can relogin and core actions still work.

### Record these fields

- Fault type and exact operation performed
- Fault start and recovery completion timestamps
- RTO (Recovery Time Objective): minutes to service normal
- During fault: peak error rate, max latency
- Data consistency checks:
  - duplicated comments?
  - lost drafts?
  - status rollback anomalies?
- Final result: pass/fail + residual risk

---

## 3.3 Basic security test (manual + tool)

### Objective

Catch high-risk common vulnerabilities quickly.

### Scope

1. Authentication/session:
   - no login should access protected APIs
   - logout invalidates session
2. Authorization:
   - viewer cannot call contributor/admin endpoints
   - contributor cannot call admin endpoints
3. Input validation:
   - SQL injection payloads in keyword/content fields
   - XSS payloads in comment/title/description
4. Sensitive data exposure:
   - response should not return password hash/internal stack traces

### How to run

1. Use browser + DevTools for role-switch and endpoint replay.
2. Use OWASP ZAP baseline scan against your domain.
3. Manually verify all high/medium findings.

### Record these fields

- Endpoint + method
- Test payload (e.g. `' OR '1'='1`, `<script>alert(1)</script>`)
- Expected vs actual behavior/status code
- Severity (Critical/High/Medium/Low)
- Repro steps and evidence
- Fix owner and due date

---

## 4. Recommended execution order (practical)

1. Run `critical-regression.js` first (fast go/no-go).
2. Run baseline load test.
3. If baseline passes, run ramp and spike.
4. Run soak overnight.
5. Perform manual smoke + recovery + security checks.
6. Compile final report and release decision.

---

## 5. Test report template (use this structure)

- Basic info:
  - date/time
  - environment URL
  - build/commit
  - tester
- Automated regression result:
  - total checks
  - pass rate
  - failed endpoints
- Performance result:
  - profile name
  - VUs/stages/duration
  - RPS
  - P50/P95/P99
  - error rate
  - server CPU/memory/DB connections
- Manual smoke result:
  - step pass/fail table
- Recovery result:
  - fault injection scenario
  - RTO
  - data consistency
- Security result:
  - finding list with severity
  - unresolved risks
- Final release recommendation:
  - `Go` / `No-Go`
  - must-fix items

---

## 6. CI/CD integration suggestion

At minimum, add one pipeline stage after deploy-to-test:

1. Run `critical-regression.js`
2. Run `load-test.js` baseline profile (5-10 min)
3. Fail pipeline when threshold fails

This gives you fast quality gating before production promotion.
