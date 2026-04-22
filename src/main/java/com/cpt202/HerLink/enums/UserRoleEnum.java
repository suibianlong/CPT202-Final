package com.cpt202.HerLink.enums;

public enum UserRoleEnum {

    REGISTERED_VIEWER("user", "REGISTERED_VIEWER", "USER"),
    ADMINISTRATOR("reviewer", "ADMINISTRATOR", "REVIEWER");

    private final String value;
    private final String apiValue;
    private final String[] aliases;

    UserRoleEnum(String value, String apiValue, String... aliases) {
        this.value = value;
        this.apiValue = apiValue;
        this.aliases = aliases;
    }

    public String getValue() {
        return value;
    }

    public String getApiValue() {
        return apiValue;
    }

    public boolean matches(String value) {
        if (value == null) {
            return false;
        }

        if (this.value.equalsIgnoreCase(value) || this.apiValue.equalsIgnoreCase(value)) {
            return true;
        }

        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }

    public static UserRoleEnum fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("User role cannot be null.");
        }

        for (UserRoleEnum roleEnum : values()) {
            if (roleEnum.matches(value)) {
                return roleEnum;
            }
        }

        throw new IllegalArgumentException("Unknown user role: " + value);
    }
}
