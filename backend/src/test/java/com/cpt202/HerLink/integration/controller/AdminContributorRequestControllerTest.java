package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.AdminContributorRequestController;
import com.cpt202.HerLink.dto.auth.ContributorReviewDecisionRequest;
import com.cpt202.HerLink.enums.ContributorApplicationStatusEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.UserAccessService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnce;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgsPrefix;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.ContributorRequestVO;
import com.cpt202.HerLink.vo.CurrentUserVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminContributorRequestController.class)
@DisplayName("AdminContributorRequestController Integration Test")
class AdminContributorRequestControllerTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 5, 5, 3, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAccessService userAccessService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private CurrentUserVO namedAdmin;
    private CurrentUserVO blankNameAdmin;

    @BeforeEach
    void setUp() {
        namedAdmin = adminUser(9L, "Nina Admin");
        blankNameAdmin = adminUser(10L, "   ");
    }

    @Nested
    @DisplayName("GET /api/admin/contributor-requests/pending")
    class ListPendingRequestsTests {

        @Test
        @DisplayName("Should return pending contributor requests for an authenticated administrator")
        void shouldReturnPendingRequests() throws Exception {
            ContributorRequestVO request = contributorRequest(
                    10L,
                    200L,
                    "Alice",
                    "alice@example.com",
                    "I want to contribute mental health resources.",
                    ContributorApplicationStatusEnum.PENDING.getValue()
            );

            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(1L);
            when(userAccessService.listPendingContributorRequests()).thenReturn(List.of(request));

            mockMvc.perform(get("/api/admin/contributor-requests/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].requestId").value(10L))
                    .andExpect(jsonPath("$[0].userId").value(200L))
                    .andExpect(jsonPath("$[0].userName").value("Alice"))
                    .andExpect(jsonPath("$[0].userEmail").value("alice@example.com"))
                    .andExpect(jsonPath("$[0].applicationReason").value("I want to contribute mental health resources."))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

            assertInvokedOnce(userAccessService, "listPendingContributorRequests");
        }

        @Test
        @DisplayName("Should return an empty list when there are no pending requests")
        void shouldReturnEmptyListWhenNoPendingRequestsExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(1L);
            when(userAccessService.listPendingContributorRequests()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/contributor-requests/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertInvokedOnce(userAccessService, "listPendingContributorRequests");
        }

        @Test
        @DisplayName("Should return 401 when administrator authentication is missing")
        void shouldReturnUnauthorizedWhenPermissionCheckerRejects() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/admin/contributor-requests/pending"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/pending"));

            assertNoInteractions(userAccessService);
        }
    }

    @Nested
    @DisplayName("GET /api/admin/contributor-requests/{requestId}")
    class GetRequestDetailTests {

        @Test
        @DisplayName("Should return contributor request detail when request id is valid")
        void shouldReturnRequestDetail() throws Exception {
            ContributorRequestVO request = contributorRequest(
                    11L,
                    201L,
                    "Bob",
                    "bob@example.com",
                    "I can review community-submitted resources.",
                    ContributorApplicationStatusEnum.PENDING.getValue()
            );

            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(2L);
            when(userAccessService.getContributorRequestDetail(11L)).thenReturn(request);

            mockMvc.perform(get("/api/admin/contributor-requests/11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(11L))
                    .andExpect(jsonPath("$.userId").value(201L))
                    .andExpect(jsonPath("$.userName").value("Bob"))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            assertInvokedOnceWithArgs(userAccessService, "getContributorRequestDetail", 11L);
        }

        @Test
        @DisplayName("Should return 400 when request id path variable is not a number")
        void shouldReturnBadRequestWhenRequestIdIsInvalid() throws Exception {
            mockMvc.perform(get("/api/admin/contributor-requests/not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/not-a-number"));
        }

        @Test
        @DisplayName("Should return 404 when contributor request does not exist")
        void shouldReturnNotFoundWhenRequestDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(2L);
            when(userAccessService.getContributorRequestDetail(999L))
                    .thenThrow(AppException.notFound("Contributor request does not exist."));

            mockMvc.perform(get("/api/admin/contributor-requests/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Contributor request does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/999"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/contributor-requests/approved-contributors")
    class ListApprovedContributorsTests {

        @Test
        @DisplayName("Should return approved contributors for an authenticated administrator")
        void shouldReturnApprovedContributors() throws Exception {
            ContributorRequestVO contributor = contributorRequest(
                    12L,
                    202L,
                    "Cathy",
                    "cathy@example.com",
                    "Approved contributor",
                    ContributorApplicationStatusEnum.APPROVED.getValue()
            );
            contributor.setReviewedBy(1L);

            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(3L);
            when(userAccessService.listApprovedContributors()).thenReturn(List.of(contributor));

            mockMvc.perform(get("/api/admin/contributor-requests/approved-contributors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].requestId").value(12L))
                    .andExpect(jsonPath("$[0].userId").value(202L))
                    .andExpect(jsonPath("$[0].userName").value("Cathy"))
                    .andExpect(jsonPath("$[0].status").value("APPROVED"))
                    .andExpect(jsonPath("$[0].reviewedBy").value(1L));

            assertInvokedOnce(userAccessService, "listApprovedContributors");
        }

        @Test
        @DisplayName("Should return an empty list when there are no approved contributors")
        void shouldReturnEmptyListWhenNoApprovedContributorsExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(3L);
            when(userAccessService.listApprovedContributors()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/contributor-requests/approved-contributors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertInvokedOnce(userAccessService, "listApprovedContributors");
        }

        @Test
        @DisplayName("Should return 403 when current user is not an administrator")
        void shouldReturnForbiddenWhenCurrentUserIsNotAdmin() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(get("/api/admin/contributor-requests/approved-contributors"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/approved-contributors"));

            assertNoInteractions(userAccessService);
        }
    }

    @Nested
    @DisplayName("POST /api/admin/contributor-requests/{requestId}/decision")
    class ReviewRequestTests {

        @Test
        @DisplayName("Should approve contributor request when decision payload is valid")
        void shouldApproveRequest() throws Exception {
            ContributorRequestVO reviewed = contributorRequest(
                    13L,
                    203L,
                    "David",
                    "david@example.com",
                    "Experienced in curating education resources.",
                    ContributorApplicationStatusEnum.APPROVED.getValue()
            );
            reviewed.setReviewedBy(8L);
            reviewed.setReviewComment("Application approved.");

            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(8L);
            when(userAccessService.reviewContributorRequest(eq(8L), eq(13L), any(ContributorReviewDecisionRequest.class)))
                    .thenReturn(reviewed);

            mockMvc.perform(post("/api/admin/contributor-requests/13/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "reviewComment": "Application approved."
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(13L))
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.reviewedBy").value(8L))
                    .andExpect(jsonPath("$.reviewComment").value("Application approved."));

            assertInvokedOnceWithArgsPrefix(userAccessService, "reviewContributorRequest", 8L, 13L);
        }

        @Test
        @DisplayName("Should reject contributor request when rejection payload is valid")
        void shouldRejectRequest() throws Exception {
            ContributorRequestVO reviewed = contributorRequest(
                    16L,
                    206L,
                    "Grace",
                    "grace@example.com",
                    "Needs more experience before contributing.",
                    ContributorApplicationStatusEnum.REJECTED.getValue()
            );
            reviewed.setReviewedBy(8L);
            reviewed.setReviewComment("Application rejected.");

            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(8L);
            when(userAccessService.reviewContributorRequest(eq(8L), eq(16L), any(ContributorReviewDecisionRequest.class)))
                    .thenReturn(reviewed);

            mockMvc.perform(post("/api/admin/contributor-requests/16/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "REJECTED",
                                      "reviewComment": "Application rejected."
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(16L))
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.reviewedBy").value(8L))
                    .andExpect(jsonPath("$.reviewComment").value("Application rejected."));

            assertInvokedOnceWithArgsPrefix(userAccessService, "reviewContributorRequest", 8L, 16L);
        }

        @Test
        @DisplayName("Should return 400 when review request id path variable is not a number")
        void shouldReturnBadRequestWhenRequestIdIsInvalid() throws Exception {
            mockMvc.perform(post("/api/admin/contributor-requests/not-a-number/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "reviewComment": "Application approved."
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/not-a-number/decision"));
        }

        @Test
        @DisplayName("Should return 400 when decision request JSON is malformed")
        void shouldReturnBadRequestWhenRequestBodyIsMalformed() throws Exception {
            mockMvc.perform(post("/api/admin/contributor-requests/13/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/13/decision"));
        }

        @Test
        @DisplayName("Should return 400 when decision service rejects the request payload")
        void shouldReturnBadRequestWhenServiceRejectsDecisionRequest() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(8L);
            when(userAccessService.reviewContributorRequest(eq(8L), eq(13L), any(ContributorReviewDecisionRequest.class)))
                    .thenThrow(AppException.badRequest("Review decision is invalid."));

            mockMvc.perform(post("/api/admin/contributor-requests/13/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "INVALID",
                                      "reviewComment": "Unsupported decision."
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Review decision is invalid."))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/13/decision"));
        }

        @Test
        @DisplayName("Should return 409 when contributor request has already been reviewed")
        void shouldReturnConflictWhenRequestAlreadyReviewed() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(8L);
            when(userAccessService.reviewContributorRequest(eq(8L), eq(13L), any(ContributorReviewDecisionRequest.class)))
                    .thenThrow(AppException.conflict("Contributor request has already been reviewed."));

            mockMvc.perform(post("/api/admin/contributor-requests/13/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "reviewComment": "Late review."
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Contributor request has already been reviewed."))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/13/decision"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/contributor-requests/contributors/{userId}/revoke")
    class RevokeContributorTests {

        @Test
        @DisplayName("Should revoke contributor access when administrator request is valid")
        void shouldRevokeContributor() throws Exception {
            ContributorRequestVO revoked = contributorRequest(
                    14L,
                    204L,
                    "Evelyn",
                    "evelyn@example.com",
                    "Contributor access revoked",
                    ContributorApplicationStatusEnum.ARCHIVED.getValue()
            );
            revoked.setReviewedBy(9L);
            revoked.setReviewComment("Contributor access revoked by administrator.");

            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdmin);
            when(userAccessService.revokeContributor(9L, 204L, "Nina Admin")).thenReturn(revoked);

            mockMvc.perform(post("/api/admin/contributor-requests/contributors/204/revoke"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(14L))
                    .andExpect(jsonPath("$.userId").value(204L))
                    .andExpect(jsonPath("$.status").value("ARCHIVED"))
                    .andExpect(jsonPath("$.reviewComment").value("Contributor access revoked by administrator."));

            assertInvokedOnceWithArgs(userAccessService, "revokeContributor", 9L, 204L, "Nina Admin");
        }

        @Test
        @DisplayName("Should fall back to user_{id} when administrator name is blank during revoke")
        void shouldFallbackToUserIdWhenAdminNameBlank() throws Exception {
            ContributorRequestVO revoked = contributorRequest(
                    15L,
                    205L,
                    "Frank",
                    "frank@example.com",
                    "Contributor access revoked",
                    ContributorApplicationStatusEnum.ARCHIVED.getValue()
            );

            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(blankNameAdmin);
            when(userAccessService.revokeContributor(10L, 205L, "user_10")).thenReturn(revoked);

            mockMvc.perform(post("/api/admin/contributor-requests/contributors/205/revoke"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(15L))
                    .andExpect(jsonPath("$.userId").value(205L));

            assertInvokedOnceWithArgs(userAccessService, "revokeContributor", 10L, 205L, "user_10");
        }

        @Test
        @DisplayName("Should return 400 when contributor user id path variable is not a number")
        void shouldReturnBadRequestWhenUserIdIsInvalid() throws Exception {
            mockMvc.perform(post("/api/admin/contributor-requests/contributors/not-a-number/revoke"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/contributors/not-a-number/revoke"));
        }

        @Test
        @DisplayName("Should return 404 when contributor to revoke does not exist")
        void shouldReturnNotFoundWhenContributorDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdmin);
            when(userAccessService.revokeContributor(9L, 999L, "Nina Admin"))
                    .thenThrow(AppException.notFound("Contributor does not exist."));

            mockMvc.perform(post("/api/admin/contributor-requests/contributors/999/revoke"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Contributor does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/contributors/999/revoke"));
        }

        @Test
        @DisplayName("Should return 500 when revoke service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdmin);
            when(userAccessService.revokeContributor(9L, 204L, "Nina Admin"))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(post("/api/admin/contributor-requests/contributors/204/revoke"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/admin/contributor-requests/contributors/204/revoke"));
        }
    }

    private CurrentUserVO adminUser(Long userId, String name) {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(userId);
        currentUser.setName(name);
        currentUser.setRole("ADMINISTRATOR");
        return currentUser;
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
        requestVO.setRequestedAt(FIXED_TIME);
        requestVO.setUpdatedAt(FIXED_TIME);
        return requestVO;
    }
}
