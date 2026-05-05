package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.ContributorRequestController;
import com.cpt202.HerLink.dto.auth.ContributorRequestSubmitRequest;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.UserAccessService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgAt;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.ContributorRequestVO;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContributorRequestController.class)
@DisplayName("ContributorRequestController Integration Test")
class ContributorRequestControllerTest {

    private static LocalDateTime fixedTime;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAccessService userAccessService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private Long currentUserId;

    @BeforeAll
    static void setUpAll() {
        fixedTime = LocalDateTime.of(2026, 5, 5, 6, 30, 0);
    }

    @BeforeEach
    void setUp() {
        currentUserId = 200L;
    }

    @AfterEach
    void tearDown() {
        clearInvocations(userAccessService, resourcePermissionChecker);
        currentUserId = null;
    }

    @AfterAll
    static void tearDownAll() {
        fixedTime = null;
    }

    @Nested
    @DisplayName("POST /api/contributor-requests")
    class SubmitContributorRequestTests {

        @Test
        @DisplayName("Should submit contributor request when payload is valid for an authenticated user")
        void shouldSubmitContributorRequest() throws Exception {
            ContributorRequestVO response = contributorRequest(
                    10L,
                    currentUserId,
                    "Alice Viewer",
                    "alice@example.com",
                    "I want to contribute verified cultural heritage resources.",
                    "PENDING"
            );

            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(userAccessService.submitContributorRequest(eq(currentUserId), any(ContributorRequestSubmitRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/contributor-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I want to contribute verified cultural heritage resources."
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.requestId").value(10L))
                    .andExpect(jsonPath("$.userId").value(200L))
                    .andExpect(jsonPath("$.userName").value("Alice Viewer"))
                    .andExpect(jsonPath("$.userEmail").value("alice@example.com"))
                    .andExpect(jsonPath("$.applicationReason")
                            .value("I want to contribute verified cultural heritage resources."))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            assertInvokedOnceWithArgAt(userAccessService, "submitContributorRequest", 0, currentUserId);
        }

        @Test
        @DisplayName("Should return 401 when contributor request submission is unauthenticated")
        void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(post("/api/contributor-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I want to help."
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));

            assertNoInteractions(userAccessService);
        }

        @Test
        @DisplayName("Should return 400 when contributor request JSON is malformed")
        void shouldReturnBadRequestWhenRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/contributor-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));

            assertNoInteractions(userAccessService, resourcePermissionChecker);
        }

        @Test
        @DisplayName("Should return 400 when service rejects an invalid contributor request payload")
        void shouldReturnBadRequestWhenServiceRejectsInvalidPayload() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(userAccessService.submitContributorRequest(eq(currentUserId), any(ContributorRequestSubmitRequest.class)))
                    .thenThrow(AppException.badRequest(
                            "Please correct your contributor application.",
                            java.util.List.of("Application reason must be 2000 characters or fewer.")
                    ));

            mockMvc.perform(post("/api/contributor-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please correct your contributor application."))
                    .andExpect(jsonPath("$.details[0]")
                            .value("Application reason must be 2000 characters or fewer."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));
        }

        @Test
        @DisplayName("Should return 403 when current user is not allowed to submit contributor request")
        void shouldReturnForbiddenWhenCurrentUserRoleIsNotAllowed() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(userAccessService.submitContributorRequest(eq(currentUserId), any(ContributorRequestSubmitRequest.class)))
                    .thenThrow(AppException.forbidden("Only registered viewers can submit contributor requests."));

            mockMvc.perform(post("/api/contributor-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I would like to contribute."
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Only registered viewers can submit contributor requests."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));
        }

        @Test
        @DisplayName("Should return 409 when user already has a pending contributor request")
        void shouldReturnConflictWhenPendingContributorRequestAlreadyExists() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(userAccessService.submitContributorRequest(eq(currentUserId), any(ContributorRequestSubmitRequest.class)))
                    .thenThrow(AppException.conflict("Your existing contributor request is still under review."));

            mockMvc.perform(post("/api/contributor-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I would like to contribute."
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Your existing contributor request is still under review."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));
        }

        @Test
        @DisplayName("Should return 500 when contributor request submission service throws unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(userAccessService.submitContributorRequest(eq(currentUserId), any(ContributorRequestSubmitRequest.class)))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(post("/api/contributor-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I would like to contribute."
                                    }
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor-requests/me")
    class GetMyLatestContributorRequestTests {

        @Test
        @DisplayName("Should return current user's latest contributor request when one exists")
        void shouldReturnMyLatestContributorRequest() throws Exception {
            ContributorRequestVO response = contributorRequest(
                    12L,
                    currentUserId,
                    "Alice Viewer",
                    "alice@example.com",
                    "I have archive research experience.",
                    "PENDING"
            );
            response.setReviewComment("Awaiting review.");

            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(userAccessService.getMyLatestContributorRequest(currentUserId)).thenReturn(response);

            mockMvc.perform(get("/api/contributor-requests/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(12L))
                    .andExpect(jsonPath("$.userId").value(200L))
                    .andExpect(jsonPath("$.userName").value("Alice Viewer"))
                    .andExpect(jsonPath("$.userEmail").value("alice@example.com"))
                    .andExpect(jsonPath("$.applicationReason").value("I have archive research experience."))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.reviewComment").value("Awaiting review."));

            assertInvokedOnceWithArgs(userAccessService, "getMyLatestContributorRequest", currentUserId);
        }

        @Test
        @DisplayName("Should return empty body when current user has no contributor request history")
        void shouldReturnEmptyBodyWhenNoContributorRequestExists() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(userAccessService.getMyLatestContributorRequest(currentUserId)).thenReturn(null);

            mockMvc.perform(get("/api/contributor-requests/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").doesNotExist());

            assertInvokedOnceWithArgs(userAccessService, "getMyLatestContributorRequest", currentUserId);
        }

        @Test
        @DisplayName("Should return 401 when querying latest contributor request without authentication")
        void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/contributor-requests/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests/me"));

            assertNoInteractions(userAccessService);
        }

        @Test
        @DisplayName("Should return 404 when current user record no longer exists")
        void shouldReturnNotFoundWhenCurrentUserDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(userAccessService.getMyLatestContributorRequest(currentUserId))
                    .thenThrow(AppException.notFound("User does not exist."));

            mockMvc.perform(get("/api/contributor-requests/me"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("User does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests/me"));
        }

        @Test
        @DisplayName("Should return 500 when latest contributor request query throws unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(userAccessService.getMyLatestContributorRequest(currentUserId))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(get("/api/contributor-requests/me"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests/me"));
        }
    }

    private ContributorRequestVO contributorRequest(Long requestId,
                                                    Long userId,
                                                    String userName,
                                                    String userEmail,
                                                    String applicationReason,
                                                    String status) {
        ContributorRequestVO requestVO = new ContributorRequestVO();
        requestVO.setRequestId(requestId);
        requestVO.setUserId(userId);
        requestVO.setUserName(userName);
        requestVO.setUserEmail(userEmail);
        requestVO.setApplicationReason(applicationReason);
        requestVO.setStatus(status);
        requestVO.setRequestedAt(fixedTime);
        requestVO.setUpdatedAt(fixedTime);
        return requestVO;
    }
}
