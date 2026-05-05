package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.AdminCategoryController;
import com.cpt202.HerLink.dto.admin.AdminCategoryResponse;
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

@WebMvcTest(AdminCategoryController.class)
@DisplayName("AdminCategoryController Integration Test")
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminClassificationManagementService classificationManagementService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    @Nested
    @DisplayName("GET /api/admin/categories")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("Should return all categories for an authenticated administrator")
        void shouldReturnCategoryList() throws Exception {
            AdminCategoryResponse category = categoryResponse(1L, "Health", ClassificationStatus.ACTIVE, 8);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(100L, "Alice Admin"));
            when(classificationManagementService.getAllCategories()).thenReturn(List.of(category));

            mockMvc.perform(get("/api/admin/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].categoryId").value(1L))
                    .andExpect(jsonPath("$[0].categoryTopic").value("Health"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].usageCount").value(8));

            assertInvokedOnce(classificationManagementService, "getAllCategories");
        }

        @Test
        @DisplayName("Should return 401 when administrator authentication is missing")
        void shouldReturnUnauthorizedWhenPermissionCheckerRejects() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/admin/categories"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/admin/categories"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/categories/active")
    class GetActiveCategoriesTests {

        @Test
        @DisplayName("Should return active categories for an authenticated administrator")
        void shouldReturnActiveCategoryList() throws Exception {
            AdminCategoryResponse category = categoryResponse(2L, "Career", ClassificationStatus.ACTIVE, 3);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(101L, "Bob Admin"));
            when(classificationManagementService.getActiveCategories()).thenReturn(List.of(category));

            mockMvc.perform(get("/api/admin/categories/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].categoryId").value(2L))
                    .andExpect(jsonPath("$[0].categoryTopic").value("Career"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].usageCount").value(3));

            assertInvokedOnce(classificationManagementService, "getActiveCategories");
        }

        @Test
        @DisplayName("Should return 403 when current user is not an administrator")
        void shouldReturnForbiddenWhenCurrentUserIsNotAdmin() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(get("/api/admin/categories/active"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/categories/active"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/categories")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create a category when request body is valid")
        void shouldReturnCreatedCategory() throws Exception {
            AdminCategoryResponse response = categoryResponse(3L, "Education", ClassificationStatus.ACTIVE, 0);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(102L, "Carol Admin"));
            when(classificationManagementService.createCategory(any(), eq("Carol Admin"))).thenReturn(response);

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "categoryTopic": "Education"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categoryId").value(3L))
                    .andExpect(jsonPath("$.categoryTopic").value("Education"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(0));

            assertInvokedOnceWithArgAt(classificationManagementService, "createCategory", 1, "Carol Admin");
        }

        @Test
        @DisplayName("Should fall back to user_{id} when administrator name is blank")
        void shouldFallbackToUserIdWhenAdminNameBlank() throws Exception {
            AdminCategoryResponse response = categoryResponse(4L, "Policy", ClassificationStatus.ACTIVE, 0);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(103L, "   "));
            when(classificationManagementService.createCategory(any(), eq("user_103"))).thenReturn(response);

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "categoryTopic": "Policy"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categoryId").value(4L))
                    .andExpect(jsonPath("$.categoryTopic").value("Policy"));

            assertInvokedOnceWithArgAt(classificationManagementService, "createCategory", 1, "user_103");
        }

        @Test
        @DisplayName("Should return 400 when request JSON is malformed")
        void shouldReturnBadRequestWhenRequestBodyIsMalformed() throws Exception {
            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/categories"));
        }

        @Test
        @DisplayName("Should return 400 when service rejects a missing request body")
        void shouldReturnBadRequestWhenServiceRejectsNullRequest() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(107L, "Grace Admin"));
            when(classificationManagementService.createCategory(eq(null), eq("Grace Admin")))
                    .thenThrow(AppException.badRequest("Category request is required."));

            mockMvc.perform(post("/api/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Category request is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/categories"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/categories/{id}")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should update a category when request body and path variable are valid")
        void shouldReturnUpdatedCategory() throws Exception {
            AdminCategoryResponse response = categoryResponse(5L, "Mental Health", ClassificationStatus.ACTIVE, 12);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(104L, "Diana Admin"));
            when(classificationManagementService.updateCategory(eq(5L), any(), eq("Diana Admin"))).thenReturn(response);

            mockMvc.perform(put("/api/admin/categories/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "categoryTopic": "Mental Health"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categoryId").value(5L))
                    .andExpect(jsonPath("$.categoryTopic").value("Mental Health"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(12));

            assertInvokedOnceWithArgAt(classificationManagementService, "updateCategory", 0, 5L);
            assertInvokedOnceWithArgAt(classificationManagementService, "updateCategory", 2, "Diana Admin");
        }

        @Test
        @DisplayName("Should return 400 when category id path variable is not a number")
        void shouldReturnBadRequestWhenPathVariableIsInvalid() throws Exception {
            mockMvc.perform(put("/api/admin/categories/not-a-number")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "categoryTopic": "Updated Topic"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/categories/not-a-number"));
        }

        @Test
        @DisplayName("Should return 404 when category to update does not exist")
        void shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(108L, "Henry Admin"));
            when(classificationManagementService.updateCategory(eq(99L), any(), eq("Henry Admin")))
                    .thenThrow(AppException.notFound("Category does not exist."));

            mockMvc.perform(put("/api/admin/categories/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "categoryTopic": "Unknown"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Category does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/admin/categories/99"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/categories/{id}/deactivate")
    class DeactivateCategoryTests {

        @Test
        @DisplayName("Should deactivate a category when request is valid")
        void shouldReturnDeactivatedCategory() throws Exception {
            AdminCategoryResponse response = categoryResponse(6L, "History", ClassificationStatus.INACTIVE, 9);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(105L, "Ethan Admin"));
            when(classificationManagementService.deactivateCategory(6L, "Ethan Admin")).thenReturn(response);

            mockMvc.perform(put("/api/admin/categories/6/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categoryId").value(6L))
                    .andExpect(jsonPath("$.categoryTopic").value("History"))
                    .andExpect(jsonPath("$.status").value("INACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(9));

            assertInvokedOnceWithArgs(classificationManagementService, "deactivateCategory", 6L, "Ethan Admin");
        }

        @Test
        @DisplayName("Should return 409 when category is already inactive")
        void shouldReturnConflictWhenCategoryAlreadyInactive() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(109L, "Ivy Admin"));
            when(classificationManagementService.deactivateCategory(6L, "Ivy Admin"))
                    .thenThrow(AppException.conflict("Category is already inactive."));

            mockMvc.perform(put("/api/admin/categories/6/deactivate"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Category is already inactive."))
                    .andExpect(jsonPath("$.path").value("/api/admin/categories/6/deactivate"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/categories/{id}/activate")
    class ActivateCategoryTests {

        @Test
        @DisplayName("Should activate a category when request is valid")
        void shouldReturnActivatedCategory() throws Exception {
            AdminCategoryResponse response = categoryResponse(7L, "Science", ClassificationStatus.ACTIVE, 15);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(106L, "Fiona Admin"));
            when(classificationManagementService.activateCategory(7L, "Fiona Admin")).thenReturn(response);

            mockMvc.perform(put("/api/admin/categories/7/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categoryId").value(7L))
                    .andExpect(jsonPath("$.categoryTopic").value("Science"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.usageCount").value(15));

            assertInvokedOnceWithArgs(classificationManagementService, "activateCategory", 7L, "Fiona Admin");
        }

        @Test
        @DisplayName("Should return 500 when service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(110L, "Jack Admin"));
            when(classificationManagementService.activateCategory(7L, "Jack Admin"))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(put("/api/admin/categories/7/activate"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/admin/categories/7/activate"));
        }
    }

    private CurrentUserVO adminUser(Long userId, String name) {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(userId);
        currentUser.setName(name);
        currentUser.setRole("ADMINISTRATOR");
        return currentUser;
    }

    private AdminCategoryResponse categoryResponse(Long id,
                                                   String topic,
                                                   ClassificationStatus status,
                                                   int usageCount) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 5, 1, 30, 0);
        return new AdminCategoryResponse(id, topic, status, usageCount, now, now);
    }
}
