package com.cpt202.HerLink.integration.controller;

import com.cpt202.HerLink.controller.ContributorResourceHistoryController;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.ContributorResourceService;
import com.cpt202.HerLink.service.ResourceVersionService;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertInvokedOnceWithArgs;
import static com.cpt202.HerLink.testutil.MockInvocationAssertions.assertNoInteractions;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceSubmissionVO;
import com.cpt202.HerLink.vo.ResourceVersionCompareVO;
import com.cpt202.HerLink.vo.ResourceVersionDiffItemVO;
import com.cpt202.HerLink.vo.ResourceVersionVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContributorResourceHistoryController.class)
@DisplayName("ContributorResourceHistoryController Integration Test")
class ContributorResourceHistoryControllerTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 5, 5, 4, 45, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContributorResourceService contributorResourceService;

    @MockBean
    private ResourceVersionService resourceVersionService;

    @MockBean
    private ResourcePermissionChecker resourcePermissionChecker;

    private ResourceSubmissionVO submissionHistory;
    private ResourceVersionVO versionHistory;
    private ResourceVersionVO singleVersion;
    private ResourceVersionCompareVO compareResult;
    private ResourceDetailVO rollbackResult;

    @BeforeEach
    void setUp() {
        submissionHistory = submissionHistory(
                10L,
                30L,
                2,
                1L,
                "Please review this update",
                "PENDING_REVIEW"
        );
        versionHistory = version(
                12L,
                30L,
                4,
                "rollback",
                "Rolled back to version 2",
                1L
        );
        singleVersion = version(
                13L,
                30L,
                2,
                "submission",
                "Initial submission",
                1L
        );
        singleVersion.setSnapshotMap(Map.of("title", "Old Title"));
        compareResult = compareResult();
        rollbackResult = rollbackResult();
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/{resourceId}/submissions")
    class ListSubmissionHistoryTests {

        @Test
        @DisplayName("Should return submission history for the current contributor")
        void shouldReturnSubmissionHistoryForCurrentContributor() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(contributorResourceService.listSubmissionHistory(1L, 30L)).thenReturn(List.of(submissionHistory));

            mockMvc.perform(get("/api/contributor/resources/30/submissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].submissionId").value(10L))
                    .andExpect(jsonPath("$[0].resourceId").value(30L))
                    .andExpect(jsonPath("$[0].versionNo").value(2))
                    .andExpect(jsonPath("$[0].submittedBy").value(1L))
                    .andExpect(jsonPath("$[0].submissionNote").value("Please review this update"))
                    .andExpect(jsonPath("$[0].statusSnapshot").value("PENDING_REVIEW"));

            assertInvokedOnceWithArgs(contributorResourceService, "listSubmissionHistory", 1L, 30L);
        }

        @Test
        @DisplayName("Should return an empty list when there is no submission history")
        void shouldReturnEmptyListWhenThereIsNoSubmissionHistory() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(contributorResourceService.listSubmissionHistory(1L, 30L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/contributor/resources/30/submissions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertInvokedOnceWithArgs(contributorResourceService, "listSubmissionHistory", 1L, 30L);
        }

        @Test
        @DisplayName("Should return 401 when contributor authentication is missing")
        void shouldReturnUnauthorizedWhenContributorAuthenticationIsMissing() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any()))
                    .thenThrow(AppException.unauthorized("Please log in first."));

            mockMvc.perform(get("/api/contributor/resources/30/submissions"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/submissions"));

            assertNoInteractions(contributorResourceService, resourceVersionService);
        }

        @Test
        @DisplayName("Should return 404 when resource submission history does not exist")
        void shouldReturnNotFoundWhenResourceSubmissionHistoryDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(contributorResourceService.listSubmissionHistory(1L, 999L))
                    .thenThrow(AppException.notFound("Resource does not exist."));

            mockMvc.perform(get("/api/contributor/resources/999/submissions"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/999/submissions"));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/{resourceId}/versions")
    class ListVersionsTests {

        @Test
        @DisplayName("Should return version history for the current contributor")
        void shouldReturnVersionHistoryForCurrentContributor() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(resourceVersionService.listVersions(1L, 30L)).thenReturn(List.of(versionHistory));

            mockMvc.perform(get("/api/contributor/resources/30/versions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].versionId").value(12L))
                    .andExpect(jsonPath("$[0].resourceId").value(30L))
                    .andExpect(jsonPath("$[0].versionNo").value(4))
                    .andExpect(jsonPath("$[0].changeType").value("rollback"))
                    .andExpect(jsonPath("$[0].changeSummary").value("Rolled back to version 2"))
                    .andExpect(jsonPath("$[0].createdBy").value(1L));

            assertInvokedOnceWithArgs(resourceVersionService, "listVersions", 1L, 30L);
        }

        @Test
        @DisplayName("Should return an empty list when no version history exists")
        void shouldReturnEmptyListWhenNoVersionHistoryExists() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(resourceVersionService.listVersions(1L, 30L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/contributor/resources/30/versions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertInvokedOnceWithArgs(resourceVersionService, "listVersions", 1L, 30L);
        }

        @Test
        @DisplayName("Should return 403 when current user is not a contributor")
        void shouldReturnForbiddenWhenCurrentUserIsNotContributor() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any()))
                    .thenThrow(AppException.forbidden("Contributor permission is required."));

            mockMvc.perform(get("/api/contributor/resources/30/versions"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Contributor permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/versions"));

            assertNoInteractions(contributorResourceService, resourceVersionService);
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/{resourceId}/versions/{versionNo}")
    class GetVersionTests {

        @Test
        @DisplayName("Should return the requested version detail for the current contributor")
        void shouldReturnRequestedVersionDetailForCurrentContributor() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(resourceVersionService.getVersion(1L, 30L, 2)).thenReturn(singleVersion);

            mockMvc.perform(get("/api/contributor/resources/30/versions/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.versionId").value(13L))
                    .andExpect(jsonPath("$.resourceId").value(30L))
                    .andExpect(jsonPath("$.versionNo").value(2))
                    .andExpect(jsonPath("$.changeType").value("submission"))
                    .andExpect(jsonPath("$.changeSummary").value("Initial submission"))
                    .andExpect(jsonPath("$.snapshotMap.title").value("Old Title"));

            assertInvokedOnceWithArgs(resourceVersionService, "getVersion", 1L, 30L, 2);
        }

        @Test
        @DisplayName("Should return 400 when version number path variable is invalid")
        void shouldReturnBadRequestWhenVersionNumberPathVariableIsInvalid() throws Exception {
            mockMvc.perform(get("/api/contributor/resources/30/versions/not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/versions/not-a-number"));
        }

        @Test
        @DisplayName("Should return 404 when requested version does not exist")
        void shouldReturnNotFoundWhenRequestedVersionDoesNotExist() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(resourceVersionService.getVersion(1L, 30L, 99))
                    .thenThrow(AppException.notFound("Version does not exist."));

            mockMvc.perform(get("/api/contributor/resources/30/versions/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Version does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/versions/99"));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/{resourceId}/versions/compare")
    class CompareVersionsTests {

        @Test
        @DisplayName("Should return version differences when comparison request is valid")
        void shouldReturnVersionDifferencesWhenComparisonRequestIsValid() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(resourceVersionService.compareVersions(1L, 30L, 1, 4)).thenReturn(compareResult);

            mockMvc.perform(get("/api/contributor/resources/30/versions/compare")
                            .param("v1", "1")
                            .param("v2", "4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceId").value(30L))
                    .andExpect(jsonPath("$.leftVersionNo").value(1))
                    .andExpect(jsonPath("$.rightVersionNo").value(4))
                    .andExpect(jsonPath("$.diffItems[0].fieldName").value("title"))
                    .andExpect(jsonPath("$.diffItems[0].fieldLabel").value("Title"))
                    .andExpect(jsonPath("$.diffItems[0].leftValue").value("Old Title"))
                    .andExpect(jsonPath("$.diffItems[0].rightValue").value("New Title"))
                    .andExpect(jsonPath("$.diffItems[0].changed").value(true));

            assertInvokedOnceWithArgs(resourceVersionService, "compareVersions", 1L, 30L, 1, 4);
        }

        @Test
        @DisplayName("Should return 400 when required comparison query parameter is missing")
        void shouldReturnBadRequestWhenRequiredComparisonQueryParameterIsMissing() throws Exception {
            mockMvc.perform(get("/api/contributor/resources/30/versions/compare")
                            .param("v1", "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/versions/compare"));
        }

        @Test
        @DisplayName("Should return 400 when comparison service rejects the version selection")
        void shouldReturnBadRequestWhenComparisonServiceRejectsVersionSelection() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(resourceVersionService.compareVersions(1L, 30L, 4, 4))
                    .thenThrow(AppException.badRequest("Please choose two different versions to compare."));

            mockMvc.perform(get("/api/contributor/resources/30/versions/compare")
                            .param("v1", "4")
                            .param("v2", "4"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please choose two different versions to compare."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/versions/compare"));
        }
    }

    @Nested
    @DisplayName("POST /api/contributor/resources/{resourceId}/versions/{versionNo}/rollback")
    class RollbackToVersionTests {

        @Test
        @DisplayName("Should rollback to the requested version when request is valid")
        void shouldRollbackToRequestedVersionWhenRequestIsValid() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(resourceVersionService.rollbackToVersion(1L, 30L, 2)).thenReturn(rollbackResult);

            mockMvc.perform(post("/api/contributor/resources/30/versions/2/rollback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "confirmed": true
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(30L))
                    .andExpect(jsonPath("$.title").value("Restored Resource"))
                    .andExpect(jsonPath("$.status").value("Rejected"))
                    .andExpect(jsonPath("$.resourceType").value("Video"))
                    .andExpect(jsonPath("$.currentVersionNo").value(5))
                    .andExpect(jsonPath("$.tagNames[0]").value("Mental Health"));

            assertInvokedOnceWithArgs(resourceVersionService, "rollbackToVersion", 1L, 30L, 2);
        }

        @Test
        @DisplayName("Should ignore missing request body and still rollback based on path parameters")
        void shouldIgnoreMissingRequestBodyAndStillRollbackBasedOnPathParameters() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(resourceVersionService.rollbackToVersion(1L, 30L, 2)).thenReturn(rollbackResult);

            mockMvc.perform(post("/api/contributor/resources/30/versions/2/rollback")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(30L))
                    .andExpect(jsonPath("$.currentVersionNo").value(5));

            assertInvokedOnceWithArgs(resourceVersionService, "rollbackToVersion", 1L, 30L, 2);
        }

        @Test
        @DisplayName("Should return 400 when rollback request JSON is malformed")
        void shouldReturnBadRequestWhenRollbackRequestJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/contributor/resources/30/versions/2/rollback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/versions/2/rollback"));
        }

        @Test
        @DisplayName("Should return 409 when rollback target cannot be restored")
        void shouldReturnConflictWhenRollbackTargetCannotBeRestored() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(resourceVersionService.rollbackToVersion(1L, 30L, 2))
                    .thenThrow(AppException.conflict("Stored resource type is unavailable and cannot be restored."));

            mockMvc.perform(post("/api/contributor/resources/30/versions/2/rollback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "confirmed": true
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Stored resource type is unavailable and cannot be restored."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/versions/2/rollback"));
        }

        @Test
        @DisplayName("Should return 500 when rollback service throws an unexpected exception")
        void shouldReturnInternalServerErrorWhenRollbackServiceThrowsUnexpectedException() throws Exception {
            when(resourcePermissionChecker.requireContributorUserId(any())).thenReturn(1L);
            when(resourceVersionService.rollbackToVersion(1L, 30L, 2))
                    .thenThrow(new RuntimeException("Database unavailable"));

            mockMvc.perform(post("/api/contributor/resources/30/versions/2/rollback")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "confirmed": true
                                    }
                                    """))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.message").value("Internal server error."))
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/30/versions/2/rollback"));
        }
    }

    private ResourceSubmissionVO submissionHistory(Long submissionId,
                                                   Long resourceId,
                                                   Integer versionNo,
                                                   Long submittedBy,
                                                   String submissionNote,
                                                   String statusSnapshot) {
        ResourceSubmissionVO submissionVO = new ResourceSubmissionVO();
        submissionVO.setSubmissionId(submissionId);
        submissionVO.setResourceId(resourceId);
        submissionVO.setVersionNo(versionNo);
        submissionVO.setSubmittedBy(submittedBy);
        submissionVO.setSubmittedAt(FIXED_TIME);
        submissionVO.setSubmissionNote(submissionNote);
        submissionVO.setStatusSnapshot(statusSnapshot);
        submissionVO.setCreatedAt(FIXED_TIME);
        return submissionVO;
    }

    private ResourceVersionVO version(Long versionId,
                                      Long resourceId,
                                      Integer versionNo,
                                      String changeType,
                                      String changeSummary,
                                      Long createdBy) {
        ResourceVersionVO versionVO = new ResourceVersionVO();
        versionVO.setVersionId(versionId);
        versionVO.setResourceId(resourceId);
        versionVO.setVersionNo(versionNo);
        versionVO.setChangeType(changeType);
        versionVO.setChangeSummary(changeSummary);
        versionVO.setCreatedBy(createdBy);
        versionVO.setCreatedAt(FIXED_TIME);
        return versionVO;
    }

    private ResourceVersionCompareVO compareResult() {
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
        return compareVO;
    }

    private ResourceDetailVO rollbackResult() {
        ResourceDetailVO detailVO = new ResourceDetailVO();
        detailVO.setId(30L);
        detailVO.setContributorId(1L);
        detailVO.setTitle("Restored Resource");
        detailVO.setDescription("Restored description");
        detailVO.setStatus("Rejected");
        detailVO.setCategoryId(8L);
        detailVO.setCategoryName("Mental Health");
        detailVO.setResourceType("Video");
        detailVO.setTagIds(List.of(11L));
        detailVO.setTagNames(List.of("Mental Health"));
        detailVO.setCurrentVersionNo(5);
        detailVO.setLatestReviewStatus("REJECTED");
        detailVO.setLatestFeedbackComment("Please revise the summary.");
        detailVO.setLatestSubmittedAt(FIXED_TIME);
        detailVO.setUpdatedAt(FIXED_TIME);
        return detailVO;
    }
}
