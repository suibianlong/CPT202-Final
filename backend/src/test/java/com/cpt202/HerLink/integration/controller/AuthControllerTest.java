package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.AuthController;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.UserAccessService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnce;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgAt;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CurrentUserVO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Integration Test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAccessService userAccessService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private CurrentUserVO registeredViewer;
    private CurrentUserVO approvedContributor;

    @BeforeEach
    void setUp() {
        registeredViewer = currentUser(
                5L,
                "Registered Viewer",
                "viewer@example.com",
                "Updated bio",
                "REGISTERED_VIEWER",
                "NONE",
                false
        );
        approvedContributor = currentUser(
                2L,
                "Approved Contributor",
                "contributor@example.com",
                "Contributor bio",
                "REGISTERED_VIEWER",
                "APPROVED",
                true
        );
    }

    @Nested
    @DisplayName("POST /api/auth/register-verification-code")
    class SendRegisterVerificationCodeTests {

        @Test
        @DisplayName("Should return 204 when verification code request is valid")
        void shouldReturnNoContentWhenVerificationCodeRequestIsValid() throws Exception {
            doNothing().when(userAccessService).sendRegisterVerificationCode(any());

            mockMvc.perform(post("/api/auth/register-verification-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "viewer@example.com"
                                    }
                                    """))
                    .andExpect(status().isNoContent());

            assertInvokedOnce(userAccessService, "sendRegisterVerificationCode");
        }

        @Test
        @DisplayName("Should return 400 when verification code request JSON is malformed")
        void shouldReturnBadRequestWhenVerificationCodeRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/auth/register-verification-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/auth/register-verification-code"));
        }

        @Test
        @DisplayName("Should return 400 when service rejects an invalid verification code request")
        void shouldReturnBadRequestWhenServiceRejectsInvalidVerificationCodeRequest() throws Exception {
            doThrow(AppException.badRequest("Please correct the email address."))
                    .when(userAccessService).sendRegisterVerificationCode(any());

            mockMvc.perform(post("/api/auth/register-verification-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "invalid-email"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please correct the email address."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register-verification-code"));
        }

        @Test
        @DisplayName("Should return 409 when verification code cannot be resent yet")
        void shouldReturnConflictWhenVerificationCodeCannotBeResentYet() throws Exception {
            doThrow(AppException.conflict("Please wait 60 seconds before requesting another verification code."))
                    .when(userAccessService).sendRegisterVerificationCode(any());

            mockMvc.perform(post("/api/auth/register-verification-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "viewer@example.com"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message")
                            .value("Please wait 60 seconds before requesting another verification code."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register-verification-code"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register a new user when registration request is valid")
        void shouldRegisterNewUserWhenRegistrationRequestIsValid() throws Exception {
            when(userAccessService.register(any())).thenReturn(registeredViewer);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Registered Viewer",
                                      "email": "viewer@example.com",
                                      "password": "Viewer123!",
                                      "confirmPassword": "Viewer123!",
                                      "verificationCode": "123456"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(5L))
                    .andExpect(jsonPath("$.name").value("Registered Viewer"))
                    .andExpect(jsonPath("$.email").value("viewer@example.com"))
                    .andExpect(jsonPath("$.role").value("REGISTERED_VIEWER"))
                    .andExpect(jsonPath("$.contributorStatus").value("NONE"))
                    .andExpect(jsonPath("$.contributor").value(false));

            assertInvokedOnce(userAccessService, "register");
        }

        @Test
        @DisplayName("Should return 400 when registration request JSON is malformed")
        void shouldReturnBadRequestWhenRegistrationRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/auth/register"));
        }

        @Test
        @DisplayName("Should return 400 when service rejects invalid registration input")
        void shouldReturnBadRequestWhenServiceRejectsInvalidRegistrationInput() throws Exception {
            when(userAccessService.register(any()))
                    .thenThrow(AppException.badRequest("Please correct the registration form."));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "",
                                      "email": "viewer@example.com",
                                      "password": "short",
                                      "confirmPassword": "different",
                                      "verificationCode": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please correct the registration form."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register"));
        }

        @Test
        @DisplayName("Should return 409 when registration email is already in use")
        void shouldReturnConflictWhenRegistrationEmailIsAlreadyInUse() throws Exception {
            when(userAccessService.register(any()))
                    .thenThrow(AppException.conflict("The email address is already in use."));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Registered Viewer",
                                      "email": "viewer@example.com",
                                      "password": "Viewer123!",
                                      "confirmPassword": "Viewer123!",
                                      "verificationCode": "123456"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("The email address is already in use."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should log in user and store session when credentials are valid")
        void shouldLogInUserAndStoreSessionWhenCredentialsAreValid() throws Exception {
            when(userAccessService.login(any())).thenReturn(approvedContributor);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "contributor@example.com",
                                      "password": "Viewer123!"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(2L))
                    .andExpect(jsonPath("$.name").value("Approved Contributor"))
                    .andExpect(jsonPath("$.email").value("contributor@example.com"))
                    .andExpect(jsonPath("$.contributorStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.contributor").value(true));

            assertInvokedOnce(userAccessService, "login");
            assertInvokedOnceWithArgAt(resourcePermissionChecker, "storeLoginSession", 1, 2L);
        }

        @Test
        @DisplayName("Should return 400 when login request JSON is malformed")
        void shouldReturnBadRequestWhenLoginRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"));
        }

        @Test
        @DisplayName("Should return 400 when service rejects incomplete login form")
        void shouldReturnBadRequestWhenServiceRejectsIncompleteLoginForm() throws Exception {
            when(userAccessService.login(any()))
                    .thenThrow(AppException.badRequest("Please complete the login form."));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "",
                                      "password": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please complete the login form."))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"));
        }

        @Test
        @DisplayName("Should return 401 when credentials are invalid")
        void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
            when(userAccessService.login(any()))
                    .thenThrow(AppException.unauthorized("Invalid email or password."));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "contributor@example.com",
                                      "password": "WrongPassword"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid email or password."))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should clear login session and return 204 when logout succeeds")
        void shouldClearLoginSessionAndReturnNoContentWhenLogoutSucceeds() throws Exception {
            doNothing().when(resourcePermissionChecker).clearLoginSession(any());

            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isNoContent());

            assertInvokedOnce(resourcePermissionChecker, "clearLoginSession");
            assertNoInteractions(userAccessService);
        }

        @Test
        @DisplayName("Should return 500 when logout handler throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenLogoutHandlerThrowsUnexpectedException() throws Exception {
            doThrow(new RuntimeException("Session store unavailable"))
                    .when(resourcePermissionChecker).clearLoginSession(any());

            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/auth/logout"));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current session user when session is valid")
        void shouldReturnCurrentSessionUserWhenSessionIsValid() throws Exception {
            when(resourcePermissionChecker.requireCurrentUser(any())).thenReturn(approvedContributor);

            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(2L))
                    .andExpect(jsonPath("$.name").value("Approved Contributor"))
                    .andExpect(jsonPath("$.email").value("contributor@example.com"))
                    .andExpect(jsonPath("$.contributorStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.contributor").value(true));
        }

        @Test
        @DisplayName("Should return 401 when current user session is missing")
        void shouldReturnUnauthorizedWhenCurrentUserSessionIsMissing() throws Exception {
            when(resourcePermissionChecker.requireCurrentUser(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/auth/me"));

            assertNoInteractions(userAccessService);
        }
    }

    @Nested
    @DisplayName("PUT /api/auth/account")
    class UpdateAccountTests {

        @Test
        @DisplayName("Should update current user account when request is valid")
        void shouldUpdateCurrentUserAccountWhenRequestIsValid() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(userAccessService.updateAccount(eq(5L), any())).thenReturn(registeredViewer);

            mockMvc.perform(put("/api/auth/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Registered Viewer",
                                      "email": "viewer@example.com",
                                      "bio": "Updated bio"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(5L))
                    .andExpect(jsonPath("$.name").value("Registered Viewer"))
                    .andExpect(jsonPath("$.email").value("viewer@example.com"))
                    .andExpect(jsonPath("$.bio").value("Updated bio"))
                    .andExpect(jsonPath("$.role").value("REGISTERED_VIEWER"));

            assertInvokedOnceWithArgAt(userAccessService, "updateAccount", 0, 5L);
        }

        @Test
        @DisplayName("Should return 400 when account update request JSON is malformed")
        void shouldReturnBadRequestWhenAccountUpdateRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(put("/api/auth/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/auth/account"));
        }

        @Test
        @DisplayName("Should return 401 when account update is requested without login")
        void shouldReturnUnauthorizedWhenAccountUpdateIsRequestedWithoutLogin() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(put("/api/auth/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Registered Viewer",
                                      "email": "viewer@example.com",
                                      "bio": "Updated bio"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/auth/account"));

            assertNoInteractions(userAccessService);
        }

        @Test
        @DisplayName("Should return 400 when service rejects invalid account settings")
        void shouldReturnBadRequestWhenServiceRejectsInvalidAccountSettings() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(userAccessService.updateAccount(eq(5L), any()))
                    .thenThrow(AppException.badRequest("Please correct your account settings."));

            mockMvc.perform(put("/api/auth/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "",
                                      "email": "invalid-email",
                                      "bio": "Updated bio"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please correct your account settings."))
                    .andExpect(jsonPath("$.path").value("/api/auth/account"));
        }

        @Test
        @DisplayName("Should return 409 when account update conflicts with another user")
        void shouldReturnConflictWhenAccountUpdateConflictsWithAnotherUser() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(userAccessService.updateAccount(eq(5L), any()))
                    .thenThrow(AppException.conflict("The email address is already in use."));

            mockMvc.perform(put("/api/auth/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Registered Viewer",
                                      "email": "viewer@example.com",
                                      "bio": "Updated bio"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("The email address is already in use."))
                    .andExpect(jsonPath("$.path").value("/api/auth/account"));
        }

        @Test
        @DisplayName("Should return 500 when account update service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenAccountUpdateServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(userAccessService.updateAccount(eq(5L), any()))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(put("/api/auth/account")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Registered Viewer",
                                      "email": "viewer@example.com",
                                      "bio": "Updated bio"
                                    }
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/auth/account"));
        }
    }

    private CurrentUserVO currentUser(Long userId,
                                      String name,
                                      String email,
                                      String bio,
                                      String role,
                                      String contributorStatus,
                                      boolean contributor) {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(userId);
        currentUser.setName(name);
        currentUser.setEmail(email);
        currentUser.setBio(bio);
        currentUser.setRole(role);
        currentUser.setContributorStatus(contributorStatus);
        currentUser.setContributor(contributor);
        return currentUser;
    }
}
