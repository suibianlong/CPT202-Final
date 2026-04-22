package com.cpt202.HerLink.service.review;

import com.cpt202.HerLink.dto.review.CategorySection;
import com.cpt202.HerLink.dto.review.ContributorSection;
import com.cpt202.HerLink.dto.review.PageResponse;
import com.cpt202.HerLink.dto.review.ResourceFileSection;
import com.cpt202.HerLink.dto.review.ResourceReviewStatus;
import com.cpt202.HerLink.dto.review.ResourceSection;
import com.cpt202.HerLink.dto.review.ReviewAction;
import com.cpt202.HerLink.dto.review.ReviewActionRequest;
import com.cpt202.HerLink.dto.review.ReviewDecisionRequest;
import com.cpt202.HerLink.dto.review.ReviewDecisionResponse;
import com.cpt202.HerLink.dto.review.ReviewDetailResponse;
import com.cpt202.HerLink.dto.review.ReviewHistoryContextType;
import com.cpt202.HerLink.dto.review.ReviewHistoryItemResponse;
import com.cpt202.HerLink.dto.review.ReviewHistoryResponse;
import com.cpt202.HerLink.dto.review.ReviewHistoryRow;
import com.cpt202.HerLink.dto.review.ReviewHistorySectionResponse;
import com.cpt202.HerLink.dto.review.ReviewListItemResponse;
import com.cpt202.HerLink.dto.review.ReviewSubmissionRow;
import com.cpt202.HerLink.dto.review.SubmissionSection;
import com.cpt202.HerLink.entity.ResourceFile;
import com.cpt202.HerLink.entity.ReviewRecord;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.ResourceFileMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ReviewRecordMapper;
import com.cpt202.HerLink.mapper.ReviewWorkflowMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewWorkflowServiceImpl implements ReviewWorkflowService {

    private static final String EMPTY_PENDING_MESSAGE = "No submissions are currently waiting for review.";
    private static final String REJECTION_COMMENTS_REQUIRED = "Rejection comments are required.";

    private final ReviewWorkflowMapper reviewWorkflowMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ResourceFileMapper resourceFileMapper;
    private final ResourceTagMapper resourceTagMapper;

    public ReviewWorkflowServiceImpl(ReviewWorkflowMapper reviewWorkflowMapper,
                                     ReviewRecordMapper reviewRecordMapper,
                                     ResourceFileMapper resourceFileMapper,
                                     ResourceTagMapper resourceTagMapper) {
        this.reviewWorkflowMapper = reviewWorkflowMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.resourceFileMapper = resourceFileMapper;
        this.resourceTagMapper = resourceTagMapper;
    }

    @Override
    public PageResponse<ReviewListItemResponse> getPendingReviews(int page, int pageSize) {
        validatePagination(page, pageSize);

        int offset = (page - 1) * pageSize;
        List<ReviewSubmissionRow> rows = reviewWorkflowMapper.selectPendingReviews(offset, pageSize);
        if (rows == null) {
            rows = Collections.emptyList();
        }

        List<ReviewListItemResponse> items = new ArrayList<>();
        for (ReviewSubmissionRow row : rows) {
            items.add(new ReviewListItemResponse(
                    row.getSubmissionId(),
                    row.getResourceId(),
                    row.getVersionNo(),
                    row.getTitle(),
                    row.getContributorId(),
                    row.getContributorName(),
                    row.getCategoryId(),
                    row.getCategoryTopic(),
                    row.getSubmittedAt(),
                    ResourceReviewStatus.fromDatabaseValue(row.getResourceStatus())
            ));
        }

        long total = reviewWorkflowMapper.countPendingReviews();
        return new PageResponse<>(
                items,
                page,
                pageSize,
                total,
                items.isEmpty() ? EMPTY_PENDING_MESSAGE : null
        );
    }

    @Override
    public ReviewDetailResponse getReviewDetail(Long submissionId) {
        ReviewSubmissionRow submission = loadSubmission(submissionId);
        List<ReviewHistoryRow> historyRows = loadHistoryRows(submission.getResourceId());
        boolean resubmission = isResubmission(submission, historyRows);

        return new ReviewDetailResponse(
                submission.getSubmissionId(),
                submission.getResourceId(),
                submission.getVersionNo(),
                ResourceReviewStatus.fromDatabaseValue(submission.getResourceStatus()),
                new ResourceSection(
                        submission.getTitle(),
                        submission.getDescription(),
                        submission.getPlace(),
                        submission.getResourceType(),
                        submission.getPreviewImage(),
                        submission.getMediaUrl(),
                        submission.getCopyrightDeclaration(),
                        submission.getCreatedAt(),
                        submission.getUpdatedAt(),
                        submission.getReviewedAt(),
                        buildFileSections(submission.getResourceId())
                ),
                new ContributorSection(submission.getContributorId(), submission.getContributorName()),
                new CategorySection(submission.getCategoryId(), submission.getCategoryTopic()),
                new SubmissionSection(
                        submission.getSubmittedAt(),
                        submission.getSubmittedBy(),
                        submission.getSubmissionNote(),
                        ResourceReviewStatus.fromDatabaseValue(submission.getStatusSnapshot()),
                        resubmission,
                        buildCurrentContextLabel(resubmission, submission.getVersionNo())
                ),
                safeTagNames(submission.getResourceId()),
                buildHistoryItems(historyRows, submission, resubmission)
        );
    }

    @Override
    public ReviewHistoryResponse getReviewHistory(Long submissionId) {
        ReviewSubmissionRow submission = loadSubmission(submissionId);
        List<ReviewHistoryRow> historyRows = loadHistoryRows(submission.getResourceId());
        boolean resubmission = isResubmission(submission, historyRows);
        List<ReviewHistoryItemResponse> historyItems = buildHistoryItems(historyRows, submission, resubmission);

        List<ReviewHistoryItemResponse> previousReviews = new ArrayList<>();
        List<ReviewHistoryItemResponse> currentSubmission = new ArrayList<>();
        for (ReviewHistoryItemResponse item : historyItems) {
            if (item.contextType() == ReviewHistoryContextType.PREVIOUS_SUBMISSION) {
                previousReviews.add(item);
            } else {
                currentSubmission.add(item);
            }
        }

        List<ReviewHistorySectionResponse> sections = new ArrayList<>();
        if (resubmission || !previousReviews.isEmpty()) {
            sections.add(new ReviewHistorySectionResponse(
                    "Previous Reviews",
                    ReviewHistoryContextType.PREVIOUS_SUBMISSION,
                    previousReviews
            ));
        }
        sections.add(new ReviewHistorySectionResponse(
                resubmission ? "Current Resubmission" : "Current Submission",
                ReviewHistoryContextType.CURRENT_SUBMISSION,
                currentSubmission
        ));

        return new ReviewHistoryResponse(
                submission.getSubmissionId(),
                submission.getResourceId(),
                submission.getVersionNo(),
                resubmission,
                sections
        );
    }

    @Override
    @Transactional
    public ReviewDecisionResponse approveSubmission(Long submissionId, Long reviewerId, ReviewActionRequest request) {
        ReviewActionRequest safeRequest = request == null
                ? new ReviewActionRequest(null, null, null, null)
                : request;
        return submitReviewDecision(
                submissionId,
                reviewerId,
                ReviewAction.APPROVE,
                safeRequest.resourceId(),
                safeRequest.versionNo(),
                safeRequest.feedbackComment()
        );
    }

    @Override
    @Transactional
    public ReviewDecisionResponse rejectSubmission(Long submissionId, Long reviewerId, ReviewActionRequest request) {
        ReviewActionRequest safeRequest = request == null
                ? new ReviewActionRequest(null, null, null, null)
                : request;
        return submitReviewDecision(
                submissionId,
                reviewerId,
                ReviewAction.REJECT,
                safeRequest.resourceId(),
                safeRequest.versionNo(),
                safeRequest.feedbackComment()
        );
    }

    @Override
    @Transactional
    public ReviewDecisionResponse submitDecision(Long submissionId, Long reviewerId, ReviewDecisionRequest request) {
        if (request == null) {
            throw AppException.badRequest("Review decision request is required.");
        }
        validateRequiredDecisionRequest(submissionId, request);

        return submitReviewDecision(
                submissionId,
                reviewerId,
                request.action(),
                request.resourceId(),
                request.versionNo(),
                request.feedbackComment()
        );
    }

    private ReviewDecisionResponse submitReviewDecision(Long submissionId,
                                                        Long reviewerId,
                                                        ReviewAction action,
                                                        Long requestedResourceId,
                                                        Integer requestedVersionNo,
                                                        String requestedFeedbackComment) {
        ReviewSubmissionRow submission = loadSubmission(submissionId);
        validateDecisionRequest(submission, action, requestedResourceId, requestedVersionNo, requestedFeedbackComment);

        LocalDateTime reviewedAt = LocalDateTime.now();
        ResourceReviewStatus nextStatus = action == ReviewAction.APPROVE
                ? ResourceReviewStatus.APPROVED
                : ResourceReviewStatus.REJECTED;
        String feedbackComment = action == ReviewAction.APPROVE
                ? normalizeComment(requestedFeedbackComment)
                : normalizeRequiredRejectionComment(requestedFeedbackComment);

        int updatedRows = reviewWorkflowMapper.updateResourceAfterDecision(
                submission.getResourceId(),
                nextStatus.toDatabaseValue(),
                reviewedAt,
                ResourceStatusEnum.PENDING_REVIEW.getValue()
        );
        if (updatedRows == 0) {
            throw AppException.conflict("This submission is no longer pending review.");
        }

        ReviewRecord reviewRecord = new ReviewRecord();
        reviewRecord.setResourceId(submission.getResourceId());
        reviewRecord.setSubmissionId(submission.getSubmissionId());
        reviewRecord.setVersionNo(submission.getVersionNo());
        reviewRecord.setReviewerId(reviewerId);
        reviewRecord.setActionDescription(action.name());
        reviewRecord.setStatus(nextStatus.toDatabaseValue());
        reviewRecord.setFeedbackComment(feedbackComment);
        reviewRecord.setReviewedAt(reviewedAt);
        reviewRecord.setCreatedAt(reviewedAt);
        reviewRecordMapper.insert(reviewRecord);

        return new ReviewDecisionResponse(
                reviewRecord.getReviewRecordId(),
                submission.getSubmissionId(),
                submission.getResourceId(),
                submission.getVersionNo(),
                action,
                nextStatus,
                feedbackComment,
                reviewedAt,
                true
        );
    }

    private ReviewSubmissionRow loadSubmission(Long submissionId) {
        if (submissionId == null) {
            throw AppException.badRequest("Submission id is required.");
        }

        ReviewSubmissionRow submission = reviewWorkflowMapper.selectSubmissionDetail(submissionId);
        if (submission == null) {
            throw AppException.notFound("Review submission does not exist.");
        }
        return submission;
    }

    private void validatePagination(int page, int pageSize) {
        List<String> details = new ArrayList<>();
        if (page < 1) {
            details.add("page must be at least 1.");
        }
        if (pageSize < 1 || pageSize > 50) {
            details.add("pageSize must be between 1 and 50.");
        }
        if (!details.isEmpty()) {
            throw AppException.badRequest("Invalid pagination request.", details);
        }
    }

    private void validateRequiredDecisionRequest(Long pathSubmissionId, ReviewDecisionRequest request) {
        List<String> details = new ArrayList<>();
        if (request.submissionId() == null) {
            details.add("submissionId is required.");
        } else if (!Objects.equals(pathSubmissionId, request.submissionId())) {
            details.add("Submission id in path and body must match.");
        }
        if (request.resourceId() == null) {
            details.add("resourceId is required.");
        }
        if (request.versionNo() == null) {
            details.add("versionNo is required.");
        }
        if (request.action() == null) {
            details.add("action is required.");
        }
        if (!details.isEmpty()) {
            throw AppException.badRequest("Review decision request is invalid.", details);
        }
    }

    private void validateDecisionRequest(ReviewSubmissionRow submission,
                                         ReviewAction action,
                                         Long requestedResourceId,
                                         Integer requestedVersionNo,
                                         String requestedFeedbackComment) {
        List<String> details = new ArrayList<>();

        if (action == null) {
            details.add("action is required.");
        }
        if (requestedResourceId != null && !Objects.equals(requestedResourceId, submission.getResourceId())) {
            details.add("resourceId does not match the selected submission.");
        }
        if (requestedVersionNo != null && !Objects.equals(requestedVersionNo, submission.getVersionNo())) {
            details.add("versionNo does not match the selected submission.");
        }
        if (action == ReviewAction.REJECT && normalizeComment(requestedFeedbackComment) == null) {
            details.add(REJECTION_COMMENTS_REQUIRED);
        }
        if (!details.isEmpty()) {
            throw AppException.badRequest("Review decision request is invalid.", details);
        }

        if (ResourceReviewStatus.fromDatabaseValue(submission.getResourceStatus()) != ResourceReviewStatus.PENDING_REVIEW) {
            throw AppException.conflict("This submission is no longer pending review.");
        }
        if (!Objects.equals(submission.getSubmissionId(), submission.getLatestSubmissionId())
                || !Objects.equals(submission.getVersionNo(), submission.getLatestVersionNo())) {
            throw AppException.conflict("Only the latest pending submission can be reviewed.");
        }
    }

    private List<ReviewHistoryRow> loadHistoryRows(Long resourceId) {
        List<ReviewHistoryRow> historyRows = reviewWorkflowMapper.selectReviewHistoryRows(resourceId);
        return historyRows == null ? Collections.emptyList() : historyRows;
    }

    private List<ResourceFileSection> buildFileSections(Long resourceId) {
        List<ResourceFile> files = resourceFileMapper.selectByResourceId(resourceId);
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        List<ResourceFileSection> fileSections = new ArrayList<>();
        for (ResourceFile file : files) {
            fileSections.add(new ResourceFileSection(
                    file.getFileId(),
                    file.getOriginalFilename(),
                    file.getStoredFilename(),
                    file.getFilePath(),
                    file.getFileType(),
                    file.getFileSize(),
                    file.getUploadedAt()
            ));
        }
        return fileSections;
    }

    private List<String> safeTagNames(Long resourceId) {
        List<String> tagNames = resourceTagMapper.selectTagNamesByResourceId(resourceId);
        return tagNames == null ? Collections.emptyList() : tagNames;
    }

    private boolean isResubmission(ReviewSubmissionRow submission, List<ReviewHistoryRow> historyRows) {
        if (submission.getVersionNo() != null && submission.getVersionNo() > 1) {
            return true;
        }
        for (ReviewHistoryRow row : historyRows) {
            if (!Objects.equals(row.getSubmissionId(), submission.getSubmissionId())) {
                return true;
            }
        }
        return false;
    }

    private List<ReviewHistoryItemResponse> buildHistoryItems(List<ReviewHistoryRow> historyRows,
                                                              ReviewSubmissionRow currentSubmission,
                                                              boolean resubmission) {
        List<ReviewHistoryItemResponse> items = new ArrayList<>();
        for (ReviewHistoryRow row : historyRows) {
            items.add(toHistoryItem(row, currentSubmission, resubmission));
        }
        return items;
    }

    private ReviewHistoryItemResponse toHistoryItem(ReviewHistoryRow row,
                                                    ReviewSubmissionRow currentSubmission,
                                                    boolean resubmission) {
        boolean current = Objects.equals(row.getSubmissionId(), currentSubmission.getSubmissionId());
        ReviewHistoryContextType contextType = current
                ? ReviewHistoryContextType.CURRENT_SUBMISSION
                : ReviewHistoryContextType.PREVIOUS_SUBMISSION;
        String contextLabel = current
                ? buildCurrentContextLabel(resubmission, row.getVersionNo())
                : "Previous Reviews - Version " + row.getVersionNo();
        ResourceReviewStatus status = ResourceReviewStatus.fromDatabaseValue(row.getStatus());

        return new ReviewHistoryItemResponse(
                row.getReviewRecordId(),
                row.getResourceId(),
                row.getSubmissionId(),
                row.getVersionNo(),
                row.getReviewerId(),
                row.getReviewerName() == null ? "reviewer_" + row.getReviewerId() : row.getReviewerName(),
                status == ResourceReviewStatus.APPROVED ? ReviewAction.APPROVE : ReviewAction.REJECT,
                row.getActionDescription(),
                status,
                row.getFeedbackComment(),
                row.getReviewedAt(),
                contextType,
                contextLabel
        );
    }

    private String buildCurrentContextLabel(boolean resubmission, Integer versionNo) {
        return (resubmission ? "Current Resubmission" : "Current Submission") + " - Version " + versionNo;
    }

    private String normalizeRequiredRejectionComment(String value) {
        String normalized = normalizeComment(value);
        if (normalized == null) {
            throw AppException.badRequest(REJECTION_COMMENTS_REQUIRED);
        }
        return normalized;
    }

    private String normalizeComment(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
