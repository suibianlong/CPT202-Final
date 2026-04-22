package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.service.ViewerFeedbackService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.AttachedFileVO;
import com.cpt202.HerLink.vo.FeedbackVO;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ViewerFeedbackController.class)
class ViewerFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ViewerFeedbackService viewerFeedbackService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    @Test
    void createFeedback_shouldAllowAuthenticatedUser() throws Exception {
        FeedbackVO feedbackVO = new FeedbackVO();
        feedbackVO.setFeedbackId(18L);
        feedbackVO.setFileNum(1);
        feedbackVO.setUploadedAt(LocalDateTime.now());
        feedbackVO.setUserId(5L);
        feedbackVO.setFeedbackType("Bug Report");
        feedbackVO.setDescription("The feedback upload works correctly.");

        AttachedFileVO attachedFileVO = new AttachedFileVO();
        attachedFileVO.setFileId(81L);
        attachedFileVO.setFeedbackId(18L);
        attachedFileVO.setOriginalFilename("viewer-note.png");
        attachedFileVO.setFileType("PNG");
        feedbackVO.setAttachments(List.of(attachedFileVO));

        when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
        when(viewerFeedbackService.createFeedback(eq(5L), any())).thenReturn(feedbackVO);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "viewer-note.png",
                "image/png",
                "png-data".getBytes()
        );

        mockMvc.perform(multipart("/api/viewer/feedback")
                        .file(file)
                        .param("feedbackType", "Bug Report")
                        .param("description", "The feedback upload works correctly."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feedbackId").value(18L))
                .andExpect(jsonPath("$.attachments[0].originalFilename").value("viewer-note.png"));

        verify(resourcePermissionChecker).requireAuthenticatedUserId(any());
    }

    @Test
    void listMyFeedback_shouldReturnCurrentUserHistory() throws Exception {
        FeedbackVO feedbackVO = new FeedbackVO();
        feedbackVO.setFeedbackId(22L);
        feedbackVO.setFeedbackType("Suggestion");
        feedbackVO.setDescription("Please add more comments.");
        feedbackVO.setUploadedAt(LocalDateTime.now());

        when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(6L);
        when(viewerFeedbackService.listMyFeedback(6L)).thenReturn(List.of(feedbackVO));

        mockMvc.perform(get("/api/viewer/feedback/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].feedbackType").value("Suggestion"));
    }
}
