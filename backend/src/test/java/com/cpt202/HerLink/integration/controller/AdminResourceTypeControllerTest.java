package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.AdminResourceTypeController;
import com.cpt202.HerLink.dto.admin.AdminResourceTypeResponse;
import com.cpt202.HerLink.dto.admin.ClassificationStatus;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.admin.AdminClassificationManagementService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnce;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgAt;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CurrentUserVO;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminResourceTypeController.class)
@DisplayName("AdminResourceTypeController Integration Test")
class AdminResourceTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminClassificationManagementService classificationManagementService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    @Nested
    @DisplayName("GET /api/admin/resource-types")
    class GetAllResourceTypesTests {

        @Test
        @DisplayName("Should return all resource types for an authenticated administrator")
        void shouldReturnAllResourceTypes() throws Exception {
            AdminResourceTypeResponse resourceType = resourceTypeResponse(1L, "Video", ClassificationStatus.ACTIVE, 8);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(100L, "Alice Admin"));
            when(classificationManagementService.getAllResourceTypes()).thenReturn(List.of(resourceType));

            mockMvc.perform(get("/api/admin/resource-types"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].resourceTypeId").value(1L))
                    .andExpect(jsonPath("$[0].typeName").value("Video"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].usageCount").value(8));

            assertInvokedOnce(classificationManagementService, "getAllResourceTypes");
        }

        @Test
        @DisplayName("Should return 401 when administrator authentication is missing")
        void shouldReturnUnauthorizedWhenAdministratorAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/admin/resource-types"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resource-types"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/resource-types/active")
    class GetActiveResourceTypesTests {

        @Test
        @DisplayName("Should return active resource types for an authenticated administrator")
        void shouldReturnActiveResourceTypes() throws Exception {
            AdminResourceTypeResponse resourceType = resourceTypeResponse(2L, "Podcast", ClassificationStatus.ACTIVE, 3);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(101L, "Bob Admin"));
            when(classificationManagementService.getActiveResourceTypes()).thenReturn(List.of(resourceType));

            mockMvc.perform(get("/api/admin/resource-types/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].resourceTypeId").value(2L))
                    .andExpect(jsonPath("$[0].typeName").value("Podcast"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].usageCount").value(3));

            assertInvokedOnce(classificationManagementService, "getActiveResourceTypes");
        }

        @Test
        @DisplayName("Should return 403 when current user is not an administrator")
        void shouldReturnForbiddenWhenCurrentUserIsNotAdministrator() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(get("/api/admin/resource-types/active"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resource-types/active"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/resource-types")
    class CreateResourceTypeTests {

        @Test
        @DisplayName("Should create a resource type when request body is valid")
        void shouldCreateResourceTypeWhenRequestBodyIsValid() throws Exception {
            AdminResourceTypeResponse response = resourceTypeResponse(3L, "Article", ClassificationStatus.ACTIVE, 0);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(102L, "Carol Admin"));
            when(classificationManagementService.createResourceType(any(), eq("Carol Admin"))).thenReturn(response);

            mockMvc.perform(post("/api/admin/resource-types")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "typeName": "Article"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceTypeId").value(3L))
                    .andExpect(jsonPath("$.typeName").value("Article"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(0));

            assertInvokedOnceWithArgAt(classificationManagementService, "createResourceType", 1, "Carol Admin");
        }

        @Test
        @DisplayName("Should fall back to user_{id} when administrator name is blank")
        void shouldFallbackToUserIdWhenAdministratorNameIsBlank() throws Exception {
            AdminResourceTypeResponse response = resourceTypeResponse(4L, "Checklist", ClassificationStatus.ACTIVE, 0);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(103L, "   "));
            when(classificationManagementService.createResourceType(any(), eq("user_103"))).thenReturn(response);

            mockMvc.perform(post("/api/admin/resource-types")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "typeName": "Checklist"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceTypeId").value(4L))
                    .andExpect(jsonPath("$.typeName").value("Checklist"));

            assertInvokedOnceWithArgAt(classificationManagementService, "createResourceType", 1, "user_103");
        }

        @Test
        @DisplayName("Should return 400 when request JSON is malformed")
        void shouldReturnBadRequestWhenRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/admin/resource-types")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/resource-types"));
        }

        @Test
        @DisplayName("Should return 400 when service rejects a missing request body")
        void shouldReturnBadRequestWhenServiceRejectsMissingRequestBody() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(107L, "Grace Admin"));
            when(classificationManagementService.createResourceType(eq(null), eq("Grace Admin")))
                    .thenThrow(AppException.badRequest("Resource type request is required."));

            mockMvc.perform(post("/api/admin/resource-types")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Resource type request is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resource-types"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/resource-types/{id}")
    class UpdateResourceTypeTests {

        @Test
        @DisplayName("Should update a resource type when request body and path variable are valid")
        void shouldUpdateResourceTypeWhenRequestBodyAndPathVariableAreValid() throws Exception {
            AdminResourceTypeResponse response = resourceTypeResponse(5L, "Workbook", ClassificationStatus.ACTIVE, 12);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(104L, "Diana Admin"));
            when(classificationManagementService.updateResourceType(eq(5L), any(), eq("Diana Admin"))).thenReturn(response);

            mockMvc.perform(put("/api/admin/resource-types/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "typeName": "Workbook"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceTypeId").value(5L))
                    .andExpect(jsonPath("$.typeName").value("Workbook"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(12));

            assertInvokedOnceWithArgAt(classificationManagementService, "updateResourceType", 0, 5L);
            assertInvokedOnceWithArgAt(classificationManagementService, "updateResourceType", 2, "Diana Admin");
        }

        @Test
        @DisplayName("Should return 400 when resource type id path variable is not a number")
        void shouldReturnBadRequestWhenResourceTypeIdPathVariableIsNotANumber() throws Exception {
            mockMvc.perform(put("/api/admin/resource-types/not-a-number")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "typeName": "Updated Type"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/resource-types/not-a-number"));
        }

        @Test
        @DisplayName("Should return 404 when resource type to update does not exist")
        void shouldReturnNotFoundWhenResourceTypeToUpdateDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(108L, "Henry Admin"));
            when(classificationManagementService.updateResourceType(eq(99L), any(), eq("Henry Admin")))
                    .thenThrow(AppException.notFound("Resource type does not exist."));

            mockMvc.perform(put("/api/admin/resource-types/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "typeName": "Unknown"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Resource type does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resource-types/99"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/resource-types/{id}/deactivate")
    class DeactivateResourceTypeTests {

        @Test
        @DisplayName("Should deactivate a resource type when request is valid")
        void shouldDeactivateResourceTypeWhenRequestIsValid() throws Exception {
            AdminResourceTypeResponse response = resourceTypeResponse(6L, "Infographic", ClassificationStatus.INACTIVE, 9);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(105L, "Ethan Admin"));
            when(classificationManagementService.deactivateResourceType(6L, "Ethan Admin")).thenReturn(response);

            mockMvc.perform(put("/api/admin/resource-types/6/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceTypeId").value(6L))
                    .andExpect(jsonPath("$.typeName").value("Infographic"))
                    .andExpect(jsonPath("$.status").value("INACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(9));

            assertInvokedOnceWithArgs(classificationManagementService, "deactivateResourceType", 6L, "Ethan Admin");
        }

        @Test
        @DisplayName("Should return 409 when resource type is already inactive")
        void shouldReturnConflictWhenResourceTypeIsAlreadyInactive() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(109L, "Ivy Admin"));
            when(classificationManagementService.deactivateResourceType(6L, "Ivy Admin"))
                    .thenThrow(AppException.conflict("Resource type is already inactive."));

            mockMvc.perform(put("/api/admin/resource-types/6/deactivate"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Resource type is already inactive."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resource-types/6/deactivate"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/resource-types/{id}/activate")
    class ActivateResourceTypeTests {

        @Test
        @DisplayName("Should activate a resource type when request is valid")
        void shouldActivateResourceTypeWhenRequestIsValid() throws Exception {
            AdminResourceTypeResponse response = resourceTypeResponse(7L, "Toolkit", ClassificationStatus.ACTIVE, 15);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(106L, "Fiona Admin"));
            when(classificationManagementService.activateResourceType(7L, "Fiona Admin")).thenReturn(response);

            mockMvc.perform(put("/api/admin/resource-types/7/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceTypeId").value(7L))
                    .andExpect(jsonPath("$.typeName").value("Toolkit"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(15));

            assertInvokedOnceWithArgs(classificationManagementService, "activateResourceType", 7L, "Fiona Admin");
        }

        @Test
        @DisplayName("Should return 500 when service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(110L, "Jack Admin"));
            when(classificationManagementService.activateResourceType(7L, "Jack Admin"))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(put("/api/admin/resource-types/7/activate"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resource-types/7/activate"));
        }
    }

    private CurrentUserVO adminUser(Long userId, String name) {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(userId);
        currentUser.setName(name);
        currentUser.setRole("ADMINISTRATOR");
        return currentUser;
    }

    private AdminResourceTypeResponse resourceTypeResponse(Long id,
                                                           String typeName,
                                                           ClassificationStatus status,
                                                           int usageCount) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 5, 5, 0, 0);
        return new AdminResourceTypeResponse(id, typeName, status, usageCount, now, now);
    }
}
