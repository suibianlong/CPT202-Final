package com.cpt202.HerLink.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// test all types of files
class FileTypeValidatorTest {

    @Test
    void isSupported_shouldReturnTrueForAllowedImageFile() {
        // setup
        String filename = "photo.jpg";

        // call
        boolean result = FileTypeValidator.isSupported(filename);

        // assertion
        assertTrue(result);
    }

    @Test
    void isSupported_shouldReturnTrueForAllowedVideoFile() {
        // setup
        String filename = "demo.mp4";

        // call
        boolean result = FileTypeValidator.isSupported(filename);

        // assertion
        assertTrue(result);
    }

    @Test
    void isSupported_shouldReturnTrueForUpperCaseExtension() {
        // setup
        String filename = "report.PDF";

        // call
        boolean result = FileTypeValidator.isSupported(filename);

        // assertion
        assertTrue(result);
    }

    @Test
    void isSupported_shouldReturnFalseForUnsupportedExtension() {
        // setup
        String filename = "virus.exe";

        // call
        boolean result = FileTypeValidator.isSupported(filename);

        // assertion
        assertFalse(result);
    }

    @Test
    void isSupported_shouldReturnFalseWhenFilenameHasNoExtension() {
        // setup
        String filename = "README";

        // call
        boolean result = FileTypeValidator.isSupported(filename);

        // assertion
        assertFalse(result);
    }

    @Test
    void isSupported_shouldReturnFalseWhenFilenameIsNull() {
        // setup
        String filename = null;

        // call
        boolean result = FileTypeValidator.isSupported(filename);

        // assertion
        assertFalse(result);
    }

    @Test
    void isSupported_shouldReturnFalseWhenFilenameIsBlank() {
        // setup
        String filename = " ";

        // call
        boolean result = FileTypeValidator.isSupported(filename);

        // assertion
        assertFalse(result);
    }

    @Test
    void isSupported_shouldReturnFalseWhenFilenameEndsWithDot() {
        // setup
        String filename = "file.";

        // call
        boolean result = FileTypeValidator.isSupported(filename);

        // assertion
        assertFalse(result);
    }

    @Test
    void isPreviewImageSupported_shouldRejectPdfPreview() {
        assertFalse(FileTypeValidator.isPreviewImageSupported("preview.pdf", "application/pdf"));
    }

    @Test
    void isMediaFileSupported_shouldMatchPhotoResourceType() {
        assertTrue(FileTypeValidator.isMediaFileSupported("cover.png", "image/png", "photo"));
        assertTrue(FileTypeValidator.isMediaFileSupported("cover.png", "image/png", "Picture"));
        assertFalse(FileTypeValidator.isMediaFileSupported("clip.mp4", "video/mp4", "photo"));
    }

    @Test
    void isMediaFileSupported_shouldMatchDocumentResourceType() {
        assertTrue(
                FileTypeValidator.isMediaFileSupported(
                        "archive.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "document"
                )
        );
        assertFalse(FileTypeValidator.isMediaFileSupported("archive.docx", "application/octet-stream", "document"));
    }

    @Test
    void isMediaFileTypeSupported_shouldUseStoredExtension() {
        assertTrue(FileTypeValidator.isMediaFileTypeSupported("mp4", "video"));
        assertFalse(FileTypeValidator.isMediaFileTypeSupported("mp4", "audio"));
        assertTrue(FileTypeValidator.isPreviewImageFileType("jpeg"));
    }

    @Test
    void isMediaFileSupported_shouldRejectUnsupportedUploadOnlyTypes() {
        assertFalse(FileTypeValidator.isMediaFileSupported("cover.png", "image/png", "extra link"));
        assertFalse(FileTypeValidator.isMediaFileTypeSupported("png", "other"));
    }

    @Test
    void isFeedbackAttachmentSupported_shouldMatchAllowedTypes() {
        assertTrue(FileTypeValidator.isFeedbackAttachmentSupported("note.jpg", "image/jpeg"));
        assertTrue(FileTypeValidator.isFeedbackAttachmentSupported("note.png", "image/png"));
        assertTrue(FileTypeValidator.isFeedbackAttachmentSupported("note.pdf", "application/pdf"));
        assertTrue(FileTypeValidator.isFeedbackAttachmentSupported("note.txt", "text/plain"));
        assertFalse(FileTypeValidator.isFeedbackAttachmentSupported("note.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertFalse(FileTypeValidator.isFeedbackAttachmentSupported("note.png", "application/octet-stream"));
    }
} 
