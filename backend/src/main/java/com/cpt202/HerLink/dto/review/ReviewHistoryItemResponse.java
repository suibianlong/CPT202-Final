package com.cpt202.HerLink.dto.review;

import java.time.LocalDateTime;

public record ReviewHistoryItemResponse(
        Long reviewRecordId,
        Long resourceId,
        Long submissionId,
        Integer versionNo,
        Long reviewerId,
        String reviewerName,
        ReviewAction action,
        String actionDescription,
        ResourceReviewStatus status,
        String feedbackComment,
        LocalDateTime reviewedAt,
        ReviewHistoryContextType contextType,
        String contextLabel
) {
}
