package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.ContributorRequest;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceSubmission;
import com.cpt202.HerLink.entity.ResourceVersion;
import com.cpt202.HerLink.entity.ReviewRecord;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.mapper.AppUserMapper;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ContributorRequestMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceSubmissionMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.mapper.ResourceVersionMapper;
import com.cpt202.HerLink.mapper.ReviewRecordMapper;
import com.cpt202.HerLink.mapper.TagMapper;
import com.cpt202.HerLink.util.PasswordHashService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("ContributorResourceHistoryController integration test")
class ContributorResourceHistoryControllerIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("herlink_history_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("sql/contributor-resource-history-integration-schema.sql");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("HerLink.demo-data-enabled", () -> "false");
        registry.add("HerLink.upload-dir", () -> "target/test-uploads-history");
        registry.add("spring.mail.host", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private ContributorRequestMapper contributorRequestMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ResourceTypeMapper resourceTypeMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ResourceMapper resourceMapper;

    @Autowired
    private ResourceSubmissionMapper resourceSubmissionMapper;

    @Autowired
    private ResourceVersionMapper resourceVersionMapper;

    @Autowired
    private ResourceTagMapper resourceTagMapper;

    @Autowired
    private ReviewRecordMapper reviewRecordMapper;

    @Autowired
    private PasswordHashService passwordHashService;

    private Long contributorUserId;
    private Long viewerUserId;
    private Long reviewerUserId;
    private Long ownerResourceId;
    private Long foreignResourceId;
    private Long activeCategoryId;
    private Long inactiveCategoryId;
    private Long videoTypeId;
    private Long photoTypeId;
    private Long templeTagId;
    private Long festivalTagId;
    private Long inactiveTagId;

    @BeforeEach
    void setUp() throws Exception {
        cleanupDatabase();
        cleanupUploadDirectory();
        seedReferenceData();
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanupDatabase();
        cleanupUploadDirectory();
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/{resourceId}/submissions")
    class ListSubmissionHistoryTests {

        @Test
        @DisplayName("Should return submission history for the owner contributor")
        void shouldReturnSubmissionHistoryForOwnerContributor() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/submissions", ownerResourceId)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].resourceId").value(ownerResourceId))
                    .andExpect(jsonPath("$[0].versionNo").value(2))
                    .andExpect(jsonPath("$[0].submittedBy").value(contributorUserId))
                    .andExpect(jsonPath("$[0].submissionNote").value("Updated for second review"))
                    .andExpect(jsonPath("$[0].statusSnapshot").value(ResourceStatusEnum.PENDING_REVIEW.getValue()))
                    .andExpect(jsonPath("$[1].versionNo").value(1))
                    .andExpect(jsonPath("$[1].submissionNote").value("Initial review request"));

            List<ResourceSubmission> submissions = resourceSubmissionMapper.selectByResourceId(ownerResourceId);
            assertEquals(2, submissions.size());
            assertEquals(2, submissions.get(0).getVersionNo());
            assertEquals(1, submissions.get(1).getVersionNo());
        }

        @Test
        @DisplayName("Should return an empty submission history when the resource has never been submitted")
        void shouldReturnEmptySubmissionHistoryWhenResourceHasNeverBeenSubmitted() throws Exception {
            Long draftOnlyResourceId = createDraftResourceWithSingleVersion("Draft Only Resource");
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/submissions", draftOnlyResourceId)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertTrue(resourceSubmissionMapper.selectByResourceId(draftOnlyResourceId).isEmpty());
        }

        @Test
        @DisplayName("Should return 401 when submission history is requested without login")
        void shouldReturnUnauthorizedWhenSubmissionHistoryRequestedWithoutLogin() throws Exception {
            mockMvc.perform(get("/api/contributor/resources/{resourceId}/submissions", ownerResourceId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when a non contributor user requests submission history")
        void shouldReturnForbiddenWhenViewerRequestsSubmissionHistory() throws Exception {
            MockHttpSession session = loginAsViewer();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/submissions", ownerResourceId)
                            .session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Contributor access requires an approved contributor request."));
        }

        @Test
        @DisplayName("Should return 404 when submission history is requested for a missing resource")
        void shouldReturnNotFoundWhenSubmissionHistoryRequestedForMissingResource() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/submissions", 999999L)
                            .session(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resource does not exist."));
        }

        @Test
        @DisplayName("Should return 403 when submission history is requested for another contributor resource")
        void shouldReturnForbiddenWhenSubmissionHistoryRequestedForForeignResource() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/submissions", foreignResourceId)
                            .session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Current user does not own this resource."));
        }

        @Test
        @DisplayName("Should return 400 when resource id path variable is malformed for submission history")
        void shouldReturnBadRequestWhenSubmissionHistoryResourceIdMalformed() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/not-a-number/submissions")
                            .session(session))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.path").value("/api/contributor/resources/not-a-number/submissions"));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/{resourceId}/versions")
    class ListVersionsTests {

        @Test
        @DisplayName("Should list all versions for the owner contributor")
        void shouldListAllVersionsForOwnerContributor() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions", ownerResourceId)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].versionNo").value(2))
                    .andExpect(jsonPath("$[0].changeType").value("submit"))
                    .andExpect(jsonPath("$[1].versionNo").value(1))
                    .andExpect(jsonPath("$[1].changeType").value("create"));

            List<ResourceVersion> versions = resourceVersionMapper.selectByResourceId(ownerResourceId);
            assertEquals(2, versions.size());
            assertEquals(2, versions.get(0).getVersionNo());
        }

        @Test
        @DisplayName("Should return an empty version list when a draft has no snapshot rows")
        void shouldReturnEmptyVersionListWhenNoSnapshotsExist() throws Exception {
            Long resourceId = createDraftWithoutVersions("No Snapshot Draft");
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions", resourceId)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertTrue(resourceVersionMapper.selectByResourceId(resourceId).isEmpty());
        }

        @Test
        @DisplayName("Should return 401 when version list is requested without login")
        void shouldReturnUnauthorizedWhenVersionListRequestedWithoutLogin() throws Exception {
            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions", ownerResourceId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when a viewer requests the version list")
        void shouldReturnForbiddenWhenViewerRequestsVersionList() throws Exception {
            MockHttpSession session = loginAsViewer();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions", ownerResourceId)
                            .session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Contributor access requires an approved contributor request."));
        }

        @Test
        @DisplayName("Should return 404 when version list is requested for a missing resource")
        void shouldReturnNotFoundWhenVersionListRequestedForMissingResource() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions", 999999L)
                            .session(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resource does not exist."));
        }

        @Test
        @DisplayName("Should return 403 when version list is requested for another contributor resource")
        void shouldReturnForbiddenWhenVersionListRequestedForForeignResource() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions", foreignResourceId)
                            .session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Current user does not own this resource."));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/{resourceId}/versions/{versionNo}")
    class GetVersionTests {

        @Test
        @DisplayName("Should return the requested version snapshot for the owner contributor")
        void shouldReturnRequestedVersionSnapshotForOwnerContributor() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/{versionNo}", ownerResourceId, 2)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceId").value(ownerResourceId))
                    .andExpect(jsonPath("$.versionNo").value(2))
                    .andExpect(jsonPath("$.changeType").value("submit"))
                    .andExpect(jsonPath("$.snapshotMap.title").value("Temple Story Revised"))
                    .andExpect(jsonPath("$.snapshotMap.resourceType").value("video"))
                    .andExpect(jsonPath("$.snapshotMap.tagNames[0]").value("Festival"));

            ResourceVersion version = resourceVersionMapper.selectByResourceIdAndVersionNo(ownerResourceId, 2);
            assertNotNull(version);
            assertEquals("submit", version.getChangeType());
        }

        @Test
        @DisplayName("Should return 400 when version number is malformed")
        void shouldReturnBadRequestWhenVersionNumberMalformed() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/not-a-number", ownerResourceId)
                            .session(session))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when version number is zero")
        void shouldReturnBadRequestWhenVersionNumberIsZero() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/{versionNo}", ownerResourceId, 0)
                            .session(session))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Version number is required."));
        }

        @Test
        @DisplayName("Should return 404 when the requested version does not exist")
        void shouldReturnNotFoundWhenRequestedVersionDoesNotExist() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/{versionNo}", ownerResourceId, 99)
                            .session(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Requested version does not exist."));
        }

        @Test
        @DisplayName("Should return 401 when version detail is requested without login")
        void shouldReturnUnauthorizedWhenVersionDetailRequestedWithoutLogin() throws Exception {
            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/{versionNo}", ownerResourceId, 1))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when a viewer requests version detail")
        void shouldReturnForbiddenWhenViewerRequestsVersionDetail() throws Exception {
            MockHttpSession session = loginAsViewer();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/{versionNo}", ownerResourceId, 1)
                            .session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Contributor access requires an approved contributor request."));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/{resourceId}/versions/compare")
    class CompareVersionsTests {

        @Test
        @DisplayName("Should compare two versions for the owner contributor")
        void shouldCompareTwoVersionsForOwnerContributor() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/compare", ownerResourceId)
                            .session(session)
                            .param("v1", "1")
                            .param("v2", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resourceId").value(ownerResourceId))
                    .andExpect(jsonPath("$.leftVersionNo").value(1))
                    .andExpect(jsonPath("$.rightVersionNo").value(2))
                    .andExpect(jsonPath("$.diffItems[0].fieldName").value("title"))
                    .andExpect(jsonPath("$.diffItems[0].leftValue").value("Temple Story"))
                    .andExpect(jsonPath("$.diffItems[0].rightValue").value("Temple Story Revised"))
                    .andExpect(jsonPath("$.diffItems[0].changed").value(true))
                    .andExpect(jsonPath("$.diffItems[5].fieldName").value("resourceType"))
                    .andExpect(jsonPath("$.diffItems[5].leftValue").value("photo"))
                    .andExpect(jsonPath("$.diffItems[5].rightValue").value("video"));

            assertEquals(2, resourceVersionMapper.selectByResourceId(ownerResourceId).size());
        }

        @Test
        @DisplayName("Should normalize blank comparison fields to dash")
        void shouldNormalizeBlankComparisonFieldsToDash() throws Exception {
            Long blankVersionResourceId = createResourceWithBlankVersionDiffs();
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/compare", blankVersionResourceId)
                            .session(session)
                            .param("v1", "1")
                            .param("v2", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.diffItems[4].fieldName").value("place"))
                    .andExpect(jsonPath("$.diffItems[4].leftValue").value("-"))
                    .andExpect(jsonPath("$.diffItems[4].rightValue").value("-"))
                    .andExpect(jsonPath("$.diffItems[8].fieldName").value("tagNames"))
                    .andExpect(jsonPath("$.diffItems[8].leftValue").value("-"))
                    .andExpect(jsonPath("$.diffItems[8].rightValue").value("-"));
        }

        @Test
        @DisplayName("Should return 400 when a required compare parameter is missing")
        void shouldReturnBadRequestWhenCompareParameterMissing() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/compare", ownerResourceId)
                            .session(session)
                            .param("v1", "1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when compare version number is invalid")
        void shouldReturnBadRequestWhenCompareVersionNumberInvalid() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/compare", ownerResourceId)
                            .session(session)
                            .param("v1", "0")
                            .param("v2", "2"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Version number is required."));
        }

        @Test
        @DisplayName("Should return 404 when compared version does not exist")
        void shouldReturnNotFoundWhenComparedVersionDoesNotExist() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/compare", ownerResourceId)
                            .session(session)
                            .param("v1", "1")
                            .param("v2", "88"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Requested version does not exist."));
        }

        @Test
        @DisplayName("Should return 401 when compare is requested without login")
        void shouldReturnUnauthorizedWhenCompareRequestedWithoutLogin() throws Exception {
            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/compare", ownerResourceId)
                            .param("v1", "1")
                            .param("v2", "2"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when viewer requests compare")
        void shouldReturnForbiddenWhenViewerRequestsCompare() throws Exception {
            MockHttpSession session = loginAsViewer();

            mockMvc.perform(get("/api/contributor/resources/{resourceId}/versions/compare", ownerResourceId)
                            .session(session)
                            .param("v1", "1")
                            .param("v2", "2"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Contributor access requires an approved contributor request."));
        }
    }

    @Nested
    @DisplayName("POST /api/contributor/resources/{resourceId}/versions/{versionNo}/rollback")
    class RollbackTests {

        @Test
        @DisplayName("Should rollback a rejected resource to a previous version and persist the restored state")
        void shouldRollbackRejectedResourceToPreviousVersionAndPersistState() throws Exception {
            MockHttpSession session = loginAsContributor();
            int versionCountBefore = resourceVersionMapper.selectByResourceId(ownerResourceId).size();
            int tagCountBefore = resourceTagMapper.selectTagIdsByResourceId(ownerResourceId).size();

            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", ownerResourceId, 1)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "confirmed": true
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ownerResourceId))
                    .andExpect(jsonPath("$.title").value("Temple Story"))
                    .andExpect(jsonPath("$.resourceType").value("photo"))
                    .andExpect(jsonPath("$.status").value(ResourceStatusEnum.REJECTED.getValue()))
                    .andExpect(jsonPath("$.tagNames[0]").value("Temple"))
                    .andExpect(jsonPath("$.currentVersionNo").value(3));

            Resource rolledBack = resourceMapper.selectById(ownerResourceId);
            assertNotNull(rolledBack);
            assertEquals("Temple Story", rolledBack.getTitle());
            assertEquals(photoTypeId, rolledBack.getResourceTypeId());
            assertEquals(activeCategoryId, rolledBack.getCategoryId());

            List<Long> tagIds = resourceTagMapper.selectTagIdsByResourceId(ownerResourceId);
            assertEquals(1, tagIds.size());
            assertEquals(templeTagId, tagIds.get(0));
            assertEquals(versionCountBefore + 1, resourceVersionMapper.selectByResourceId(ownerResourceId).size());
            assertTrue(resourceTagMapper.selectTagIdsByResourceId(ownerResourceId).size() < tagCountBefore + 2);
        }

        @Test
        @DisplayName("Should ignore missing rollback request body and still rollback")
        void shouldIgnoreMissingRollbackRequestBodyAndStillRollback() throws Exception {
            MockHttpSession session = loginAsContributor();

            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", ownerResourceId, 1)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Temple Story"))
                    .andExpect(jsonPath("$.currentVersionNo").value(3));
        }

        @Test
        @DisplayName("Should return 400 when rollback body JSON is malformed")
        void shouldReturnBadRequestWhenRollbackBodyMalformed() throws Exception {
            MockHttpSession session = loginAsContributor();
            int versionCountBefore = resourceVersionMapper.selectByResourceId(ownerResourceId).size();

            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", ownerResourceId, 1)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest());

            assertEquals(versionCountBefore, resourceVersionMapper.selectByResourceId(ownerResourceId).size());
        }

        @Test
        @DisplayName("Should return 401 when rollback is requested without login")
        void shouldReturnUnauthorizedWhenRollbackRequestedWithoutLogin() throws Exception {
            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", ownerResourceId, 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when a viewer requests rollback")
        void shouldReturnForbiddenWhenViewerRequestsRollback() throws Exception {
            MockHttpSession session = loginAsViewer();
            int versionCountBefore = resourceVersionMapper.selectByResourceId(ownerResourceId).size();

            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", ownerResourceId, 1)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Contributor access requires an approved contributor request."));

            assertEquals(versionCountBefore, resourceVersionMapper.selectByResourceId(ownerResourceId).size());
        }

        @Test
        @DisplayName("Should return 404 when rollback target version does not exist")
        void shouldReturnNotFoundWhenRollbackTargetVersionMissing() throws Exception {
            MockHttpSession session = loginAsContributor();
            int versionCountBefore = resourceVersionMapper.selectByResourceId(ownerResourceId).size();

            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", ownerResourceId, 99)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Requested version does not exist."));

            assertEquals(versionCountBefore, resourceVersionMapper.selectByResourceId(ownerResourceId).size());
        }

        @Test
        @DisplayName("Should return 409 when current resource status does not allow rollback")
        void shouldReturnConflictWhenCurrentStatusDoesNotAllowRollback() throws Exception {
            Long pendingResourceId = createPendingResourceWithVersions();
            MockHttpSession session = loginAsContributor();
            int versionCountBefore = resourceVersionMapper.selectByResourceId(pendingResourceId).size();

            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", pendingResourceId, 1)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Current resource status does not allow rollback."));

            Resource pending = resourceMapper.selectById(pendingResourceId);
            assertEquals(ResourceStatusEnum.PENDING_REVIEW.getValue(), pending.getStatus());
            assertEquals(versionCountBefore, resourceVersionMapper.selectByResourceId(pendingResourceId).size());
        }

        @Test
        @DisplayName("Should return 409 when stored category is inactive and keep database unchanged")
        void shouldReturnConflictWhenStoredCategoryInactiveAndKeepDatabaseUnchanged() throws Exception {
            Long resourceId = createRollbackConflictResourceWithInactiveCategory();
            MockHttpSession session = loginAsContributor();
            Resource before = resourceMapper.selectById(resourceId);
            int versionCountBefore = resourceVersionMapper.selectByResourceId(resourceId).size();

            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", resourceId, 1)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Stored category is inactive and cannot be restored."));

            Resource after = resourceMapper.selectById(resourceId);
            assertNotNull(after);
            assertEquals(before.getTitle(), after.getTitle());
            assertEquals(before.getCategoryId(), after.getCategoryId());
            assertEquals(versionCountBefore, resourceVersionMapper.selectByResourceId(resourceId).size());
        }

        @Test
        @DisplayName("Should return 409 when stored resource type is unavailable and keep database unchanged")
        void shouldReturnConflictWhenStoredResourceTypeUnavailableAndKeepDatabaseUnchanged() throws Exception {
            Long resourceId = createRollbackConflictResourceWithUnknownType();
            MockHttpSession session = loginAsContributor();
            Resource before = resourceMapper.selectById(resourceId);
            int versionCountBefore = resourceVersionMapper.selectByResourceId(resourceId).size();

            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", resourceId, 1)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Stored resource type is unavailable and cannot be restored."));

            Resource after = resourceMapper.selectById(resourceId);
            assertNotNull(after);
            assertEquals(before.getTitle(), after.getTitle());
            assertEquals(before.getResourceTypeId(), after.getResourceTypeId());
            assertEquals(versionCountBefore, resourceVersionMapper.selectByResourceId(resourceId).size());
        }

        @Test
        @DisplayName("Should return 409 when stored tag is inactive and roll back the whole transaction")
        void shouldReturnConflictWhenStoredTagInactiveAndRollbackWholeTransaction() throws Exception {
            Long resourceId = createRollbackConflictResourceWithInactiveTag();
            MockHttpSession session = loginAsContributor();
            Resource before = resourceMapper.selectById(resourceId);
            List<Long> tagsBefore = resourceTagMapper.selectTagIdsByResourceId(resourceId);
            int versionCountBefore = resourceVersionMapper.selectByResourceId(resourceId).size();

            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", resourceId, 1)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Tag \"Inactive Legacy Tag\" exists but is inactive."));

            Resource after = resourceMapper.selectById(resourceId);
            assertNotNull(after);
            assertEquals(before.getTitle(), after.getTitle());
            assertEquals(before.getResourceTypeId(), after.getResourceTypeId());
            assertEquals(before.getCategoryId(), after.getCategoryId());
            assertEquals(tagsBefore, resourceTagMapper.selectTagIdsByResourceId(resourceId));
            assertEquals(versionCountBefore, resourceVersionMapper.selectByResourceId(resourceId).size());
        }

        @Test
        @DisplayName("Should return 403 when rollback is requested for another contributor resource")
        void shouldReturnForbiddenWhenRollbackRequestedForForeignResource() throws Exception {
            MockHttpSession session = loginAsContributor();
            int versionCountBefore = resourceVersionMapper.selectByResourceId(foreignResourceId).size();

            mockMvc.perform(post("/api/contributor/resources/{resourceId}/versions/{versionNo}/rollback", foreignResourceId, 1)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Current user does not own this resource."));

            assertEquals(versionCountBefore, resourceVersionMapper.selectByResourceId(foreignResourceId).size());
        }
    }

    private MockHttpSession loginAsContributor() throws Exception {
        return login("contributor@test.local", "Contributor123!");
    }

    private MockHttpSession loginAsViewer() throws Exception {
        return login("viewer@test.local", "Viewer123!");
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void seedReferenceData() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO category (categoryTopic, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "stories", "ACTIVE", 0, now, now
        );
        jdbcTemplate.update(
                "INSERT INTO category (categoryTopic, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "archived-stories", "INACTIVE", 0, now, now
        );
        jdbcTemplate.update(
                "INSERT INTO resourceType (typeName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "photo", "ACTIVE", 0, now, now
        );
        jdbcTemplate.update(
                "INSERT INTO resourceType (typeName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "video", "ACTIVE", 0, now, now
        );
        jdbcTemplate.update(
                "INSERT INTO tag (tagName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "Temple", "ACTIVE", 0, now, now
        );
        jdbcTemplate.update(
                "INSERT INTO tag (tagName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "Festival", "ACTIVE", 0, now, now
        );
        jdbcTemplate.update(
                "INSERT INTO tag (tagName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "Inactive Legacy Tag", "INACTIVE", 0, now, now
        );

        activeCategoryId = categoryMapper.selectByTopic("stories").getCategoryId();
        inactiveCategoryId = categoryMapper.selectByTopic("archived-stories").getCategoryId();
        photoTypeId = resourceTypeMapper.selectActiveByTypeName("photo").getResourceTypeId();
        videoTypeId = resourceTypeMapper.selectActiveByTypeName("video").getResourceTypeId();
        templeTagId = tagMapper.selectByName("Temple").getTagId();
        festivalTagId = tagMapper.selectByName("Festival").getTagId();
        inactiveTagId = tagMapper.selectByName("Inactive Legacy Tag").getTagId();

        AppUser contributor = createUser("Contributor User", "contributor@test.local", "Contributor123!", true, UserRoleEnum.REGISTERED_VIEWER.getValue());
        AppUser viewer = createUser("Viewer User", "viewer@test.local", "Viewer123!", false, UserRoleEnum.REGISTERED_VIEWER.getValue());
        AppUser reviewer = createUser("Reviewer User", "reviewer@test.local", "Reviewer123!", false, UserRoleEnum.ADMINISTRATOR.getValue());
        AppUser otherContributor = createUser("Other Contributor", "other-contributor@test.local", "OtherContributor123!", true, UserRoleEnum.REGISTERED_VIEWER.getValue());

        contributorUserId = contributor.getUserId();
        viewerUserId = viewer.getUserId();
        reviewerUserId = reviewer.getUserId();

        createApprovedContributorRequest(contributorUserId);
        createApprovedContributorRequest(otherContributor.getUserId());

        ownerResourceId = createRejectedHistoryResource(contributorUserId);
        foreignResourceId = createRejectedHistoryResource(otherContributor.getUserId());
    }

    private AppUser createUser(String name, String email, String password, boolean contributor, String role) {
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordHashService.hash(password));
        user.setRole(role);
        user.setContributor(contributor);
        user.setBio(name + " bio");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        appUserMapper.insert(user);
        return user;
    }

    private void createApprovedContributorRequest(Long userId) {
        ContributorRequest request = new ContributorRequest();
        request.setUserId(userId);
        request.setApplicationReason("Approved for contributor access");
        request.setStatus("APPROVED");
        request.setRequestedAt(LocalDateTime.now().minusDays(2));
        request.setReviewedBy(reviewerUserId);
        request.setReviewedAt(LocalDateTime.now().minusDays(1));
        request.setReviewComment("Approved");
        request.setUpdatedAt(LocalDateTime.now().minusDays(1));
        contributorRequestMapper.insert(request);
    }

    private Long createRejectedHistoryResource(Long resourceOwnerId) {
        Resource resource = new Resource();
        resource.setContributorId(resourceOwnerId);
        resource.setTitle("Temple Story Revised");
        resource.setDescription("Revised temple description");
        resource.setCopyright("Community copyright");
        resource.setCategoryId(activeCategoryId);
        resource.setResourceTypeId(videoTypeId);
        resource.setResourceType("video");
        resource.setPlace("Suzhou");
        resource.setPreviewImage("resource-history/revised-cover.jpg");
        resource.setMediaUrl("resource-history/revised-video.mp4");
        resource.setStatus(ResourceStatusEnum.REJECTED.getValue());
        resource.setCreatedAt(LocalDateTime.now().minusDays(5));
        resource.setUpdatedAt(LocalDateTime.now().minusHours(8));
        resourceMapper.insert(resource);

        jdbcTemplate.update(
                "INSERT INTO resourceTag(resourceId, tagId) VALUES (?, ?)",
                resource.getId(), festivalTagId
        );
        jdbcTemplate.update(
                "INSERT INTO resourceTag(resourceId, tagId) VALUES (?, ?)",
                resource.getId(), templeTagId
        );

        insertVersion(
                resource.getId(),
                1,
                """
                        {"title":"Temple Story","description":"Original temple description","copyright":"Community copyright","categoryId":%d,"categoryName":"stories","place":"Suzhou","resourceType":"photo","previewImage":"resource-history/original-cover.jpg","mediaUrl":"resource-history/original-photo.jpg","tagNames":["Temple"]}
                        """.formatted(activeCategoryId),
                "create",
                "Draft created",
                resourceOwnerId
        );
        insertVersion(
                resource.getId(),
                2,
                """
                        {"title":"Temple Story Revised","description":"Revised temple description","copyright":"Community copyright","categoryId":%d,"categoryName":"stories","place":"Suzhou","resourceType":"video","previewImage":"resource-history/revised-cover.jpg","mediaUrl":"resource-history/revised-video.mp4","tagNames":["Festival","Temple"]}
                        """.formatted(activeCategoryId),
                "submit",
                "Submitted for review",
                resourceOwnerId
        );

        insertSubmission(resource.getId(), 1, resourceOwnerId, "Initial review request", ResourceStatusEnum.PENDING_REVIEW.getValue());
        Long secondSubmissionId = insertSubmission(resource.getId(), 2, resourceOwnerId, "Updated for second review", ResourceStatusEnum.PENDING_REVIEW.getValue());
        insertReviewRecord(resource.getId(), secondSubmissionId, 2, "Rejected", "Please revert to the earlier story structure.");
        return resource.getId();
    }

    private Long createDraftResourceWithSingleVersion(String title) {
        Resource resource = new Resource();
        resource.setContributorId(contributorUserId);
        resource.setTitle(title);
        resource.setDescription("Draft only");
        resource.setCopyright("Draft copyright");
        resource.setCategoryId(activeCategoryId);
        resource.setResourceTypeId(photoTypeId);
        resource.setResourceType("photo");
        resource.setPreviewImage("draft/cover.jpg");
        resource.setMediaUrl("draft/photo.jpg");
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCreatedAt(LocalDateTime.now().minusDays(1));
        resource.setUpdatedAt(LocalDateTime.now().minusHours(3));
        resourceMapper.insert(resource);
        insertVersion(
                resource.getId(),
                1,
                """
                        {"title":"%s","description":"Draft only","copyright":"Draft copyright","categoryId":%d,"categoryName":"stories","place":"","resourceType":"photo","previewImage":"draft/cover.jpg","mediaUrl":"draft/photo.jpg","tagNames":[]}
                        """.formatted(title, activeCategoryId),
                "create",
                "Draft created",
                contributorUserId
        );
        return resource.getId();
    }

    private Long createDraftWithoutVersions(String title) {
        Resource resource = new Resource();
        resource.setContributorId(contributorUserId);
        resource.setTitle(title);
        resource.setDescription("No snapshots");
        resource.setCopyright("Draft copyright");
        resource.setCategoryId(activeCategoryId);
        resource.setResourceTypeId(photoTypeId);
        resource.setResourceType("photo");
        resource.setPreviewImage("no-version/cover.jpg");
        resource.setMediaUrl("no-version/photo.jpg");
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCreatedAt(LocalDateTime.now().minusDays(1));
        resource.setUpdatedAt(LocalDateTime.now().minusHours(2));
        resourceMapper.insert(resource);
        return resource.getId();
    }

    private Long createResourceWithBlankVersionDiffs() {
        Resource resource = new Resource();
        resource.setContributorId(contributorUserId);
        resource.setTitle("Blank Compare");
        resource.setDescription("Blank compare description");
        resource.setCopyright("Copyright");
        resource.setCategoryId(activeCategoryId);
        resource.setResourceTypeId(photoTypeId);
        resource.setResourceType("photo");
        resource.setPreviewImage(null);
        resource.setMediaUrl(null);
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCreatedAt(LocalDateTime.now().minusDays(1));
        resource.setUpdatedAt(LocalDateTime.now().minusHours(1));
        resourceMapper.insert(resource);
        insertVersion(
                resource.getId(),
                1,
                """
                        {"title":"Blank Compare","description":"Before","copyright":"Copyright","categoryId":%d,"categoryName":"stories","place":"","resourceType":"photo","previewImage":"","mediaUrl":"","tagNames":[]}
                        """.formatted(activeCategoryId),
                "create",
                "Draft created",
                contributorUserId
        );
        insertVersion(
                resource.getId(),
                2,
                """
                        {"title":"Blank Compare","description":"After","copyright":"Copyright","categoryId":%d,"categoryName":"stories","place":"   ","resourceType":"photo","previewImage":null,"mediaUrl":null,"tagNames":[]}
                        """.formatted(activeCategoryId),
                "edit",
                "Edited blank fields",
                contributorUserId
        );
        return resource.getId();
    }

    private Long createPendingResourceWithVersions() {
        Resource resource = new Resource();
        resource.setContributorId(contributorUserId);
        resource.setTitle("Pending Resource");
        resource.setDescription("Pending description");
        resource.setCopyright("Pending copyright");
        resource.setCategoryId(activeCategoryId);
        resource.setResourceTypeId(photoTypeId);
        resource.setResourceType("photo");
        resource.setPreviewImage("pending/cover.jpg");
        resource.setMediaUrl("pending/photo.jpg");
        resource.setStatus(ResourceStatusEnum.PENDING_REVIEW.getValue());
        resource.setCreatedAt(LocalDateTime.now().minusDays(2));
        resource.setUpdatedAt(LocalDateTime.now().minusHours(5));
        resourceMapper.insert(resource);
        insertVersion(
                resource.getId(),
                1,
                """
                        {"title":"Pending Resource","description":"Pending description","copyright":"Pending copyright","categoryId":%d,"categoryName":"stories","place":"Nanjing","resourceType":"photo","previewImage":"pending/cover.jpg","mediaUrl":"pending/photo.jpg","tagNames":["Temple"]}
                        """.formatted(activeCategoryId),
                "create",
                "Draft created",
                contributorUserId
        );
        jdbcTemplate.update("INSERT INTO resourceTag(resourceId, tagId) VALUES (?, ?)", resource.getId(), templeTagId);
        return resource.getId();
    }

    private Long createRollbackConflictResourceWithInactiveCategory() {
        Resource resource = new Resource();
        resource.setContributorId(contributorUserId);
        resource.setTitle("Current Stable Title");
        resource.setDescription("Current stable description");
        resource.setCopyright("Stable copyright");
        resource.setCategoryId(activeCategoryId);
        resource.setResourceTypeId(videoTypeId);
        resource.setResourceType("video");
        resource.setPreviewImage("conflict/category-cover.jpg");
        resource.setMediaUrl("conflict/category-video.mp4");
        resource.setStatus(ResourceStatusEnum.REJECTED.getValue());
        resource.setCreatedAt(LocalDateTime.now().minusDays(2));
        resource.setUpdatedAt(LocalDateTime.now().minusHours(6));
        resourceMapper.insert(resource);
        jdbcTemplate.update("INSERT INTO resourceTag(resourceId, tagId) VALUES (?, ?)", resource.getId(), festivalTagId);
        insertVersion(
                resource.getId(),
                1,
                """
                        {"title":"Inactive Category Title","description":"Old description","copyright":"Stable copyright","categoryId":%d,"categoryName":"archived-stories","place":"Old Place","resourceType":"photo","previewImage":"conflict/old-cover.jpg","mediaUrl":"conflict/old-photo.jpg","tagNames":["Temple"]}
                        """.formatted(inactiveCategoryId),
                "create",
                "Draft created",
                contributorUserId
        );
        insertVersion(
                resource.getId(),
                2,
                """
                        {"title":"Current Stable Title","description":"Current stable description","copyright":"Stable copyright","categoryId":%d,"categoryName":"stories","place":"New Place","resourceType":"video","previewImage":"conflict/category-cover.jpg","mediaUrl":"conflict/category-video.mp4","tagNames":["Festival"]}
                        """.formatted(activeCategoryId),
                "submit",
                "Submitted for review",
                contributorUserId
        );
        return resource.getId();
    }

    private Long createRollbackConflictResourceWithUnknownType() {
        Resource resource = new Resource();
        resource.setContributorId(contributorUserId);
        resource.setTitle("Current Type Stable");
        resource.setDescription("Current type description");
        resource.setCopyright("Stable copyright");
        resource.setCategoryId(activeCategoryId);
        resource.setResourceTypeId(videoTypeId);
        resource.setResourceType("video");
        resource.setPreviewImage("conflict/type-cover.jpg");
        resource.setMediaUrl("conflict/type-video.mp4");
        resource.setStatus(ResourceStatusEnum.REJECTED.getValue());
        resource.setCreatedAt(LocalDateTime.now().minusDays(2));
        resource.setUpdatedAt(LocalDateTime.now().minusHours(6));
        resourceMapper.insert(resource);
        jdbcTemplate.update("INSERT INTO resourceTag(resourceId, tagId) VALUES (?, ?)", resource.getId(), festivalTagId);
        insertVersion(
                resource.getId(),
                1,
                """
                        {"title":"Unavailable Type Title","description":"Old type description","copyright":"Stable copyright","categoryId":%d,"categoryName":"stories","place":"Old Place","resourceType":"hologram","previewImage":"conflict/type-old-cover.jpg","mediaUrl":"conflict/type-old.bin","tagNames":["Temple"]}
                        """.formatted(activeCategoryId),
                "create",
                "Draft created",
                contributorUserId
        );
        insertVersion(
                resource.getId(),
                2,
                """
                        {"title":"Current Type Stable","description":"Current type description","copyright":"Stable copyright","categoryId":%d,"categoryName":"stories","place":"New Place","resourceType":"video","previewImage":"conflict/type-cover.jpg","mediaUrl":"conflict/type-video.mp4","tagNames":["Festival"]}
                        """.formatted(activeCategoryId),
                "submit",
                "Submitted for review",
                contributorUserId
        );
        return resource.getId();
    }

    private Long createRollbackConflictResourceWithInactiveTag() {
        Resource resource = new Resource();
        resource.setContributorId(contributorUserId);
        resource.setTitle("Current Tag Stable");
        resource.setDescription("Current tag description");
        resource.setCopyright("Stable copyright");
        resource.setCategoryId(activeCategoryId);
        resource.setResourceTypeId(videoTypeId);
        resource.setResourceType("video");
        resource.setPreviewImage("conflict/tag-cover.jpg");
        resource.setMediaUrl("conflict/tag-video.mp4");
        resource.setStatus(ResourceStatusEnum.REJECTED.getValue());
        resource.setCreatedAt(LocalDateTime.now().minusDays(2));
        resource.setUpdatedAt(LocalDateTime.now().minusHours(6));
        resourceMapper.insert(resource);
        jdbcTemplate.update("INSERT INTO resourceTag(resourceId, tagId) VALUES (?, ?)", resource.getId(), festivalTagId);
        insertVersion(
                resource.getId(),
                1,
                """
                        {"title":"Inactive Tag Title","description":"Old tag description","copyright":"Stable copyright","categoryId":%d,"categoryName":"stories","place":"Old Place","resourceType":"photo","previewImage":"conflict/tag-old-cover.jpg","mediaUrl":"conflict/tag-old.jpg","tagNames":["Inactive Legacy Tag"]}
                        """.formatted(activeCategoryId),
                "create",
                "Draft created",
                contributorUserId
        );
        insertVersion(
                resource.getId(),
                2,
                """
                        {"title":"Current Tag Stable","description":"Current tag description","copyright":"Stable copyright","categoryId":%d,"categoryName":"stories","place":"New Place","resourceType":"video","previewImage":"conflict/tag-cover.jpg","mediaUrl":"conflict/tag-video.mp4","tagNames":["Festival"]}
                        """.formatted(activeCategoryId),
                "submit",
                "Submitted for review",
                contributorUserId
        );
        return resource.getId();
    }

    private void insertVersion(Long resourceId,
                               int versionNo,
                               String snapshot,
                               String changeType,
                               String changeSummary,
                               Long createdBy) {
        ResourceVersion version = new ResourceVersion();
        version.setResourceId(resourceId);
        version.setVersionNo(versionNo);
        version.setSnapshot(snapshot.replace("\r", "").replace("\n", ""));
        version.setChangeType(changeType);
        version.setChangeSummary(changeSummary);
        version.setCreatedBy(createdBy);
        version.setCreatedAt(LocalDateTime.now().minusDays(2).plusMinutes(versionNo));
        resourceVersionMapper.insert(version);
    }

    private Long insertSubmission(Long resourceId,
                                  int versionNo,
                                  Long submittedBy,
                                  String note,
                                  String statusSnapshot) {
        ResourceSubmission submission = new ResourceSubmission();
        submission.setResourceId(resourceId);
        submission.setVersionNo(versionNo);
        submission.setSubmittedBy(submittedBy);
        submission.setSubmittedAt(LocalDateTime.now().minusDays(1).plusMinutes(versionNo));
        submission.setSubmissionNote(note);
        submission.setStatusSnapshot(statusSnapshot);
        submission.setCreatedAt(LocalDateTime.now().minusDays(1).plusMinutes(versionNo));
        resourceSubmissionMapper.insert(submission);
        return submission.getSubmissionId();
    }

    private void insertReviewRecord(Long resourceId,
                                    Long submissionId,
                                    int versionNo,
                                    String status,
                                    String feedbackComment) {
        ReviewRecord reviewRecord = new ReviewRecord();
        reviewRecord.setResourceId(resourceId);
        reviewRecord.setSubmissionId(submissionId);
        reviewRecord.setVersionNo(versionNo);
        reviewRecord.setReviewerId(reviewerUserId);
        reviewRecord.setActionDescription(status);
        reviewRecord.setStatus(status);
        reviewRecord.setFeedbackComment(feedbackComment);
        reviewRecord.setReviewedAt(LocalDateTime.now().minusHours(6));
        reviewRecord.setCreatedAt(LocalDateTime.now().minusHours(6));
        reviewRecordMapper.insert(reviewRecord);
    }

    private void cleanupDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.update("DELETE FROM reviewRecord");
        jdbcTemplate.update("DELETE FROM comment");
        jdbcTemplate.update("DELETE FROM attachedFile");
        jdbcTemplate.update("DELETE FROM feedback");
        jdbcTemplate.update("DELETE FROM resourceVersion");
        jdbcTemplate.update("DELETE FROM resourceFile");
        jdbcTemplate.update("DELETE FROM resourceTag");
        jdbcTemplate.update("DELETE FROM resourceSubmission");
        jdbcTemplate.update("DELETE FROM resourceArchive");
        jdbcTemplate.update("DELETE FROM resource");
        jdbcTemplate.update("DELETE FROM contributorApplicationArchive");
        jdbcTemplate.update("DELETE FROM contributorApplication");
        jdbcTemplate.update("DELETE FROM `user`");
        jdbcTemplate.update("DELETE FROM adminOperationHistory");
        jdbcTemplate.update("DELETE FROM tag");
        jdbcTemplate.update("DELETE FROM resourceType");
        jdbcTemplate.update("DELETE FROM category");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private void cleanupUploadDirectory() throws Exception {
        Path uploadRoot = Path.of("target", "test-uploads-history");
        if (!Files.exists(uploadRoot)) {
            return;
        }
        try (var paths = Files.walk(uploadRoot)) {
            paths.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }
}
