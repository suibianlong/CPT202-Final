package com.cpt202.HerLink.unit.util;

import com.cpt202.HerLink.util.*;
import com.cpt202.HerLink.util.FileStorageManager;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.cpt202.HerLink.exception.AppException;

@ExtendWith(MockitoExtension.class)
class FileStorageManagerTest {

    private static final String TEST_UPLOAD_DIR = "test-uploads";
    private FileStorageManager fileStorageManager;

    @Mock
    private MultipartFile mockMultipartFile;

    @BeforeEach
    void setUp() {
        fileStorageManager = new FileStorageManager(TEST_UPLOAD_DIR);
    }

    @Nested
    @DisplayName("store() method full test")
    class StoreMethodTest {

        @Test
        @DisplayName("store: pass null file return null")
        void store_withNullFile_returnsNull() {
            String path = fileStorageManager.store(null, "test");
            assertNull(path);
        }

        @Test
        @DisplayName("store: pass empty file return null")
        void store_withEmptyFile_returnsNull() {
            when(mockMultipartFile.isEmpty()).thenReturn(true);
            String path = fileStorageManager.store(mockMultipartFile, "test");
            assertNull(path);
        }

        @Test
        @DisplayName("store: unsupported file type throw AppException")
        void store_unsupportedFileType_throws400() {
            when(mockMultipartFile.isEmpty()).thenReturn(false);
            when(mockMultipartFile.getOriginalFilename()).thenReturn("file.xyz");

            AppException ex = assertThrows(AppException.class,
                    () -> fileStorageManager.store(mockMultipartFile, "test"));

            assertEquals("Unsupported file type.", ex.getMessage());
        }

        @Test
        @DisplayName("store: file transfer failed throw AppException")
        void store_transferFailed_throws500() throws Exception {

            when(mockMultipartFile.isEmpty()).thenReturn(false);
            when(mockMultipartFile.getOriginalFilename()).thenReturn("test.jpg");

            doThrow(new IOException("Disk full")).when(mockMultipartFile).transferTo(any(java.io.File.class));

            AppException ex = assertThrows(AppException.class,
                    () -> fileStorageManager.store(mockMultipartFile, "test"));

            assertEquals("Failed to store uploaded file.", ex.getMessage());
        }

        @Test
        @DisplayName("store: valid file with subfolder return correct relative path")
        void store_validFileWithFolder_returnsRelativePath() {
            when(mockMultipartFile.isEmpty()).thenReturn(false);
            when(mockMultipartFile.getOriginalFilename()).thenReturn("profile.png");

            String path = fileStorageManager.store(mockMultipartFile, "avatar");

            assertNotNull(path);
            assertTrue(path.startsWith("avatar/"));
            assertTrue(path.endsWith(".png"));
        }

        @Test
        @DisplayName("store: valid file without folder return simple file name path")
        void store_validFileNoFolder_returnsFileName() {
            when(mockMultipartFile.isEmpty()).thenReturn(false);
            when(mockMultipartFile.getOriginalFilename()).thenReturn("note.pdf");

            String path = fileStorageManager.store(mockMultipartFile, null);

            assertNotNull(path);
            assertFalse(path.contains("/"));
            assertTrue(path.endsWith(".pdf"));
        }
    }

    @Nested
    @DisplayName("storeFile() core method test")
    class StoreFileMethodTest {

        @Test
        @DisplayName("storeFile: pass null file return null")
        void storeFile_nullFile_returnsNull() {
            var res = fileStorageManager.storeFile(null, "test");
            assertNull(res);
        }

        @Test
        @DisplayName("storeFile: pass empty file return null")
        void storeFile_emptyFile_returnsNull() {
            when(mockMultipartFile.isEmpty()).thenReturn(true);
            assertNull(fileStorageManager.storeFile(mockMultipartFile, "test"));
        }

        @Test
        @DisplayName("storeFile: unsupported file type throw AppException")
        void storeFile_unsupported_400() {
            when(mockMultipartFile.isEmpty()).thenReturn(false);
            when(mockMultipartFile.getOriginalFilename()).thenReturn("a.xyz");
            AppException ex = assertThrows(AppException.class,
                    () -> fileStorageManager.storeFile(mockMultipartFile, "test"));
            assertEquals("Unsupported file type.", ex.getMessage());
        }

        @Test
        @DisplayName("storeFile: invalid traversal folder throw AppException")
        void storeFile_invalidFolder_400() {
            when(mockMultipartFile.isEmpty()).thenReturn(false);
            when(mockMultipartFile.getOriginalFilename()).thenReturn("a.jpg");
            AppException ex = assertThrows(AppException.class,
                    () -> fileStorageManager.storeFile(mockMultipartFile, "../outside"));
            assertEquals("Invalid upload folder.", ex.getMessage());
        }

        @Test
        @DisplayName("storeFile: file transfer IO fail throw AppException")
        void storeFile_transferFail_500() throws Exception {
            when(mockMultipartFile.isEmpty()).thenReturn(false);
            when(mockMultipartFile.getOriginalFilename()).thenReturn("a.jpg");

            // 同时stub两个transferTo重载方法，覆盖所有情况
            doThrow(new IOException("Mock IO error"))
                    .when(mockMultipartFile)
                    .transferTo(any(java.io.File.class));

            AppException ex = assertThrows(AppException.class,
                    () -> fileStorageManager.storeFile(mockMultipartFile, "test"));

            assertEquals("Failed to store uploaded file.", ex.getMessage());
        }

        @Test
        @DisplayName("storeFile: valid file without folder return correct result")
        void storeFile_validNoFolder_success() {
            when(mockMultipartFile.isEmpty()).thenReturn(false);
            when(mockMultipartFile.getOriginalFilename()).thenReturn("a.jpg");
            when(mockMultipartFile.getSize()).thenReturn(100L);
            var file = fileStorageManager.storeFile(mockMultipartFile, null);
            assertNotNull(file);
            assertEquals("jpg", file.getFileType());
            assertEquals(100L, file.getFileSize());
        }

        @Test
        @DisplayName("storeFile: valid file with folder return correct path")
        void storeFile_validWithFolder_pathCorrect() {
            when(mockMultipartFile.isEmpty()).thenReturn(false);
            when(mockMultipartFile.getOriginalFilename()).thenReturn("doc.pdf");
            var file = fileStorageManager.storeFile(mockMultipartFile, "docs");
            assertTrue(file.getFilePath().startsWith("docs/"));
        }

        @Test
        @DisplayName("storeFile: file with valid extension returns correct type")
        void storeFile_validExtension_typeCorrect() {
            when(mockMultipartFile.isEmpty()).thenReturn(false);
            when(mockMultipartFile.getOriginalFilename()).thenReturn("README.txt");
            var file = fileStorageManager.storeFile(mockMultipartFile, "test");
            assertEquals("txt", file.getFileType());
        }
    }

    @Nested
    @DisplayName("delete() method test including normal scenario")
    class DeleteTest {

        @Test
        @DisplayName("delete: null path do nothing no exception")
        void delete_nullPath_noOp() {
            assertDoesNotThrow(() -> fileStorageManager.delete(null));
        }

        @Test
        @DisplayName("delete: blank path do nothing no exception")
        void delete_blankPath_noOp() {
            assertDoesNotThrow(() -> fileStorageManager.delete("   "));
        }

        @Test
        @DisplayName("delete: invalid traversal path throw AppException")
        void delete_invalidPath_400() {
            AppException ex = assertThrows(AppException.class,
                    () -> fileStorageManager.delete("../etc/shadow"));
            assertEquals("Invalid file path.", ex.getMessage());
        }

        @Test
        @DisplayName("delete: valid normal path execute successfully no exception")
        void delete_validPath_normalCase_noException() {
            String validPath = "images/valid-file.jpg";
            assertDoesNotThrow(() -> fileStorageManager.delete(validPath));
        }
    }

    @Nested
    @DisplayName("deleteQuietly() silent delete method test including normal scenario")
    class DeleteQuietlyTest {

        @Test
        @DisplayName("deleteQuietly: null path no operation no exception")
        void deleteQuietly_nullPath_noEx() {
            assertDoesNotThrow(() -> fileStorageManager.deleteQuietly(null));
        }

        @Test
        @DisplayName("deleteQuietly: invalid path no operation no exception")
        void deleteQuietly_invalidPath_noEx() {
            assertDoesNotThrow(() -> fileStorageManager.deleteQuietly("../outside"));
        }

        @Test
        @DisplayName("deleteQuietly: delete failure silently ignore exception")
        void deleteQuietly_fail_ignore() {
            assertDoesNotThrow(() -> fileStorageManager.deleteQuietly("any"));
        }

        @Test
        @DisplayName("deleteQuietly: valid normal path execute successfully no exception")
        void deleteQuietly_validPath_normalCase_success() {
            String validPath = "docs/report.pdf";
            assertDoesNotThrow(() -> fileStorageManager.deleteQuietly(validPath));
        }
    }

    @Test
    @DisplayName("resolveFileType: full test for normal, boundary and special cases")
    void resolveFileType_allCases() {
        when(mockMultipartFile.isEmpty()).thenReturn(false);

        when(mockMultipartFile.getOriginalFilename()).thenReturn("test.JPG");
        assertEquals("jpg", fileStorageManager.storeFile(mockMultipartFile, "test").getFileType());

        when(mockMultipartFile.getOriginalFilename()).thenReturn("data.tar.gif");
        assertEquals("gif", fileStorageManager.storeFile(mockMultipartFile, "test").getFileType());

        when(mockMultipartFile.getOriginalFilename()).thenReturn("test@123.txt");
        assertEquals("txt", fileStorageManager.storeFile(mockMultipartFile, "test").getFileType());

        when(mockMultipartFile.getOriginalFilename()).thenReturn("report.docx");
        assertEquals("docx", fileStorageManager.storeFile(mockMultipartFile, "test").getFileType());

        when(mockMultipartFile.getOriginalFilename()).thenReturn("audio.wav");
        assertEquals("wav", fileStorageManager.storeFile(mockMultipartFile, "test").getFileType());
    }
}
