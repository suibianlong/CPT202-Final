package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.ReviewWorkflowController;
import com.cpt202.HerLink.dto.review.CategorySection;
import com.cpt202.HerLink.dto.review.ContributorSection;
import com.cpt202.HerLink.dto.review.PageResponse;
import com.cpt202.HerLink.dto.review.ResourceReviewStatus;
import com.cpt202.HerLink.dto.review.ResourceSection;
import com.cpt202.HerLink.dto.review.ReviewAction;
import com.cpt202.HerLink.dto.review.ReviewActionRequest;
import com.cpt202.HerLink.dto.review.ReviewDecisionResponse;
import com.cpt202.HerLink.dto.review.ReviewDetailResponse;
import com.cpt202.HerLink.dto.review.ReviewHistoryContextType;
import com.cpt202.HerLink.dto.review.ReviewHistoryItemResponse;
import com.cpt202.HerLink.dto.review.ReviewHistoryResponse;
import com.cpt202.HerLink.dto.review.ReviewHistorySectionResponse;
import com.cpt202.HerLink.dto.review.ReviewListItemResponse;
import com.cpt202.HerLink.dto.review.SubmissionSection;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.review.ReviewWorkflowService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgAt;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
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

@WebMvcTest(ReviewWorkflowController.class)
@DisplayName("ReviewWorkflowController Integration Test")
class ReviewWorkflowControllerTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 5, 5, 5, 30, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewWorkflowService reviewWorkflowService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private PageResponse<ReviewListItemResponse> pendingReviewsPage;
    private ReviewDetailResponse reviewDetail;
    private ReviewHistoryResponse reviewHistory;
    private ReviewDecisionResponse approveResponse;
    private ReviewDecisionResponse rejectResponse;
    private ReviewDecisionResponse decisionResponse;

    @BeforeEach
    void setUp() {
        pendingReviewsPage = new PageResponse<>(
                List.of(new ReviewListItemResponse(
                        1001L,
                        301L,
                        2,
                        "Festival Archive",
                        77L,
                        "Alice Contributor",
                        8L,
                        "Culture",
                        FIXED_TIME,
                        ResourceReviewStatus.PENDING_REVIEW
                )),
                1,
                10,
                1
        );

        ReviewHistoryItemResponse historyItem = new ReviewHistoryItemResponse(
                9001L,
                301L,
                1001L,
                2,
                11L,
                "Nina Reviewer",
                ReviewAction.REJECT,
                "Rejected with revision request",
                ResourceReviewStatus.REJECTED,
                "Please improve the summary.",
                FIXED_TIME,
                ReviewHistoryContextType.CURRENT_SUBMISSION,
                "Current Submission"
        );

        reviewDetail = new ReviewDetailResponse(
                1001L,
                301L,
                2,
                ResourceReviewStatus.PENDING_REVIEW,
                new ResourceSection(
                        "Festival Archive",
                        "A local heritage collection.",
                        "Liverpool",
                        "Video",
                        "/images/festival.png",
                        "/media/festival.mp4",
                        "Contributor-owned",
                        FIXED_TIME.minusDays(5),
                        FIXED_TIME.minusDays(1),
                        null,
                        Collections.emptyList()
                ),
                new ContributorSection(77L, "Alice Contributor"),
                new CategorySection(8L, "Culture"),
                new SubmissionSection(
                        FIXED_TIME,
                        77L,
                        "Please review the updated version.",
                        ResourceReviewStatus.PENDING_REVIEW,
                        true,
                        "Resubmission after revision"
                ),
                List.of("Museum", "Community"),
                List.of(historyItem)
        );

        reviewHistory = new ReviewHistoryResponse(
                1001L,
                301L,
                2,
                true,
                List.of(new ReviewHistorySectionResponse(
                        "Current Submission",
                        ReviewHistoryContextType.CURRENT_SUBMISSION,
                        List.of(historyItem)
                ))
        );

        approveResponse = new ReviewDecisionResponse(
                7001L,
                1001L,
                301L,
                2,
                ReviewAction.APPROVE,
                ResourceReviewStatus.APPROVED,
                "Approved for publication.",
                FIXED_TIME,
                true
        );

        rejectResponse = new ReviewDecisionResponse(
                7002L,
                1001L,
                301L,
                2,
                ReviewAction.REJECT,
                ResourceReviewStatus.REJECTED,
                "Please revise the introduction.",
                FIXED_TIME,
                true
        );

        decisionResponse = new ReviewDecisionResponse(
                7003L,
                1001L,
                301L,
                2,
                ReviewAction.APPROVE,
                ResourceReviewStatus.APPROVED,
                "Approved via unified decision endpoint.",
                FIXED_TIME,
                true
        );
    }

    @Nested
    @DisplayName("GET /api/reviewer/reviews/pending")
    class GetPendingReviewsTests {

        @Test
        @DisplayName("Should return pending reviews for an authenticated reviewer")
        void shouldReturnPendingReviewsForAuthenticatedReviewer() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.getPendingReviews(1, 10)).thenReturn(pendingReviewsPage);

            mockMvc.perform(get("/api/reviewer/reviews/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].submissionId").value(1001L))
                    .andExpect(jsonPath("$.items[0].resourceId").value(301L))
                    .andExpect(jsonPath("$.items[0].versionNo").value(2))
                    .andExpect(jsonPath("$.items[0].title").value("Festival Archive"))
                    .andExpect(jsonPath("$.items[0].contributorName").value("Alice Contributor"))
                    .andExpect(jsonPath("$.items[0].categoryTopic").value("Culture"))
                    .andExpect(jsonPath("$.items[0].resourceStatus").value("PENDING_REVIEW"))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(10))
                    .andExpect(jsonPath("$.total").value(1));

            assertInvokedOnceWithArgs(reviewWorkflowService, "getPendingReviews", 1, 10);
        }

        @Test
        @DisplayName("Should return an empty pending review page when no records exist")
        void shouldReturnEmptyPendingReviewPageWhenNoRecordsExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.getPendingReviews(2, 5))
                    .thenReturn(new PageResponse<>(Collections.emptyList(), 2, 5, 0, "No pending reviews."));

            mockMvc.perform(get("/api/reviewer/reviews/pending")
                            .param("page", "2")
                            .param("pageSize", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isEmpty())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.emptyMessage").value("No pending reviews."));

            assertInvokedOnceWithArgs(reviewWorkflowService, "getPendingReviews", 2, 5);
        }

        @Test
        @DisplayName("Should return 401 when reviewer authentication is missing")
        void shouldReturnUnauthorizedWhenReviewerAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/reviewer/reviews/pending"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/pending"));

            assertNoInteractions(reviewWorkflowService);
        }

        @Test
        @DisplayName("Should return 403 when current user is not a reviewer")
        void shouldReturnForbiddenWhenCurrentUserIsNotReviewer() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(get("/api/reviewer/reviews/pending"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/pending"));

            assertNoInteractions(reviewWorkflowService);
        }

        @Test
        @DisplayName("Should return 400 when page query parameter is invalid")
        void shouldReturnBadRequestWhenPageQueryParameterIsInvalid() throws Exception {
            mockMvc.perform(get("/api/reviewer/reviews/pending").param("page", "not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/pending"));
        }
    }

    @Nested
    @DisplayName("GET /api/reviewer/reviews/submissions/{submissionId}")
    class GetReviewDetailTests {

        @Test
        @DisplayName("Should return review detail for a valid submission id")
        void shouldReturnReviewDetailForValidSubmissionId() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.getReviewDetail(1001L)).thenReturn(reviewDetail);

            mockMvc.perform(get("/api/reviewer/reviews/submissions/1001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(1001L))
                    .andExpect(jsonPath("$.resourceId").value(301L))
                    .andExpect(jsonPath("$.versionNo").value(2))
                    .andExpect(jsonPath("$.resourceStatus").value("PENDING_REVIEW"))
                    .andExpect(jsonPath("$.resource.title").value("Festival Archive"))
                    .andExpect(jsonPath("$.contributor.username").value("Alice Contributor"))
                    .andExpect(jsonPath("$.category.categoryTopic").value("Culture"))
                    .andExpect(jsonPath("$.submission.submissionNote").value("Please review the updated version."))
                    .andExpect(jsonPath("$.tags[0]").value("Museum"))
                    .andExpect(jsonPath("$.reviewHistory[0].reviewRecordId").value(9001L));

            assertInvokedOnceWithArgs(reviewWorkflowService, "getReviewDetail", 1001L);
        }

        @Test
        @DisplayName("Should return 404 when review detail submission does not exist")
        void shouldReturnNotFoundWhenReviewDetailSubmissionDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.getReviewDetail(999L))
                    .thenThrow(AppException.notFound("Submission does not exist."));

            mockMvc.perform(get("/api/reviewer/reviews/submissions/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Submission does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/999"));
        }
    }

    @Nested
    @DisplayName("GET /api/reviewer/reviews/{submissionId}")
    class GetReviewDetailAliasTests {

        @Test
        @DisplayName("Should return review detail through alias endpoint")
        void shouldReturnReviewDetailThroughAliasEndpoint() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.getReviewDetail(1001L)).thenReturn(reviewDetail);

            mockMvc.perform(get("/api/reviewer/reviews/1001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(1001L))
                    .andExpect(jsonPath("$.resource.title").value("Festival Archive"));

            assertInvokedOnceWithArgs(reviewWorkflowService, "getReviewDetail", 1001L);
        }

        @Test
        @DisplayName("Should return 400 when alias submission id path variable is invalid")
        void shouldReturnBadRequestWhenAliasSubmissionIdPathVariableIsInvalid() throws Exception {
            mockMvc.perform(get("/api/reviewer/reviews/not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/not-a-number"));
        }
    }

    @Nested
    @DisplayName("GET /api/reviewer/reviews/submissions/{submissionId}/history")
    class GetReviewHistoryTests {

        @Test
        @DisplayName("Should return review history for a valid submission id")
        void shouldReturnReviewHistoryForValidSubmissionId() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.getReviewHistory(1001L)).thenReturn(reviewHistory);

            mockMvc.perform(get("/api/reviewer/reviews/submissions/1001/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(1001L))
                    .andExpect(jsonPath("$.resourceId").value(301L))
                    .andExpect(jsonPath("$.versionNo").value(2))
                    .andExpect(jsonPath("$.resubmission").value(true))
                    .andExpect(jsonPath("$.sections[0].label").value("Current Submission"))
                    .andExpect(jsonPath("$.sections[0].contextType").value("CURRENT_SUBMISSION"))
                    .andExpect(jsonPath("$.sections[0].items[0].action").value("REJECT"))
                    .andExpect(jsonPath("$.sections[0].items[0].reviewerName").value("Nina Reviewer"));

            assertInvokedOnceWithArgs(reviewWorkflowService, "getReviewHistory", 1001L);
        }

        @Test
        @DisplayName("Should return 500 when history service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenHistoryServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.getReviewHistory(1001L))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(get("/api/reviewer/reviews/submissions/1001/history"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/1001/history"));
        }
    }

    @Nested
    @DisplayName("GET /api/reviewer/reviews/{submissionId}/history")
    class GetReviewHistoryAliasTests {

        @Test
        @DisplayName("Should return review history through alias endpoint")
        void shouldReturnReviewHistoryThroughAliasEndpoint() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.getReviewHistory(1001L)).thenReturn(reviewHistory);

            mockMvc.perform(get("/api/reviewer/reviews/1001/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(1001L))
                    .andExpect(jsonPath("$.sections[0].items[0].action").value("REJECT"));

            assertInvokedOnceWithArgs(reviewWorkflowService, "getReviewHistory", 1001L);
        }

        @Test
        @DisplayName("Should return 400 when history alias submission id path variable is invalid")
        void shouldReturnBadRequestWhenHistoryAliasSubmissionIdPathVariableIsInvalid() throws Exception {
            mockMvc.perform(get("/api/reviewer/reviews/not-a-number/history"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/not-a-number/history"));
        }
    }

    @Nested
    @DisplayName("POST /api/reviewer/reviews/{submissionId}/approve")
    class ApproveSubmissionTests {

        @Test
        @DisplayName("Should approve submission when request body is valid")
        void shouldApproveSubmissionWhenRequestBodyIsValid() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.approveSubmission(eq(1001L), eq(11L), any(ReviewActionRequest.class)))
                    .thenReturn(approveResponse);

            mockMvc.perform(post("/api/reviewer/reviews/1001/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": 301,
                                      "versionNo": 2,
                                      "reviewerId": 11,
                                      "feedbackComment": "Approved for publication."
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewRecordId").value(7001L))
                    .andExpect(jsonPath("$.submissionId").value(1001L))
                    .andExpect(jsonPath("$.resourceId").value(301L))
                    .andExpect(jsonPath("$.versionNo").value(2))
                    .andExpect(jsonPath("$.action").value("APPROVE"))
                    .andExpect(jsonPath("$.resourceStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.feedbackComment").value("Approved for publication."))
                    .andExpect(jsonPath("$.removedFromPendingQueue").value(true));

            assertInvokedOnceWithArgAt(reviewWorkflowService, "approveSubmission", 0, 1001L);
            assertInvokedOnceWithArgAt(reviewWorkflowService, "approveSubmission", 1, 11L);
        }

        @Test
        @DisplayName("Should return 400 when approve request JSON is malformed")
        void shouldReturnBadRequestWhenApproveRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/reviewer/reviews/1001/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/1001/approve"));
        }

        @Test
        @DisplayName("Should return 409 when submission can no longer be approved")
        void shouldReturnConflictWhenSubmissionCanNoLongerBeApproved() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.approveSubmission(eq(1001L), eq(11L), any(ReviewActionRequest.class)))
                    .thenThrow(AppException.conflict("Submission is no longer pending review."));

            mockMvc.perform(post("/api/reviewer/reviews/1001/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": 301,
                                      "versionNo": 2,
                                      "reviewerId": 11,
                                      "feedbackComment": "Approved for publication."
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Submission is no longer pending review."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/1001/approve"));
        }
    }

    @Nested
    @DisplayName("POST /api/reviewer/reviews/{submissionId}/reject")
    class RejectSubmissionTests {

        @Test
        @DisplayName("Should reject submission when request body is valid")
        void shouldRejectSubmissionWhenRequestBodyIsValid() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.rejectSubmission(eq(1001L), eq(11L), any(ReviewActionRequest.class)))
                    .thenReturn(rejectResponse);

            mockMvc.perform(post("/api/reviewer/reviews/1001/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": 301,
                                      "versionNo": 2,
                                      "reviewerId": 11,
                                      "feedbackComment": "Please revise the introduction."
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewRecordId").value(7002L))
                    .andExpect(jsonPath("$.action").value("REJECT"))
                    .andExpect(jsonPath("$.resourceStatus").value("REJECTED"))
                    .andExpect(jsonPath("$.feedbackComment").value("Please revise the introduction."))
                    .andExpect(jsonPath("$.removedFromPendingQueue").value(true));

            assertInvokedOnceWithArgAt(reviewWorkflowService, "rejectSubmission", 0, 1001L);
            assertInvokedOnceWithArgAt(reviewWorkflowService, "rejectSubmission", 1, 11L);
        }

        @Test
        @DisplayName("Should return 404 when submission to reject does not exist")
        void shouldReturnNotFoundWhenSubmissionToRejectDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.rejectSubmission(eq(999L), eq(11L), any(ReviewActionRequest.class)))
                    .thenThrow(AppException.notFound("Submission does not exist."));

            mockMvc.perform(post("/api/reviewer/reviews/999/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": 301,
                                      "versionNo": 2,
                                      "reviewerId": 11,
                                      "feedbackComment": "Please revise the introduction."
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Submission does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/999/reject"));
        }
    }

    @Nested
    @DisplayName("POST /api/reviewer/reviews/submissions/{submissionId}/decision")
    class SubmitDecisionTests {

        @Test
        @DisplayName("Should submit a unified review decision when request body is valid")
        void shouldSubmitUnifiedReviewDecisionWhenRequestBodyIsValid() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.submitDecision(eq(1001L), eq(11L), any()))
                    .thenReturn(decisionResponse);

            mockMvc.perform(post("/api/reviewer/reviews/submissions/1001/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionId": 1001,
                                      "resourceId": 301,
                                      "versionNo": 2,
                                      "reviewerId": 11,
                                      "action": "APPROVE",
                                      "feedbackComment": "Approved via unified decision endpoint."
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewRecordId").value(7003L))
                    .andExpect(jsonPath("$.action").value("APPROVE"))
                    .andExpect(jsonPath("$.resourceStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.feedbackComment").value("Approved via unified decision endpoint."));

            assertInvokedOnceWithArgAt(reviewWorkflowService, "submitDecision", 0, 1001L);
            assertInvokedOnceWithArgAt(reviewWorkflowService, "submitDecision", 1, 11L);
        }

        @Test
        @DisplayName("Should return 400 when unified decision request JSON is malformed")
        void shouldReturnBadRequestWhenUnifiedDecisionRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/reviewer/reviews/submissions/1001/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/1001/decision"));
        }

        @Test
        @DisplayName("Should return 400 when service rejects an invalid unified decision payload")
        void shouldReturnBadRequestWhenServiceRejectsInvalidUnifiedDecisionPayload() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(11L);
            when(reviewWorkflowService.submitDecision(eq(1001L), eq(11L), any()))
                    .thenThrow(AppException.badRequest("Review action must be APPROVE or REJECT."));

            mockMvc.perform(post("/api/reviewer/reviews/submissions/1001/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionId": 1001,
                                      "resourceId": 301,
                                      "versionNo": 2,
                                      "reviewerId": 11,
                                      "feedbackComment": "Missing action."
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Review action must be APPROVE or REJECT."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/1001/decision"));
        }
    }
}
