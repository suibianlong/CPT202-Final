package com.cpt202.HerLink.dto.review;

import java.util.List;

public record ReviewHistoryResponse(
        Long submissionId,
        Long resourceId,
        Integer versionNo,
        boolean resubmission,
        List<ReviewHistorySectionResponse> sections
) {
}
