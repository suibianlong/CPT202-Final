package com.cpt202.HerLink.dto.admin;

import java.time.LocalDateTime;

public record AdminResourceTypeResponse(
        Long resourceTypeId,
        String typeName,
        ClassificationStatus status,
        Integer usageCount,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {
}
