package com.cpt202.HerLink.util;

import com.cpt202.HerLink.exception.AppException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.mock.web.MockMultipartFile;

class FileStorageManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void storeFile_shouldReturnStoredMetadata() {
        FileStorageManager fileStorageManager = new FileStorageManager(tempDir.toString());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "mediaFile",
                "archive.pdf",
                "application/pdf",
                "document".getBytes()
        );

        FileStorageManager.StoredFile storedFile = fileStorageManager.storeFile(multipartFile, "resource-1");

        assertEquals("archive.pdf", storedFile.getOriginalFilename());
        assertEquals("pdf", storedFile.getFileType());
        assertEquals((long) "document".getBytes().length, storedFile.getFileSize());
        assertTrue(storedFile.getStoredFilename().endsWith(".pdf"));
        assertTrue(Files.exists(tempDir.resolve(storedFile.getFilePath())));
    }

    @Test
    void storeFile_shouldRejectFolderOutsideUploadRoot() {
        FileStorageManager fileStorageManager = new FileStorageManager(tempDir.toString());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "mediaFile",
                "archive.pdf",
                "application/pdf",
                "document".getBytes()
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageManager.storeFile(multipartFile, "../outside")
        );

        assertEquals(400, exception.getStatusCode());
    }

    @Test
    void delete_shouldRemoveStoredFile() throws IOException {
        FileStorageManager fileStorageManager = new FileStorageManager(tempDir.toString());
        Path storedFile = tempDir.resolve("resource-1/preview.jpg");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "preview");

        fileStorageManager.delete("resource-1/preview.jpg");

        assertFalse(Files.exists(storedFile));
    }

    @Test
    void delete_shouldRejectPathOutsideUploadRoot() {
        FileStorageManager fileStorageManager = new FileStorageManager(tempDir.toString());

        AppException exception = assertThrows(
                AppException.class,
                () -> fileStorageManager.delete("../outside.txt")
        );

        assertEquals(400, exception.getStatusCode());
    }

    @Test
    void deleteQuietly_shouldIgnoreMissingFile() {
        FileStorageManager fileStorageManager = new FileStorageManager(tempDir.toString());

        assertDoesNotThrow(() -> fileStorageManager.deleteQuietly("resource-1/missing.jpg"));
    }
}
