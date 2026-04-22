package com.cpt202.HerLink.util;

import java.util.Locale;
import java.util.Set;

import com.cpt202.HerLink.enums.ResourceTypeEnum;

public final class FileTypeValidator {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> FEEDBACK_EXTENSIONS = Set.of("jpg", "png", "pdf", "txt");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "mp4", "mp3", "wav", "pdf", "doc", "docx", "txt"
    );
    private static final Set<String> DOCUMENT_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private FileTypeValidator() {
    }

    public static boolean isSupported(String filename) {
        String extension = getNormalizedExtension(filename);
        return extension != null && ALLOWED_EXTENSIONS.contains(extension);
    }

    public static String getNormalizedExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }

        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex < 0 || lastDotIndex == filename.length() - 1) {
            return null;
        }

        return filename.substring(lastDotIndex + 1).trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isPreviewImageSupported(String filename, String contentType) {
        String extension = getNormalizedExtension(filename);
        return extension != null
                && IMAGE_EXTENSIONS.contains(extension)
                && hasContentTypePrefix(contentType, "image/");
    }

    public static boolean isMediaFileSupported(String filename, String contentType, String resourceType) {
        String extension = getNormalizedExtension(filename);
        if (extension == null) {
            return false;
        }

        ResourceTypeEnum normalizedResourceType;
        try {
            normalizedResourceType = ResourceTypeEnum.fromValue(resourceType);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        return switch (normalizedResourceType) {
            case IMAGE -> IMAGE_EXTENSIONS.contains(extension) && hasContentTypePrefix(contentType, "image/");
            case VIDEO -> VIDEO_EXTENSIONS.contains(extension) && hasContentTypePrefix(contentType, "video/");
            case AUDIO -> AUDIO_EXTENSIONS.contains(extension) && hasContentTypePrefix(contentType, "audio/");
            case DOCUMENT -> DOCUMENT_EXTENSIONS.contains(extension) && isDocumentContentType(contentType);
            case EXTRA_LINK, OTHER -> false;
        };
    }

    public static boolean isPreviewImageFileType(String fileType) {
        String normalizedFileType = normalizeFileType(fileType);
        return normalizedFileType != null && IMAGE_EXTENSIONS.contains(normalizedFileType);
    }

    public static boolean isMediaFileTypeSupported(String fileType, String resourceType) {
        String normalizedFileType = normalizeFileType(fileType);
        if (normalizedFileType == null) {
            return false;
        }

        ResourceTypeEnum normalizedResourceType;
        try {
            normalizedResourceType = ResourceTypeEnum.fromValue(resourceType);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        return switch (normalizedResourceType) {
            case IMAGE -> IMAGE_EXTENSIONS.contains(normalizedFileType);
            case VIDEO -> VIDEO_EXTENSIONS.contains(normalizedFileType);
            case AUDIO -> AUDIO_EXTENSIONS.contains(normalizedFileType);
            case DOCUMENT -> DOCUMENT_EXTENSIONS.contains(normalizedFileType);
            case EXTRA_LINK, OTHER -> false;
        };
    }

    public static String describeMediaFileTypes(String resourceType) {
        try {
            return switch (ResourceTypeEnum.fromValue(resourceType)) {
                case IMAGE -> "JPG, JPEG, PNG, or GIF";
                case VIDEO -> "MP4";
                case AUDIO -> "MP3 or WAV";
                case DOCUMENT -> "PDF, DOC, or DOCX";
                case EXTRA_LINK, OTHER -> "This resource type does not support file uploads in the current workflow";
            };
        } catch (IllegalArgumentException exception) {
            return "supported files";
        }
    }

    public static boolean isFeedbackAttachmentSupported(String filename, String contentType) {
        String extension = getNormalizedExtension(filename);
        if (extension == null || !FEEDBACK_EXTENSIONS.contains(extension)) {
            return false;
        }

        return switch (extension) {
            case "jpg" -> hasContentTypePrefix(contentType, "image/");
            case "png" -> isExactContentType(contentType, "image/png");
            case "pdf" -> isExactContentType(contentType, "application/pdf");
            case "txt" -> isExactContentType(contentType, "text/plain");
            default -> false;
        };
    }

    public static String normalizeFeedbackFileType(String filename) {
        String extension = getNormalizedExtension(filename);
        if (extension == null || !FEEDBACK_EXTENSIONS.contains(extension)) {
            return null;
        }

        return extension.toUpperCase(Locale.ROOT);
    }

    public static String describeFeedbackAttachmentTypes() {
        return "JPG, PNG, PDF, or TXT";
    }

    private static boolean hasContentTypePrefix(String contentType, String prefix) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        return contentType.trim().toLowerCase(Locale.ROOT).startsWith(prefix);
    }

    private static boolean isDocumentContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        return DOCUMENT_CONTENT_TYPES.contains(contentType.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean isExactContentType(String contentType, String expected) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        return expected.equals(contentType.trim().toLowerCase(Locale.ROOT));
    }

    private static String normalizeFileType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return null;
        }

        return fileType.trim().toLowerCase(Locale.ROOT);
    }
}
