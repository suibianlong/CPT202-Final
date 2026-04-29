package com.cpt202.HerLink.dto.review;

import java.time.LocalDateTime;

public record ResourceFileSection(
        Long fileId,
        String originalFilename,
        String storedFilename,
        String filePath,
        String fileType,
        Long fileSize,
        LocalDateTime uploadedAt
) {
}
