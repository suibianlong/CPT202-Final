package com.cpt202.HerLink.dto.admin;

import com.cpt202.HerLink.dto.review.ResourceReviewStatus;
import java.time.LocalDateTime;

public record AdminResourceLifecycleResponse(
        Long resourceId,
        String title,
        ResourceReviewStatus previousStatus,
        ResourceReviewStatus resourceStatus,
        LocalDateTime archivedAt,
        LocalDateTime updatedAt,
        boolean changed,
        String message
) {
}
