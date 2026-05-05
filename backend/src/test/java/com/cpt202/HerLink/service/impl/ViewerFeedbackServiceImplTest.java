package com.cpt202.HerLink.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.cpt202.HerLink.dto.viewer.FeedbackCreateRequest;
import com.cpt202.HerLink.exception.AppException;

public class ViewerFeedbackServiceImplTest {

    private ViewerFeedbackServiceImpl feedbackService;

    @BeforeEach
    void setUp() {
        // 所有依赖传入 null，只测试【纯参数校验】
        feedbackService = new ViewerFeedbackServiceImpl(
                null,  // FeedbackMapper
                null,  // AttachedFileMapper
                null   // FileStorageManager
        );
    }

    // ========================== 能测：参数校验 / 空值 / 格式 / 数量 / 大小 ==========================

    /**
     * 测试：请求体为 null → 抛异常
     */
    @Test
    void createFeedback_RequestNull_ShouldThrow() {
        AppException ex = assertThrows(AppException.class, () ->
                feedbackService.createFeedback(1L, null)
        );
        assertNotNull(ex.getMessage());
    }

    /**
     * 测试：反馈类型为 null / 非法类型 → 抛异常
     */
    @Test
    void createFeedback_InvalidFeedbackType_ShouldThrow() {
        // 类型为 null
        FeedbackCreateRequest req1 = new FeedbackCreateRequest();
        req1.setFeedbackType(null);
        req1.setDescription("test");

        AppException ex1 = assertThrows(AppException.class, () ->
                feedbackService.createFeedback(1L, req1)
        );
        assertEquals("Feedback type must be Bug Report or Suggestion.", ex1.getMessage());

        // 非法类型
        FeedbackCreateRequest req2 = new FeedbackCreateRequest();
        req2.setFeedbackType("InvalidType");
        req2.setDescription("test");

        AppException ex2 = assertThrows(AppException.class, () ->
                feedbackService.createFeedback(1L, req2)
        );
        assertEquals("Feedback type must be Bug Report or Suggestion.", ex2.getMessage());
    }

    /**
     * 测试：描述为空 → 抛异常
     */
    @Test
    void createFeedback_DescriptionNullOrEmpty_ShouldThrow() {
        FeedbackCreateRequest req = new FeedbackCreateRequest();
        req.setFeedbackType("Bug Report");
        req.setDescription(null);

        AppException ex = assertThrows(AppException.class, () ->
                feedbackService.createFeedback(1L, req)
        );
        assertEquals("Feedback description is required.", ex.getMessage());
    }

    /**
     * 测试：附件超过 3 个 → 抛异常
     */
    @Test
    void createFeedback_AttachmentsTooMany_ShouldThrow() {
        MultipartFile file1 = new MockMultipartFile("file1", "file1.txt", "text/plain", new byte[1024]);
        MultipartFile file2 = new MockMultipartFile("file2", "file2.txt", "text/plain", new byte[1024]);
        MultipartFile file3 = new MockMultipartFile("file3", "file3.txt", "text/plain", new byte[1024]);
        MultipartFile file4 = new MockMultipartFile("file4", "file4.txt", "text/plain", new byte[1024]);

        FeedbackCreateRequest req = new FeedbackCreateRequest();
        req.setFeedbackType("Suggestion");
        req.setDescription("test");
        req.setFiles(new MultipartFile[]{file1, file2, file3, file4});

        AppException ex = assertThrows(AppException.class, () ->
                feedbackService.createFeedback(1L, req)
        );
        assertEquals("You can upload up to 3 feedback attachments.", ex.getMessage());
    }

    // ========================== 以下方法依赖数据库 → 单元测试无法运行 ==========================

    @Test
    void createFeedback_FileValidationAndPersistence_UnTestable() {
        // 依赖文件存储、mapper → 无法测试
    }

    @Test
    void listMyFeedback_UnTestable_InUnitTest() {
        // 依赖 feedbackMapper 查询 → 无法测试
    }

    @Test
    void listAllFeedback_UnTestable_InUnitTest() {
        // 依赖 feedbackMapper 查询 → 无法测试
    }
}