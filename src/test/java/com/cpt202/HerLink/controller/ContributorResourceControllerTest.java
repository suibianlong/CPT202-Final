package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.resource.ResourceQueryRequest;
import com.cpt202.HerLink.service.ContributorResourceService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// controller test
@WebMvcTest(ContributorResourceController.class)
class ContributorResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContributorResourceService contributorResourceService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    // get my resource list
    @Test
    void listMyResources_shouldReturnMyResourceList() throws Exception {
        // setup
        when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);

        ResourceListItemVO item = new ResourceListItemVO();
        item.setId(10L);
        item.setTitle("My Resource");
        item.setStatus("DRAFT");
        item.setUpdatedAt(LocalDateTime.now());

        when(contributorResourceService.listMyResources(eq(1L), any(ResourceQueryRequest.class)))
                .thenReturn(List.of(item));

        // call & assertion
        mockMvc.perform(get("/api/contributor/resources/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].title").value("My Resource"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    // edit resources
    @Test
    void updateResource_shouldReturnUpdatedDetail() throws Exception {
        // setup
        when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);

        ResourceDetailVO detailVO = new ResourceDetailVO();
        detailVO.setId(20L);
        detailVO.setTitle("Updated Title");
        detailVO.setDescription("Updated Description");
        detailVO.setCategoryId(2L);
        detailVO.setPlace("Suzhou");
        detailVO.setResourceType("IMAGE");
        detailVO.setStatus("DRAFT");

        when(contributorResourceService.updateResource(eq(1L), eq(20L), any()))
                .thenReturn(detailVO);

        String requestBody = """
                {
                "title": "Updated Title",
                "description": "Updated Description",
                "categoryId": 2,
                "place": "Suzhou",
                "resourceType": "IMAGE",
                "tagIds": [1, 2]
                }
                """;

        // call & assertion
        mockMvc.perform(put("/api/contributor/resources/20")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20L))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.categoryId").value(2))
                .andExpect(jsonPath("$.place").value("Suzhou"))
                .andExpect(jsonPath("$.resourceType").value("IMAGE"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(contributorResourceService).updateResource(eq(1L), eq(20L), any());
    }

    // test submit
    @Test
    void submitResource_shouldDelegateToService() throws Exception {
        // setup
        when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);

        String requestBody = """
                {
                "submissionNote": "Please review this submission"
                }
                """;

        // call & assertion
        mockMvc.perform(post("/api/contributor/resources/30/submit")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(contributorResourceService).submitResource(eq(1L), eq(30L), any());
    }
}