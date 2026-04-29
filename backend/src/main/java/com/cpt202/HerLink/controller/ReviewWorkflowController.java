package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.review.PageResponse;
import com.cpt202.HerLink.dto.review.ReviewActionRequest;
import com.cpt202.HerLink.dto.review.ReviewDecisionRequest;
import com.cpt202.HerLink.dto.review.ReviewDecisionResponse;
import com.cpt202.HerLink.dto.review.ReviewDetailResponse;
import com.cpt202.HerLink.dto.review.ReviewHistoryResponse;
import com.cpt202.HerLink.dto.review.ReviewListItemResponse;
import com.cpt202.HerLink.service.review.ReviewWorkflowService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviewer/reviews")
public class ReviewWorkflowController {

    private final ReviewWorkflowService reviewWorkflowService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public ReviewWorkflowController(ReviewWorkflowService reviewWorkflowService,
                                    ResourcePermissionChecker resourcePermissionChecker) {
        this.reviewWorkflowService = reviewWorkflowService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @GetMapping("/pending")
    public PageResponse<ReviewListItemResponse> getPendingReviews(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                                  HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUserId(request);
        return reviewWorkflowService.getPendingReviews(page, pageSize);
    }

    @GetMapping("/submissions/{submissionId}")
    public ReviewDetailResponse getReviewDetail(@PathVariable Long submissionId, HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUserId(request);
        return reviewWorkflowService.getReviewDetail(submissionId);
    }

    @GetMapping("/{submissionId}")
    public ReviewDetailResponse getReviewDetailAlias(@PathVariable Long submissionId, HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUserId(request);
        return reviewWorkflowService.getReviewDetail(submissionId);
    }

    @GetMapping("/submissions/{submissionId}/history")
    public ReviewHistoryResponse getReviewHistory(@PathVariable Long submissionId, HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUserId(request);
        return reviewWorkflowService.getReviewHistory(submissionId);
    }

    @GetMapping("/{submissionId}/history")
    public ReviewHistoryResponse getReviewHistoryAlias(@PathVariable Long submissionId, HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUserId(request);
        return reviewWorkflowService.getReviewHistory(submissionId);
    }

    @PostMapping("/{submissionId}/approve")
    public ReviewDecisionResponse approveSubmission(@PathVariable Long submissionId,
                                                    @RequestBody(required = false) ReviewActionRequest request,
                                                    HttpServletRequest httpServletRequest) {
        Long reviewerId = resourcePermissionChecker.requireAdminUserId(httpServletRequest);
        return reviewWorkflowService.approveSubmission(submissionId, reviewerId, request);
    }

    @PostMapping("/{submissionId}/reject")
    public ReviewDecisionResponse rejectSubmission(@PathVariable Long submissionId,
                                                   @RequestBody(required = false) ReviewActionRequest request,
                                                   HttpServletRequest httpServletRequest) {
        Long reviewerId = resourcePermissionChecker.requireAdminUserId(httpServletRequest);
        return reviewWorkflowService.rejectSubmission(submissionId, reviewerId, request);
    }

    @PostMapping("/submissions/{submissionId}/decision")
    public ReviewDecisionResponse submitDecision(@PathVariable Long submissionId,
                                                 @RequestBody ReviewDecisionRequest request,
                                                 HttpServletRequest httpServletRequest) {
        Long reviewerId = resourcePermissionChecker.requireAdminUserId(httpServletRequest);
        return reviewWorkflowService.submitDecision(submissionId, reviewerId, request);
    }
}
