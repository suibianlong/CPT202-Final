# Stability and Recovery Test Record (HerLink)

## 1. Test Metadata

| Field | Value |
|---|---|
| Project | HerLink |
| Environment | http://47.99.122.227 |
| Test Date | 2026.5.7 |
| Tester | Yan |
| Build Version / Commit ID | d6f8a9a |

---

## 2. Test Environment

| Component | Container Name | Image |
|---|---|---|
| Application | herlink-app | ghcr.io/jiafei88853/herlink-app:sha-d6f8a9a |
| Web Server | herlink-nginx | nginx:stable-alpine |
| Database | herlink-mysql | mysql:8.4 |

---

## 3. Baseline State (Before Fault Injection)

| Check Item | Value | Notes |
|---|---|---|
| Viewer can login | Yes | |
| Resource list loads | Yes | |
| Comment CRUD works | Yes | |
| Contributor can create draft | Yes | |
| Admin pages accessible | Yes | |

---

## 4. Fault Injection Scenarios

### Scenario 1: Backend Container Restart

| Field | Value |
|---|---|
| **Fault Type** | Application container restart |
| **Command Executed** | `docker restart herlink-app` |
| **Fault Start Time** | 05:55:17 |
| **Fault End Time (container Up)** | 05:55:27 |
| **Full Recovery Time (page functional)** | 05:55:39 |
| **RTO (Recovery Time Objective)** | 22s |
| **Peak Error Rate During Fault** | Partial requests return 502 (when accessing a new page). |
| **Error Type Observed** | 502 Bad Gateway / Connection Refused / Other |

**Post-Recovery Checks:**

| Check Item | Before Fault | After Recovery | Match? | Notes |
|---|---|---|---|---|
| Resource count (Viewer) | 7 | 7 | Yes | |
| Comment count (same resource) | 1 | 1 | Yes | |
| Duplicate comments? | N/A | | No | |
| Can create new comment? | N/A | Yes | Yes/No | |
| Can create new draft? | N/A | Yes | Yes/No | |
| Login required again? | N/A | Yes | N/A | |

**Result:** Pass

**Notes:**

---

### Scenario 2: Database Container Restart

| Field | Value |
|---|---|
| **Fault Type** | Database container restart |
| **Command Executed** | `docker restart herlink-mysql` |
| **Fault Start Time** | 06:14:42 |
| **Fault End Time (container Up)** | 06:14:48 |
| **Full Recovery Time (page functional)** | 06:14:56 |
| **RTO (Recovery Time Objective)** | 10s |
| **Peak Error Rate During Fault** | Some requests failed (Metadata loading failed) |
| **Error Type Observed** | "Resource types failed to load. Metadata save and submit are temporarily disabled." |

**Post-Recovery Checks:**

| Check Item | Before Fault | After Recovery | Match? | Notes |
|---|---|---|---|---|
| Resource count (Viewer) | 7 | 7 | Yes | |
| Comment count | 1 | 1 | Yes | |
| Draft count (Contributor) | 12 | 12 | Yes | |
| Draft content intact? | Suzhou Gardens | Suzhou Gardens | Yes | ID45 |
| Can login? | N/A | Yes | N/A | |
| Can write new data? | N/A | Yes | N/A |  |
| Can read existing data? | N/A | Yes | N/A | |

**Result:** Pass

**Notes:** The database will resume operation approximately 10 seconds after restarting. During this period, the metadata interface will be temporarily unavailable, and the page will display a friendly prompt message. After recovery, the number and content of drafts will remain the same, with normal read and write operations and no data loss or damage.

---

### Scenario 3: Nginx Container Restart

| Field | Value |
|---|---|
| **Fault Type** | Nginx container restart |
| **Command Executed** | `docker restart herlink-nginx` |
| **Fault Start Time** | 7 06:25:05 |
| **Fault End Time (container Up)** | 06:25:11 |
| **Full Recovery Time (page functional)** | 06:25:15 |
| **RTO (Recovery Time Objective)** | 10秒 |
| **Peak Error Rate During Fault** | 100% |
| **Error Type Observed** | Connection Refused |

**Post-Recovery Checks:**

| Check Item | After Recovery | Result |
|---|---|---|
| Home page loads | Yes | Pass |
| Static assets load (images, CSS, JS) | Yes | Pass |
| API requests pass through to backend | Yes | Pass |
| Login works | Yes | Pass |

**Result:** Pass

**Notes:** Nginx fully recovers about 10 seconds after restarting. During the fault, the site was completely inaccessible. After recovery, all functions are normal, and static resources are loaded normally.

---

### Scenario 4: Network Interruption

| Field | Value |
|---|---|
| **Fault Type** | Network disconnect between app and database |
| **Command Executed** | `docker network disconnect cpt202-final_herlink-network herlink-app` / `docker network connect cpt202-final_herlink-network herlink-app` |
| **Interruption Duration** | 30s |
| **Fault Start Time** | 06:34:47 |
| **Fault End Time** | 06:35:17 |
| **RTO** | 5秒 |

**Post-Recovery Checks:**

| Check Item | After Recovery | Result |
|---|---|---|
| Data integrity (no corruption) | Yes | Pass |
| Login works | Yes | Pass |
| CRUD operations normal | Yes | Pass |

**Result:** Pass

**Notes:** After disconnecting from the network, all requests return a "502 Bad Gateway" error. After reconnecting, the system fully recovers within approximately 5 seconds. There is no need to log in again, and the data remains intact.

## 5. Summary

| Scenario | RTO | Data Consistency | Result |
|---|---|---|---|
| Backend Container Restart | 20s | Pass | Pass |
| Database Container Restart | 10s | Pass | Pass |
| Nginx Container Restart | 10s | Pass | Pass |
| Network Interruption | 5s | Pass| Pass |

---

## 6. Issues Found

| Issue ID | Scenario | Description | Severity | Status |
|---|---|---|---|---|
| no | | | | |

---

## 7. Final Result

- Overall Result: **Pass**
- Residual Risk: **Low**
- Notes: 
    - All three container restart scenarios recovered within 20 seconds, showing a good RTO performance.
    - There was no data loss, no data duplication, and no data corruption in all scenarios.
    - The error prompts were user - friendly when the database was restarted, and the user experience was acceptable.
    - The current deployment is a single - point architecture (App/MySQL/Nginx are all single - instances). It is recommended to consider redundant deployment in the production environment.
    - The network interruption scenario was not tested (due to insufficient permissions). It is recommended that the operation and maintenance team conduct supplementary tests later.
