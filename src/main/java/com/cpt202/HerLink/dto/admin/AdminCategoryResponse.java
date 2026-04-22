package com.cpt202.HerLink.dto.admin;

import java.time.LocalDateTime;

public record AdminCategoryResponse(
        Long categoryId,
        String categoryTopic,
        ClassificationStatus status,
        Integer usageCount,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {
}
