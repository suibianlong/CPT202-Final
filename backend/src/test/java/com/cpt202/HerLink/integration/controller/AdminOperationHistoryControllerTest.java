package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.AdminOperationHistoryController;
import com.cpt202.HerLink.dto.admin.AdminOperationHistoryResponse;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.admin.AdminOperationHistoryService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CurrentUserVO;
import java.time.LocalDateTime;
import java.util.Collections;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOperationHistoryController.class)
@DisplayName("AdminOperationHistoryController Integration Test")
class AdminOperationHistoryControllerTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 5, 5, 4, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminOperationHistoryService operationHistoryService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private CurrentUserVO adminUser;
    private AdminOperationHistoryResponse resourceHistory;
    private AdminOperationHistoryResponse classificationHistory;

    @BeforeEach
    void setUp() {
        adminUser = adminUser(1L, "Olivia Admin");
        resourceHistory = operationHistory(
                101L,
                "Crisis Hotline Guide",
                "RESOURCE",
                "RESOURCE",
                "CREATE",
                "Olivia Admin",
                "DATABASE"
        );
        classificationHistory = operationHistory(
                102L,
                "Mental Health",
                "CLASSIFICATION",
                "CLASSIFICATION",
                "UPDATE",
                "Olivia Admin",
                "DATABASE"
        );
    }

    @Nested
    @DisplayName("GET /api/admin/operation-history")
    class GetOperationHistoryTests {

        @Test
        @DisplayName("Should return all operation history when module filter is not provided")
        void shouldReturnAllOperationHistoryWhenModuleFilterIsNotProvided() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser);
            when(operationHistoryService.getOperationHistory(null))
                    .thenReturn(List.of(resourceHistory, classificationHistory));

            mockMvc.perform(get("/api/admin/operation-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].historyId").value(101L))
                    .andExpect(jsonPath("$[0].itemName").value("Crisis Hotline Guide"))
                    .andExpect(jsonPath("$[0].kind").value("RESOURCE"))
                    .andExpect(jsonPath("$[0].module").value("RESOURCE"))
                    .andExpect(jsonPath("$[0].action").value("CREATE"))
                    .andExpect(jsonPath("$[0].administrator").value("Olivia Admin"))
                    .andExpect(jsonPath("$[0].persistenceMode").value("DATABASE"))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[1].historyId").value(102L))
                    .andExpect(jsonPath("$[1].module").value("CLASSIFICATION"))
                    .andExpect(jsonPath("$[1].action").value("UPDATE"));

            assertInvokedOnceWithArgs(operationHistoryService, "getOperationHistory", (Object) null);
        }

        @Test
        @DisplayName("Should return filtered operation history when module query parameter is provided")
        void shouldReturnFilteredOperationHistoryWhenModuleQueryParameterIsProvided() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser);
            when(operationHistoryService.getOperationHistory("RESOURCE"))
                    .thenReturn(List.of(resourceHistory));

            mockMvc.perform(get("/api/admin/operation-history").param("module", "RESOURCE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].historyId").value(101L))
                    .andExpect(jsonPath("$[0].itemName").value("Crisis Hotline Guide"))
                    .andExpect(jsonPath("$[0].module").value("RESOURCE"))
                    .andExpect(jsonPath("$[0].action").value("CREATE"))
                    .andExpect(jsonPath("$[0].administrator").value("Olivia Admin"))
                    .andExpect(jsonPath("$").isArray());

            assertInvokedOnceWithArgs(operationHistoryService, "getOperationHistory", "RESOURCE");
        }

        @Test
        @DisplayName("Should return an empty list when no operation history exists")
        void shouldReturnEmptyListWhenNoOperationHistoryExists() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser);
            when(operationHistoryService.getOperationHistory(null)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/operation-history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertInvokedOnceWithArgs(operationHistoryService, "getOperationHistory", (Object) null);
        }

        @Test
        @DisplayName("Should return 401 when administrator authentication is missing")
        void shouldReturnUnauthorizedWhenAdministratorAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/admin/operation-history"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/admin/operation-history"));

            assertNoInteractions(operationHistoryService);
        }

        @Test
        @DisplayName("Should return 403 when current user is not an administrator")
        void shouldReturnForbiddenWhenCurrentUserIsNotAdministrator() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any()))
                    .thenThrow(AppException.forbidden("Administrator permission is required."));

            mockMvc.perform(get("/api/admin/operation-history"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/admin/operation-history"));

            assertNoInteractions(operationHistoryService);
        }

        @Test
        @DisplayName("Should return propagated business error when module filter is rejected by the service")
        void shouldReturnPropagatedBusinessErrorWhenModuleFilterIsRejectedByService() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser);
            when(operationHistoryService.getOperationHistory("UNKNOWN"))
                    .thenThrow(AppException.badRequest("Module filter is invalid."));

            mockMvc.perform(get("/api/admin/operation-history").param("module", "UNKNOWN"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Module filter is invalid."))
                    .andExpect(jsonPath("$.path").value("/api/admin/operation-history"));
        }

        @Test
        @DisplayName("Should return 500 when service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAdminUser(any())).thenReturn(adminUser);
            when(operationHistoryService.getOperationHistory("RESOURCE"))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(get("/api/admin/operation-history").param("module", "RESOURCE"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/admin/operation-history"));
        }
    }

    private CurrentUserVO adminUser(Long userId, String name) {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(userId);
        currentUser.setName(name);
        currentUser.setRole("ADMINISTRATOR");
        return currentUser;
    }

    private AdminOperationHistoryResponse operationHistory(Long historyId,
                                                           String itemName,
                                                           String kind,
                                                           String module,
                                                           String action,
                                                           String administrator,
                                                           String persistenceMode) {
        AdminOperationHistoryResponse response = new AdminOperationHistoryResponse();
        response.setHistoryId(historyId);
        response.setItemName(itemName);
        response.setKind(kind);
        response.setModule(module);
        response.setAction(action);
        response.setAdministrator(administrator);
        response.setCreatedAt(FIXED_TIME);
        response.setPersistenceMode(persistenceMode);
        return response;
    }
}
