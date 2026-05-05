package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.service.impl.*;
import com.cpt202.HerLink.service.impl.ViewerCommentServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cpt202.HerLink.dto.viewer.CommentCreateRequest;
import com.cpt202.HerLink.entity.Comment;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CommentMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.vo.CommentVO;
import com.cpt202.HerLink.vo.CurrentUserVO;


@ExtendWith(MockitoExtension.class)
class ViewerCommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ResourceMapper resourceMapper;

    @Mock
    private UserAccessService userAccessService;

    @InjectMocks
    private ViewerCommentServiceImpl viewerCommentService;

    private static final Long TEST_RESOURCE_ID = 1L;
    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_COMMENT_ID = 1000L;
    private static final int MAX_LENGTH = 1000;

    // ====================== Test Helper Methods ======================
    private Comment buildTestComment() {
        Comment comment = new Comment();
        comment.setId(TEST_COMMENT_ID);
        comment.setResourceId(TEST_RESOURCE_ID);
        comment.setUserId(TEST_USER_ID);
        comment.setUserName("testUser");
        comment.setContent("test comment");
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }

    private CommentCreateRequest buildTestRequest(String content) {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent(content);
        return request;
    }

    private CurrentUserVO buildTestUser(UserRoleEnum role) {
        CurrentUserVO userVO = new CurrentUserVO();
        userVO.setUserId(TEST_USER_ID);
        userVO.setRole(role.getApiValue());
        return userVO;
    }

    // ====================== listComments Tests ======================
    @Nested
    @DisplayName("listComments - Query comment list by resource id")
    class ListCommentsTests {

        @Test
        @DisplayName("Throw bad request when resource id is null")
        void listComments_ResourceIdNull_ThrowBadRequest() {
            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.listComments(null));
            assertEquals("Resource id is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Throw not found when resource is not approved")
        void listComments_ResourceNotApproved_ThrowNotFound() {
            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(null);

            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.listComments(TEST_RESOURCE_ID));
            assertEquals("Approved resource does not exist.", exception.getMessage());
        }

        @Test
        @DisplayName("Return empty list when no comment exists")
        void listComments_NoComments_ReturnEmptyList() {
            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectByResourceId(TEST_RESOURCE_ID)).thenReturn(null);

            List<CommentVO> result = viewerCommentService.listComments(TEST_RESOURCE_ID);

            assertNotNull(result);
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Return comment vo list when comments exist")
        void listComments_HasComments_ReturnCommentVOList() {
            Comment comment = buildTestComment();
            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectByResourceId(TEST_RESOURCE_ID)).thenReturn(List.of(comment));

            List<CommentVO> result = viewerCommentService.listComments(TEST_RESOURCE_ID);

            assertNotNull(result);
            assertEquals(1, result.size());
            CommentVO vo = result.get(0);
            assertEquals(TEST_COMMENT_ID, vo.getId());
            assertEquals(TEST_RESOURCE_ID, vo.getResourceId());
            assertEquals(TEST_USER_ID, vo.getUserId());
            assertEquals("testUser", vo.getUserName());
            assertEquals("test comment", vo.getContent());
            assertNotNull(vo.getCreatedAt());
        }
    }

    // ====================== createComment Tests ======================
    @Nested
    @DisplayName("createComment - Create new comment")
    class CreateCommentTests {

        @Test
        @DisplayName("Throw bad request when resource id is null")
        void createComment_ResourceIdNull_ThrowBadRequest() {
            CommentCreateRequest request = buildTestRequest("test");
            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.createComment(TEST_USER_ID, null, request));
            assertEquals("Resource id is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Throw bad request when comment content is empty or blank")
        void createComment_ContentEmpty_ThrowBadRequest() {
            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());

            assertAll(
                    () -> assertThrows(AppException.class,
                            () -> viewerCommentService.createComment(TEST_USER_ID, TEST_RESOURCE_ID, null)),
                    () -> assertThrows(AppException.class,
                            () -> viewerCommentService.createComment(TEST_USER_ID, TEST_RESOURCE_ID, buildTestRequest(""))),
                    () -> assertThrows(AppException.class,
                            () -> viewerCommentService.createComment(TEST_USER_ID, TEST_RESOURCE_ID, buildTestRequest("   ")))
            );
        }

        @Test
        @DisplayName("Throw bad request when content exceeds max length limit")
        void createComment_ContentOverMaxLength_ThrowBadRequest() {
            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            String longContent = "a".repeat(MAX_LENGTH + 1);
            CommentCreateRequest request = buildTestRequest(longContent);

            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.createComment(TEST_USER_ID, TEST_RESOURCE_ID, request));
            assertEquals("Comment content cannot exceed 1000 characters.", exception.getMessage());
        }

        @Test
        @DisplayName("Throw conflict when duplicate comment submitted within time window")
        void createComment_DuplicateInTimeWindow_ThrowConflict() {
            String content = "duplicate comment";
            LocalDateTime now = LocalDateTime.now();
            Comment duplicateComment = buildTestComment();
            duplicateComment.setContent(content);
            duplicateComment.setCreatedAt(now.minusSeconds(10));

            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectLatestByResourceIdAndUserIdAndContent(
                    eq(TEST_RESOURCE_ID), eq(TEST_USER_ID), eq(content.trim())))
                    .thenReturn(duplicateComment);

            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.createComment(TEST_USER_ID, TEST_RESOURCE_ID, buildTestRequest(content)));
            assertEquals("Please wait before submitting the same comment again.", exception.getMessage());
        }

        @Test
        @DisplayName("Create successfully when content reaches max boundary length")
        void createComment_ContentMaxLength_Success() {
            String maxContent = "a".repeat(MAX_LENGTH);
            Comment comment = buildTestComment();
            comment.setContent(maxContent);

            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectLatestByResourceIdAndUserIdAndContent(any(), any(), any())).thenReturn(null);
            // 模拟插入并设置 ID
            when(commentMapper.insert(org.mockito.ArgumentMatchers.any(Comment.class))).thenAnswer(invocation -> {
                Comment insertedComment = invocation.getArgument(0);
                insertedComment.setId(TEST_COMMENT_ID);
                return 1;
            });
            when(commentMapper.selectById(any())).thenReturn(comment);

            CommentVO result = viewerCommentService.createComment(TEST_USER_ID, TEST_RESOURCE_ID, buildTestRequest(maxContent));

            assertNotNull(result);
            assertEquals(maxContent, result.getContent());
            assertEquals(MAX_LENGTH, result.getContent().length());
        }

        @Test
        @DisplayName("Create successfully with valid normal comment content")
        void createComment_ValidContent_Success() {
            String content = "valid comment";
            Comment comment = buildTestComment();
            comment.setContent(content);

            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectLatestByResourceIdAndUserIdAndContent(any(), any(), any())).thenReturn(null);
            // 模拟插入并设置 ID
            when(commentMapper.insert(org.mockito.ArgumentMatchers.any(Comment.class))).thenAnswer(invocation -> {
                Comment insertedComment = invocation.getArgument(0);
                insertedComment.setId(TEST_COMMENT_ID);
                return 1;
            });
            when(commentMapper.selectById(any())).thenReturn(comment);

            CommentVO result = viewerCommentService.createComment(TEST_USER_ID, TEST_RESOURCE_ID, buildTestRequest(content));

            assertNotNull(result);
            assertEquals(TEST_COMMENT_ID, result.getId());
            assertEquals(content, result.getContent());
            assertEquals(TEST_USER_ID, result.getUserId());
        }

        @Test
        @DisplayName("Throw not found when saved comment cannot be reloaded")
        void createComment_SavedButCannotReload_ThrowNotFound() {
            String content = "test";
            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectLatestByResourceIdAndUserIdAndContent(any(), any(), any())).thenReturn(null);
            // 模拟插入并设置 ID
            when(commentMapper.insert(org.mockito.ArgumentMatchers.any(Comment.class))).thenAnswer(invocation -> {
                Comment insertedComment = invocation.getArgument(0);
                insertedComment.setId(TEST_COMMENT_ID);
                return 1;
            });
            when(commentMapper.selectById(any())).thenReturn(null);

            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.createComment(TEST_USER_ID, TEST_RESOURCE_ID, buildTestRequest(content)));
            assertEquals("Comment was saved but cannot be reloaded.", exception.getMessage());
        }
    }
    // ====================== deleteComment Tests ======================
    @Nested
    @DisplayName("deleteComment - Delete existing comment")
    class DeleteCommentTests {

        @Test
        @DisplayName("Throw bad request when resource id is null")
        void deleteComment_ResourceIdNull_ThrowBadRequest() {
            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.deleteComment(TEST_USER_ID, null, TEST_COMMENT_ID));
            assertEquals("Resource id is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Throw bad request when comment id is null")
        void deleteComment_CommentIdNull_ThrowBadRequest() {
            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());

            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.deleteComment(TEST_USER_ID, TEST_RESOURCE_ID, null));
            assertEquals("Comment id is required.", exception.getMessage());
        }

        @Test
        @DisplayName("Throw not found when target comment does not exist")
        void deleteComment_CommentNotExist_ThrowNotFound() {
            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectById(TEST_COMMENT_ID)).thenReturn(null);

            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.deleteComment(TEST_USER_ID, TEST_RESOURCE_ID, TEST_COMMENT_ID));
            assertEquals("Comment does not exist.", exception.getMessage());
        }

        @Test
        @DisplayName("Throw bad request when comment does not belong to current resource")
        void deleteComment_CommentNotBelongToResource_ThrowBadRequest() {
            Comment comment = buildTestComment();
            comment.setResourceId(999L);

            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectById(TEST_COMMENT_ID)).thenReturn(comment);

            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.deleteComment(TEST_USER_ID, TEST_RESOURCE_ID, TEST_COMMENT_ID));
            assertEquals("Comment does not belong to this resource.", exception.getMessage());
        }

        @Test
        @DisplayName("Throw forbidden when non-owner and non-admin user tries to delete")
        void deleteComment_NotOwnerNotAdmin_ThrowForbidden() {
            Comment comment = buildTestComment();
            comment.setUserId(999L);
            CurrentUserVO user = buildTestUser(UserRoleEnum.REGISTERED_VIEWER);

            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectById(TEST_COMMENT_ID)).thenReturn(comment);
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(user);

            AppException exception = assertThrows(AppException.class,
                    () -> viewerCommentService.deleteComment(TEST_USER_ID, TEST_RESOURCE_ID, TEST_COMMENT_ID));
            assertEquals("You do not have permission to delete this comment.", exception.getMessage());
        }

        @Test
        @DisplayName("Delete successfully when current user is comment owner")
        void deleteComment_IsOwner_Success() {
            Comment comment = buildTestComment();
            CurrentUserVO user = buildTestUser(UserRoleEnum.REGISTERED_VIEWER);

            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectById(TEST_COMMENT_ID)).thenReturn(comment);
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(user);

            assertDoesNotThrow(() ->
                    viewerCommentService.deleteComment(TEST_USER_ID, TEST_RESOURCE_ID, TEST_COMMENT_ID));
        }

        @Test
        @DisplayName("Delete successfully when current user is administrator")
        void deleteComment_IsAdmin_Success() {
            Comment comment = buildTestComment();
            comment.setUserId(999L);
            CurrentUserVO admin = buildTestUser(UserRoleEnum.ADMINISTRATOR);

            when(resourceMapper.selectApprovedById(TEST_RESOURCE_ID)).thenReturn(new Resource());
            when(commentMapper.selectById(TEST_COMMENT_ID)).thenReturn(comment);
            when(userAccessService.getCurrentUserById(TEST_USER_ID)).thenReturn(admin);

            assertDoesNotThrow(() ->
                    viewerCommentService.deleteComment(TEST_USER_ID, TEST_RESOURCE_ID, TEST_COMMENT_ID));
        }
    }
}
