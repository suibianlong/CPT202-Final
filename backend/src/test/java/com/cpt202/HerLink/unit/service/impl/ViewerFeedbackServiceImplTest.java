package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.service.impl.*;
import com.cpt202.HerLink.service.impl.ViewerFeedbackServiceImpl;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import com.cpt202.HerLink.dto.viewer.FeedbackCreateRequest;
import com.cpt202.HerLink.entity.AttachedFile;
import com.cpt202.HerLink.entity.Feedback;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.AttachedFileMapper;
import com.cpt202.HerLink.mapper.FeedbackMapper;
import com.cpt202.HerLink.util.FileStorageManager;
import com.cpt202.HerLink.vo.FeedbackVO;

@ExtendWith(MockitoExtension.class)
class ViewerFeedbackServiceImplTest {

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private AttachedFileMapper attachedFileMapper;

    @Mock
    private FileStorageManager fileStorageManager;

    @InjectMocks
    private ViewerFeedbackServiceImpl viewerFeedbackService;

    private static final Long TEST_USER_ID = 1001L;
    private static final Long TEST_FEEDBACK_ID = 5001L;
    private static final int MAX_ATTACH_COUNT = 3;
    private static final long MAX_ATTACH_SIZE = 10L * 1024 * 1024;

    // General test object
    private Feedback testFeedback;
    private FeedbackCreateRequest validRequest;

    @BeforeEach
    void setUp() {
        // Initialize the test feedback entity
        testFeedback = new Feedback();
        testFeedback.setFeedbackId(TEST_FEEDBACK_ID);
        testFeedback.setUserId(TEST_USER_ID);
        testFeedback.setFeedbackType("Bug Report");
        testFeedback.setDescription("Test description");
        testFeedback.setFileNum(0);
        testFeedback.setUploadedAt(LocalDateTime.now());

        // Initialize the valid request object
        validRequest = new FeedbackCreateRequest();
        validRequest.setFeedbackType("Bug Report");
        validRequest.setDescription("Valid feedback content");
        validRequest.setFiles(new MultipartFile[0]);
    }

    // Public business method testing
    @Nested
    @DisplayName("Create Feedback - createFeedback")
    class CreateFeedbackTests {

        @Test
        @DisplayName("Normal scenario: Create feedback successfully without attachments")
        void createFeedback_WithoutAttachments_Success() {
            when(feedbackMapper.insert(any(Feedback.class))).thenAnswer(invocation -> {
                Feedback feedback = invocation.getArgument(0);
                feedback.setFeedbackId(TEST_FEEDBACK_ID); // Manually set the ID for the insertion of the Mock
                return 1;
            });
            when(feedbackMapper.selectById(eq(TEST_FEEDBACK_ID))).thenReturn(testFeedback);
            when(attachedFileMapper.selectByFeedbackId(eq(TEST_FEEDBACK_ID))).thenReturn(Collections.emptyList());

            FeedbackVO result = viewerFeedbackService.createFeedback(TEST_USER_ID, validRequest);

            assertNotNull(result);
            assertEquals(TEST_FEEDBACK_ID, result.getFeedbackId());
            assertEquals(TEST_USER_ID, result.getUserId());
            assertEquals("Bug Report", result.getFeedbackType());
            assertEquals("Test description", result.getDescription());
            assertEquals(0, result.getFileNum());
            assertTrue(result.getAttachments().isEmpty());
        }

        @Test
        @DisplayName("Boundary scenario: Create feedback successfully with maximum valid attachments (3 files)")
        void createFeedback_WithMaxValidAttachments_Success() {
            MultipartFile file1 = mock(MultipartFile.class);
            MultipartFile file2 = mock(MultipartFile.class);
            MultipartFile file3 = mock(MultipartFile.class);
            validRequest.setFiles(new MultipartFile[]{file1, file2, file3});

            when(file1.isEmpty()).thenReturn(false);
            when(file1.getSize()).thenReturn(1024L);
            when(file1.getOriginalFilename()).thenReturn("test1.jpg");
            when(file1.getContentType()).thenReturn("image/jpeg");

            when(file2.isEmpty()).thenReturn(false);
            when(file2.getSize()).thenReturn(1024L);
            when(file2.getOriginalFilename()).thenReturn("test2.png");
            when(file2.getContentType()).thenReturn("image/png");

            when(file3.isEmpty()).thenReturn(false);
            when(file3.getSize()).thenReturn(1024L);
            when(file3.getOriginalFilename()).thenReturn("test3.pdf");
            when(file3.getContentType()).thenReturn("application/pdf");

            FileStorageManager.StoredFile storedFile = mock(FileStorageManager.StoredFile.class);
            when(storedFile.getFilePath()).thenReturn("test/path");
            when(storedFile.getOriginalFilename()).thenReturn("test");
            when(storedFile.getStoredFilename()).thenReturn("stored");
            when(storedFile.getFileType()).thenReturn("jpg");
            when(storedFile.getFileSize()).thenReturn(1024L);
            when(fileStorageManager.storeFile(any(), any())).thenReturn(storedFile);

            when(feedbackMapper.insert(any())).thenReturn(1);
            testFeedback.setFileNum(3);
            when(feedbackMapper.selectById(any())).thenReturn(testFeedback);
            when(attachedFileMapper.selectByFeedbackId(any())).thenReturn(List.of(new AttachedFile()));

            FeedbackVO result = viewerFeedbackService.createFeedback(TEST_USER_ID, validRequest);

            assertNotNull(result);
            assertEquals(3, result.getFileNum());
            assertFalse(result.getAttachments().isEmpty());
        }

        @Test
        @DisplayName("Exception scenario: Request is null, throw parameter exception")
        void createFeedback_RequestIsNull_ThrowException() {
            AppException exception = assertThrows(AppException.class,
                    () -> viewerFeedbackService.createFeedback(TEST_USER_ID, null));
            assertEquals("Feedback type must be Bug Report or Suggestion.", exception.getMessage());
        }

        @Test
        @DisplayName("Exception scenario: Attachment count exceeds maximum limit (3 files), throw exception")
        void createFeedback_AttachmentsExceedMaxCount_ThrowException() {
            MultipartFile[] files = new MultipartFile[4];
            for (int i = 0; i < 4; i++) {
                MultipartFile file = mock(MultipartFile.class);
                when(file.isEmpty()).thenReturn(false);
                files[i] = file;
            }
            validRequest.setFiles(files);

            AppException exception = assertThrows(AppException.class,
                    () -> viewerFeedbackService.createFeedback(TEST_USER_ID, validRequest));
            assertEquals("You can upload up to 3 feedback attachments.", exception.getMessage());
        }

        @Test
        @DisplayName("Exception scenario: Attachment size exceeds 10MB, throw exception")
        void createFeedback_AttachmentExceedMaxSize_ThrowException() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(MAX_ATTACH_SIZE + 1);
            validRequest.setFiles(new MultipartFile[]{file});

            AppException exception = assertThrows(AppException.class,
                    () -> viewerFeedbackService.createFeedback(TEST_USER_ID, validRequest));
            assertEquals("Each feedback attachment must be 10MB or smaller.", exception.getMessage());
        }
        @Test
        @DisplayName("Boundary scenario: Attachment size is exactly 10MB, create successfully")
        void createFeedback_AttachmentSizeExactly10MB_Success() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(MAX_ATTACH_SIZE);
            when(file.getOriginalFilename()).thenReturn("test.pdf");
            when(file.getContentType()).thenReturn("application/pdf");

            validRequest.setFiles(new MultipartFile[]{file});

            FileStorageManager.StoredFile storedFile = mock(FileStorageManager.StoredFile.class);
            when(storedFile.getFilePath()).thenReturn("test/path/exact-10mb");
            when(storedFile.getOriginalFilename()).thenReturn("test");
            when(storedFile.getStoredFilename()).thenReturn("stored");
            when(storedFile.getFileType()).thenReturn("pdf");
            when(storedFile.getFileSize()).thenReturn(MAX_ATTACH_SIZE);
            when(fileStorageManager.storeFile(any(), any())).thenReturn(storedFile);

            when(feedbackMapper.insert(any())).thenReturn(1);
            testFeedback.setFileNum(1);
            when(feedbackMapper.selectById(any())).thenReturn(testFeedback);
            when(attachedFileMapper.selectByFeedbackId(any())).thenReturn(List.of(new AttachedFile()));

            FeedbackVO result = viewerFeedbackService.createFeedback(TEST_USER_ID, validRequest);

            assertNotNull(result);
            assertEquals(1, result.getFileNum());
            assertFalse(result.getAttachments().isEmpty());
        }
    }

    @Nested
    @DisplayName("List My Feedback - listMyFeedback")
    class ListMyFeedbackTests {

        @Test
        @DisplayName("Normal scenario: Multiple feedbacks exist, return corresponding VO list")
        void listMyFeedback_HasFeedbacks_ReturnList() {
            when(feedbackMapper.selectByUserId(eq(TEST_USER_ID))).thenReturn(List.of(testFeedback));
            when(attachedFileMapper.selectByFeedbackId(eq(TEST_FEEDBACK_ID))).thenReturn(Collections.emptyList());

            List<FeedbackVO> result = viewerFeedbackService.listMyFeedback(TEST_USER_ID);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(TEST_FEEDBACK_ID, result.get(0).getFeedbackId());
        }

        @Test
        @DisplayName("Boundary scenario: No feedback data, return empty list")
        void listMyFeedback_NoFeedbacks_ReturnEmptyList() {
            when(feedbackMapper.selectByUserId(eq(TEST_USER_ID))).thenReturn(Collections.emptyList());

            List<FeedbackVO> result = viewerFeedbackService.listMyFeedback(TEST_USER_ID);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Boundary scenario: Mapper returns null, return empty list")
        void listMyFeedback_MapperReturnNull_ReturnEmptyList() {
            when(feedbackMapper.selectByUserId(eq(TEST_USER_ID))).thenReturn(null);

            List<FeedbackVO> result = viewerFeedbackService.listMyFeedback(TEST_USER_ID);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("List All Feedback - listAllFeedback")
    class ListAllFeedbackTests {

        @Test
        @DisplayName("Normal scenario: Multiple feedbacks exist, return full list")
        void listAllFeedback_HasFeedbacks_ReturnList() {
            when(feedbackMapper.selectAll()).thenReturn(List.of(testFeedback));
            when(attachedFileMapper.selectByFeedbackId(eq(TEST_FEEDBACK_ID))).thenReturn(Collections.emptyList());

            List<FeedbackVO> result = viewerFeedbackService.listAllFeedback();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Bug Report", result.get(0).getFeedbackType());
        }

        @Test
        @DisplayName("Boundary scenario: No feedback data, return empty list")
        void listAllFeedback_NoFeedbacks_ReturnEmptyList() {
            when(feedbackMapper.selectAll()).thenReturn(Collections.emptyList());

            List<FeedbackVO> result = viewerFeedbackService.listAllFeedback();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Boundary scenario: Mapper returns null, return empty list")
        void listAllFeedback_MapperReturnNull_ReturnEmptyList() {
            when(feedbackMapper.selectAll()).thenReturn(null);

            List<FeedbackVO> result = viewerFeedbackService.listAllFeedback();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    //Private tool method (Reflection testing)
    @Nested
    @DisplayName("Normalize Feedback Type - normalizeFeedbackType")
    class NormalizeFeedbackTypeTests {

        @Test
        @DisplayName("Normal scenario: Mixed case Bug Report, normalize to standard format")
        void normalizeFeedbackType_BugReportMixedCase_Success() {
            var method = getNormalizeMethod("normalizeFeedbackType", String.class);

            String result = invokeMethod(method, "bUg rEpOrT");

            assertEquals("Bug Report", result);
        }

        @Test
        @DisplayName("Normal scenario: Standard Suggestion format, return directly")
        void normalizeFeedbackType_ValidSuggestion_Success() {
            var method = getNormalizeMethod("normalizeFeedbackType", String.class);

            String result = invokeMethod(method, "Suggestion");

            assertEquals("Suggestion", result);
        }

        @Test
        @DisplayName("Exception scenario: Type is null, throw exception")
        void normalizeFeedbackType_Null_ThrowException() {
            var method = getNormalizeMethod("normalizeFeedbackType", String.class);

            assertThrows(AppException.class, () -> invokeMethod(method, (String) null));
        }

        @Test
        @DisplayName("Exception scenario: Invalid feedback type, throw exception")
        void normalizeFeedbackType_InvalidType_ThrowException() {
            var method = getNormalizeMethod("normalizeFeedbackType", String.class);

            AppException exception = assertThrows(AppException.class,
                    () -> invokeMethod(method, "OtherType"));
            assertEquals("Feedback type must be Bug Report or Suggestion.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Normalize Description - normalizeDescription")
    class NormalizeDescriptionTests {

        @Test
        @DisplayName("Normal scenario: Description with whitespace, return normalized value")
        void normalizeDescription_WithWhitespace_Success() {
            var method = getNormalizeMethod("normalizeDescription", String.class);

            String result = invokeMethod(method, "  Good feedback  ");

            assertEquals("Good feedback", result);
        }

        @Test
        @DisplayName("Exception scenario: Description is null, throw exception")
        void normalizeDescription_Null_ThrowException() {
            var method = getNormalizeMethod("normalizeDescription", String.class);

            AppException exception = assertThrows(AppException.class,
                    () -> invokeMethod(method, (String) null));
            assertEquals("Feedback description is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Exception scenario: Description is empty string, throw exception")
        void normalizeDescription_Empty_ThrowException() {
            var method = getNormalizeMethod("normalizeDescription", String.class);

            AppException exception = assertThrows(AppException.class,
                    () -> invokeMethod(method, ""));
            assertEquals("Feedback description is required.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Normalize Attachments - normalizeAttachments")
    class NormalizeAttachmentsTests {

        @Test
        @DisplayName("Boundary scenario: File array is null, return empty list")
        void normalizeAttachments_NullArray_ReturnEmpty() {
            var method = getNormalizeMethod("normalizeAttachments", MultipartFile[].class);

            List<MultipartFile> result = invokeMethod(method, (MultipartFile[]) null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Boundary scenario: Empty file array, return empty list")
        void normalizeAttachments_EmptyArray_ReturnEmpty() {
            var method = getNormalizeMethod("normalizeAttachments", MultipartFile[].class);

            List<MultipartFile> result = invokeMethod(method, new MultipartFile[0]);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Normal scenario: Filter null/empty files, keep valid files")
        void normalizeAttachments_FilterInvalidFiles_ReturnValid() {
            var method = getNormalizeMethod("normalizeAttachments", MultipartFile[].class);
            MultipartFile validFile = mock(MultipartFile.class);
            when(validFile.isEmpty()).thenReturn(false);
            MultipartFile emptyFile = mock(MultipartFile.class);
            when(emptyFile.isEmpty()).thenReturn(true);

            List<MultipartFile> result = invokeMethod(method, new MultipartFile[]{null, validFile, emptyFile});

            assertEquals(1, result.size());
            assertEquals(validFile, result.get(0));
        }
    }

    // Reflection tool method
    private java.lang.reflect.Method getNormalizeMethod(String name, Class<?> paramType) {
        try {
            java.lang.reflect.Method method = ViewerFeedbackServiceImpl.class.getDeclaredMethod(name, paramType);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeMethod(java.lang.reflect.Method method, Object arg) {
        try {
            return (T) method.invoke(viewerFeedbackService, arg);
        } catch (Exception e) {
            if (e.getCause() instanceof AppException) {
                throw (AppException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }
}
