package com.cpt202.HerLink.dto.review;

public record ReviewDecisionRequest(
        Long submissionId,
        Long resourceId,
        Integer versionNo,
        Long reviewerId,
        ReviewAction action,
        String feedbackComment
) {
}
