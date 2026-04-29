package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.service.ContributorResourceService;
import com.cpt202.HerLink.service.ResourceVersionService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceSubmissionVO;
import com.cpt202.HerLink.vo.ResourceVersionCompareVO;
import com.cpt202.HerLink.vo.ResourceVersionDiffItemVO;
import com.cpt202.HerLink.vo.ResourceVersionVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContributorResourceHistoryController.class)
class ContributorResourceHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContributorResourceService contributorResourceService;

    @MockBean
    private ResourceVersionService resourceVersionService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    @Test
    void listSubmissionHistory_shouldReturnSubmissionRecords() throws Exception {
        when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);

        ResourceSubmissionVO submissionVO = new ResourceSubmissionVO();
        submissionVO.setSubmissionId(10L);
        submissionVO.setVersionNo(2);
        submissionVO.setSubmissionNote("Please review this update");

        when(contributorResourceService.listSubmissionHistory(1L, 30L)).thenReturn(List.of(submissionVO));

        mockMvc.perform(get("/api/contributor/resources/30/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].submissionId").value(10L))
                .andExpect(jsonPath("$[0].versionNo").value(2))
                .andExpect(jsonPath("$[0].submissionNote").value("Please review this update"));
    }

    @Test
    void listVersions_shouldReturnVersionHistory() throws Exception {
        when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);

        ResourceVersionVO versionVO = new ResourceVersionVO();
        versionVO.setVersionId(12L);
        versionVO.setVersionNo(4);
        versionVO.setChangeType("rollback");
        versionVO.setCreatedAt(LocalDateTime.now());

        when(resourceVersionService.listVersions(1L, 30L)).thenReturn(List.of(versionVO));

        mockMvc.perform(get("/api/contributor/resources/30/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionId").value(12L))
                .andExpect(jsonPath("$[0].versionNo").value(4))
                .andExpect(jsonPath("$[0].changeType").value("rollback"));
    }

    @Test
    void compareVersions_shouldReturnDiffItems() throws Exception {
        when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);

        ResourceVersionDiffItemVO diffItemVO = new ResourceVersionDiffItemVO();
        diffItemVO.setFieldName("title");
        diffItemVO.setFieldLabel("Title");
        diffItemVO.setLeftValue("Old Title");
        diffItemVO.setRightValue("New Title");
        diffItemVO.setChanged(true);

        ResourceVersionCompareVO compareVO = new ResourceVersionCompareVO();
        compareVO.setResourceId(30L);
        compareVO.setLeftVersionNo(1);
        compareVO.setRightVersionNo(4);
        compareVO.setDiffItems(List.of(diffItemVO));

        when(resourceVersionService.compareVersions(1L, 30L, 1, 4)).thenReturn(compareVO);

        mockMvc.perform(get("/api/contributor/resources/30/versions/compare?v1=1&v2=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value(30L))
                .andExpect(jsonPath("$.diffItems[0].fieldLabel").value("Title"))
                .andExpect(jsonPath("$.diffItems[0].changed").value(true));
    }

    @Test
    void rollbackToVersion_shouldReturnUpdatedResourceDetail() throws Exception {
        when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);

        ResourceDetailVO detailVO = new ResourceDetailVO();
        detailVO.setId(30L);
        detailVO.setStatus("Rejected");
        detailVO.setCurrentVersionNo(5);

        when(resourceVersionService.rollbackToVersion(1L, 30L, 2)).thenReturn(detailVO);

        mockMvc.perform(post("/api/contributor/resources/30/versions/2/rollback")
                        .contentType(APPLICATION_JSON)
                        .content("{\"confirmed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30L))
                .andExpect(jsonPath("$.status").value("Rejected"))
                .andExpect(jsonPath("$.currentVersionNo").value(5));

        verify(resourceVersionService).rollbackToVersion(eq(1L), eq(30L), eq(2));
    }
}
