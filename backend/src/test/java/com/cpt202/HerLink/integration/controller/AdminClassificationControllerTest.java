package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.AdminClassificationController;
import com.cpt202.HerLink.dto.admin.ClassificationUsageHistoryResponse;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.admin.AdminUsageHistoryService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnce;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CurrentUserVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminClassificationController.class)
@DisplayName("AdminClassificationController Integration Test")
class AdminClassificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUsageHistoryService usageHistoryService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    @Nested
    @DisplayName("GET /api/admin/classifications/usage-history")
    class GetClassificationUsageHistoryTests {

        @Test
        @DisplayName("Should return classification usage history for an authenticated administrator")
        void shouldReturnClassificationUsageHistory() throws Exception {
            ClassificationUsageHistoryResponse history = usageHistoryResponse(
                    10L,
                    "Mental Health",
                    "Topic",
                    99L,
                    "Resource A",
                    LocalDateTime.of(2026, 5, 5, 2, 30, 0)
            );

            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(1L, "Admin User"));
            when(usageHistoryService.getClassificationUsageHistory()).thenReturn(List.of(history));

            mockMvc.perform(get("/api/admin/classifications/usage-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].classificationId").value(10L))
                    .andExpect(jsonPath("$[0].name").value("Mental Health"))
                    .andExpect(jsonPath("$[0].kind").value("Topic"))
                    .andExpect(jsonPath("$[0].resourceId").value(99L))
                    .andExpect(jsonPath("$[0].relatedRecordName").value("Resource A"))
                    .andExpect(jsonPath("$[0].dateOfUse").exists());

            assertInvokedOnce(usageHistoryService, "getClassificationUsageHistory");
        }

        @Test
        @DisplayName("Should return an empty list when no classification usage history exists")
        void shouldReturnEmptyListWhenNoHistoryExists() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(2L, "Admin User"));
            when(usageHistoryService.getClassificationUsageHistory()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/classifications/usage-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertInvokedOnce(usageHistoryService, "getClassificationUsageHistory");
        }

        @Test
        @DisplayName("Should return 401 when administrator authentication is missing")
        void shouldReturnUnauthorizedWhenPermissionCheckerRejects() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/admin/classifications/usage-history"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/admin/classifications/usage-history"));

            assertNoInteractions(usageHistoryService);
        }

        @Test
        @DisplayName("Should return 403 when current user is not an administrator")
        void shouldReturnForbiddenWhenCurrentUserIsNotAdmin() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(get("/api/admin/classifications/usage-history"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/classifications/usage-history"));

            assertNoInteractions(usageHistoryService);
        }

        @Test
        @DisplayName("Should return propagated business error when service throws an AppException")
        void shouldReturnAppExceptionFromService() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(3L, "Admin User"));
            when(usageHistoryService.getClassificationUsageHistory())
                    .thenThrow(AppException.notFound("Classification usage history does not exist."));

            mockMvc.perform(get("/api/admin/classifications/usage-history"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Classification usage history does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/admin/classifications/usage-history"));
        }

        @Test
        @DisplayName("Should return 500 when service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser(4L, "Admin User"));
            when(usageHistoryService.getClassificationUsageHistory())
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(get("/api/admin/classifications/usage-history"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/admin/classifications/usage-history"));
        }
    }

    private CurrentUserVO adminUser(Long userId, String name) {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(userId);
        currentUser.setName(name);
        currentUser.setRole("ADMINISTRATOR");
        return currentUser;
    }

    private ClassificationUsageHistoryResponse usageHistoryResponse(Long classificationId,
                                                                    String name,
                                                                    String kind,
                                                                    Long resourceId,
                                                                    String relatedRecordName,
                                                                    LocalDateTime dateOfUse) {
        ClassificationUsageHistoryResponse response = new ClassificationUsageHistoryResponse();
        response.setClassificationId(classificationId);
        response.setName(name);
        response.setKind(kind);
        response.setResourceId(resourceId);
        response.setRelatedRecordName(relatedRecordName);
        response.setDateOfUse(dateOfUse);
        return response;
    }
}
