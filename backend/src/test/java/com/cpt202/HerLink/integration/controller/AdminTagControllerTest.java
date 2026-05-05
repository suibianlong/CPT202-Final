package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.AdminTagController;
import com.cpt202.HerLink.dto.admin.AdminTagResponse;
import com.cpt202.HerLink.dto.admin.ClassificationStatus;
import com.cpt202.HerLink.dto.admin.TagUsageHistoryResponse;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.admin.AdminClassificationManagementService;
import com.cpt202.HerLink.service.admin.AdminUsageHistoryService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnce;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgAt;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CurrentUserVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminTagController.class)
@DisplayName("AdminTagController Integration Test")
class AdminTagControllerTest {

    private static LocalDateTime fixedTime;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminClassificationManagementService classificationManagementService;

    @MockBean
    private AdminUsageHistoryService usageHistoryService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private CurrentUserVO namedAdminUser;
    private CurrentUserVO blankNameAdminUser;

    @BeforeAll
    static void setUpAll() {
        fixedTime = LocalDateTime.of(2026, 5, 5, 6, 0, 0);
    }

    @BeforeEach
    void setUp() {
        namedAdminUser = adminUser(100L, "Alice Admin");
        blankNameAdminUser = adminUser(103L, "   ");
    }

    @AfterEach
    void tearDown() {
        clearInvocations(classificationManagementService, usageHistoryService, resourcePermissionChecker);
        namedAdminUser = null;
        blankNameAdminUser = null;
    }

    @AfterAll
    static void tearDownAll() {
        fixedTime = null;
    }

    @Nested
    @DisplayName("GET /api/admin/tags")
    class GetAllTagsTests {

        @Test
        @DisplayName("Should return all tags for an authenticated administrator")
        void shouldReturnAllTags() throws Exception {
            AdminTagResponse response = tagResponse(1L, "Traditional Craft", ClassificationStatus.ACTIVE, 8);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(classificationManagementService.getAllTags()).thenReturn(List.of(response));

            mockMvc.perform(get("/api/admin/tags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].tagId").value(1L))
                    .andExpect(jsonPath("$[0].tagName").value("Traditional Craft"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].usageCount").value(8));

            assertInvokedOnce(classificationManagementService, "getAllTags");
        }

        @Test
        @DisplayName("Should return 401 when administrator authentication is missing")
        void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/admin/tags"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags"));

            assertNoInteractions(classificationManagementService);
        }
    }

    @Nested
    @DisplayName("GET /api/admin/tags/active")
    class GetActiveTagsTests {

        @Test
        @DisplayName("Should return active tags for an authenticated administrator")
        void shouldReturnActiveTags() throws Exception {
            AdminTagResponse response = tagResponse(2L, "Festival", ClassificationStatus.ACTIVE, 3);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(classificationManagementService.getActiveTags()).thenReturn(List.of(response));

            mockMvc.perform(get("/api/admin/tags/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].tagId").value(2L))
                    .andExpect(jsonPath("$[0].tagName").value("Festival"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].usageCount").value(3));

            assertInvokedOnce(classificationManagementService, "getActiveTags");
        }

        @Test
        @DisplayName("Should return 403 when current user is not an administrator")
        void shouldReturnForbiddenWhenCurrentUserIsNotAdministrator() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(get("/api/admin/tags/active"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/active"));

            assertNoInteractions(classificationManagementService);
        }
    }

    @Nested
    @DisplayName("GET /api/admin/tags/usage-history")
    class GetTagUsageHistoryTests {

        @Test
        @DisplayName("Should return tag usage history for an authenticated administrator")
        void shouldReturnTagUsageHistory() throws Exception {
            TagUsageHistoryResponse history = usageHistoryResponse(
                    10L,
                    "Embroidery",
                    99L,
                    "Silk Embroidery Record",
                    fixedTime.plusHours(1)
            );

            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(usageHistoryService.getTagUsageHistory()).thenReturn(List.of(history));

            mockMvc.perform(get("/api/admin/tags/usage-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].tagId").value(10L))
                    .andExpect(jsonPath("$[0].tagName").value("Embroidery"))
                    .andExpect(jsonPath("$[0].resourceId").value(99L))
                    .andExpect(jsonPath("$[0].relatedRecordName").value("Silk Embroidery Record"))
                    .andExpect(jsonPath("$[0].dateOfUse").exists());

            assertInvokedOnce(usageHistoryService, "getTagUsageHistory");
        }

        @Test
        @DisplayName("Should return an empty list when no tag usage history exists")
        void shouldReturnEmptyListWhenNoUsageHistoryExists() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(usageHistoryService.getTagUsageHistory()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/tags/usage-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertInvokedOnce(usageHistoryService, "getTagUsageHistory");
        }

        @Test
        @DisplayName("Should return 401 when administrator authentication is missing")
        void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/admin/tags/usage-history"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/usage-history"));

            assertNoInteractions(usageHistoryService);
        }

        @Test
        @DisplayName("Should return 403 when current user is not an administrator")
        void shouldReturnForbiddenWhenCurrentUserIsNotAdministrator() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(get("/api/admin/tags/usage-history"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/usage-history"));

            assertNoInteractions(usageHistoryService);
        }

        @Test
        @DisplayName("Should return propagated business error when service throws an AppException")
        void shouldReturnAppExceptionFromUsageHistoryService() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(usageHistoryService.getTagUsageHistory())
                    .thenThrow(AppException.notFound("Tag usage history does not exist."));

            mockMvc.perform(get("/api/admin/tags/usage-history"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Tag usage history does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/usage-history"));
        }

        @Test
        @DisplayName("Should return 500 when service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(usageHistoryService.getTagUsageHistory())
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(get("/api/admin/tags/usage-history"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/usage-history"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/tags/{id}")
    class UpdateTagTests {

        @Test
        @DisplayName("Should update a tag when request body and path variable are valid")
        void shouldUpdateTagWhenRequestIsValid() throws Exception {
            AdminTagResponse response = tagResponse(5L, "Ancient Music", ClassificationStatus.ACTIVE, 12);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(classificationManagementService.updateTag(eq(5L), any(), eq("Alice Admin"))).thenReturn(response);

            mockMvc.perform(put("/api/admin/tags/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tagName": "Ancient Music"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagId").value(5L))
                    .andExpect(jsonPath("$.tagName").value("Ancient Music"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(12));

            assertInvokedOnceWithArgAt(classificationManagementService, "updateTag", 0, 5L);
            assertInvokedOnceWithArgAt(classificationManagementService, "updateTag", 2, "Alice Admin");
        }

        @Test
        @DisplayName("Should fall back to user_{id} when administrator name is blank")
        void shouldFallbackToUserIdWhenAdministratorNameIsBlank() throws Exception {
            AdminTagResponse response = tagResponse(6L, "Folk Song", ClassificationStatus.ACTIVE, 4);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(blankNameAdminUser);
            when(classificationManagementService.updateTag(eq(6L), any(), eq("user_103"))).thenReturn(response);

            mockMvc.perform(put("/api/admin/tags/6")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tagName": "Folk Song"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagId").value(6L))
                    .andExpect(jsonPath("$.tagName").value("Folk Song"));

            assertInvokedOnceWithArgAt(classificationManagementService, "updateTag", 0, 6L);
            assertInvokedOnceWithArgAt(classificationManagementService, "updateTag", 2, "user_103");
        }

        @Test
        @DisplayName("Should return 400 when request JSON is malformed")
        void shouldReturnBadRequestWhenRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(put("/api/admin/tags/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/5"));

            assertNoInteractions(classificationManagementService, resourcePermissionChecker);
        }

        @Test
        @DisplayName("Should return 400 when service rejects a missing request body")
        void shouldReturnBadRequestWhenServiceRejectsMissingRequestBody() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(classificationManagementService.updateTag(eq(5L), eq(null), eq("Alice Admin")))
                    .thenThrow(AppException.badRequest("Tag request is required."));

            mockMvc.perform(put("/api/admin/tags/5")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Tag request is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/5"));
        }

        @Test
        @DisplayName("Should return 400 when tag id path variable is not a number")
        void shouldReturnBadRequestWhenTagIdPathVariableIsNotANumber() throws Exception {
            mockMvc.perform(put("/api/admin/tags/not-a-number")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tagName": "Updated Tag"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/not-a-number"));

            assertNoInteractions(classificationManagementService, resourcePermissionChecker);
        }

        @Test
        @DisplayName("Should return 404 when tag to update does not exist")
        void shouldReturnNotFoundWhenTagToUpdateDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(classificationManagementService.updateTag(eq(99L), any(), eq("Alice Admin")))
                    .thenThrow(AppException.notFound("Tag does not exist."));

            mockMvc.perform(put("/api/admin/tags/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tagName": "Unknown Tag"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Tag does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/99"));
        }

        @Test
        @DisplayName("Should return 409 when service rejects a duplicate tag name")
        void shouldReturnConflictWhenServiceRejectsDuplicateTagName() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(classificationManagementService.updateTag(eq(5L), any(), eq("Alice Admin")))
                    .thenThrow(AppException.conflict("Tag already exists."));

            mockMvc.perform(put("/api/admin/tags/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tagName": "Duplicate Tag"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Tag already exists."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/5"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/tags/{id}/deactivate")
    class DeactivateTagTests {

        @Test
        @DisplayName("Should deactivate a tag when request is valid")
        void shouldDeactivateTagWhenRequestIsValid() throws Exception {
            AdminTagResponse response = tagResponse(7L, "Weaving", ClassificationStatus.INACTIVE, 9);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(classificationManagementService.deactivateTag(7L, "Alice Admin")).thenReturn(response);

            mockMvc.perform(put("/api/admin/tags/7/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagId").value(7L))
                    .andExpect(jsonPath("$.tagName").value("Weaving"))
                    .andExpect(jsonPath("$.status").value("INACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(9));

            assertInvokedOnceWithArgs(classificationManagementService, "deactivateTag", 7L, "Alice Admin");
        }

        @Test
        @DisplayName("Should fall back to user_{id} when administrator name is blank during deactivate")
        void shouldFallbackToUserIdWhenAdministratorNameIsBlankDuringDeactivate() throws Exception {
            AdminTagResponse response = tagResponse(8L, "Needlework", ClassificationStatus.INACTIVE, 6);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(blankNameAdminUser);
            when(classificationManagementService.deactivateTag(8L, "user_103")).thenReturn(response);

            mockMvc.perform(put("/api/admin/tags/8/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagId").value(8L))
                    .andExpect(jsonPath("$.tagName").value("Needlework"))
                    .andExpect(jsonPath("$.status").value("INACTIVE"));

            assertInvokedOnceWithArgs(classificationManagementService, "deactivateTag", 8L, "user_103");
        }

        @Test
        @DisplayName("Should return 404 when tag to deactivate does not exist")
        void shouldReturnNotFoundWhenTagToDeactivateDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(classificationManagementService.deactivateTag(99L, "Alice Admin"))
                    .thenThrow(AppException.notFound("Tag does not exist."));

            mockMvc.perform(put("/api/admin/tags/99/deactivate"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Tag does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/99/deactivate"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/tags/{id}/activate")
    class ActivateTagTests {

        @Test
        @DisplayName("Should activate a tag when request is valid")
        void shouldActivateTagWhenRequestIsValid() throws Exception {
            AdminTagResponse response = tagResponse(9L, "Opera", ClassificationStatus.ACTIVE, 15);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(classificationManagementService.activateTag(9L, "Alice Admin")).thenReturn(response);

            mockMvc.perform(put("/api/admin/tags/9/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagId").value(9L))
                    .andExpect(jsonPath("$.tagName").value("Opera"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(15));

            assertInvokedOnceWithArgs(classificationManagementService, "activateTag", 9L, "Alice Admin");
        }

        @Test
        @DisplayName("Should fall back to user_{id} when administrator name is blank during activate")
        void shouldFallbackToUserIdWhenAdministratorNameIsBlankDuringActivate() throws Exception {
            AdminTagResponse response = tagResponse(10L, "Paper Cutting", ClassificationStatus.ACTIVE, 7);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(blankNameAdminUser);
            when(classificationManagementService.activateTag(10L, "user_103")).thenReturn(response);

            mockMvc.perform(put("/api/admin/tags/10/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tagId").value(10L))
                    .andExpect(jsonPath("$.tagName").value("Paper Cutting"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            assertInvokedOnceWithArgs(classificationManagementService, "activateTag", 10L, "user_103");
        }

        @Test
        @DisplayName("Should return 500 when service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(classificationManagementService.activateTag(9L, "Alice Admin"))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(put("/api/admin/tags/9/activate"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/admin/tags/9/activate"));
        }
    }

    private CurrentUserVO adminUser(Long userId, String name) {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(userId);
        currentUser.setName(name);
        currentUser.setRole("ADMINISTRATOR");
        return currentUser;
    }

    private AdminTagResponse tagResponse(Long tagId,
                                         String tagName,
                                         ClassificationStatus status,
                                         int usageCount) {
        return new AdminTagResponse(tagId, tagName, status, usageCount, fixedTime, fixedTime);
    }

    private TagUsageHistoryResponse usageHistoryResponse(Long tagId,
                                                         String tagName,
                                                         Long resourceId,
                                                         String relatedRecordName,
                                                         LocalDateTime dateOfUse) {
        TagUsageHistoryResponse response = new TagUsageHistoryResponse();
        response.setTagId(tagId);
        response.setTagName(tagName);
        response.setResourceId(resourceId);
        response.setRelatedRecordName(relatedRecordName);
        response.setDateOfUse(dateOfUse);
        return response;
    }
}
