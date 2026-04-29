package com.cpt202.HerLink.service.review;

import com.cpt202.HerLink.dto.review.PageResponse;
import com.cpt202.HerLink.dto.review.ReviewActionRequest;
import com.cpt202.HerLink.dto.review.ReviewDecisionRequest;
import com.cpt202.HerLink.dto.review.ReviewDecisionResponse;
import com.cpt202.HerLink.dto.review.ReviewDetailResponse;
import com.cpt202.HerLink.dto.review.ReviewHistoryResponse;
import com.cpt202.HerLink.dto.review.ReviewListItemResponse;

public interface ReviewWorkflowService {

    PageResponse<ReviewListItemResponse> getPendingReviews(int page, int pageSize);

    ReviewDetailResponse getReviewDetail(Long submissionId);

    ReviewHistoryResponse getReviewHistory(Long submissionId);

    ReviewDecisionResponse approveSubmission(Long submissionId, Long reviewerId, ReviewActionRequest request);

    ReviewDecisionResponse rejectSubmission(Long submissionId, Long reviewerId, ReviewActionRequest request);

    ReviewDecisionResponse submitDecision(Long submissionId, Long reviewerId, ReviewDecisionRequest request);
}
