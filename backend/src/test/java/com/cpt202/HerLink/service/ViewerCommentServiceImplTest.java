package com.cpt202.HerLink.service;

import com.cpt202.HerLink.dto.viewer.CommentCreateRequest;
import com.cpt202.HerLink.entity.Comment;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CommentMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.service.impl.ViewerCommentServiceImpl;
import com.cpt202.HerLink.vo.CommentVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewerCommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ResourceMapper resourceMapper;

    @Mock
    private UserAccessService userAccessService;

    private ViewerCommentServiceImpl viewerCommentService;

    @BeforeEach
    void setUp() {
        viewerCommentService = new ViewerCommentServiceImpl(commentMapper, resourceMapper, userAccessService);
    }

    @Test
    void createComment_shouldPersistCommentForApprovedResource() {
        Long currentUserId = 7L;
        Long resourceId = 12L;

        Resource approvedResource = new Resource();
        approvedResource.setId(resourceId);
        when(resourceMapper.selectApprovedById(resourceId)).thenReturn(approvedResource);
        when(commentMapper.selectLatestByResourceIdAndUserIdAndContent(resourceId, currentUserId, "Wonderful archive."))
                .thenReturn(null);
        doAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(31L);
            return 1;
        }).when(commentMapper).insert(any(Comment.class));

        Comment savedComment = new Comment();
        savedComment.setId(31L);
        savedComment.setResourceId(resourceId);
        savedComment.setUserId(currentUserId);
        savedComment.setUserName("Registered Viewer");
        savedComment.setContent("Wonderful archive.");
        savedComment.setCreatedAt(LocalDateTime.now());
        when(commentMapper.selectById(31L)).thenReturn(savedComment);

        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("  Wonderful archive.  ");

        CommentVO result = viewerCommentService.createComment(currentUserId, resourceId, request);

        assertNotNull(result);
        assertEquals(31L, result.getId());
        assertEquals(resourceId, result.getResourceId());
        assertEquals(currentUserId, result.getUserId());
        assertEquals("Registered Viewer", result.getUserName());
        assertEquals("Wonderful archive.", result.getContent());

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertEquals(resourceId, captor.getValue().getResourceId());
        assertEquals(currentUserId, captor.getValue().getUserId());
        assertEquals("Wonderful archive.", captor.getValue().getContent());
        assertNotNull(captor.getValue().getCreatedAt());
    }

    @Test
    void createComment_shouldRejectDuplicateCommentWithinWindow() {
        Long currentUserId = 9L;
        Long resourceId = 18L;

        Resource approvedResource = new Resource();
        approvedResource.setId(resourceId);
        when(resourceMapper.selectApprovedById(resourceId)).thenReturn(approvedResource);

        Comment duplicateComment = new Comment();
        duplicateComment.setId(55L);
        duplicateComment.setCreatedAt(LocalDateTime.now().minusSeconds(8));
        when(commentMapper.selectLatestByResourceIdAndUserIdAndContent(resourceId, currentUserId, "Repeated comment"))
                .thenReturn(duplicateComment);

        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("Repeated comment");

        AppException exception = assertThrows(
                AppException.class,
                () -> viewerCommentService.createComment(currentUserId, resourceId, request)
        );

        assertEquals(409, exception.getStatusCode());
        assertEquals("Please wait before submitting the same comment again.", exception.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void listComments_shouldReturnCommentViewObjects() {
        Long resourceId = 21L;

        Resource approvedResource = new Resource();
        approvedResource.setId(resourceId);
        when(resourceMapper.selectApprovedById(resourceId)).thenReturn(approvedResource);

        Comment comment = new Comment();
        comment.setId(66L);
        comment.setResourceId(resourceId);
        comment.setUserId(3L);
        comment.setUserName("Viewer A");
        comment.setContent("Insightful resource.");
        comment.setCreatedAt(LocalDateTime.now().minusDays(1));
        when(commentMapper.selectByResourceId(resourceId)).thenReturn(List.of(comment));

        List<CommentVO> result = viewerCommentService.listComments(resourceId);

        assertEquals(1, result.size());
        assertEquals("Viewer A", result.get(0).getUserName());
        assertEquals("Insightful resource.", result.get(0).getContent());
    }
}
