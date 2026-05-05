package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.ViewerCommentController;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.ViewerCommentService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgAt;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CommentVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ViewerCommentController.class)
@DisplayName("ViewerCommentController Integration Test")
class ViewerCommentControllerTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 5, 5, 6, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ViewerCommentService viewerCommentService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private CommentVO comment;

    @BeforeEach
    void setUp() {
        comment = comment(
                12L,
                8L,
                3L,
                "Registered Viewer",
                "This record is helpful."
        );
    }

    @Nested
    @DisplayName("GET /api/viewer/resources/{resourceId}/comments")
    class ListCommentsTests {

        @Test
        @DisplayName("Should return comments for an authenticated user")
        void shouldReturnCommentsForAuthenticatedUser() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(4L);
            when(viewerCommentService.listComments(9L)).thenReturn(List.of(comment(15L, 9L, 4L, "Viewer A", "Very informative.")));

            mockMvc.perform(get("/api/viewer/resources/9/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(15L))
                    .andExpect(jsonPath("$[0].resourceId").value(9L))
                    .andExpect(jsonPath("$[0].userId").value(4L))
                    .andExpect(jsonPath("$[0].userName").value("Viewer A"))
                    .andExpect(jsonPath("$[0].content").value("Very informative."));

            assertInvokedOnceWithArgs(viewerCommentService, "listComments", 9L);
        }

        @Test
        @DisplayName("Should return an empty list when resource has no comments")
        void shouldReturnEmptyListWhenResourceHasNoComments() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(4L);
            when(viewerCommentService.listComments(9L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/viewer/resources/9/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertInvokedOnceWithArgs(viewerCommentService, "listComments", 9L);
        }

        @Test
        @DisplayName("Should return 401 when comment list is requested without login")
        void shouldReturnUnauthorizedWhenCommentListIsRequestedWithoutLogin() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/viewer/resources/9/comments"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/9/comments"));

            assertNoInteractions(viewerCommentService);
        }

        @Test
        @DisplayName("Should return 404 when resource for comment list does not exist")
        void shouldReturnNotFoundWhenResourceForCommentListDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(4L);
            when(viewerCommentService.listComments(999L))
                    .thenThrow(AppException.notFound("Resource does not exist."));

            mockMvc.perform(get("/api/viewer/resources/999/comments"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/999/comments"));
        }
    }

    @Nested
    @DisplayName("POST /api/viewer/resources/{resourceId}/comments")
    class CreateCommentTests {

        @Test
        @DisplayName("Should create a comment for an authenticated user")
        void shouldCreateCommentForAuthenticatedUser() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(3L);
            when(viewerCommentService.createComment(eq(3L), eq(8L), any())).thenReturn(comment);

            mockMvc.perform(post("/api/viewer/resources/8/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "This record is helpful."
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(12L))
                    .andExpect(jsonPath("$.resourceId").value(8L))
                    .andExpect(jsonPath("$.userId").value(3L))
                    .andExpect(jsonPath("$.userName").value("Registered Viewer"))
                    .andExpect(jsonPath("$.content").value("This record is helpful."));

            assertInvokedOnceWithArgAt(viewerCommentService, "createComment", 0, 3L);
            assertInvokedOnceWithArgAt(viewerCommentService, "createComment", 1, 8L);
        }

        @Test
        @DisplayName("Should return 400 when create comment request JSON is malformed")
        void shouldReturnBadRequestWhenCreateCommentRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/viewer/resources/8/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/8/comments"));
        }

        @Test
        @DisplayName("Should return 400 when service rejects invalid comment content")
        void shouldReturnBadRequestWhenServiceRejectsInvalidCommentContent() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(3L);
            when(viewerCommentService.createComment(eq(3L), eq(8L), any()))
                    .thenThrow(AppException.badRequest("Comment content is required."));

            mockMvc.perform(post("/api/viewer/resources/8/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Comment content is required."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/8/comments"));
        }

        @Test
        @DisplayName("Should return 409 when same comment is submitted too quickly")
        void shouldReturnConflictWhenSameCommentIsSubmittedTooQuickly() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(3L);
            when(viewerCommentService.createComment(eq(3L), eq(8L), any()))
                    .thenThrow(AppException.conflict("Please wait before submitting the same comment again."));

            mockMvc.perform(post("/api/viewer/resources/8/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "This record is helpful."
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Please wait before submitting the same comment again."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/8/comments"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/viewer/resources/{resourceId}/comments/{commentId}")
    class DeleteCommentTests {

        @Test
        @DisplayName("Should delete comment when current user owns the comment")
        void shouldDeleteCommentWhenCurrentUserOwnsTheComment() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(3L);
            doNothing().when(viewerCommentService).deleteComment(3L, 8L, 12L);

            mockMvc.perform(delete("/api/viewer/resources/8/comments/12"))
                    .andExpect(status().isNoContent());

            assertInvokedOnceWithArgs(viewerCommentService, "deleteComment", 3L, 8L, 12L);
        }

        @Test
        @DisplayName("Should return 400 when comment id path variable is invalid")
        void shouldReturnBadRequestWhenCommentIdPathVariableIsInvalid() throws Exception {
            mockMvc.perform(delete("/api/viewer/resources/8/comments/not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/8/comments/not-a-number"));
        }

        @Test
        @DisplayName("Should return 403 when current user is not allowed to delete the comment")
        void shouldReturnForbiddenWhenCurrentUserIsNotAllowedToDeleteTheComment() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(4L);
            doThrow(AppException.forbidden("You do not have permission to delete this comment."))
                    .when(viewerCommentService).deleteComment(4L, 8L, 12L);

            mockMvc.perform(delete("/api/viewer/resources/8/comments/12"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("You do not have permission to delete this comment."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/8/comments/12"));
        }

        @Test
        @DisplayName("Should return 404 when comment to delete does not exist")
        void shouldReturnNotFoundWhenCommentToDeleteDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(3L);
            doThrow(AppException.notFound("Comment does not exist."))
                    .when(viewerCommentService).deleteComment(3L, 8L, 999L);

            mockMvc.perform(delete("/api/viewer/resources/8/comments/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Comment does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/8/comments/999"));
        }
    }

    private CommentVO comment(Long id,
                              Long resourceId,
                              Long userId,
                              String userName,
                              String content) {
        CommentVO commentVO = new CommentVO();
        commentVO.setId(id);
        commentVO.setResourceId(resourceId);
        commentVO.setUserId(userId);
        commentVO.setUserName(userName);
        commentVO.setContent(content);
        commentVO.setCreatedAt(FIXED_TIME);
        return commentVO;
    }
}
