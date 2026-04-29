package com.cpt202.HerLink.dto.review;

import java.time.LocalDateTime;
import java.util.List;

public record ResourceSection(
        String title,
        String description,
        String place,
        String resourceType,
        String previewImage,
        String mediaUrl,
        String copyrightDeclaration,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime reviewedAt,
        List<ResourceFileSection> files
) {
}
