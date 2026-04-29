package com.cpt202.HerLink.dto.review;

import com.cpt202.HerLink.enums.ResourceStatusEnum;

public enum ResourceReviewStatus {
    DRAFT(ResourceStatusEnum.DRAFT.getValue()),
    PENDING_REVIEW(ResourceStatusEnum.PENDING_REVIEW.getValue()),
    APPROVED(ResourceStatusEnum.APPROVED.getValue()),
    REJECTED(ResourceStatusEnum.REJECTED.getValue()),
    ARCHIVED(ResourceStatusEnum.ARCHIVED.getValue());

    private final String databaseValue;

    ResourceReviewStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String toDatabaseValue() {
        return databaseValue;
    }

    public static ResourceReviewStatus fromDatabaseValue(String value) {
        ResourceStatusEnum status = ResourceStatusEnum.fromValue(value);
        for (ResourceReviewStatus reviewStatus : values()) {
            if (reviewStatus.databaseValue.equals(status.getValue())) {
                return reviewStatus;
            }
        }
        throw new IllegalArgumentException("Unsupported resource status: " + value);
    }
}
