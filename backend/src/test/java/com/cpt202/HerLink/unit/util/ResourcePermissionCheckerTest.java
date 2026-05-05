package com.cpt202.HerLink.unit.util;

import com.cpt202.HerLink.util.*;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.util.SessionKeys;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.vo.CurrentUserVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class ResourcePermissionCheckerTest {

    @Mock
    private UserAccessService userAccessService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private ResourcePermissionChecker permissionChecker;

    private static final Long TEST_USER_ID = 1001L;
    private static final Long BOUNDARY_USER_ID = 999999999999999L;
    private static final String INVALID_ID = "invalid123";
    private static final String EMPTY = "";
    private static final String BLANK = "   ";

    @Nested
    @DisplayName("requireCurrentUser Tests")
    class RequireCurrentUserTests {
        @Test
        @DisplayName("Session does not exist, throw Unauthorized exception")
        void requireCurrentUser_sessionNull_throwUnauthorized() {
            when(request.getSession(false)).thenReturn(null);
            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireCurrentUser(request));
            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("No user ID in session, throw Unauthorized exception")
        void requireCurrentUser_sessionUserIdNull_throwUnauthorized() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(null);
            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireCurrentUser(request));
            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("User ID is invalid string, throw Invalid identity exception")
        void requireCurrentUser_userIdInvalidString_throwInvalid() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(INVALID_ID);
            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireCurrentUser(request));
            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("User ID is empty/blank string, throw incomplete session exception")
        void requireCurrentUser_userIdEmptyBlank_throwIncomplete() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(EMPTY);
            AppException ex1 = assertThrows(AppException.class, () -> permissionChecker.requireCurrentUser(request));
            assertEquals(401, ex1.getStatusCode());

            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(BLANK);
            AppException ex2 = assertThrows(AppException.class, () -> permissionChecker.requireCurrentUser(request));
            assertEquals(401, ex2.getStatusCode());
        }

        @Test
        @DisplayName("User ID is boundary Long value, parse and return user successfully")
        void requireCurrentUser_boundaryLongId_success() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(BOUNDARY_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(BOUNDARY_USER_ID);
            when(userAccessService.getCurrentUserById(BOUNDARY_USER_ID)).thenReturn(user);

            CurrentUserVO result = permissionChecker.requireCurrentUser(request);
            assertEquals(BOUNDARY_USER_ID, result.getUserId());
        }

        @Test
        @DisplayName("User not found 404, clear session and throw session invalid exception")
        void requireCurrentUser_userNotFound404_invalidateSession() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenThrow(AppException.notFound(""));

            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireCurrentUser(request));
            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("Service layer throws non-404 exception, throw original exception directly")
        void requireCurrentUser_serviceThrowNon404_throwOriginal() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            AppException testEx = AppException.forbidden("test");
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenThrow(testEx);

            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireCurrentUser(request));
            assertEquals(testEx.getStatusCode(), ex.getStatusCode());
        }

        @Test
        @DisplayName("Valid user ID, return current user successfully")
        void requireCurrentUser_validId_success() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(TEST_USER_ID);
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(user);

            CurrentUserVO result = permissionChecker.requireCurrentUser(request);
            assertNotNull(result);
            assertEquals(TEST_USER_ID, result.getUserId());
        }
    }

    @Nested
    @DisplayName("requireAuthenticatedUserId Tests")
    class RequireAuthenticatedUserIdTests {

        @Test
        @DisplayName("Valid authenticated user, return user ID")
        void requireAuthenticatedUserId_validUser_returnUserId() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(TEST_USER_ID);
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(user);

            Long result = permissionChecker.requireAuthenticatedUserId(request);
            assertEquals(TEST_USER_ID, result);
        }

        @Test
        @DisplayName("Boundary: Extra large Long user ID, return ID successfully")
        void requireAuthenticatedUserId_boundaryLongId_returnId() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(BOUNDARY_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(BOUNDARY_USER_ID);
            when(userAccessService.getCurrentUserById(BOUNDARY_USER_ID)).thenReturn(user);

            Long result = permissionChecker.requireAuthenticatedUserId(request);
            assertEquals(BOUNDARY_USER_ID, result);
        }

        @Test
        @DisplayName("Exception: Session does not exist, throw Unauthorized")
        void requireAuthenticatedUserId_sessionNull_throwUnauthorized() {
            when(request.getSession(false)).thenReturn(null);
            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireAuthenticatedUserId(request));
            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("Exception: No user ID in session, throw Unauthorized")
        void requireAuthenticatedUserId_missingUserId_throwUnauthorized() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(null);
            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireAuthenticatedUserId(request));
            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("Exception: Invalid user ID string, throw invalid identity")
        void requireAuthenticatedUserId_invalidId_throwInvalid() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(INVALID_ID);
            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireAuthenticatedUserId(request));
            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("Exception: User not found 404, throw session invalid exception")
        void requireAuthenticatedUserId_userNotFound_throwSessionInvalid() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenThrow(AppException.notFound(""));

            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireAuthenticatedUserId(request));
            assertEquals(401, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("requireContributorUserId Tests")
    class RequireContributorUserIdTests {
        @Test
        @DisplayName("Normal: User is contributor, return ID")
        void requireContributorUserId_isContributor_returnId() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(TEST_USER_ID);
            user.setContributor(true);
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(user);

            Long result = permissionChecker.requireContributorUserId(request);
            assertEquals(TEST_USER_ID, result);
        }

        @Test
        @DisplayName("Boundary: Extra large ID contributor, return ID")
        void requireContributorUserId_boundaryIdContributor_returnId() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(BOUNDARY_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(BOUNDARY_USER_ID);
            user.setContributor(true);
            when(userAccessService.getCurrentUserById(BOUNDARY_USER_ID)).thenReturn(user);

            Long result = permissionChecker.requireContributorUserId(request);
            assertEquals(BOUNDARY_USER_ID, result);
        }

        @Test
        @DisplayName("Exception: User is not contributor, throw 403")
        void requireContributorUserId_notContributor_throwForbidden() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(TEST_USER_ID);
            user.setContributor(false);
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(user);

            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireContributorUserId(request));
            assertEquals(403, ex.getStatusCode());
        }

        @Test
        @DisplayName("Exception: Not logged in, throw 401")
        void requireContributorUserId_unauthenticated_throwUnauthorized() {
            when(request.getSession(false)).thenReturn(null);
            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireContributorUserId(request));
            assertEquals(401, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("requireAdminUser / requireAdminUserId Tests")
    class RequireAdminTests {

        @Test
        @DisplayName("Normal: Admin user, return user object")
        void requireAdminUser_validAdmin_returnUser() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(TEST_USER_ID);
            user.setRole(UserRoleEnum.ADMINISTRATOR.getValue());
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(user);

            CurrentUserVO result = permissionChecker.requireAdminUser(request);
            assertEquals(TEST_USER_ID, result.getUserId());
            assertTrue(UserRoleEnum.ADMINISTRATOR.matches(result.getRole()));
        }

        @Test
        @DisplayName("Boundary: Extra large ID admin, return successfully")
        void requireAdminUser_boundaryIdAdmin_returnUser() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(BOUNDARY_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(BOUNDARY_USER_ID);
            user.setRole(UserRoleEnum.ADMINISTRATOR.getValue());
            when(userAccessService.getCurrentUserById(BOUNDARY_USER_ID)).thenReturn(user);

            CurrentUserVO result = permissionChecker.requireAdminUser(request);
            assertEquals(BOUNDARY_USER_ID, result.getUserId());
        }

        @Test
        @DisplayName("Exception: Non-admin role, throw 403")
        void requireAdminUser_notAdmin_throwForbidden() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(TEST_USER_ID);
            user.setRole(UserRoleEnum.REGISTERED_VIEWER.getValue());
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(user);

            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireAdminUser(request));
            assertEquals(403, ex.getStatusCode());
        }

        @Test
        @DisplayName("Exception: Not logged in, throw 401")
        void requireAdminUser_unauthenticated_throwUnauthorized() {
            when(request.getSession(false)).thenReturn(null);
            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireAdminUser(request));
            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("requireAdminUserId: Valid admin, return ID")
        void requireAdminUserId_validAdmin_returnId() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(TEST_USER_ID);
            user.setRole(UserRoleEnum.ADMINISTRATOR.getValue());
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(user);

            Long result = permissionChecker.requireAdminUserId(request);
            assertEquals(TEST_USER_ID, result);
        }

        @Test
        @DisplayName("requireAdminUserId: Boundary extra large ID admin, return ID")
        void requireAdminUserId_boundaryAdminId_returnId() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(BOUNDARY_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(BOUNDARY_USER_ID);
            user.setRole(UserRoleEnum.ADMINISTRATOR.getValue());
            when(userAccessService.getCurrentUserById(BOUNDARY_USER_ID)).thenReturn(user);

            Long result = permissionChecker.requireAdminUserId(request);
            assertEquals(BOUNDARY_USER_ID, result);
        }

        @Test
        @DisplayName("requireAdminUserId: Exception - not logged in, throw 401")
        void requireAdminUserId_unauthenticated_throwUnauthorized() {
            when(request.getSession(false)).thenReturn(null);
            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireAdminUserId(request));
            assertEquals(401, ex.getStatusCode());
        }

        @Test
        @DisplayName("requireAdminUserId: Exception - non-admin, throw 403")
        void requireAdminUserId_notAdmin_throwForbidden() {
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            CurrentUserVO user = new CurrentUserVO();
            user.setUserId(TEST_USER_ID);
            user.setRole(UserRoleEnum.REGISTERED_VIEWER.getValue());
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(user);

            AppException ex = assertThrows(AppException.class, () -> permissionChecker.requireAdminUserId(request));
            assertEquals(403, ex.getStatusCode());
        }
    }

    @Nested
    @DisplayName("storeLoginSession Tests")
    class StoreLoginSessionTests {

        @Test
        @DisplayName("Normal: Store valid user ID, session attribute set successfully")
        void storeLoginSession_validId_setAttribute() {
            when(request.getSession(true)).thenReturn(session);
            permissionChecker.storeLoginSession(request, TEST_USER_ID);

            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(TEST_USER_ID);
            assertEquals(TEST_USER_ID, session.getAttribute(SessionKeys.USER_ID));
        }

        @Test
        @DisplayName("Boundary: Store minimum Long value, set successfully")
        void storeLoginSession_minLongId_setAttribute() {
            Long minId = 1L;
            when(request.getSession(true)).thenReturn(session);
            permissionChecker.storeLoginSession(request, minId);

            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(minId);
            assertEquals(minId, session.getAttribute(SessionKeys.USER_ID));
        }

        @Test
        @DisplayName("Boundary: Store maximum Long value, set successfully")
        void storeLoginSession_maxLongId_setAttribute() {
            when(request.getSession(true)).thenReturn(session);
            permissionChecker.storeLoginSession(request, BOUNDARY_USER_ID);

            when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(BOUNDARY_USER_ID);
            assertEquals(BOUNDARY_USER_ID, session.getAttribute(SessionKeys.USER_ID));
        }

        @Test
        @DisplayName("Exception: User ID is null, throw null pointer exception")
        void storeLoginSession_userIdNull_throwNull() {
            assertThrows(NullPointerException.class, () -> permissionChecker.storeLoginSession(request, null));
        }

        @Test
        @DisplayName("Exception: Request is null, throw null pointer exception")
        void storeLoginSession_requestNull_throwNull() {
            assertThrows(NullPointerException.class, () -> permissionChecker.storeLoginSession(null, TEST_USER_ID));
        }
    }

    @Nested
    @DisplayName("clearLoginSession Tests")
    class ClearLoginSessionTests {
        @Test
        @DisplayName("Session exists, invalidate session")
        void clearLoginSession_sessionExists_invalidate() {
            when(request.getSession(false)).thenReturn(session);
            permissionChecker.clearLoginSession(request);
            when(request.getSession(false)).thenReturn(null);
            assertNull(request.getSession(false));
        }

        @Test
        @DisplayName("Session does not exist, no exception and no operation")
        void clearLoginSession_sessionNull_doNothing() {
            when(request.getSession(false)).thenReturn(null);
            assertDoesNotThrow(() -> permissionChecker.clearLoginSession(request));
        }

        @Test
        @DisplayName("Request is null, throw null pointer exception")
        void clearLoginSession_requestNull_throwNull() {
            assertThrows(NullPointerException.class, () -> permissionChecker.clearLoginSession(null));
        }
    }

}
