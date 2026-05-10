# HerLink Acceptance Test Report

## Test Basic Information

| Item | Content |
|------|------|
| Test Date | 2026-05-07 |
| Start Time | 22:41 |
| End Time | 23:50 |
| Tester | Yan |
| Environment URL | http://47.99.122.227 |
| Build Version | d6f8a9a |

## Pre-Checks

| Check Item | Result (PASS/FAIL) | Remarks |
|--------|-----------------|------|
| Website is accessible | PASS | |
| Viewer1 can log in | PASS | |
| Viewer2 can log in | PASS | |
| Contributor can log in | PASS | |
| Admin can log in | PASS | |
| At least one approved resource exists | PASS | |

## PBI Acceptance Results Summary

| PBI | Priority | Corresponding Feature | Test Result | Failure Reason | Defect ID |
|-----|--------|---------|----------|----------|--------|
| PBI-01 | P1 | Registration/Login/Logout/Account Management | PASS | | |
| PBI-02 | P1 | Viewer applies to become Contributor | PASS | | |
| PBI-03 | P1 | Admin approves Contributor applications | PASS | | |
| PBI-04 | P1 | Contributor views own submissions | PASS | | |
| PBI-05 | P1 | Contributor creates resource draft | PASS | | |
| PBI-06 | P1 | Upload file and submit for review | PASS | | |
| PBI-08 | P1 | Admin views pending resources | PASS | | |
| PBI-09 | P1 | Admin approves/rejects resources | PASS | | |
| PBI-10 | P1 | Viewer browses approved resources | PASS | | |
| PBI-11 | P1 | Search/Filter/Sort | PASS | | |
| PBI-16 | P1 | Deployment and test support | PASS | Image loading slightly slow (approx 5-6 seconds), red console errors present but do not affect functionality | Logged as SUG-001 and BUG-001 |
| PBI-07 | P2 | Modify rejected resource and resubmit | PASS | | |
| PBI-12 | P2 | Viewer comments and feedback | PASS | | |
| PBI-13 | P2 | Admin manages comments and feedback | PASS | | |
| PBI-14 | P2 | Admin manages categories/types/tags | PASS | | |
| PBI-15 | P2 | Admin archives/unarchives resources | PASS | | |

## Defect List Found

| Defect ID | PBI Related | Title | Severity | Steps to Reproduce | Status |
|--------|---------|------|----------|----------|------|
| BUG-001 | PBI-16 | Red error messages appear in browser console | Medium | 1. Open website homepage http://47.99.122.227 2. Press F12 to open Developer Tools 3. Check Console tab 4. See red error messages | Pending fix |
| SUG-001 | PBI-16 | Image loading speed is slow, optimization recommended | Low (Performance Suggestion) | 1. Navigate to resource list page 2. Images take 5-6 seconds to fully display | Optimization suggested |

## Final Acceptance Conclusion

| P1 Pass Count | Total P1 | P1 Pass Rate |
|-----------|--------|----------|
| 11 | 11 | 100% |

- [x] **Pass** - All P1 acceptance criteria passed, ready for release
- [ ] **Conditional Pass** - All P1 passed but with P2 issues, log before release
- [ ] **Fail** - P1 issues exist, fix and re-accept

## Signatures

| Role | Name | Signature | Date |
|------|------|------|------|
| Tester | Yan | Yan | 2026-05-07 |

## Remarks

This acceptance test covers all 16 PBIs. All core P1 features have passed acceptance.

Two non-blocking issues found:
1. Red console errors (BUG-001), development team should investigate
2. Slightly slow image loading speed (SUG-001), recommended for future optimization

Recommendation: Ready for release, but it is suggested to fix the above two issues in the next iteration.
