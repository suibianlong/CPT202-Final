package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.ViewerFeedbackController;
import com.cpt202.HerLink.dto.viewer.FeedbackCreateRequest;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.ViewerFeedbackService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgAt;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.AttachedFileVO;
import com.cpt202.HerLink.vo.FeedbackVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ViewerFeedbackController.class)
@DisplayName("ViewerFeedbackController Integration Test")
class ViewerFeedbackControllerTest {

    private static LocalDateTime fixedTime;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ViewerFeedbackService viewerFeedbackService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private Long currentUserId;

    @BeforeAll
    static void setUpAll() {
        fixedTime = LocalDateTime.of(2026, 5, 5, 7, 30, 0);
    }

    @BeforeEach
    void setUp() {
        currentUserId = 5L;
    }

    @AfterEach
    void tearDown() {
        clearInvocations(viewerFeedbackService, resourcePermissionChecker);
        currentUserId = null;
    }

    @AfterAll
    static void tearDownAll() {
        fixedTime = null;
    }

    @Nested
    @DisplayName("POST /api/viewer/feedback")
    class CreateFeedbackTests {

        @Test
        @DisplayName("Should create feedback for an authenticated user without attachments")
        void shouldCreateFeedbackWithoutAttachments() throws Exception {
            FeedbackVO response = feedback(18L, currentUserId, "Bug Report", "Feedback works correctly.", 0);
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(viewerFeedbackService.createFeedback(eq(currentUserId), any(FeedbackCreateRequest.class))).thenReturn(response);

            mockMvc.perform(multipart("/api/viewer/feedback")
                            .param("feedbackType", "Bug Report")
                            .param("description", "Feedback works correctly."))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.feedbackId").value(18L))
                    .andExpect(jsonPath("$.userId").value(5L))
                    .andExpect(jsonPath("$.feedbackType").value("Bug Report"))
                    .andExpect(jsonPath("$.description").value("Feedback works correctly."));

            assertInvokedOnceWithArgAt(viewerFeedbackService, "createFeedback", 0, currentUserId);
        }

        @Test
        @DisplayName("Should create feedback with attachments when payload is valid")
        void shouldCreateFeedbackWithAttachments() throws Exception {
            FeedbackVO response = feedback(19L, currentUserId, "Suggestion", "Add more filters.", 1);
            AttachedFileVO fileVO = new AttachedFileVO();
            fileVO.setFileId(81L);
            fileVO.setFeedbackId(19L);
            fileVO.setOriginalFilename("viewer-note.png");
            fileVO.setFileType("PNG");
            response.setAttachments(List.of(fileVO));

            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(viewerFeedbackService.createFeedback(eq(currentUserId), any(FeedbackCreateRequest.class))).thenReturn(response);

            MockMultipartFile file = new MockMultipartFile(
                    "files", "viewer-note.png", "image/png", "png-data".getBytes());

            mockMvc.perform(multipart("/api/viewer/feedback")
                            .file(file)
                            .param("feedbackType", "Suggestion")
                            .param("description", "Add more filters."))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.feedbackId").value(19L))
                    .andExpect(jsonPath("$.attachments[0].originalFilename").value("viewer-note.png"));
        }

        @Test
        @DisplayName("Should return 401 when feedback submission is unauthenticated")
        void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(multipart("/api/viewer/feedback")
                            .param("feedbackType", "Bug Report")
                            .param("description", "Feedback works correctly."))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/feedback"));

            assertNoInteractions(viewerFeedbackService);
        }

        @Test
        @DisplayName("Should return 400 when feedback type is missing")
        void shouldReturnBadRequestWhenFeedbackTypeMissing() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(viewerFeedbackService.createFeedback(eq(currentUserId), any(FeedbackCreateRequest.class)))
                    .thenThrow(AppException.badRequest("Feedback type must be Bug Report or Suggestion."));

            mockMvc.perform(multipart("/api/viewer/feedback")
                            .param("description", "Feedback works correctly."))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Feedback type must be Bug Report or Suggestion."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/feedback"));
        }

        @Test
        @DisplayName("Should return 400 when description is missing")
        void shouldReturnBadRequestWhenDescriptionMissing() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(viewerFeedbackService.createFeedback(eq(currentUserId), any(FeedbackCreateRequest.class)))
                    .thenThrow(AppException.badRequest("Feedback description is required."));

            mockMvc.perform(multipart("/api/viewer/feedback")
                            .param("feedbackType", "Bug Report"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Feedback description is required."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/feedback"));
        }

        @Test
        @DisplayName("Should return 400 when attachment count exceeds limit")
        void shouldReturnBadRequestWhenAttachmentCountExceedsLimit() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(viewerFeedbackService.createFeedback(eq(currentUserId), any(FeedbackCreateRequest.class)))
                    .thenThrow(AppException.badRequest("You can upload up to 3 feedback attachments."));

            MockMultipartFile f1 = new MockMultipartFile("files", "a.png", "image/png", "1".getBytes());
            MockMultipartFile f2 = new MockMultipartFile("files", "b.png", "image/png", "2".getBytes());
            MockMultipartFile f3 = new MockMultipartFile("files", "c.png", "image/png", "3".getBytes());
            MockMultipartFile f4 = new MockMultipartFile("files", "d.png", "image/png", "4".getBytes());

            mockMvc.perform(multipart("/api/viewer/feedback")
                            .file(f1).file(f2).file(f3).file(f4)
                            .param("feedbackType", "Suggestion")
                            .param("description", "Add more filters."))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("You can upload up to 3 feedback attachments."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/feedback"));
        }

        @Test
        @DisplayName("Should return 500 when feedback creation throws unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            doThrow(new RuntimeException("Database unavailable"))
                    .when(viewerFeedbackService)
                    .createFeedback(eq(currentUserId), any(FeedbackCreateRequest.class));

            mockMvc.perform(multipart("/api/viewer/feedback")
                            .param("feedbackType", "Bug Report")
                            .param("description", "Feedback works correctly."))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/feedback"));
        }
    }

    @Nested
    @DisplayName("GET /api/viewer/feedback/mine")
    class ListMyFeedbackTests {

        @Test
        @DisplayName("Should return current user's feedback history")
        void shouldReturnMyFeedbackHistory() throws Exception {
            FeedbackVO response = feedback(22L, currentUserId, "Suggestion", "Please add more comments.", 0);
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(viewerFeedbackService.listMyFeedback(currentUserId)).thenReturn(List.of(response));

            mockMvc.perform(get("/api/viewer/feedback/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].feedbackId").value(22L))
                    .andExpect(jsonPath("$[0].feedbackType").value("Suggestion"))
                    .andExpect(jsonPath("$[0].description").value("Please add more comments."));

            assertInvokedOnceWithArgs(viewerFeedbackService, "listMyFeedback", currentUserId);
        }

        @Test
        @DisplayName("Should return empty list when user has no feedback history")
        void shouldReturnEmptyListWhenNoFeedbackExists() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(viewerFeedbackService.listMyFeedback(currentUserId)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/viewer/feedback/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").doesNotExist());
        }

        @Test
        @DisplayName("Should return 401 when querying own feedback without authentication")
        void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/viewer/feedback/mine"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/feedback/mine"));

            assertNoInteractions(viewerFeedbackService);
        }

        @Test
        @DisplayName("Should return 404 when current user no longer exists")
        void shouldReturnNotFoundWhenCurrentUserDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(currentUserId);
            when(viewerFeedbackService.listMyFeedback(currentUserId))
                    .thenThrow(AppException.notFound("User does not exist."));

            mockMvc.perform(get("/api/viewer/feedback/mine"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("User does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/feedback/mine"));
        }
    }

    @Nested
    @DisplayName("GET /api/viewer/feedback/all")
    class ListAllFeedbackTests {

        @Test
        @DisplayName("Should return all feedback for administrator")
        void shouldReturnAllFeedback() throws Exception {
            FeedbackVO response = feedback(30L, 99L, "Bug Report", "Visible to admin.", 0);
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(1L);
            when(viewerFeedbackService.listAllFeedback()).thenReturn(List.of(response));

            mockMvc.perform(get("/api/viewer/feedback/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].feedbackId").value(30L))
                    .andExpect(jsonPath("$[0].userId").value(99L))
                    .andExpect(jsonPath("$[0].feedbackType").value("Bug Report"));

            assertInvokedOnceWithArgs(viewerFeedbackService, "listAllFeedback");
        }

        @Test
        @DisplayName("Should return empty list when no feedback exists")
        void shouldReturnEmptyListWhenNoFeedbackExists() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(1L);
            when(viewerFeedbackService.listAllFeedback()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/viewer/feedback/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").doesNotExist());
        }

        @Test
        @DisplayName("Should return 401 when admin authentication is missing")
        void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/viewer/feedback/all"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/feedback/all"));

            assertNoInteractions(viewerFeedbackService);
        }

        @Test
        @DisplayName("Should return 403 when current user is not an administrator")
        void shouldReturnForbiddenWhenCurrentUserIsNotAdministrator() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(get("/api/viewer/feedback/all"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/feedback/all"));

            assertNoInteractions(viewerFeedbackService);
        }

        @Test
        @DisplayName("Should return 500 when listing all feedback throws unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(1L);
            doThrow(new RuntimeException("Database unavailable"))
                    .when(viewerFeedbackService)
                    .listAllFeedback();

            mockMvc.perform(get("/api/viewer/feedback/all"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/feedback/all"));
        }
    }

    private FeedbackVO feedback(Long id, Long userId, String type, String description, Integer fileNum) {
        FeedbackVO vo = new FeedbackVO();
        vo.setFeedbackId(id);
        vo.setUserId(userId);
        vo.setFeedbackType(type);
        vo.setDescription(description);
        vo.setFileNum(fileNum);
        vo.setUploadedAt(fixedTime);
        return vo;
    }
}
