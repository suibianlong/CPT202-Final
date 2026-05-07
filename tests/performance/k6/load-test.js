import http from 'k6/http';
import exec from 'k6/execution';
import { check, group, sleep } from 'k6';

const BASE_URL = sanitizeBaseUrl(__ENV.BASE_URL || 'http://localhost:8080');
const VIEWER_EMAIL = __ENV.VIEWER_EMAIL || 'viewer@heritage.local';
const VIEWER_PASSWORD = __ENV.VIEWER_PASSWORD || 'Viewer123!';
const SHARED_SESSION_ID = (__ENV.SHARED_SESSION_ID || '').trim();
const USE_SHARED_SESSION = SHARED_SESSION_ID.length > 0;
const THINK_TIME_SECONDS = envNumber('THINK_TIME_SECONDS', 1);
const LOAD_PROFILE = (__ENV.LOAD_PROFILE || 'baseline').toLowerCase();
const KNOWN_RESOURCE_ID = envNumber('RESOURCE_ID', 0);
const ENABLE_WRITE_FLOW = envBool('ENABLE_WRITE_FLOW', false);

const JSON_HEADERS = { 'Content-Type': 'application/json' };

let vuLoggedIn = USE_SHARED_SESSION;
let vuResourceId = KNOWN_RESOURCE_ID > 0 ? KNOWN_RESOURCE_ID : null;

export const options = buildOptions(LOAD_PROFILE);

function buildOptions(profile) {
  return {
    summaryTimeUnit: 'ms',
    insecureSkipTLSVerify: envBool('INSECURE_TLS', false),
    scenarios: selectScenarios(profile),
    thresholds: {
      http_req_failed: [`rate<${envNumber('MAX_HTTP_ERROR_RATE', 0.01)}`],
      http_req_duration: [`p(95)<${envNumber('MAX_P95_MS', 800)}`],
      checks: [`rate>${envNumber('MIN_CHECK_RATE', 0.99)}`],
      'http_req_duration{endpoint:auth_login}': [`p(95)<${envNumber('MAX_LOGIN_P95_MS', 1200)}`],
      'http_req_duration{endpoint:viewer_list_resources}': [`p(95)<${envNumber('MAX_LIST_P95_MS', 1000)}`],
    },
  };
}

function selectScenarios(profile) {
  switch (profile) {
    case 'ramp':
      return {
        ramp: {
          executor: 'ramping-vus',
          startVUs: 0,
          stages: [
            { duration: __ENV.RAMP_STAGE_1_DURATION || '2m', target: envNumber('RAMP_STAGE_1_TARGET', 20) },
            { duration: __ENV.RAMP_STAGE_2_DURATION || '5m', target: envNumber('RAMP_STAGE_2_TARGET', 80) },
            { duration: __ENV.RAMP_STAGE_3_DURATION || '3m', target: envNumber('RAMP_STAGE_3_TARGET', 120) },
            { duration: __ENV.RAMP_STAGE_4_DURATION || '2m', target: 0 },
          ],
          gracefulRampDown: '30s',
        },
      };
    case 'spike':
      return {
        spike: {
          executor: 'ramping-vus',
          startVUs: envNumber('SPIKE_START_VUS', 10),
          stages: [
            { duration: __ENV.SPIKE_STAGE_1_DURATION || '1m', target: envNumber('SPIKE_STAGE_1_TARGET', 10) },
            { duration: __ENV.SPIKE_STAGE_2_DURATION || '20s', target: envNumber('SPIKE_STAGE_2_TARGET', 180) },
            { duration: __ENV.SPIKE_STAGE_3_DURATION || '2m', target: envNumber('SPIKE_STAGE_3_TARGET', 180) },
            { duration: __ENV.SPIKE_STAGE_4_DURATION || '1m', target: envNumber('SPIKE_STAGE_4_TARGET', 20) },
            { duration: __ENV.SPIKE_STAGE_5_DURATION || '40s', target: 0 },
          ],
          gracefulRampDown: '15s',
        },
      };
    case 'soak':
      return {
        soak: {
          executor: 'constant-vus',
          vus: envNumber('SOAK_VUS', 50),
          duration: __ENV.SOAK_DURATION || '2h',
          gracefulStop: '30s',
        },
      };
    case 'baseline':
    default:
      return {
        baseline: {
          executor: 'constant-vus',
          vus: envNumber('BASELINE_VUS', 30),
          duration: __ENV.BASELINE_DURATION || '10m',
          gracefulStop: '30s',
        },
      };
  }
}

export default function () {
  group('viewer_flow', function () {
    if (!vuLoggedIn) {
      vuLoggedIn = loginAsViewer();
      if (!vuLoggedIn) {
        return;
      }
    }

    const meRes = http.get(`${BASE_URL}/api/auth/me`, requestParams('auth_me'));
    check(meRes, {
      'me status is 200': (r) => r.status === 200,
      'me has userId': (r) => {
        const body = safeJson(r);
        return body !== null && typeof body.userId === 'number';
      },
    });

    const listRes = http.get(`${BASE_URL}/api/viewer/resources`, requestParams('viewer_list_resources'));
    let resources = null;
    check(listRes, {
      'list status is 200': (r) => r.status === 200,
      'list body is array': (r) => Array.isArray(safeJson(r)),
    });

    if (listRes.status === 200) {
      resources = safeJson(listRes);
      if (Array.isArray(resources) && resources.length > 0 && !vuResourceId) {
        vuResourceId = resources[0].id;
      }
    }

    if (vuResourceId) {
      const detailRes = http.get(
        `${BASE_URL}/api/viewer/resources/${vuResourceId}`,
        requestParams('viewer_resource_detail')
      );
      check(detailRes, {
        'detail status is 200': (r) => r.status === 200,
      });

      const commentsRes = http.get(
        `${BASE_URL}/api/viewer/resources/${vuResourceId}/comments`,
        requestParams('viewer_comments_list')
      );
      check(commentsRes, {
        'comments status is 200': (r) => r.status === 200,
        'comments body is array': (r) => Array.isArray(safeJson(r)),
      });

      if (ENABLE_WRITE_FLOW) {
        createAndDeleteComment(vuResourceId);
      }
    }
  });

  sleep(THINK_TIME_SECONDS);
}

function loginAsViewer() {
  const payload = JSON.stringify({
    email: VIEWER_EMAIL,
    password: VIEWER_PASSWORD,
  });

  const res = http.post(`${BASE_URL}/api/auth/login`, payload, {
    headers: JSON_HEADERS,
    tags: { endpoint: 'auth_login' },
  });

  return check(res, {
    'login status is 200': (r) => r.status === 200,
    'login response has userId': (r) => {
      const body = safeJson(r);
      return body !== null && typeof body.userId === 'number';
    },
  });
}

function createAndDeleteComment(resourceId) {
  const uniqueContent = `k6 load test comment ${exec.vu.idInTest}-${exec.vu.iterationInScenario}-${Date.now()}`;
  const createRes = http.post(
    `${BASE_URL}/api/viewer/resources/${resourceId}/comments`,
    JSON.stringify({ content: uniqueContent }),
    withSessionCookies({
      headers: JSON_HEADERS,
      tags: { endpoint: 'viewer_comment_create' },
    })
  );

  let createdCommentId = null;
  check(createRes, {
    'create comment status is 201': (r) => r.status === 201,
    'create comment has id': (r) => {
      const body = safeJson(r);
      if (body !== null && typeof body.id === 'number') {
        createdCommentId = body.id;
        return true;
      }
      return false;
    },
  });

  if (createdCommentId !== null) {
    const deleteRes = http.del(
      `${BASE_URL}/api/viewer/resources/${resourceId}/comments/${createdCommentId}`,
      null,
      requestParams('viewer_comment_delete')
    );
    check(deleteRes, {
      'delete comment status is 204': (r) => r.status === 204,
    });
  }
}

function requestParams(endpointTag) {
  return withSessionCookies({ tags: { endpoint: endpointTag } });
}

function withSessionCookies(params) {
  if (!USE_SHARED_SESSION) {
    return params;
  }

  const cookies = params.cookies || {};
  return {
    ...params,
    cookies: {
      ...cookies,
      JSESSIONID: { value: SHARED_SESSION_ID, replace: true },
    },
  };
}

function safeJson(response) {
  try {
    return response.json();
  } catch (error) {
    return null;
  }
}

function sanitizeBaseUrl(url) {
  return String(url).replace(/\/+$/, '');
}

function envNumber(name, fallback) {
  const raw = __ENV[name];
  if (raw === undefined || raw === null || raw === '') {
    return fallback;
  }

  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function envBool(name, fallback) {
  const raw = __ENV[name];
  if (raw === undefined || raw === null || raw === '') {
    return fallback;
  }

  const lowered = String(raw).toLowerCase();
  return lowered === '1' || lowered === 'true' || lowered === 'yes' || lowered === 'y';
}
