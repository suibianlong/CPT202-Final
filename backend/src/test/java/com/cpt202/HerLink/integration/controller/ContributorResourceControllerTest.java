package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.ContributorResourceController;
import com.cpt202.HerLink.dto.resource.ResourceQueryRequest;
import com.cpt202.HerLink.dto.resource.ResourceSubmitRequest;
import com.cpt202.HerLink.dto.resource.ResourceUpdateRequest;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.ContributorResourceService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnce;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgAt;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.clearInvocations;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(ContributorResourceController.class)
@DisplayName("ContributorResourceController Integration Test")
class ContributorResourceControllerTest {

    private static LocalDateTime fixedTime;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContributorResourceService contributorResourceService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private Long currentUserId;

    @BeforeAll
    static void setUpAll() {
        fixedTime = LocalDateTime.of(2026, 5, 5, 7, 0, 0);
    }

    @BeforeEach
    void setUp() {
        currentUserId = 1L;
    }

    @AfterEach
    void tearDown() {
        clearInvocations(contributorResourceService, resourcePermissionChecker);
        currentUserId = null;
    }

    @AfterAll
    static void tearDownAll() {
        fixedTime = null;
    }

    @Nested
    @DisplayName("POST /api/contributor/resources")
    class CreateDraftTests {

        @Test
        @DisplayName("Should create a draft resource for an authenticated contributor")
        void shouldCreateDraft() throws Exception {
            ResourceDetailVO response = detail(10L, "Draft Title", "DRAFT");
            response.setContributorId(currentUserId);

            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.createDraft(currentUserId)).thenReturn(response);

            mockMvc.perform(post("/api/contributor/resources"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.contributorId").value(1L))
                    .andExpect(jsonPath("$.title").value("Draft Title"))
                    .andExpect(jsonPath("$.status").value("DRAFT"));

            assertInvokedOnceWithArgAt(contributorResourceService, "createDraft", 0, currentUserId);
        }

        @Test
        @DisplayName("Should return 401 when contributor authentication is missing")
        void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(post("/api/contributor/resources"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources"));

            assertNoInteractions(contributorResourceService);
        }

        @Test
        @DisplayName("Should return 404 when draft creation fails to reload resource")
        void shouldReturnNotFoundWhenDraftCannotBeReloaded() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.createDraft(currentUserId))
                    .thenThrow(AppException.notFound("Draft was created but cannot be reloaded."));

            mockMvc.perform(post("/api/contributor/resources"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Draft was created but cannot be reloaded."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources"));
        }
    }

    @Nested
    @DisplayName("PUT /api/contributor/resources/{resourceId}")
    class UpdateResourceTests {

        @Test
        @DisplayName("Should update a resource when request body is valid")
        void shouldUpdateResource() throws Exception {
            ResourceDetailVO response = detail(20L, "Updated Title", "DRAFT");
            response.setDescription("Updated Description");
            response.setCategoryId(2L);
            response.setPlace("Suzhou");
            response.setResourceType("IMAGE");

            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.updateResource(eq(currentUserId), eq(20L), any(ResourceUpdateRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/contributor/resources/20")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Updated Title",
                                      "description": "Updated Description",
                                      "categoryId": 2,
                                      "place": "Suzhou",
                                      "resourceType": "IMAGE",
                                      "tagIds": [1, 2]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(20L))
                    .andExpect(jsonPath("$.title").value("Updated Title"))
                    .andExpect(jsonPath("$.description").value("Updated Description"))
                    .andExpect(jsonPath("$.categoryId").value(2L))
                    .andExpect(jsonPath("$.place").value("Suzhou"))
                    .andExpect(jsonPath("$.resourceType").value("IMAGE"))
                    .andExpect(jsonPath("$.status").value("DRAFT"));

            assertInvokedOnceWithArgAt(contributorResourceService, "updateResource", 0, currentUserId);
            assertInvokedOnceWithArgAt(contributorResourceService, "updateResource", 1, 20L);
        }

        @Test
        @DisplayName("Should return 400 when resource id path variable is not a number")
        void shouldReturnBadRequestWhenResourceIdIsInvalid() throws Exception {
            mockMvc.perform(put("/api/contributor/resources/not-a-number")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Updated Title"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/not-a-number"));

            assertNoInteractions(contributorResourceService, resourcePermissionChecker);
        }

        @Test
        @DisplayName("Should return 400 when update request JSON is malformed")
        void shouldReturnBadRequestWhenRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(put("/api/contributor/resources/20")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/20"));

            assertNoInteractions(contributorResourceService, resourcePermissionChecker);
        }

        @Test
        @DisplayName("Should return 400 when update request body is missing")
        void shouldReturnBadRequestWhenRequestBodyIsMissing() throws Exception {
            mockMvc.perform(put("/api/contributor/resources/20")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("Required request body is missing")))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/20"));

            assertNoInteractions(contributorResourceService, resourcePermissionChecker);
        }

        @Test
        @DisplayName("Should return 400 when service rejects conflicting tag fields")
        void shouldReturnBadRequestWhenBothTagFieldsAreProvided() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.updateResource(eq(currentUserId), eq(20L), any(ResourceUpdateRequest.class)))
                    .thenThrow(AppException.badRequest("Tag ids and tag names cannot be submitted together."));

            mockMvc.perform(put("/api/contributor/resources/20")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tagIds": [1],
                                      "tagNames": ["test"]
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Tag ids and tag names cannot be submitted together."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/20"));
        }

        @Test
        @DisplayName("Should return 403 when current user cannot edit this resource")
        void shouldReturnForbiddenWhenResourceIsNotOwned() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.updateResource(eq(currentUserId), eq(20L), any(ResourceUpdateRequest.class)))
                    .thenThrow(AppException.forbidden("Current user does not own this resource."));

            mockMvc.perform(put("/api/contributor/resources/20")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Updated Title"
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Current user does not own this resource."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/20"));
        }

        @Test
        @DisplayName("Should return 404 when resource to update does not exist")
        void shouldReturnNotFoundWhenResourceDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.updateResource(eq(currentUserId), eq(99L), any(ResourceUpdateRequest.class)))
                    .thenThrow(AppException.notFound("Resource does not exist."));

            mockMvc.perform(put("/api/contributor/resources/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Unknown"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/99"));
        }
    }

    @Nested
    @DisplayName("POST /api/contributor/resources/{resourceId}/files")
    class UploadFilesTests {

        @Test
        @DisplayName("Should upload preview and media files when request is valid")
        void shouldUploadFiles() throws Exception {
            ResourceDetailVO response = detail(30L, "File Updated", "DRAFT");
            response.setPreviewImage("resource-30/preview.jpg");
            response.setMediaUrl("resource-30/video.mp4");

            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.uploadFiles(eq(currentUserId), eq(30L), any(), any()))
                    .thenReturn(response);

            MockMultipartFile previewImage = new MockMultipartFile(
                    "previewImage", "preview.jpg", "image/jpeg", "preview-data".getBytes());
            MockMultipartFile mediaFile = new MockMultipartFile(
                    "mediaFile", "video.mp4", "video/mp4", "media-data".getBytes());

            mockMvc.perform(multipart("/api/contributor/resources/30/files")
                            .file(previewImage)
                            .file(mediaFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(30L))
                    .andExpect(jsonPath("$.previewImage").value("resource-30/preview.jpg"))
                    .andExpect(jsonPath("$.mediaUrl").value("resource-30/video.mp4"));

            assertInvokedOnceWithArgAt(contributorResourceService, "uploadFiles", 0, currentUserId);
            assertInvokedOnceWithArgAt(contributorResourceService, "uploadFiles", 1, 30L);
        }

        @Test
        @DisplayName("Should return 400 when resource id path variable is not a number")
        void shouldReturnBadRequestWhenResourceIdIsInvalid() throws Exception {
            mockMvc.perform(multipart("/api/contributor/resources/not-a-number/files"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/not-a-number/files"));
        }

        @Test
        @DisplayName("Should return 400 when no file is uploaded")
        void shouldReturnBadRequestWhenNoFileUploaded() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.uploadFiles(eq(currentUserId), eq(30L), eq(null), eq(null)))
                    .thenThrow(AppException.badRequest("At least one file must be uploaded."));

            mockMvc.perform(multipart("/api/contributor/resources/30/files"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("At least one file must be uploaded."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/files"));
        }

        @Test
        @DisplayName("Should return 400 when upload request is rejected by service")
        void shouldReturnBadRequestWhenServiceRejectsUpload() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.uploadFiles(eq(currentUserId), eq(30L), any(), any()))
                    .thenThrow(AppException.badRequest("Unsupported file type."));

            MockMultipartFile previewImage = new MockMultipartFile(
                    "previewImage", "preview.gif", "image/gif", "preview-data".getBytes());

            mockMvc.perform(multipart("/api/contributor/resources/30/files")
                            .file(previewImage))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Unsupported file type."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/files"));
        }

        @Test
        @DisplayName("Should return 403 when current user cannot upload files")
        void shouldReturnForbiddenWhenResourceIsNotOwned() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.uploadFiles(eq(currentUserId), eq(30L), any(), any()))
                    .thenThrow(AppException.forbidden("Current user does not own this resource."));

            mockMvc.perform(multipart("/api/contributor/resources/30/files"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Current user does not own this resource."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/files"));
        }

        @Test
        @DisplayName("Should return 404 when resource to upload files into does not exist")
        void shouldReturnNotFoundWhenResourceDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.uploadFiles(eq(currentUserId), eq(99L), any(), any()))
                    .thenThrow(AppException.notFound("Resource does not exist."));

            mockMvc.perform(multipart("/api/contributor/resources/99/files"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/99/files"));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/my")
    class ListMyResourcesTests {

        @Test
        @DisplayName("Should return my resource list when contributor is authenticated")
        void shouldReturnMyResourceList() throws Exception {
            ResourceListItemVO item = listItem(40L, "My Resource", "DRAFT");
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.listMyResources(eq(currentUserId), any(ResourceQueryRequest.class)))
                    .thenReturn(List.of(item));

            mockMvc.perform(get("/api/contributor/resources/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(40L))
                    .andExpect(jsonPath("$[0].title").value("My Resource"))
                    .andExpect(jsonPath("$[0].status").value("DRAFT"));

            assertInvokedOnceWithArgAt(contributorResourceService, "listMyResources", 0, currentUserId);
        }

        @Test
        @DisplayName("Should return empty list when no resources exist")
        void shouldReturnEmptyListWhenNoResourcesExist() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.listMyResources(eq(currentUserId), any(ResourceQueryRequest.class)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/contributor/resources/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should return 401 when listing my resources without authentication")
        void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/contributor/resources/my"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/my"));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/{resourceId}")
    class GetMyResourceDetailTests {

        @Test
        @DisplayName("Should return my resource detail when resource exists and is owned")
        void shouldReturnMyResourceDetail() throws Exception {
            ResourceDetailVO response = detail(50L, "Detail Title", "DRAFT");
            response.setContributorId(currentUserId);
            response.setTagIds(List.of(1L, 2L));
            response.setTagNames(List.of("Heritage", "Archive"));

            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.getMyResourceDetail(currentUserId, 50L)).thenReturn(response);

            mockMvc.perform(get("/api/contributor/resources/50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(50L))
                    .andExpect(jsonPath("$.contributorId").value(1L))
                    .andExpect(jsonPath("$.title").value("Detail Title"))
                    .andExpect(jsonPath("$.status").value("DRAFT"));

            assertInvokedOnceWithArgAt(contributorResourceService, "getMyResourceDetail", 0, currentUserId);
            assertInvokedOnceWithArgAt(contributorResourceService, "getMyResourceDetail", 1, 50L);
        }

        @Test
        @DisplayName("Should return 400 when resource id path variable is not a number")
        void shouldReturnBadRequestWhenResourceIdIsInvalid() throws Exception {
            mockMvc.perform(get("/api/contributor/resources/not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/not-a-number"));
        }

        @Test
        @DisplayName("Should return 404 when resource does not exist")
        void shouldReturnNotFoundWhenResourceDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.getMyResourceDetail(currentUserId, 99L))
                    .thenThrow(AppException.notFound("Resource does not exist."));

            mockMvc.perform(get("/api/contributor/resources/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/99"));
        }

        @Test
        @DisplayName("Should return 403 when resource is not owned by current user")
        void shouldReturnForbiddenWhenResourceIsNotOwned() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            when(contributorResourceService.getMyResourceDetail(currentUserId, 50L))
                    .thenThrow(AppException.forbidden("Current user does not own this resource."));

            mockMvc.perform(get("/api/contributor/resources/50"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Current user does not own this resource."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/50"));
        }
    }

    @Nested
    @DisplayName("POST /api/contributor/resources/{resourceId}/submit")
    class SubmitResourceTests {

        @Test
        @DisplayName("Should submit a resource for review when payload is valid")
        void shouldSubmitResource() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);

            mockMvc.perform(post("/api/contributor/resources/60/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionNote": "Please review this submission"
                                    }
                                    """))
                    .andExpect(status().isOk());

            assertInvokedOnceWithArgAt(contributorResourceService, "submitResource", 0, currentUserId);
            assertInvokedOnceWithArgAt(contributorResourceService, "submitResource", 1, 60L);
        }

        @Test
        @DisplayName("Should return 401 when submission is unauthenticated")
        void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(post("/api/contributor/resources/60/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionNote": "Please review this submission"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/60/submit"));
        }

        @Test
        @DisplayName("Should return 400 when submit request JSON is malformed")
        void shouldReturnBadRequestWhenRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/contributor/resources/60/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/60/submit"));
        }

        @Test
        @DisplayName("Should return 400 when submit request is rejected")
        void shouldReturnBadRequestWhenServiceRejectsRequest() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            doThrow(AppException.badRequest("Title is required."))
                    .when(contributorResourceService)
                    .submitResource(eq(currentUserId), eq(60L), any(ResourceSubmitRequest.class));

            mockMvc.perform(post("/api/contributor/resources/60/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionNote": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Title is required."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/60/submit"));
        }

        @Test
        @DisplayName("Should return 403 when current user cannot submit this resource")
        void shouldReturnForbiddenWhenResourceIsNotOwned() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            doThrow(AppException.forbidden("Current user does not own this resource."))
                    .when(contributorResourceService)
                    .submitResource(eq(currentUserId), eq(60L), any(ResourceSubmitRequest.class));

            mockMvc.perform(post("/api/contributor/resources/60/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionNote": "Please review this submission"
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Current user does not own this resource."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/60/submit"));
        }

        @Test
        @DisplayName("Should return 409 when resource is already under review")
        void shouldReturnConflictWhenResourceAlreadyUnderReview() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(currentUserId);
            doThrow(AppException.conflict("Resource is already under review."))
                    .when(contributorResourceService)
                    .submitResource(eq(currentUserId), eq(60L), any(ResourceSubmitRequest.class));

            mockMvc.perform(post("/api/contributor/resources/60/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionNote": "Please review this submission"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Resource is already under review."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/60/submit"));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/category-options")
    class ListCategoryOptionsTests {

        @Test
        @DisplayName("Should return available category options")
        void shouldReturnCategoryOptions() throws Exception {
            CategoryTagOptionVO option = option(1L, "Education");
            when(contributorResourceService.listCategoryOptions()).thenReturn(List.of(option));

            mockMvc.perform(get("/api/contributor/resources/category-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].name").value("Education"));

            assertInvokedOnce(contributorResourceService, "listCategoryOptions");
        }

        @Test
        @DisplayName("Should return empty list when no category options exist")
        void shouldReturnEmptyListWhenNoCategoryOptionsExist() throws Exception {
            when(contributorResourceService.listCategoryOptions()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/contributor/resources/category-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/resource-type-options")
    class ListResourceTypeOptionsTests {

        @Test
        @DisplayName("Should return available resource type options")
        void shouldReturnResourceTypeOptions() throws Exception {
            CategoryTagOptionVO option = option(2L, "Video");
            when(contributorResourceService.listResourceTypeOptions()).thenReturn(List.of(option));

            mockMvc.perform(get("/api/contributor/resources/resource-type-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(2L))
                    .andExpect(jsonPath("$[0].name").value("Video"));

            assertInvokedOnce(contributorResourceService, "listResourceTypeOptions");
        }

        @Test
        @DisplayName("Should return empty list when no resource type options exist")
        void shouldReturnEmptyListWhenNoResourceTypeOptionsExist() throws Exception {
            when(contributorResourceService.listResourceTypeOptions()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/contributor/resources/resource-type-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/tag-options")
    class ListTagOptionsTests {

        @Test
        @DisplayName("Should return available tag options")
        void shouldReturnTagOptions() throws Exception {
            CategoryTagOptionVO option = option(3L, "Heritage");
            when(contributorResourceService.listTagOptions()).thenReturn(List.of(option));

            mockMvc.perform(get("/api/contributor/resources/tag-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(3L))
                    .andExpect(jsonPath("$[0].name").value("Heritage"));

            assertInvokedOnce(contributorResourceService, "listTagOptions");
        }

        @Test
        @DisplayName("Should return empty list when no tag options exist")
        void shouldReturnEmptyListWhenNoTagOptionsExist() throws Exception {
            when(contributorResourceService.listTagOptions()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/contributor/resources/tag-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").doesNotExist());
        }
    }

    private ResourceDetailVO detail(Long id, String title, String status) {
        ResourceDetailVO vo = new ResourceDetailVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setStatus(status);
        vo.setCreatedAt(fixedTime);
        vo.setUpdatedAt(fixedTime);
        return vo;
    }

    private ResourceListItemVO listItem(Long id, String title, String status) {
        ResourceListItemVO vo = new ResourceListItemVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setStatus(status);
        vo.setUpdatedAt(fixedTime);
        return vo;
    }

    private CategoryTagOptionVO option(Long id, String name) {
        CategoryTagOptionVO vo = new CategoryTagOptionVO();
        vo.setId(id);
        vo.setName(name);
        return vo;
    }
}
