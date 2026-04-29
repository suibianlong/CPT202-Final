package com.cpt202.HerLink.dto.review;

public record ReviewActionRequest(
        Long resourceId,
        Integer versionNo,
        Long reviewerId,
        String feedbackComment
) {
}
