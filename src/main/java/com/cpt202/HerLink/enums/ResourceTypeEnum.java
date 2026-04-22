package com.cpt202.HerLink.enums;

public enum ResourceTypeEnum {

    IMAGE("photo", "Picture", "IMAGE", "PICTURE", "PHOTO", "PHOTO/IMAGE", "PHOTO_IMAGE"),
    VIDEO("video", "Video", "VIDEO"),
    AUDIO("audio", "Audio", "AUDIO"),
    DOCUMENT("document", "Document", "DOCUMENT", "FILE/DOCUMENT", "FILE_DOCUMENT"),
    EXTRA_LINK("extra link", "Extra Link", "EXTRA LINK", "EXTRA_LINK"),
    OTHER("other", "Other", "OTHER");

    private final String value;
    private final String[] aliases;

    ResourceTypeEnum(String value, String... aliases) {
        this.value = value;
        this.aliases = aliases;
    }

    public String getValue() {
        return value;
    }

    public String[] getLookupValues() {
        String[] lookupValues = new String[aliases.length + 1];
        lookupValues[0] = value;
        System.arraycopy(aliases, 0, lookupValues, 1, aliases.length);
        return lookupValues;
    }

    public static ResourceTypeEnum fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Resource type cannot be null");
        }

        String normalizedValue = normalize(value);

        for (ResourceTypeEnum resourceType : values()) {
            if (normalize(resourceType.value).equals(normalizedValue)) {
                return resourceType;
            }

            for (String alias : resourceType.aliases) {
                if (normalize(alias).equals(normalizedValue)) {
                    return resourceType;
                }
            }
        }

        throw new IllegalArgumentException("Unknown resource type: " + value);
    }

    private static String normalize(String value) {
        return value
                .trim()
                .replace('/', ' ')
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }
}
