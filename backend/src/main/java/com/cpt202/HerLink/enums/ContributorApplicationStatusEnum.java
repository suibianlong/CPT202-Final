package com.cpt202.HerLink.enums;

public enum ContributorApplicationStatusEnum {

    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    ARCHIVED("ARCHIVED");

    private final String value;

    ContributorApplicationStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ContributorApplicationStatusEnum fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Contributor application status cannot be null");
        }

        switch (value.toUpperCase()) {
            case "PENDING":
                return PENDING;
            case "APPROVED":
                return APPROVED;
            case "REJECTED":
                return REJECTED;
            case "ARCHIVED":
                return ARCHIVED;
            default:
                throw new IllegalArgumentException("Unknown contributor application status: " + value);
        }
    }
}