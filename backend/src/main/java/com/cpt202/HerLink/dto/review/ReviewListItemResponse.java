package com.cpt202.HerLink.dto.review;

import java.time.LocalDateTime;

public record ReviewListItemResponse(
        Long submissionId,
        Long resourceId,
        Integer versionNo,
        String title,
        Long contributorId,
        String contributorName,
        Long categoryId,
        String categoryTopic,
        LocalDateTime submittedAt,
        ResourceReviewStatus resourceStatus
) {
}
