package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CurrentUserVO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAccessService userAccessService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    @Test
    void register_shouldReturnCreatedUser() throws Exception {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(5L);
        currentUser.setName("Registered Viewer");
        currentUser.setEmail("viewer@example.com");
        currentUser.setRole("REGISTERED_VIEWER");
        currentUser.setContributorStatus("NONE");

        when(userAccessService.register(any())).thenReturn(currentUser);

        String body = """
                {
                  "name": "Registered Viewer",
                  "email": "viewer@example.com",
                  "password": "Viewer123!"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(5L))
                .andExpect(jsonPath("$.role").value("REGISTERED_VIEWER"));
    }

    @Test
    void getCurrentUser_shouldReturnSessionUser() throws Exception {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(2L);
        currentUser.setName("Approved Contributor");
        currentUser.setRole("REGISTERED_VIEWER");
        currentUser.setContributorStatus("APPROVED");
        currentUser.setContributor(true);

        when(resourcePermissionChecker.requireCurrentUser(any())).thenReturn(currentUser);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2L))
                .andExpect(jsonPath("$.contributor").value(true));
    }

    @Test
    void updateAccount_shouldDelegateToService() throws Exception {
        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(3L);
        currentUser.setName("Viewer Updated");
        currentUser.setEmail("viewer-updated@example.com");
        currentUser.setRole("REGISTERED_VIEWER");
        currentUser.setContributorStatus("NONE");

        when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(3L);
        when(userAccessService.updateAccount(org.mockito.Mockito.eq(3L), any())).thenReturn(currentUser);

        String body = """
                {
                  "name": "Viewer Updated",
                  "email": "viewer-updated@example.com"
                }
                """;

        mockMvc.perform(put("/api/auth/account")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("viewer-updated@example.com"));

        verify(userAccessService).updateAccount(org.mockito.Mockito.eq(3L), any());
    }
}
