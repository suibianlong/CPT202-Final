package com.cpt202.HerLink.dto.review;

import java.time.LocalDateTime;

public record ReviewDecisionResponse(
        Long reviewRecordId,
        Long submissionId,
        Long resourceId,
        Integer versionNo,
        ReviewAction action,
        ResourceReviewStatus resourceStatus,
        String feedbackComment,
        LocalDateTime reviewedAt,
        boolean removedFromPendingQueue
) {
}
