package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.AdminResourceController;
import com.cpt202.HerLink.dto.admin.AdminResourceLifecycleResponse;
import com.cpt202.HerLink.dto.admin.ResourceLifecycleRow;
import com.cpt202.HerLink.dto.review.ResourceReviewStatus;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.admin.AdminResourceLifecycleService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CurrentUserVO;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminResourceController.class)
@DisplayName("AdminResourceController Integration Test")
class AdminResourceControllerTest {

    private static final LocalDateTime FIXED_ARCHIVED_AT = LocalDateTime.of(2026, 5, 5, 4, 30, 0);
    private static final LocalDateTime FIXED_UPDATED_AT = LocalDateTime.of(2026, 5, 5, 4, 31, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminResourceLifecycleService lifecycleService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private CurrentUserVO namedAdminUser;
    private CurrentUserVO emailFallbackAdminUser;
    private CurrentUserVO idFallbackAdminUser;
    private AdminResourceLifecycleResponse archivedResponse;
    private AdminResourceLifecycleResponse unarchivedResponse;

    @BeforeEach
    void setUp() {
        namedAdminUser = adminUser(1L, "Olivia Admin", "olivia@example.com");
        emailFallbackAdminUser = adminUser(2L, "   ", "ops@example.com");
        idFallbackAdminUser = adminUser(3L, "", "   ");

        archivedResponse = new AdminResourceLifecycleResponse(
                301L,
                "Crisis Hotline Guide",
                ResourceReviewStatus.APPROVED,
                ResourceReviewStatus.ARCHIVED,
                FIXED_ARCHIVED_AT,
                FIXED_UPDATED_AT,
                true,
                "Resource archived and hidden from public discovery."
        );
        unarchivedResponse = new AdminResourceLifecycleResponse(
                302L,
                "Archived Resource",
                ResourceReviewStatus.ARCHIVED,
                ResourceReviewStatus.APPROVED,
                null,
                FIXED_UPDATED_AT,
                true,
                "Resource restored to approved and visible to viewers."
        );
    }

    @Nested
    @DisplayName("GET /api/admin/resources")
    class ListResourcesTests {

        @Test
        @DisplayName("Should list resources for an authenticated administrator")
        void shouldListResourcesForAuthenticatedAdministrator() throws Exception {
            ResourceLifecycleRow row = lifecycleRow(301L, "Visible Resource", "Approved", null);
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(lifecycleService.listResources("Approved")).thenReturn(List.of(row));

            mockMvc.perform(get("/api/admin/resources").param("status", "Approved"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].resourceId").value(301L))
                    .andExpect(jsonPath("$[0].title").value("Visible Resource"))
                    .andExpect(jsonPath("$[0].status").value("Approved"))
                    .andExpect(jsonPath("$[0].archivedAt").doesNotExist())
                    .andExpect(jsonPath("$[0].updatedAt").exists());

            assertInvokedOnceWithArgs(lifecycleService, "listResources", "Approved");
        }

        @Test
        @DisplayName("Should return 401 when administrator authentication is missing for list")
        void shouldReturnUnauthorizedWhenAdministratorAuthenticationIsMissingForList() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/admin/resources"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resources"));

            assertNoInteractions(lifecycleService);
        }

        @Test
        @DisplayName("Should return 403 when current user is not an administrator for list")
        void shouldReturnForbiddenWhenCurrentUserIsNotAdministratorForList() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(get("/api/admin/resources"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resources"));

            assertNoInteractions(lifecycleService);
        }
    }

    @Nested
    @DisplayName("POST /api/admin/resources/{resourceId}/archive")
    class ArchiveResourceTests {

        @Test
        @DisplayName("Should archive an approved resource for an authenticated administrator")
        void shouldArchiveApprovedResourceForAuthenticatedAdministrator() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(lifecycleService.archiveResource(301L, "Olivia Admin")).thenReturn(archivedResponse);

            mockMvc.perform(post("/api/admin/resources/301/archive"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceId").value(301L))
                    .andExpect(jsonPath("$.title").value("Crisis Hotline Guide"))
                    .andExpect(jsonPath("$.previousStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.resourceStatus").value("ARCHIVED"))
                    .andExpect(jsonPath("$.archivedAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(jsonPath("$.changed").value(true))
                    .andExpect(jsonPath("$.message").value("Resource archived and hidden from public discovery."));

            assertInvokedOnceWithArgs(lifecycleService, "archiveResource", 301L, "Olivia Admin");
        }

        @Test
        @DisplayName("Should fallback to administrator email when display name is blank")
        void shouldFallbackToAdministratorEmailWhenDisplayNameIsBlank() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(emailFallbackAdminUser);
            when(lifecycleService.archiveResource(301L, "ops@example.com")).thenReturn(archivedResponse);

            mockMvc.perform(post("/api/admin/resources/301/archive"))
                    .andExpect(status().isOk());

            assertInvokedOnceWithArgs(lifecycleService, "archiveResource", 301L, "ops@example.com");
        }

        @Test
        @DisplayName("Should fallback to administrator id when name and email are blank")
        void shouldFallbackToAdministratorIdWhenNameAndEmailAreBlank() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(idFallbackAdminUser);
            when(lifecycleService.archiveResource(301L, "admin#3")).thenReturn(archivedResponse);

            mockMvc.perform(post("/api/admin/resources/301/archive"))
                    .andExpect(status().isOk());

            assertInvokedOnceWithArgs(lifecycleService, "archiveResource", 301L, "admin#3");
        }

        @Test
        @DisplayName("Should return 401 when administrator authentication is missing")
        void shouldReturnUnauthorizedWhenAdministratorAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(post("/api/admin/resources/301/archive"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resources/301/archive"));

            assertNoInteractions(lifecycleService);
        }

        @Test
        @DisplayName("Should return 403 when current user is not an administrator")
        void shouldReturnForbiddenWhenCurrentUserIsNotAdministrator() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(post("/api/admin/resources/301/archive"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resources/301/archive"));

            assertNoInteractions(lifecycleService);
        }

        @Test
        @DisplayName("Should return 400 when resource id path variable is not a number")
        void shouldReturnBadRequestWhenResourceIdPathVariableIsNotANumber() throws Exception {
            mockMvc.perform(post("/api/admin/resources/not-a-number/archive"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/admin/resources/not-a-number/archive"));
        }

        @Test
        @DisplayName("Should return 404 when resource does not exist")
        void shouldReturnNotFoundWhenResourceDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(lifecycleService.archiveResource(999L, "Olivia Admin"))
                    .thenThrow(AppException.notFound("Resource does not exist."));

            mockMvc.perform(post("/api/admin/resources/999/archive"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resources/999/archive"));
        }

        @Test
        @DisplayName("Should return 409 when resource cannot be archived from its current status")
        void shouldReturnConflictWhenResourceCannotBeArchivedFromCurrentStatus() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(lifecycleService.archiveResource(303L, "Olivia Admin"))
                    .thenThrow(AppException.conflict("Only Approved resources can be archived."));

            mockMvc.perform(post("/api/admin/resources/303/archive"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Only Approved resources can be archived."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resources/303/archive"));
        }

        @Test
        @DisplayName("Should return 500 when archive service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenArchiveServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(lifecycleService.archiveResource(301L, "Olivia Admin"))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(post("/api/admin/resources/301/archive"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resources/301/archive"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/resources/{resourceId}/unarchive")
    class UnarchiveResourceTests {

        @Test
        @DisplayName("Should unarchive an archived resource for an authenticated administrator")
        void shouldUnarchiveArchivedResourceForAuthenticatedAdministrator() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(lifecycleService.unarchiveResource(302L, "Olivia Admin")).thenReturn(unarchivedResponse);

            mockMvc.perform(post("/api/admin/resources/302/unarchive"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceId").value(302L))
                    .andExpect(jsonPath("$.title").value("Archived Resource"))
                    .andExpect(jsonPath("$.previousStatus").value("ARCHIVED"))
                    .andExpect(jsonPath("$.resourceStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.changed").value(true))
                    .andExpect(jsonPath("$.message").value("Resource restored to approved and visible to viewers."));

            assertInvokedOnceWithArgs(lifecycleService, "unarchiveResource", 302L, "Olivia Admin");
        }

        @Test
        @DisplayName("Should return 404 when resource to unarchive does not exist")
        void shouldReturnNotFoundWhenResourceToUnarchiveDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(namedAdminUser);
            when(lifecycleService.unarchiveResource(998L, "Olivia Admin"))
                    .thenThrow(AppException.notFound("Resource does not exist."));

            mockMvc.perform(post("/api/admin/resources/998/unarchive"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/admin/resources/998/unarchive"));
        }
    }

    private CurrentUserVO adminUser(Long userId, String name, String email) {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(userId);
        currentUser.setName(name);
        currentUser.setEmail(email);
        currentUser.setRole("ADMINISTRATOR");
        return currentUser;
    }

    private ResourceLifecycleRow lifecycleRow(Long resourceId, String title, String status, LocalDateTime archivedAt) {
        ResourceLifecycleRow row = new ResourceLifecycleRow();
        row.setResourceId(resourceId);
        row.setTitle(title);
        row.setStatus(status);
        row.setArchivedAt(archivedAt);
        row.setUpdatedAt(FIXED_UPDATED_AT);
        return row;
    }
}
