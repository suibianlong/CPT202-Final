package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.ViewerResourceController;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.ViewerResourceService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
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

@WebMvcTest(ViewerResourceController.class)
@DisplayName("ViewerResourceController Integration Test")
class ViewerResourceControllerTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 5, 5, 6, 30, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ViewerResourceService viewerResourceService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private ResourceListItemVO resourceListItem;
    private ResourceDetailVO resourceDetail;
    private CategoryTagOptionVO categoryOption;
    private CategoryTagOptionVO resourceTypeOption;

    @BeforeEach
    void setUp() {
        resourceListItem = resourceListItem(
                301L,
                "Festival Archive",
                "A local heritage collection.",
                "video",
                8L,
                "educational materials"
        );
        resourceDetail = resourceDetail();
        categoryOption = option(8L, "Culture");
        resourceTypeOption = option(5L, "video");
    }

    @Nested
    @DisplayName("GET /api/viewer/resources")
    class ListApprovedResourcesTests {

        @Test
        @DisplayName("Should return approved resources for an authenticated user")
        void shouldReturnApprovedResourcesForAuthenticatedUser() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(viewerResourceService.listApprovedResources(null, null, null, null))
                    .thenReturn(List.of(resourceListItem));

            mockMvc.perform(get("/api/viewer/resources"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(301L))
                    .andExpect(jsonPath("$[0].title").value("Festival Archive"))
                    .andExpect(jsonPath("$[0].description").value("A local heritage collection."))
                    .andExpect(jsonPath("$[0].resourceType").value("video"))
                    .andExpect(jsonPath("$[0].categoryId").value(8L))
                    .andExpect(jsonPath("$[0].categoryName").value("educational materials"))
                    .andExpect(jsonPath("$[0].hasReviewFeedback").value(false));

            assertInvokedOnceWithArgs(viewerResourceService, "listApprovedResources", null, null, null, null);
        }

        @Test
        @DisplayName("Should return filtered approved resources when search parameters are provided")
        void shouldReturnFilteredApprovedResourcesWhenSearchParametersAreProvided() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(viewerResourceService.listApprovedResources("festival", "video", 8L, "title"))
                    .thenReturn(List.of(resourceListItem));

            mockMvc.perform(get("/api/viewer/resources")
                            .param("keyword", "festival")
                            .param("type", "video")
                            .param("categoryId", "8")
                            .param("sortBy", "title"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(301L))
                    .andExpect(jsonPath("$[0].resourceType").value("video"));

            assertInvokedOnceWithArgs(viewerResourceService, "listApprovedResources", "festival", "video", 8L, "title");
        }

        @Test
        @DisplayName("Should return an empty list when no approved resources match the filters")
        void shouldReturnEmptyListWhenNoApprovedResourcesMatchTheFilters() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(viewerResourceService.listApprovedResources("unknown", null, null, null))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/viewer/resources").param("keyword", "unknown"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertInvokedOnceWithArgs(viewerResourceService, "listApprovedResources", "unknown", null, null, null);
        }

        @Test
        @DisplayName("Should return 401 when approved resource list is requested without login")
        void shouldReturnUnauthorizedWhenApprovedResourceListIsRequestedWithoutLogin() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/viewer/resources"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources"));

            assertNoInteractions(viewerResourceService);
        }

        @Test
        @DisplayName("Should return 400 when service rejects an unsupported sort option")
        void shouldReturnBadRequestWhenServiceRejectsUnsupportedSortOption() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(viewerResourceService.listApprovedResources(null, null, null, "popularity"))
                    .thenThrow(AppException.badRequest("Unsupported sort option."));

            mockMvc.perform(get("/api/viewer/resources").param("sortBy", "popularity"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Unsupported sort option."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources"));
        }

        @Test
        @DisplayName("Should return 400 when category id query parameter is invalid")
        void shouldReturnBadRequestWhenCategoryIdQueryParameterIsInvalid() throws Exception {
            mockMvc.perform(get("/api/viewer/resources").param("categoryId", "not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources"));
        }
    }

    @Nested
    @DisplayName("GET /api/viewer/resources/{resourceId}")
    class GetApprovedResourceDetailTests {

        @Test
        @DisplayName("Should return approved resource detail for an authenticated user")
        void shouldReturnApprovedResourceDetailForAuthenticatedUser() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(viewerResourceService.getApprovedResourceDetail(301L)).thenReturn(resourceDetail);

            mockMvc.perform(get("/api/viewer/resources/301"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(301L))
                    .andExpect(jsonPath("$.contributorId").value(77L))
                    .andExpect(jsonPath("$.title").value("Festival Archive"))
                    .andExpect(jsonPath("$.description").value("A local heritage collection."))
                    .andExpect(jsonPath("$.categoryId").value(8L))
                    .andExpect(jsonPath("$.categoryName").value("educational materials"))
                    .andExpect(jsonPath("$.place").value("Liverpool"))
                    .andExpect(jsonPath("$.previewImage").value("/images/festival.png"))
                    .andExpect(jsonPath("$.mediaUrl").value("/media/festival.mp4"))
                    .andExpect(jsonPath("$.status").value("approved"))
                    .andExpect(jsonPath("$.resourceType").value("video"))
                    .andExpect(jsonPath("$.tagIds[0]").value(11L))
                    .andExpect(jsonPath("$.tagNames[0]").value("Museum"));

            assertInvokedOnceWithArgs(viewerResourceService, "getApprovedResourceDetail", 301L);
        }

        @Test
        @DisplayName("Should return 400 when resource id path variable is invalid")
        void shouldReturnBadRequestWhenResourceIdPathVariableIsInvalid() throws Exception {
            mockMvc.perform(get("/api/viewer/resources/not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/not-a-number"));
        }

        @Test
        @DisplayName("Should return 404 when approved resource detail does not exist")
        void shouldReturnNotFoundWhenApprovedResourceDetailDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(viewerResourceService.getApprovedResourceDetail(999L))
                    .thenThrow(AppException.notFound("Approved resource does not exist."));

            mockMvc.perform(get("/api/viewer/resources/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Approved resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/999"));
        }
    }

    @Nested
    @DisplayName("GET /api/viewer/resources/category-options")
    class ListCategoryOptionsTests {

        @Test
        @DisplayName("Should return category options for an authenticated user")
        void shouldReturnCategoryOptionsForAuthenticatedUser() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(viewerResourceService.listCategoryOptions()).thenReturn(List.of(categoryOption));

            mockMvc.perform(get("/api/viewer/resources/category-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(8L))
                    .andExpect(jsonPath("$[0].name").value("Culture"));

            assertInvokedOnceWithArgs(viewerResourceService, "listCategoryOptions");
        }

        @Test
        @DisplayName("Should return an empty list when no category options exist")
        void shouldReturnEmptyListWhenNoCategoryOptionsExist() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(viewerResourceService.listCategoryOptions()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/viewer/resources/category-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertInvokedOnceWithArgs(viewerResourceService, "listCategoryOptions");
        }
    }

    @Nested
    @DisplayName("GET /api/viewer/resources/resource-type-options")
    class ListResourceTypeOptionsTests {

        @Test
        @DisplayName("Should return resource type options for an authenticated user")
        void shouldReturnResourceTypeOptionsForAuthenticatedUser() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(viewerResourceService.listResourceTypeOptions()).thenReturn(List.of(resourceTypeOption));

            mockMvc.perform(get("/api/viewer/resources/resource-type-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(5L))
                    .andExpect(jsonPath("$[0].name").value("video"));

            assertInvokedOnceWithArgs(viewerResourceService, "listResourceTypeOptions");
        }

        @Test
        @DisplayName("Should return 500 when resource type options service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenResourceTypeOptionsServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireAuthenticatedUserId(any())).thenReturn(5L);
            when(viewerResourceService.listResourceTypeOptions())
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(get("/api/viewer/resources/resource-type-options"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/resource-type-options"));
        }
    }

    private ResourceListItemVO resourceListItem(Long id,
                                                String title,
                                                String description,
                                                String resourceType,
                                                Long categoryId,
                                                String categoryName) {
        ResourceListItemVO itemVO = new ResourceListItemVO();
        itemVO.setId(id);
        itemVO.setTitle(title);
        itemVO.setDescription(description);
        itemVO.setPreviewImage("/images/festival.png");
        itemVO.setStatus("approved");
        itemVO.setResourceType(resourceType);
        itemVO.setCategoryId(categoryId);
        itemVO.setCategoryName(categoryName);
        itemVO.setUpdatedAt(FIXED_TIME);
        itemVO.setCurrentVersionNo(2);
        itemVO.setLastSubmittedAt(FIXED_TIME);
        itemVO.setHasReviewFeedback(false);
        return itemVO;
    }

    private ResourceDetailVO resourceDetail() {
        ResourceDetailVO detailVO = new ResourceDetailVO();
        detailVO.setId(301L);
        detailVO.setContributorId(77L);
        detailVO.setTitle("Festival Archive");
        detailVO.setDescription("A local heritage collection.");
        detailVO.setCopyright("Contributor-owned");
        detailVO.setCategoryId(8L);
        detailVO.setCategoryName("educational materials");
        detailVO.setPlace("Liverpool");
        detailVO.setPreviewImage("/images/festival.png");
        detailVO.setMediaUrl("/media/festival.mp4");
        detailVO.setStatus("approved");
        detailVO.setReviewedAt(FIXED_TIME);
        detailVO.setCreatedAt(FIXED_TIME.minusDays(5));
        detailVO.setUpdatedAt(FIXED_TIME.minusDays(1));
        detailVO.setResourceType("video");
        detailVO.setTagIds(List.of(11L, 12L));
        detailVO.setTagNames(List.of("Museum", "Community"));
        return detailVO;
    }

    private CategoryTagOptionVO option(Long id, String name) {
        CategoryTagOptionVO optionVO = new CategoryTagOptionVO();
        optionVO.setId(id);
        optionVO.setName(name);
        return optionVO;
    }
}
