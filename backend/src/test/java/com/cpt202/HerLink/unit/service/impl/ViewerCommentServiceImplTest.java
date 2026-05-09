package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.dto.viewer.CommentCreateRequest;
import com.cpt202.HerLink.entity.Comment;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CommentMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.service.impl.ViewerCommentServiceImpl;
import com.cpt202.HerLink.vo.CommentVO;
import com.cpt202.HerLink.vo.CurrentUserVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewerCommentServiceImplTest {

    private static final Long RESOURCE_ID = 10L;
    private static final Long USER_ID = 20L;
    private static final Long COMMENT_ID = 30L;

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private ResourceMapper resourceMapper;
    @Mock
    private UserAccessService userAccessService;

    @InjectMocks
    private ViewerCommentServiceImpl service;

    @Test
    @DisplayName("List comments rejects null resource id")
    void listComments_nullResourceId_throwsBadRequest() {
        AppException exception = assertThrows(AppException.class, () -> service.listComments(null));

        assertAll(
                () -> assertEquals(400, exception.getStatusCode()),
                () -> assertEquals("Resource id is required.", exception.getMessage())
        );
    }

    @Test
    @DisplayName("List comments rejects resource that is not approved")
    void listComments_resourceNotApproved_throwsNotFound() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(null);

        AppException exception = assertThrows(AppException.class, () -> service.listComments(RESOURCE_ID));

        assertEquals("Approved resource does not exist.", exception.getMessage());
    }

    @Test
    @DisplayName("List comments returns empty list when mapper returns null")
    void listComments_mapperReturnsNull_returnsEmptyList() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());
        when(commentMapper.selectByResourceId(RESOURCE_ID)).thenReturn(null);

        List<CommentVO> result = service.listComments(RESOURCE_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("List comments maps comment fields to response")
    void listComments_mapperRowsExist_returnsMappedResponses() {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(5);
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());
        when(commentMapper.selectByResourceId(RESOURCE_ID)).thenReturn(List.of(comment(COMMENT_ID, RESOURCE_ID, USER_ID, "Olivia", "Nice", createdAt)));

        List<CommentVO> result = service.listComments(RESOURCE_ID);

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(COMMENT_ID, result.get(0).getId()),
                () -> assertEquals(RESOURCE_ID, result.get(0).getResourceId()),
                () -> assertEquals(USER_ID, result.get(0).getUserId()),
                () -> assertEquals("Olivia", result.get(0).getUserName()),
                () -> assertEquals("Nice", result.get(0).getContent()),
                () -> assertEquals(createdAt, result.get(0).getCreatedAt())
        );
    }

    @Test
    @DisplayName("Create comment rejects null request and blank content")
    void createComment_blankContent_throwsBadRequest() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());

        AppException nullRequest = assertThrows(AppException.class,
                () -> service.createComment(USER_ID, RESOURCE_ID, null));
        AppException blankContent = assertThrows(AppException.class,
                () -> service.createComment(USER_ID, RESOURCE_ID, request("   ")));

        assertAll(
                () -> assertEquals("Comment content cannot be empty.", nullRequest.getMessage()),
                () -> assertEquals(400, blankContent.getStatusCode()),
                () -> assertEquals("Comment content cannot be empty.", blankContent.getMessage())
        );
    }

    @Test
    @DisplayName("Create comment rejects content above max boundary")
    void createComment_contentTooLong_throwsBadRequest() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());

        AppException exception = assertThrows(AppException.class,
                () -> service.createComment(USER_ID, RESOURCE_ID, request("a".repeat(1001))));

        assertEquals("Comment content cannot exceed 1000 characters.", exception.getMessage());
    }

    @Test
    @DisplayName("Create comment rejects duplicate within 30 second window")
    void createComment_recentDuplicate_throwsConflict() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());
        when(commentMapper.selectLatestByResourceIdAndUserIdAndContent(RESOURCE_ID, USER_ID, "Repeat"))
                .thenReturn(comment(COMMENT_ID, RESOURCE_ID, USER_ID, "Olivia", "Repeat", LocalDateTime.now().minusSeconds(5)));

        AppException exception = assertThrows(AppException.class,
                () -> service.createComment(USER_ID, RESOURCE_ID, request(" Repeat ")));

        assertAll(
                () -> assertEquals(409, exception.getStatusCode()),
                () -> assertEquals("Please wait before submitting the same comment again.", exception.getMessage())
        );
    }

    @Test
    @DisplayName("Create comment allows max length boundary and trims content")
    void createComment_maxLengthContent_returnsSavedComment() {
        String maxContent = "a".repeat(1000);
        AtomicReference<Comment> inserted = new AtomicReference<>();
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());
        when(commentMapper.selectLatestByResourceIdAndUserIdAndContent(RESOURCE_ID, USER_ID, maxContent)).thenReturn(null);
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(COMMENT_ID);
            inserted.set(comment);
            return 1;
        });
        when(commentMapper.selectById(COMMENT_ID)).thenAnswer(invocation ->
                comment(COMMENT_ID, RESOURCE_ID, USER_ID, "Olivia", inserted.get().getContent(), inserted.get().getCreatedAt()));

        CommentVO result = service.createComment(USER_ID, RESOURCE_ID, request(maxContent));

        assertAll(
                () -> assertEquals(COMMENT_ID, result.getId()),
                () -> assertEquals(maxContent, result.getContent()),
                () -> assertEquals(1000, result.getContent().length()),
                () -> assertEquals(USER_ID, inserted.get().getUserId()),
                () -> assertEquals(RESOURCE_ID, inserted.get().getResourceId()),
                () -> assertNotNull(inserted.get().getCreatedAt())
        );
    }

    @Test
    @DisplayName("Create comment throws not found when saved comment cannot be reloaded")
    void createComment_savedCommentMissing_throwsNotFound() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());
        when(commentMapper.selectLatestByResourceIdAndUserIdAndContent(RESOURCE_ID, USER_ID, "Hello")).thenReturn(null);
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(COMMENT_ID);
            return 1;
        });
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(null);

        AppException exception = assertThrows(AppException.class,
                () -> service.createComment(USER_ID, RESOURCE_ID, request("Hello")));

        assertEquals("Comment was saved but cannot be reloaded.", exception.getMessage());
    }

    @Test
    @DisplayName("Delete comment rejects null comment id")
    void deleteComment_nullCommentId_throwsBadRequest() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());

        AppException exception = assertThrows(AppException.class,
                () -> service.deleteComment(USER_ID, RESOURCE_ID, null));

        assertEquals("Comment id is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Delete comment rejects missing comment")
    void deleteComment_missingComment_throwsNotFound() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(null);

        AppException exception = assertThrows(AppException.class,
                () -> service.deleteComment(USER_ID, RESOURCE_ID, COMMENT_ID));

        assertEquals("Comment does not exist.", exception.getMessage());
    }

    @Test
    @DisplayName("Delete comment rejects comment belonging to another resource")
    void deleteComment_otherResource_throwsBadRequest() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment(COMMENT_ID, 999L, USER_ID, "Olivia", "Nice", LocalDateTime.now()));

        AppException exception = assertThrows(AppException.class,
                () -> service.deleteComment(USER_ID, RESOURCE_ID, COMMENT_ID));

        assertEquals("Comment does not belong to this resource.", exception.getMessage());
    }

    @Test
    @DisplayName("Delete comment rejects non-owner non-admin user")
    void deleteComment_notOwnerOrAdmin_throwsForbidden() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment(COMMENT_ID, RESOURCE_ID, 999L, "Other", "Nice", LocalDateTime.now()));
        when(userAccessService.getCurrentUserById(USER_ID)).thenReturn(user(UserRoleEnum.REGISTERED_VIEWER));

        AppException exception = assertThrows(AppException.class,
                () -> service.deleteComment(USER_ID, RESOURCE_ID, COMMENT_ID));

        assertAll(
                () -> assertEquals(403, exception.getStatusCode()),
                () -> assertEquals("You do not have permission to delete this comment.", exception.getMessage())
        );
    }

    @Test
    @DisplayName("Delete comment succeeds when current user owns the comment")
    void deleteComment_ownerUser_doesNotThrow() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment(COMMENT_ID, RESOURCE_ID, USER_ID, "Olivia", "Nice", LocalDateTime.now()));
        when(userAccessService.getCurrentUserById(USER_ID)).thenReturn(user(UserRoleEnum.REGISTERED_VIEWER));

        assertDoesNotThrow(() -> service.deleteComment(USER_ID, RESOURCE_ID, COMMENT_ID));
    }

    @Test
    @DisplayName("Delete comment succeeds when current user is administrator")
    void deleteComment_adminUser_doesNotThrow() {
        when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(new Resource());
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment(COMMENT_ID, RESOURCE_ID, 999L, "Other", "Nice", LocalDateTime.now()));
        when(userAccessService.getCurrentUserById(USER_ID)).thenReturn(user(UserRoleEnum.ADMINISTRATOR));

        assertDoesNotThrow(() -> service.deleteComment(USER_ID, RESOURCE_ID, COMMENT_ID));
    }

    private CommentCreateRequest request(String content) {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent(content);
        return request;
    }

    private Comment comment(Long id, Long resourceId, Long userId, String userName, String content, LocalDateTime createdAt) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setResourceId(resourceId);
        comment.setUserId(userId);
        comment.setUserName(userName);
        comment.setContent(content);
        comment.setCreatedAt(createdAt);
        return comment;
    }

    private CurrentUserVO user(UserRoleEnum role) {
        CurrentUserVO user = new CurrentUserVO();
        user.setUserId(USER_ID);
        user.setRole(role.getApiValue());
        return user;
    }
}
