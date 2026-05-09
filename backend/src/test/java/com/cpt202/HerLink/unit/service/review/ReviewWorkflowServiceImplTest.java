package com.cpt202.HerLink.unit.service.review;

import com.cpt202.HerLink.service.review.*;
import com.cpt202.HerLink.service.review.ReviewWorkflowServiceImpl;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cpt202.HerLink.dto.review.PageResponse;
import com.cpt202.HerLink.dto.review.ResourceReviewStatus;
import com.cpt202.HerLink.dto.review.ReviewAction;
import com.cpt202.HerLink.dto.review.ReviewActionRequest;
import com.cpt202.HerLink.dto.review.ReviewDecisionRequest;
import com.cpt202.HerLink.dto.review.ReviewDecisionResponse;
import com.cpt202.HerLink.dto.review.ReviewDetailResponse;
import com.cpt202.HerLink.dto.review.ReviewHistoryResponse;
import com.cpt202.HerLink.dto.review.ReviewHistoryRow;
import com.cpt202.HerLink.dto.review.ReviewListItemResponse;
import com.cpt202.HerLink.dto.review.ReviewSubmissionRow;
import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.ReviewRecord;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.AppUserMapper;
import com.cpt202.HerLink.mapper.ResourceFileMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ReviewRecordMapper;
import com.cpt202.HerLink.mapper.ReviewWorkflowMapper;
import com.cpt202.HerLink.service.notification.EmailNotificationService;

@ExtendWith(MockitoExtension.class)
class ReviewWorkflowServiceImplTest {

    @Mock
    private ReviewWorkflowMapper reviewWorkflowMapper;

    @Mock
    private ReviewRecordMapper reviewRecordMapper;

    @Mock
    private ResourceFileMapper resourceFileMapper;

    @Mock
    private ResourceTagMapper resourceTagMapper;

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private ReviewWorkflowServiceImpl reviewWorkflowService;

    private static final Long TEST_SUBMISSION_ID = 1L;
    private static final Long TEST_RESOURCE_ID = 10L;
    private static final Long TEST_REVIEWER_ID = 100L;
    private static final Long TEST_CONTRIBUTOR_ID = 200L;
    private static final Integer TEST_VERSION = 1;
    private static final String TEST_COMMENT = "Test feedback";
    private static final String EMPTY_COMMENT = "   ";

    private ReviewSubmissionRow mockSubmissionRow() {
        ReviewSubmissionRow row = new ReviewSubmissionRow();
        row.setSubmissionId(TEST_SUBMISSION_ID);
        row.setResourceId(TEST_RESOURCE_ID);
        row.setVersionNo(TEST_VERSION);
        row.setResourceStatus(ResourceStatusEnum.PENDING_REVIEW.getValue());
        row.setStatusSnapshot(ResourceStatusEnum.PENDING_REVIEW.getValue());
        row.setLatestSubmissionId(TEST_SUBMISSION_ID);
        row.setLatestVersionNo(TEST_VERSION);
        row.setContributorId(TEST_CONTRIBUTOR_ID);
        row.setTitle("Test Resource");
        return row;
    }

    private ReviewHistoryRow mockHistoryRow() {
        ReviewHistoryRow row = new ReviewHistoryRow();
        row.setSubmissionId(TEST_SUBMISSION_ID);
        row.setResourceId(TEST_RESOURCE_ID);
        row.setVersionNo(TEST_VERSION);
        row.setStatus(ResourceReviewStatus.APPROVED.toDatabaseValue());
        row.setReviewRecordId(1L);
        return row;
    }

    @Nested
    @DisplayName("getPendingReviews: Retrieve pending review list with pagination")
    class GetPendingReviewsTests {

        @Test
        @DisplayName("Should throw exception when page number is less than 1")
        void getPendingReviews_PageLessThan1_ThrowException() {
            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.getPendingReviews(0, 10));
            assertTrue(exception.getMessage().contains("Invalid pagination request"));
        }

        @Test
        @DisplayName("Should throw exception when page size exceeds 50")
        void getPendingReviews_PageSizeOverLimit_ThrowException() {
            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.getPendingReviews(1, 60));
            assertEquals("Invalid pagination request.", exception.getMessage());
        }

        @Test
        @DisplayName("Boundary: Should return success when page size equals 50")
        void getPendingReviews_PageSizeEquals50_ReturnSuccess() {
            ReviewSubmissionRow row = mockSubmissionRow();
            when(reviewWorkflowMapper.selectPendingReviews(0, 50)).thenReturn(List.of(row));
            when(reviewWorkflowMapper.countPendingReviews()).thenReturn(1L);

            PageResponse<ReviewListItemResponse> response = reviewWorkflowService.getPendingReviews(1, 50);

            assertEquals(1, response.items().size());
            assertEquals(50, response.pageSize());
            assertEquals(1, response.total());
            assertNull(response.emptyMessage());
        }

        @Test
        @DisplayName("Should return empty list with message when no pending data")
        void getPendingReviews_NoData_ReturnEmptyList() {
            when(reviewWorkflowMapper.selectPendingReviews(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(reviewWorkflowMapper.countPendingReviews()).thenReturn(0L);

            PageResponse<ReviewListItemResponse> response = reviewWorkflowService.getPendingReviews(1, 10);

            assertEquals(0, response.items().size());
            assertEquals(0, response.total());
            assertEquals("No submissions are currently waiting for review.", response.emptyMessage());
        }

        @Test
        @DisplayName("Should treat null pending rows as empty list")
        void getPendingReviews_NullRows_ReturnEmptyListWithMessage() {
            when(reviewWorkflowMapper.selectPendingReviews(anyInt(), anyInt()))
                    .thenReturn(null);
            when(reviewWorkflowMapper.countPendingReviews()).thenReturn(0L);

            PageResponse<ReviewListItemResponse> response = reviewWorkflowService.getPendingReviews(1, 10);

            assertAll(
                    () -> assertNotNull(response.items()),
                    () -> assertTrue(response.items().isEmpty()),
                    () -> assertEquals("No submissions are currently waiting for review.", response.emptyMessage())
            );
        }

        @Test
        @DisplayName("Should return valid pagination response when data exists")
        void getPendingReviews_HasData_ReturnValidResponse() {
            ReviewSubmissionRow row = mockSubmissionRow();
            when(reviewWorkflowMapper.selectPendingReviews(anyInt(), anyInt()))
                    .thenReturn(List.of(row));
            when(reviewWorkflowMapper.countPendingReviews()).thenReturn(1L);

            PageResponse<ReviewListItemResponse> response = reviewWorkflowService.getPendingReviews(1, 10);

            assertEquals(1, response.items().size());
            assertEquals(1, response.total());
            assertEquals(1, response.page());
            assertEquals(10, response.pageSize());
            assertNull(response.emptyMessage());
        }
    }

    @Nested
    @DisplayName("getReviewDetail: Retrieve review submission details")
    class GetReviewDetailTests {

        @Test
        @DisplayName("Should throw exception when submission ID is null")
        void getReviewDetail_NullSubmissionId_ThrowException() {
            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.getReviewDetail(null));
            assertEquals("Submission id is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw not found exception when submission does not exist")
        void getReviewDetail_InvalidSubmissionId_ThrowNotFound() {
            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(null);

            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.getReviewDetail(TEST_SUBMISSION_ID));
            assertEquals("Review submission does not exist.", exception.getMessage());
        }

        @Test
        @DisplayName("Should return complete details for valid submission ID")
        void getReviewDetail_ValidId_ReturnDetail() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);
            when(reviewWorkflowMapper.selectReviewHistoryRows(TEST_RESOURCE_ID)).thenReturn(Collections.emptyList());
            when(resourceFileMapper.selectByResourceId(TEST_RESOURCE_ID)).thenReturn(Collections.emptyList());
            when(resourceTagMapper.selectTagNamesByResourceId(TEST_RESOURCE_ID)).thenReturn(Collections.emptyList());

            ReviewDetailResponse response = reviewWorkflowService.getReviewDetail(TEST_SUBMISSION_ID);

            assertEquals(TEST_SUBMISSION_ID, response.submissionId());
            assertEquals(TEST_RESOURCE_ID, response.resourceId());
            assertEquals(TEST_VERSION, response.versionNo());
            assertEquals(ResourceReviewStatus.PENDING_REVIEW, response.resourceStatus());
        }

        @Test
        @DisplayName("Should mark as resubmission when history contains other submission ids")
        void getReviewDetail_HistoryHasOtherSubmission_MarkResubmission() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            submission.setVersionNo(1);
            ReviewHistoryRow oldRow = new ReviewHistoryRow();
            oldRow.setSubmissionId(999L);
            oldRow.setResourceId(TEST_RESOURCE_ID);
            oldRow.setVersionNo(0);
            oldRow.setStatus(ResourceReviewStatus.REJECTED.toDatabaseValue());
            oldRow.setReviewRecordId(3L);

            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);
            when(reviewWorkflowMapper.selectReviewHistoryRows(TEST_RESOURCE_ID)).thenReturn(List.of(oldRow));
            when(resourceFileMapper.selectByResourceId(TEST_RESOURCE_ID)).thenReturn(Collections.emptyList());
            when(resourceTagMapper.selectTagNamesByResourceId(TEST_RESOURCE_ID)).thenReturn(Collections.emptyList());

            ReviewDetailResponse response = reviewWorkflowService.getReviewDetail(TEST_SUBMISSION_ID);

            assertTrue(response.submission().resubmission());
            assertTrue(response.submission().currentContextLabel().contains("Current Resubmission"));
        }
    }

    @Nested
    @DisplayName("getReviewHistory: Retrieve review history sections")
    class GetReviewHistoryTests {

        @Test
        @DisplayName("Should return structured sections for valid submission")
        void getReviewHistory_ValidSubmission_ReturnSections() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            ReviewHistoryRow historyRow = mockHistoryRow();

            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);
            when(reviewWorkflowMapper.selectReviewHistoryRows(TEST_RESOURCE_ID)).thenReturn(List.of(historyRow));

            ReviewHistoryResponse response = reviewWorkflowService.getReviewHistory(TEST_SUBMISSION_ID);

            assertEquals(TEST_SUBMISSION_ID, response.submissionId());
            assertFalse(response.resubmission());
            assertEquals(1, response.sections().size());
        }

        @Test
        @DisplayName("Should return previous and current sections for resubmission history")
        void getReviewHistory_Resubmission_ReturnTwoSections() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            submission.setVersionNo(2);
            submission.setLatestVersionNo(2);
            ReviewHistoryRow previousRow = new ReviewHistoryRow();
            previousRow.setSubmissionId(1L);
            previousRow.setResourceId(TEST_RESOURCE_ID);
            previousRow.setVersionNo(1);
            previousRow.setStatus(ResourceReviewStatus.REJECTED.toDatabaseValue());
            previousRow.setReviewRecordId(10L);
            ReviewHistoryRow currentRow = new ReviewHistoryRow();
            currentRow.setSubmissionId(TEST_SUBMISSION_ID);
            currentRow.setResourceId(TEST_RESOURCE_ID);
            currentRow.setVersionNo(2);
            currentRow.setStatus(ResourceReviewStatus.PENDING_REVIEW.toDatabaseValue());
            currentRow.setReviewRecordId(11L);

            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);
            when(reviewWorkflowMapper.selectReviewHistoryRows(TEST_RESOURCE_ID)).thenReturn(List.of(previousRow, currentRow));

            ReviewHistoryResponse response = reviewWorkflowService.getReviewHistory(TEST_SUBMISSION_ID);

            assertAll(
                    () -> assertTrue(response.resubmission()),
                    () -> assertEquals(2, response.sections().size()),
                    () -> assertEquals("Previous Reviews", response.sections().get(0).label()),
                    () -> assertEquals("Current Resubmission", response.sections().get(1).label())
            );
        }
    }

    @Nested
    @DisplayName("approveSubmission: Approve a pending submission")
    class ApproveSubmissionTests {

        @Test
        @DisplayName("Should process successfully with null request")
        void approveSubmission_NullRequest_ProcessSuccess() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);
            when(reviewWorkflowMapper.updateResourceAfterDecision(anyLong(), anyString(), any(LocalDateTime.class), anyString())).thenReturn(1);
            when(reviewRecordMapper.insert(any(ReviewRecord.class))).thenReturn(1);
            when(appUserMapper.selectById(anyLong())).thenReturn(new AppUser());

            ReviewDecisionResponse response = reviewWorkflowService.approveSubmission(TEST_SUBMISSION_ID, TEST_REVIEWER_ID, null);

            assertEquals(ReviewAction.APPROVE, response.action());
            assertEquals(ResourceReviewStatus.APPROVED, response.resourceStatus());
            assertNotNull(response.reviewedAt());
        }

        @Test
        @DisplayName("Normal case: Should approve successfully with non-null valid request")
        void approveSubmission_ValidNonNullRequest_Success() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            ReviewActionRequest request = new ReviewActionRequest(
                    TEST_RESOURCE_ID, TEST_VERSION, null, "Approved with comment");

            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);
            when(reviewWorkflowMapper.updateResourceAfterDecision(anyLong(), anyString(), any(LocalDateTime.class), anyString()))
                    .thenReturn(1);
            when(reviewRecordMapper.insert(any(ReviewRecord.class))).thenReturn(1);
            when(appUserMapper.selectById(anyLong())).thenReturn(new AppUser());

            ReviewDecisionResponse response = reviewWorkflowService.approveSubmission(
                    TEST_SUBMISSION_ID, TEST_REVIEWER_ID, request);

            assertEquals(ReviewAction.APPROVE, response.action());
            assertEquals(ResourceReviewStatus.APPROVED, response.resourceStatus());
            assertEquals("Approved with comment", response.feedbackComment());
            assertNotNull(response.reviewedAt());
            assertTrue(response.removedFromPendingQueue());
        }
    }

    @Nested
    @DisplayName("rejectSubmission: Reject a pending submission")
    class RejectSubmissionTests {

        @Test
        @DisplayName("Should throw exception when rejecting without comments")
        void rejectSubmission_EmptyComment_ThrowException() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);
            ReviewActionRequest request = new ReviewActionRequest(TEST_RESOURCE_ID, TEST_VERSION, null, EMPTY_COMMENT);

            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.rejectSubmission(TEST_SUBMISSION_ID, TEST_REVIEWER_ID, request));
            assertEquals("Review decision request is invalid.", exception.getMessage());
        }

        @Test
        @DisplayName("Should process successfully with valid rejection comment")
        void rejectSubmission_ValidComment_ProcessSuccess() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            ReviewActionRequest request = new ReviewActionRequest(TEST_RESOURCE_ID, TEST_VERSION, null, TEST_COMMENT);

            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);
            when(reviewWorkflowMapper.updateResourceAfterDecision(anyLong(), anyString(), any(LocalDateTime.class), anyString())).thenReturn(1);
            when(reviewRecordMapper.insert(any(ReviewRecord.class))).thenReturn(1);
            when(appUserMapper.selectById(anyLong())).thenReturn(new AppUser());

            ReviewDecisionResponse response = reviewWorkflowService.rejectSubmission(TEST_SUBMISSION_ID, TEST_REVIEWER_ID, request);

            assertEquals(ReviewAction.REJECT, response.action());
            assertEquals(ResourceReviewStatus.REJECTED, response.resourceStatus());
            assertEquals(TEST_COMMENT, response.feedbackComment());
        }
    }

    @Nested
    @DisplayName("submitDecision: Submit final review decision")
    class SubmitDecisionTests {

        @Test
        @DisplayName("Should throw exception when request is null")
        void submitDecision_NullRequest_ThrowException() {
            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.submitDecision(TEST_SUBMISSION_ID, TEST_REVIEWER_ID, null));
            assertEquals("Review decision request is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when path ID does not match body ID")
        void submitDecision_IdMismatch_ThrowException() {
            ReviewDecisionRequest request = new ReviewDecisionRequest(2L, TEST_RESOURCE_ID, TEST_VERSION, TEST_REVIEWER_ID, ReviewAction.APPROVE, TEST_COMMENT);

            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.submitDecision(TEST_SUBMISSION_ID, TEST_REVIEWER_ID, request));
            assertEquals("Review decision request is invalid.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when request misses required fields")
        void submitDecision_MissingRequiredFields_ThrowException() {
            ReviewDecisionRequest request = new ReviewDecisionRequest(
                    TEST_SUBMISSION_ID,
                    null,
                    null,
                    TEST_REVIEWER_ID,
                    null,
                    TEST_COMMENT
            );

            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.submitDecision(TEST_SUBMISSION_ID, TEST_REVIEWER_ID, request));

            assertAll(
                    () -> assertEquals("Review decision request is invalid.", exception.getMessage()),
                    () -> assertTrue(exception.getDetails().contains("resourceId is required.")),
                    () -> assertTrue(exception.getDetails().contains("versionNo is required.")),
                    () -> assertTrue(exception.getDetails().contains("action is required."))
            );
        }

        @Test
        @DisplayName("Should throw conflict when reviewing non-latest submission")
        void submitDecision_NotLatestSubmission_ThrowConflict() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            submission.setLatestSubmissionId(2L);
            ReviewDecisionRequest request = new ReviewDecisionRequest(TEST_SUBMISSION_ID, TEST_RESOURCE_ID, TEST_VERSION, TEST_REVIEWER_ID, ReviewAction.APPROVE, TEST_COMMENT);

            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);

            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.submitDecision(TEST_SUBMISSION_ID, TEST_REVIEWER_ID, request));
            assertEquals("Only the latest pending submission can be reviewed.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw conflict when submission is not in pending status")
        void submitDecision_NotPendingStatus_ThrowConflict() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            submission.setResourceStatus(ResourceStatusEnum.APPROVED.getValue());
            ReviewDecisionRequest request = new ReviewDecisionRequest(TEST_SUBMISSION_ID, TEST_RESOURCE_ID, TEST_VERSION, TEST_REVIEWER_ID, ReviewAction.APPROVE, TEST_COMMENT);

            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);

            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.submitDecision(TEST_SUBMISSION_ID, TEST_REVIEWER_ID, request));
            assertEquals("This submission is no longer pending review.", exception.getMessage());
        }

        @Test
        @DisplayName("Normal case: Should submit successfully with valid parameters")
        void submitDecision_ValidRequest_NoException_Success() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            ReviewDecisionRequest request = new ReviewDecisionRequest(
                    TEST_SUBMISSION_ID, TEST_RESOURCE_ID, TEST_VERSION, TEST_REVIEWER_ID, ReviewAction.APPROVE, "Looks good");

            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);
            when(reviewWorkflowMapper.updateResourceAfterDecision(anyLong(), anyString(), any(LocalDateTime.class), anyString()))
                    .thenReturn(1);
            when(reviewRecordMapper.insert(any(ReviewRecord.class))).thenReturn(1);
            when(appUserMapper.selectById(anyLong())).thenReturn(new AppUser());

            ReviewDecisionResponse response = reviewWorkflowService.submitDecision(
                    TEST_SUBMISSION_ID, TEST_REVIEWER_ID, request);

            assertEquals(TEST_SUBMISSION_ID, response.submissionId());
            assertEquals(TEST_RESOURCE_ID, response.resourceId());
            assertEquals(ReviewAction.APPROVE, response.action());
            assertEquals(ResourceReviewStatus.APPROVED, response.resourceStatus());
            assertEquals("Looks good", response.feedbackComment());
            assertTrue(response.removedFromPendingQueue());
            assertNotNull(response.reviewedAt());
        }

        @Test
        @DisplayName("Should throw conflict when update row count is zero")
        void submitDecision_UpdateRowsZero_ThrowConflict() {
            ReviewSubmissionRow submission = mockSubmissionRow();
            ReviewDecisionRequest request = new ReviewDecisionRequest(
                    TEST_SUBMISSION_ID, TEST_RESOURCE_ID, TEST_VERSION, TEST_REVIEWER_ID, ReviewAction.APPROVE, TEST_COMMENT);
            when(reviewWorkflowMapper.selectSubmissionDetail(TEST_SUBMISSION_ID)).thenReturn(submission);
            when(reviewWorkflowMapper.updateResourceAfterDecision(anyLong(), anyString(), any(LocalDateTime.class), anyString()))
                    .thenReturn(0);

            AppException exception = assertThrows(AppException.class,
                    () -> reviewWorkflowService.submitDecision(TEST_SUBMISSION_ID, TEST_REVIEWER_ID, request));

            assertEquals("This submission is no longer pending review.", exception.getMessage());
        }
    }
}
