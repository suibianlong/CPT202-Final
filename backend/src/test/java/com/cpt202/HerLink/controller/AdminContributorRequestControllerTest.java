package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.ContributorRequestVO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminContributorRequestController.class)
class AdminContributorRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAccessService userAccessService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    @Test
    void listPendingRequests_shouldReturnPendingList() throws Exception {
        ContributorRequestVO pendingRequest = new ContributorRequestVO();
        pendingRequest.setRequestId(9L);
        pendingRequest.setUserId(4L);
        pendingRequest.setUserName("Pending Applicant");
        pendingRequest.setUserEmail("pending@example.com");
        pendingRequest.setStatus("PENDING");

        when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(1L);
        when(userAccessService.listPendingContributorRequests()).thenReturn(List.of(pendingRequest));

        mockMvc.perform(get("/api/admin/contributor-requests/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(9L))
                .andExpect(jsonPath("$[0].userName").value("Pending Applicant"));
    }

    @Test
    void reviewRequest_shouldDelegateDecision() throws Exception {
        ContributorRequestVO approvedRequest = new ContributorRequestVO();
        approvedRequest.setRequestId(9L);
        approvedRequest.setStatus("APPROVED");

        when(resourcePermissionChecker.requireAdminUserId(any())).thenReturn(1L);
        when(userAccessService.reviewContributorRequest(org.mockito.Mockito.eq(1L), org.mockito.Mockito.eq(9L), any()))
                .thenReturn(approvedRequest);

        String body = """
                {
                  "decision": "APPROVED",
                  "reviewComment": "Approved for contributor access."
                }
                """;

        mockMvc.perform(post("/api/admin/contributor-requests/9/decision")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(userAccessService).reviewContributorRequest(org.mockito.Mockito.eq(1L), org.mockito.Mockito.eq(9L), any());
    }
}
