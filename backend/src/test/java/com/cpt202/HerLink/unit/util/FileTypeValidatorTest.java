package com.cpt202.HerLink.unit.util;

import com.cpt202.HerLink.util.*;
import com.cpt202.HerLink.util.FileTypeValidator;

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

    @Test
    void isSupportedFileType_shouldHandleValidInvalidAndBlankValues() {
        assertTrue(FileTypeValidator.isSupportedFileType("jpg"));
        assertTrue(FileTypeValidator.isSupportedFileType(" JPG "));
        assertTrue(FileTypeValidator.isSupportedFileType("docx"));
        assertFalse(FileTypeValidator.isSupportedFileType("exe"));
        assertFalse(FileTypeValidator.isSupportedFileType(" "));
        assertFalse(FileTypeValidator.isSupportedFileType(null));
    }

    @Test
    void describeMethods_shouldReturnStableDescriptions() {
        assertEquals("JPG, JPEG, PNG, GIF, MP4, MP3, WAV, PDF, DOC, DOCX, or TXT",
                FileTypeValidator.describeSupportedFileTypes());
        assertEquals("JPG, PNG, PDF, or TXT", FileTypeValidator.describeFeedbackAttachmentTypes());
    }

    @Test
    void describeMediaFileTypes_shouldCoverKnownAndUnknownResourceTypes() {
        assertEquals("JPG, JPEG, PNG, or GIF", FileTypeValidator.describeMediaFileTypes("photo"));
        assertEquals("MP4", FileTypeValidator.describeMediaFileTypes("video"));
        assertEquals("MP3 or WAV", FileTypeValidator.describeMediaFileTypes("audio"));
        assertEquals("PDF, DOC, or DOCX", FileTypeValidator.describeMediaFileTypes("document"));
        assertEquals("This resource type does not support file uploads in the current workflow",
                FileTypeValidator.describeMediaFileTypes("extra link"));
        assertEquals("This resource type does not support file uploads in the current workflow",
                FileTypeValidator.describeMediaFileTypes("other"));
        assertEquals("supported files", FileTypeValidator.describeMediaFileTypes("unknown-type"));
        assertEquals("supported files", FileTypeValidator.describeMediaFileTypes(null));
    }

    @Test
    void isPreviewImageSupported_shouldValidateExtensionAndContentTypeTogether() {
        assertTrue(FileTypeValidator.isPreviewImageSupported("cover.jpg", "image/jpeg"));
        assertFalse(FileTypeValidator.isPreviewImageSupported("cover.jpg", "application/octet-stream"));
        assertFalse(FileTypeValidator.isPreviewImageSupported("cover.jpg", ""));
        assertFalse(FileTypeValidator.isPreviewImageSupported("cover.mp4", "image/jpeg"));
    }

    @Test
    void isMediaFileSupported_shouldCoverVideoAudioAndInvalidPaths() {
        assertTrue(FileTypeValidator.isMediaFileSupported("clip.mp4", "video/mp4", "video"));
        assertFalse(FileTypeValidator.isMediaFileSupported("clip.mp4", "audio/mp4", "video"));
        assertTrue(FileTypeValidator.isMediaFileSupported("voice.mp3", "audio/mpeg", "audio"));
        assertFalse(FileTypeValidator.isMediaFileSupported("voice.mp3", "video/mp3", "audio"));

        assertFalse(FileTypeValidator.isMediaFileSupported("readme.doc", null, "document"));
        assertFalse(FileTypeValidator.isMediaFileSupported("readme.doc", " ", "document"));
        assertFalse(FileTypeValidator.isMediaFileSupported("readme.doc", "application/msword", "bad-type"));
        assertFalse(FileTypeValidator.isMediaFileSupported(null, "video/mp4", "video"));
    }

    @Test
    void isMediaFileTypeSupported_shouldHandleNullBlankAndUnknownResourceType() {
        assertTrue(FileTypeValidator.isMediaFileTypeSupported("pdf", "document"));
        assertTrue(FileTypeValidator.isMediaFileTypeSupported(" wav ", "audio"));
        assertFalse(FileTypeValidator.isMediaFileTypeSupported("pdf", "audio"));
        assertFalse(FileTypeValidator.isMediaFileTypeSupported("pdf", "unknown"));
        assertFalse(FileTypeValidator.isMediaFileTypeSupported(null, "video"));
        assertFalse(FileTypeValidator.isMediaFileTypeSupported("   ", "video"));
    }

    @Test
    void isPreviewImageFileType_shouldCoverPositiveAndNegativeCases() {
        assertTrue(FileTypeValidator.isPreviewImageFileType("PNG"));
        assertFalse(FileTypeValidator.isPreviewImageFileType("pdf"));
        assertFalse(FileTypeValidator.isPreviewImageFileType(null));
        assertFalse(FileTypeValidator.isPreviewImageFileType(" "));
    }

    @Test
    void normalizeFeedbackFileType_shouldReturnUppercaseOrNull() {
        assertEquals("JPG", FileTypeValidator.normalizeFeedbackFileType("x.jpg"));
        assertEquals("PDF", FileTypeValidator.normalizeFeedbackFileType("x.PDF"));
        assertNull(FileTypeValidator.normalizeFeedbackFileType("x.docx"));
        assertNull(FileTypeValidator.normalizeFeedbackFileType("filename-without-extension"));
        assertNull(FileTypeValidator.normalizeFeedbackFileType(null));
    }

    @Test
    void isFeedbackAttachmentSupported_shouldValidateEachFeedbackContentTypeRule() {
        assertFalse(FileTypeValidator.isFeedbackAttachmentSupported("img.jpg", "text/plain"));
        assertFalse(FileTypeValidator.isFeedbackAttachmentSupported("img.png", "image/jpeg"));
        assertFalse(FileTypeValidator.isFeedbackAttachmentSupported("doc.pdf", "application/msword"));
        assertFalse(FileTypeValidator.isFeedbackAttachmentSupported("note.txt", null));
        assertFalse(FileTypeValidator.isFeedbackAttachmentSupported("note.txt", " "));
    }

    @Test
    void getNormalizedExtension_shouldTrimAndLowerCaseExtension() {
        assertEquals("jpeg", FileTypeValidator.getNormalizedExtension("a. JPEG "));
        assertEquals("gz", FileTypeValidator.getNormalizedExtension("archive.tar.gz"));
        assertNull(FileTypeValidator.getNormalizedExtension("."));
        assertNull(FileTypeValidator.getNormalizedExtension("no_extension"));
    }
} 
