package com.cpt202.HerLink.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cpt202.HerLink.dto.viewer.CommentCreateRequest;
import com.cpt202.HerLink.exception.AppException;

public class ViewerCommentServiceImplTest {

    private ViewerCommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        // 所有 Mapper / Service 传 null，只测试【参数校验、纯逻辑】
        commentService = new ViewerCommentServiceImpl(
                null,  // CommentMapper
                null,  // ResourceMapper
                null   // UserAccessService
        );
    }

    // ========================== 能测：参数 null / 空值 / 格式 / 长度 异常 ==========================

    /**
     * 测试：资源 ID 为 null → 抛异常
     */
    @Test
    void listComments_ResourceIdNull_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class, () ->
                commentService.listComments(null)
        );
        assertEquals("Resource id is required.", exception.getMessage());
    }

    /**
     * 测试：创建评论 - 内容为 null / 空字符串 → 抛异常
     */
    @Test
    void createComment_ContentNullOrEmpty_ShouldThrow() {
        // 内容 null
        CommentCreateRequest req1 = new CommentCreateRequest();
        req1.setContent(null);
        AppException ex1 = assertThrows(AppException.class, () ->
                commentService.createComment(1L, 100L, req1)
        );

        // 内容空串
        CommentCreateRequest req2 = new CommentCreateRequest();
        req2.setContent("   ");
        AppException ex2 = assertThrows(AppException.class, () ->
                commentService.createComment(1L, 100L, req2)
        );

        assertEquals("Comment content cannot be empty.", ex1.getMessage());
        assertEquals("Comment content cannot be empty.", ex2.getMessage());
    }

    /**
     * 测试：创建评论 - 内容超长（>1000）→ 抛异常
     */
    @Test
    void createComment_ContentTooLong_ShouldThrow() {
        String longContent = "a".repeat(1001);
        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent(longContent);

        AppException exception = assertThrows(AppException.class, () ->
                commentService.createComment(1L, 100L, req)
        );
        assertEquals("Comment content cannot exceed 1000 characters.", exception.getMessage());
    }

    /**
     * 测试：删除评论 - commentId 为 null → 抛异常
     */
    @Test
    void deleteComment_CommentIdNull_ShouldThrow() {
        AppException exception = assertThrows(AppException.class, () ->
                commentService.deleteComment(1L, 100L, null)
        );
        assertEquals("Comment id is required.", exception.getMessage());
    }

    // ========================== 以下方法依赖数据库 → 单元测试无法运行 ==========================
    // 一运行就 NPE，必须留到【集成测试】

    @Test
    void createComment_DuplicateCheck_UnTestable_InUnitTest() {
        // 依赖 mapper 查询重复评论 → 无法测试
    }

    @Test
    void deleteComment_PermissionCheck_UnTestable_InUnitTest() {
        // 依赖用户权限、资源校验 → 无法测试
    }
}