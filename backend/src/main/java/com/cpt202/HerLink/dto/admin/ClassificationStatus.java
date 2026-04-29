package com.cpt202.HerLink.dto.admin;

public enum ClassificationStatus {
    ACTIVE,
    INACTIVE;

    public static ClassificationStatus fromDatabaseValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Classification status cannot be null.");
        }

        for (ClassificationStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unsupported classification status: " + value);
    }
}
