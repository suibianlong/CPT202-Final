import http from 'k6/http';
import { check, fail, group } from 'k6';

const BASE_URL = sanitizeBaseUrl(__ENV.BASE_URL || 'http://localhost:8080');
const VIEWER_EMAIL = __ENV.VIEWER_EMAIL || 'viewer@heritage.local';
const VIEWER_PASSWORD = __ENV.VIEWER_PASSWORD || 'Viewer123!';
const CONTRIBUTOR_EMAIL = __ENV.CONTRIBUTOR_EMAIL || 'contributor@heritage.local';
const CONTRIBUTOR_PASSWORD = __ENV.CONTRIBUTOR_PASSWORD || 'Contributor123!';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'admin@heritage.local';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'Admin123!';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  vus: 1,
  iterations: 1,
  insecureSkipTLSVerify: envBool('INSECURE_TLS', false),
  thresholds: {
    http_req_failed: ['rate<0.001'],
    checks: ['rate=1'],
  },
};

export default function () {
  group('auth + viewer critical flow', function () {
    const anonMeRes = http.get(`${BASE_URL}/api/auth/me`, requestParams('auth_me_anon'));
    assertCheck(anonMeRes, {
      'anon /auth/me should be 401': (r) => r.status === 401,
    }, 'anonymous me check failed');

    const viewerLoginRes = login(VIEWER_EMAIL, VIEWER_PASSWORD, 'auth_login_viewer');
    assertCheck(viewerLoginRes, {
      'viewer login status 200': (r) => r.status === 200,
      'viewer login has userId': (r) => numericField(r, 'userId'),
    }, 'viewer login failed');

    const viewerMeRes = http.get(`${BASE_URL}/api/auth/me`, requestParams('auth_me_viewer'));
    assertCheck(viewerMeRes, {
      'viewer /auth/me status 200': (r) => r.status === 200,
      'viewer /auth/me role exists': (r) => stringField(r, 'role'),
    }, 'viewer me failed');

    const viewerResourcesRes = http.get(`${BASE_URL}/api/viewer/resources`, requestParams('viewer_resources'));
    assertCheck(viewerResourcesRes, {
      'viewer resources status 200': (r) => r.status === 200,
      'viewer resources is array': (r) => Array.isArray(safeJson(r)),
    }, 'viewer resources failed');

    const resources = safeJson(viewerResourcesRes);
    if (!Array.isArray(resources) || resources.length === 0) {
      fail('No approved resource available for regression. Seed at least one approved resource.');
    }
    const resourceId = resources[0].id;
    if (typeof resourceId !== 'number') {
      fail('Resource id is missing or invalid in /api/viewer/resources response.');
    }

    const viewerResourceDetailRes = http.get(
      `${BASE_URL}/api/viewer/resources/${resourceId}`,
      requestParams('viewer_resource_detail')
    );
    assertCheck(viewerResourceDetailRes, {
      'viewer detail status 200': (r) => r.status === 200,
      'viewer detail id matches': (r) => {
        const body = safeJson(r);
        return body !== null && body.id === resourceId;
      },
    }, 'viewer resource detail failed');

    const createCommentPayload = JSON.stringify({
      content: `k6 regression comment ${Date.now()}`,
    });

    const createCommentRes = http.post(
      `${BASE_URL}/api/viewer/resources/${resourceId}/comments`,
      createCommentPayload,
      {
        headers: JSON_HEADERS,
        tags: { endpoint: 'viewer_comment_create' },
      }
    );

    let commentId = null;
    assertCheck(createCommentRes, {
      'viewer create comment status 201': (r) => r.status === 201,
      'viewer create comment id exists': (r) => {
        const body = safeJson(r);
        if (body !== null && typeof body.id === 'number') {
          commentId = body.id;
          return true;
        }
        return false;
      },
    }, 'viewer create comment failed');

    const listCommentsRes = http.get(
      `${BASE_URL}/api/viewer/resources/${resourceId}/comments`,
      requestParams('viewer_comment_list')
    );
    assertCheck(listCommentsRes, {
      'viewer list comments status 200': (r) => r.status === 200,
      'viewer list comments is array': (r) => Array.isArray(safeJson(r)),
      'viewer list comments includes created comment': (r) => {
        const items = safeJson(r);
        if (!Array.isArray(items)) {
          return false;
        }
        return items.some((item) => item && item.id === commentId);
      },
    }, 'viewer list comments failed');

    const deleteCommentRes = http.del(
      `${BASE_URL}/api/viewer/resources/${resourceId}/comments/${commentId}`,
      null,
      requestParams('viewer_comment_delete')
    );
    assertCheck(deleteCommentRes, {
      'viewer delete comment status 204': (r) => r.status === 204,
    }, 'viewer delete comment failed');

    const viewerLogoutRes = http.post(`${BASE_URL}/api/auth/logout`, null, requestParams('auth_logout_viewer'));
    assertCheck(viewerLogoutRes, {
      'viewer logout status 204': (r) => r.status === 204,
    }, 'viewer logout failed');
  });

  group('contributor critical flow', function () {
    const contributorLoginRes = login(CONTRIBUTOR_EMAIL, CONTRIBUTOR_PASSWORD, 'auth_login_contributor');
    assertCheck(contributorLoginRes, {
      'contributor login status 200': (r) => r.status === 200,
      'contributor flag true': (r) => {
        const body = safeJson(r);
        return body !== null && body.contributor === true;
      },
    }, 'contributor login failed');

    const createDraftRes = http.post(
      `${BASE_URL}/api/contributor/resources`,
      null,
      requestParams('contributor_create_draft')
    );

    let draftId = null;
    assertCheck(createDraftRes, {
      'create draft status 200': (r) => r.status === 200,
      'create draft id exists': (r) => {
        const body = safeJson(r);
        if (body !== null && typeof body.id === 'number') {
          draftId = body.id;
          return true;
        }
        return false;
      },
    }, 'create contributor draft failed');

    const categoryOptionsRes = http.get(
      `${BASE_URL}/api/contributor/resources/category-options`,
      requestParams('contributor_category_options')
    );
    assertCheck(categoryOptionsRes, {
      'category options status 200': (r) => r.status === 200,
      'category options is non-empty array': (r) => {
        const body = safeJson(r);
        return Array.isArray(body) && body.length > 0;
      },
    }, 'category options failed');

    const resourceTypeOptionsRes = http.get(
      `${BASE_URL}/api/contributor/resources/resource-type-options`,
      requestParams('contributor_resource_type_options')
    );
    assertCheck(resourceTypeOptionsRes, {
      'resource type options status 200': (r) => r.status === 200,
      'resource type options is non-empty array': (r) => {
        const body = safeJson(r);
        return Array.isArray(body) && body.length > 0;
      },
    }, 'resource type options failed');

    const tagOptionsRes = http.get(
      `${BASE_URL}/api/contributor/resources/tag-options`,
      requestParams('contributor_tag_options')
    );
    assertCheck(tagOptionsRes, {
      'tag options status 200': (r) => r.status === 200,
      'tag options is non-empty array': (r) => {
        const body = safeJson(r);
        return Array.isArray(body) && body.length > 0;
      },
    }, 'tag options failed');

    const categoryOptions = safeJson(categoryOptionsRes);
    const typeOptions = safeJson(resourceTypeOptionsRes);
    const tagOptions = safeJson(tagOptionsRes);

    if (!Array.isArray(categoryOptions) || categoryOptions.length === 0) {
      fail('No category options available for contributor regression test.');
    }
    if (!Array.isArray(typeOptions) || typeOptions.length === 0) {
      fail('No resource type options available for contributor regression test.');
    }

    const updatePayload = {
      title: `k6-regression-draft-${Date.now()}`,
      description: 'Updated by automated regression test',
      copyright: 'Automated regression test data',
      categoryId: categoryOptions[0].id,
      place: 'Suzhou',
      resourceType: typeOptions[0].name,
      tagIds: Array.isArray(tagOptions) && tagOptions.length > 0 ? [tagOptions[0].id] : [],
    };

    const updateDraftRes = http.put(
      `${BASE_URL}/api/contributor/resources/${draftId}`,
      JSON.stringify(updatePayload),
      {
        headers: JSON_HEADERS,
        tags: { endpoint: 'contributor_update_draft' },
      }
    );

    assertCheck(updateDraftRes, {
      'update draft status 200': (r) => r.status === 200,
      'update draft title matches': (r) => {
        const body = safeJson(r);
        return body !== null && body.title === updatePayload.title;
      },
      'update draft category matches': (r) => {
        const body = safeJson(r);
        return body !== null && body.categoryId === updatePayload.categoryId;
      },
    }, 'update contributor draft failed');

    const myResourcesRes = http.get(`${BASE_URL}/api/contributor/resources/my`, requestParams('contributor_my_resources'));
    assertCheck(myResourcesRes, {
      'my resources status 200': (r) => r.status === 200,
      'my resources includes new draft': (r) => {
        const items = safeJson(r);
        if (!Array.isArray(items)) {
          return false;
        }
        return items.some((item) => item && item.id === draftId);
      },
    }, 'contributor my resources failed');

    const draftDetailRes = http.get(
      `${BASE_URL}/api/contributor/resources/${draftId}`,
      requestParams('contributor_draft_detail')
    );
    assertCheck(draftDetailRes, {
      'draft detail status 200': (r) => r.status === 200,
      'draft detail id matches': (r) => {
        const body = safeJson(r);
        return body !== null && body.id === draftId;
      },
    }, 'contributor draft detail failed');

    const contributorLogoutRes = http.post(
      `${BASE_URL}/api/auth/logout`,
      null,
      requestParams('auth_logout_contributor')
    );
    assertCheck(contributorLogoutRes, {
      'contributor logout status 204': (r) => r.status === 204,
    }, 'contributor logout failed');
  });

  group('admin critical flow', function () {
    const adminLoginRes = login(ADMIN_EMAIL, ADMIN_PASSWORD, 'auth_login_admin');
    assertCheck(adminLoginRes, {
      'admin login status 200': (r) => r.status === 200,
      'admin role is administrator': (r) => {
        const body = safeJson(r);
        return body !== null && body.role === 'ADMINISTRATOR';
      },
    }, 'admin login failed');

    const adminPendingRes = http.get(
      `${BASE_URL}/api/admin/contributor-requests/pending`,
      requestParams('admin_contributor_pending')
    );
    assertCheck(adminPendingRes, {
      'admin pending requests status 200': (r) => r.status === 200,
      'admin pending requests is array': (r) => Array.isArray(safeJson(r)),
    }, 'admin pending contributor requests failed');

    const adminCategoriesRes = http.get(`${BASE_URL}/api/admin/categories`, requestParams('admin_categories'));
    assertCheck(adminCategoriesRes, {
      'admin categories status 200': (r) => r.status === 200,
      'admin categories is array': (r) => Array.isArray(safeJson(r)),
    }, 'admin categories failed');

    const adminTypesRes = http.get(`${BASE_URL}/api/admin/resource-types`, requestParams('admin_resource_types'));
    assertCheck(adminTypesRes, {
      'admin resource types status 200': (r) => r.status === 200,
      'admin resource types is array': (r) => Array.isArray(safeJson(r)),
    }, 'admin resource types failed');

    const adminTagsRes = http.get(`${BASE_URL}/api/admin/tags`, requestParams('admin_tags'));
    assertCheck(adminTagsRes, {
      'admin tags status 200': (r) => r.status === 200,
      'admin tags is array': (r) => Array.isArray(safeJson(r)),
    }, 'admin tags failed');

    const adminOperationHistoryRes = http.get(
      `${BASE_URL}/api/admin/operation-history`,
      requestParams('admin_operation_history')
    );
    assertCheck(adminOperationHistoryRes, {
      'admin operation history status 200': (r) => r.status === 200,
      'admin operation history is array': (r) => Array.isArray(safeJson(r)),
    }, 'admin operation history failed');

    const adminLogoutRes = http.post(`${BASE_URL}/api/auth/logout`, null, requestParams('auth_logout_admin'));
    assertCheck(adminLogoutRes, {
      'admin logout status 204': (r) => r.status === 204,
    }, 'admin logout failed');
  });
}

function login(email, password, endpointTag) {
  return http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    {
      headers: JSON_HEADERS,
      tags: { endpoint: endpointTag },
    }
  );
}

function assertCheck(response, conditions, failMessage) {
  const passed = check(response, conditions);
  if (!passed) {
    fail(`${failMessage}. status=${response.status} body=${truncate(response.body, 280)}`);
  }
}

function numericField(response, fieldName) {
  const body = safeJson(response);
  return body !== null && typeof body[fieldName] === 'number';
}

function stringField(response, fieldName) {
  const body = safeJson(response);
  return body !== null && typeof body[fieldName] === 'string' && body[fieldName].length > 0;
}

function safeJson(response) {
  try {
    return response.json();
  } catch (error) {
    return null;
  }
}

function truncate(text, maxLength) {
  if (typeof text !== 'string') {
    return '';
  }
  if (text.length <= maxLength) {
    return text;
  }
  return `${text.slice(0, maxLength)}...`;
}

function sanitizeBaseUrl(url) {
  return String(url).replace(/\/+$/, '');
}

function envBool(name, fallback) {
  const raw = __ENV[name];
  if (raw === undefined || raw === null || raw === '') {
    return fallback;
  }

  const lowered = String(raw).toLowerCase();
  return lowered === '1' || lowered === 'true' || lowered === 'yes' || lowered === 'y';
}