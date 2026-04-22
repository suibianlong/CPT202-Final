package com.cpt202.HerLink.dto.admin;

import java.time.LocalDateTime;

public record AdminTagResponse(
        Long tagId,
        String tagName,
        ClassificationStatus status,
        Integer usageCount,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {
}
