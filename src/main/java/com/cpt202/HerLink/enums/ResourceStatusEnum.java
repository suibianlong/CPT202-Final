package com.cpt202.HerLink.enums;

public enum ResourceStatusEnum {

    DRAFT("Draft", "DRAFT"),
    PENDING_REVIEW("Pending Review", "PENDING_REVIEW"),
    APPROVED("Approved", "APPROVED"),
    REJECTED("Rejected", "REJECTED"),
    ARCHIVED("Archived", "ARCHIVED");

    private final String value;
    private final String[] aliases;

    ResourceStatusEnum(String value, String... aliases) {
        this.value = value;
        this.aliases = aliases;
    }

    public String getValue() {
        return value;
    }

    public static ResourceStatusEnum fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Resource status cannot be null");
        }

        String normalizedValue = normalize(value);

        for (ResourceStatusEnum resourceStatus : values()) {
            if (normalize(resourceStatus.value).equals(normalizedValue)) {
                return resourceStatus;
            }

            for (String alias : resourceStatus.aliases) {
                if (normalize(alias).equals(normalizedValue)) {
                    return resourceStatus;
                }
            }
        }

        throw new IllegalArgumentException("Unknown resource status: " + value);
    }

    private static String normalize(String value) {
        return value
                .trim()
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }
}
