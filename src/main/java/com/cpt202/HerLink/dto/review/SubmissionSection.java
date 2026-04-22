package com.cpt202.HerLink.dto.review;

import java.time.LocalDateTime;

public record SubmissionSection(
        LocalDateTime submittedAt,
        Long submittedBy,
        String submissionNote,
        ResourceReviewStatus statusSnapshot,
        boolean resubmission,
        String currentContextLabel
) {
}
