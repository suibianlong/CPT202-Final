package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.service.ViewerCommentService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CommentVO;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ViewerCommentController.class)
class ViewerCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ViewerCommentService viewerCommentService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    @Test
    void createComment_shouldAllowAuthenticatedUser() throws Exception {
        CommentVO commentVO = new CommentVO();
        commentVO.setId(12L);
        commentVO.setResourceId(8L);
        commentVO.setUserId(3L);
        commentVO.setUserName("Registered Viewer");
        commentVO.setContent("This record is helpful.");
        commentVO.setCreatedAt(LocalDateTime.now());

        when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(3L);
        when(viewerCommentService.createComment(eq(3L), eq(8L), any())).thenReturn(commentVO);

        mockMvc.perform(post("/api/viewer/resources/8/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "This record is helpful."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resourceId").value(8L))
                .andExpect(jsonPath("$.userId").value(3L))
                .andExpect(jsonPath("$.userName").value("Registered Viewer"));

        verify(resourcePermissionChecker).requireAuthenticatedUserId(any());
    }

    @Test
    void listComments_shouldReturnApprovedResourceComments() throws Exception {
        CommentVO commentVO = new CommentVO();
        commentVO.setId(15L);
        commentVO.setResourceId(9L);
        commentVO.setUserId(4L);
        commentVO.setUserName("Viewer A");
        commentVO.setContent("Very informative.");
        commentVO.setCreatedAt(LocalDateTime.now());

        when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(4L);
        when(viewerCommentService.listComments(9L)).thenReturn(java.util.List.of(commentVO));

        mockMvc.perform(get("/api/viewer/resources/9/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Very informative."));
    }
}
