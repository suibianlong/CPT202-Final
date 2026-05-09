package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceSubmission;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.Tag;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.mapper.AppUserMapper;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceFileMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceSubmissionMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.mapper.ResourceVersionMapper;
import com.cpt202.HerLink.mapper.TagMapper;
import com.cpt202.HerLink.util.PasswordHashService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link ContributorResourceController}.
 * These tests verify contributor resource workflows using MockMvc,
 * session-based authentication, and a Testcontainers MySQL database.
 * Covered scenarios include draft creation, metadata updates, file uploads,
 * resource submission, owned resource lookup, and metadata option retrieval.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("ContributorResourceController Integration Test")
class ContributorResourceControllerIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("herlink_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("sql/contributor-resource-history-integration-schema.sql");

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("HerLink.demo-data-enabled", () -> "false");
        registry.add("HerLink.upload-dir", () -> "target/test-uploads");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppUserMapper appUserMapper;

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
    private ResourceTagMapper resourceTagMapper;

    @Autowired
    private ResourceFileMapper resourceFileMapper;

    @Autowired
    private ResourceVersionMapper resourceVersionMapper;

    @Autowired
    private PasswordHashService passwordHashService;

    private Long contributorUserId;
    private Long viewerUserId;
    private Long categoryId;
    private Long resourceTypeId;
    private Long videoResourceTypeId;
    private Long tagId;

    @BeforeEach
    void setUp() {
        cleanupDatabase();
        seedReferenceData();
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should log in the contributor and return a valid session")
        void shouldLogInContributorAndReturnAValidSession() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "contributor@test.local",
                                      "password": "Contributor123!"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(contributorUserId))
                    .andExpect(jsonPath("$.email").value("contributor@test.local"))
                    .andExpect(jsonPath("$.contributor").value(true))
                    .andExpect(jsonPath("$.contributorStatus").value("APPROVED"));
        }
    }

    @Nested
    @DisplayName("POST /api/contributor/resources")
    class CreateDraftTests {

        @Test
        @DisplayName("Should create a draft resource for a logged-in contributor")
        void shouldCreateDraftResource() throws Exception {
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            MvcResult result = mockMvc.perform(post("/api/contributor/resources")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(ResourceStatusEnum.DRAFT.getValue()))
                    .andExpect(jsonPath("$.contributorId").value(contributorUserId))
                    .andReturn();

            Long createdId = resourceMapper.selectMyResources(contributorUserId, null, null, null).get(0).getId();
            assertNotNull(resourceMapper.selectById(createdId));
            assertEquals(1, resourceVersionMapper.selectByResourceId(createdId).size());
        }

        @Test
        @DisplayName("Should return 401 when the contributor session is missing")
        void shouldReturnUnauthorizedWhenSessionIsMissing() throws Exception {
            mockMvc.perform(post("/api/contributor/resources"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when a viewer tries to create a draft")
        void shouldReturnForbiddenWhenViewerTriesToCreateDraft() throws Exception {
            org.springframework.mock.web.MockHttpSession session = loginAsViewerAndGetSession();

            mockMvc.perform(post("/api/contributor/resources").session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Contributor access requires an approved contributor request."));
        }
    }

    @Nested
    @DisplayName("PUT /api/contributor/resources/{resourceId}")
    class UpdateResourceTests {

        @Test
        @DisplayName("Should update draft metadata and persist the changes")
        void shouldUpdateDraftMetadataAndPersistChanges() throws Exception {
            Long resourceId = insertDraftResource("Old Title");
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(put("/api/contributor/resources/" + resourceId)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "New Title",
                                      "description": "New Description",
                                      "copyright": "Updated Copyright",
                                      "categoryId": %d,
                                      "place": "Suzhou",
                                      "resourceType": "video"
                                    }
                                    """.formatted(categoryId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("New Title"))
                    .andExpect(jsonPath("$.description").value("New Description"))
                    .andExpect(jsonPath("$.place").value("Suzhou"))
                    .andExpect(jsonPath("$.resourceType").value("video"));

            Resource updated = resourceMapper.selectById(resourceId);
            assertNotNull(updated);
            assertEquals("New Title", updated.getTitle());
            assertEquals("New Description", updated.getDescription());
            assertEquals("Suzhou", updated.getPlace());
            assertEquals(categoryId, updated.getCategoryId());
            assertEquals(1, resourceVersionMapper.selectByResourceId(resourceId).size());
        }

        @Test
        @DisplayName("Should return 409 when trying to edit a non-editable resource")
        void shouldReturnConflictWhenResourceIsNotEditable() throws Exception {
            Long resourceId = insertPendingResource();
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(put("/api/contributor/resources/" + resourceId)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Changed"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Current resource status does not allow editing."));

            Resource unchanged = resourceMapper.selectById(resourceId);
            assertNotNull(unchanged);
            assertEquals(ResourceStatusEnum.PENDING_REVIEW.getValue(), unchanged.getStatus());
            assertEquals(0, resourceVersionMapper.selectByResourceId(resourceId).size());
        }

        @Test
        @DisplayName("Should return 404 when the resource does not exist")
        void shouldReturnNotFoundWhenResourceDoesNotExist() throws Exception {
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(put("/api/contributor/resources/999999")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Changed"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resource does not exist."));
        }

        @Test
        @DisplayName("Should return 403 when a viewer tries to update another contributor resource")
        void shouldReturnForbiddenWhenViewerTriesToUpdateResource() throws Exception {
            Long resourceId = insertDraftResource("Old Title");
            org.springframework.mock.web.MockHttpSession session = loginAsViewerAndGetSession();

            mockMvc.perform(put("/api/contributor/resources/" + resourceId)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Changed"
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Contributor access requires an approved contributor request."));
        }

        @Test
        @DisplayName("Should return 400 when the update payload is malformed")
        void shouldReturnBadRequestWhenUpdatePayloadIsMalformed() throws Exception {
            Long resourceId = insertDraftResource("Old Title");
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(put("/api/contributor/resources/" + resourceId)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/contributor/resources/{resourceId}/submit")
    class SubmitResourceTests {

        @Test
        @DisplayName("Should submit a complete draft and store a submission row")
        void shouldSubmitCompleteDraftAndStoreSubmissionRow() throws Exception {
            Long resourceId = insertSubmittableDraftResource("Submit Ready");
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(post("/api/contributor/resources/" + resourceId + "/submit")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionNote": "Ready for review"
                                    }
                                    """))
                    .andExpect(status().isOk());

            Resource submitted = resourceMapper.selectById(resourceId);
            assertNotNull(submitted);
            assertEquals(ResourceStatusEnum.PENDING_REVIEW.getValue(), submitted.getStatus());
            List<ResourceSubmission> submissions = resourceSubmissionMapper.selectByResourceId(resourceId);
            assertEquals(1, submissions.size());
            assertEquals("Ready for review", submissions.get(0).getSubmissionNote());
            assertEquals(1, resourceVersionMapper.selectByResourceId(resourceId).size());
        }

        @Test
        @DisplayName("Should return 400 when the resource cannot be submitted because media is missing")
        void shouldReturnBadRequestWhenMediaIsMissing() throws Exception {
            Long resourceId = insertDraftResourceWithoutMedia("No Media Draft");
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(post("/api/contributor/resources/" + resourceId + "/submit")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionNote": "Ready"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Media file is required."));

            Resource untouched = resourceMapper.selectById(resourceId);
            assertEquals(ResourceStatusEnum.DRAFT.getValue(), untouched.getStatus());
            assertEquals(0, resourceSubmissionMapper.selectByResourceId(resourceId).size());
        }

        @Test
        @DisplayName("Should return 404 when the submitted resource does not exist")
        void shouldReturnNotFoundWhenSubmittedResourceDoesNotExist() throws Exception {
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(post("/api/contributor/resources/999999/submit")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionNote": "Ready"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resource does not exist."));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/my")
    class ListMyResourcesTests {

        @Test
        @DisplayName("Should return only the current contributor resources")
        void shouldReturnOnlyCurrentContributorResources() throws Exception {
            Long draftId = insertDraftResource("My Draft");
            insertViewerResource("Other User Resource");
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(get("/api/contributor/resources/my").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(draftId))
                    .andExpect(jsonPath("$[0].title").value("My Draft"));
        }

        @Test
        @DisplayName("Should return 401 when resource list is requested without login")
        void shouldReturnUnauthorizedWhenResourceListIsRequestedWithoutLogin() throws Exception {
            mockMvc.perform(get("/api/contributor/resources/my"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/{resourceId}")
    class GetDetailTests {

        @Test
        @DisplayName("Should return resource detail for the owner")
        void shouldReturnResourceDetailForOwner() throws Exception {
            Long resourceId = insertDraftResource("Detail Title");
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(get("/api/contributor/resources/" + resourceId).session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(resourceId))
                    .andExpect(jsonPath("$.title").value("Detail Title"));
        }

        @Test
        @DisplayName("Should return 404 when the requested resource detail does not exist")
        void shouldReturnNotFoundWhenRequestedResourceDetailDoesNotExist() throws Exception {
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(get("/api/contributor/resources/999999").session(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Resource does not exist."));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/category-options")
    class CategoryOptionsTests {

        @Test
        @DisplayName("Should return active category options")
        void shouldReturnActiveCategoryOptions() throws Exception {
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(get("/api/contributor/resources/category-options").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(categoryId))
                    .andExpect(jsonPath("$[0].name").value("places"));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/resource-type-options")
    class ResourceTypeOptionsTests {

        @Test
        @DisplayName("Should return active resource type options")
        void shouldReturnActiveResourceTypeOptions() throws Exception {
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(get("/api/contributor/resources/resource-type-options").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(resourceTypeId))
                    .andExpect(jsonPath("$[0].name").value("photo"));
        }
    }

    @Nested
    @DisplayName("GET /api/contributor/resources/tag-options")
    class TagOptionsTests {

        @Test
        @DisplayName("Should return active tag options")
        void shouldReturnActiveTagOptions() throws Exception {
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(get("/api/contributor/resources/tag-options").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(tagId))
                    .andExpect(jsonPath("$[0].name").value("Temple"));
        }
    }

    @Nested
    @DisplayName("POST /api/contributor/resources/{resourceId}/files")
    class UploadFilesTests {

        @Test
        @DisplayName("Should upload files and persist file rows")
        void shouldUploadFilesAndPersistFileRows() throws Exception {
            Long resourceId = insertDraftResource("File Draft");
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();
            MockMultipartFile previewImage = new MockMultipartFile("previewImage", "cover.jpg", "image/jpeg", "img".getBytes());
            MockMultipartFile mediaFile = new MockMultipartFile("mediaFile", "gallery.jpg", "image/jpeg", "photo".getBytes());

            mockMvc.perform(multipart("/api/contributor/resources/" + resourceId + "/files")
                            .file(previewImage)
                            .file(mediaFile)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(resourceId));

            assertFalse(resourceFileMapper.selectByResourceId(resourceId).isEmpty());
        }

        @Test
        @DisplayName("Should return 400 when no file is provided")
        void shouldReturnBadRequestWhenNoFileIsProvided() throws Exception {
            Long resourceId = insertDraftResource("File Draft");
            org.springframework.mock.web.MockHttpSession session = loginAndGetSession();

            mockMvc.perform(multipart("/api/contributor/resources/" + resourceId + "/files").session(session))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("At least one file must be uploaded."));

            assertEquals(0, resourceFileMapper.selectByResourceId(resourceId).size());
        }

        @Test
        @DisplayName("Should return 403 when a viewer tries to upload files")
        void shouldReturnForbiddenWhenViewerTriesToUploadFiles() throws Exception {
            Long resourceId = insertDraftResource("File Draft");
            org.springframework.mock.web.MockHttpSession session = loginAsViewerAndGetSession();
            MockMultipartFile previewImage = new MockMultipartFile("previewImage", "cover.jpg", "image/jpeg", "img".getBytes());

            mockMvc.perform(multipart("/api/contributor/resources/" + resourceId + "/files")
                            .file(previewImage)
                            .session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Contributor access requires an approved contributor request."));
        }
    }

    private org.springframework.mock.web.MockHttpSession loginAndGetSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "contributor@test.local",
                                  "password": "Contributor123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false);
    }

    private org.springframework.mock.web.MockHttpSession loginAsViewerAndGetSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "viewer@test.local",
                                  "password": "Viewer123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false);
    }

    private Long insertDraftResource(String title) {
        return insertResource(title, ResourceStatusEnum.DRAFT.getValue(), "photo", resourceTypeId, null, null);
    }

    private Long insertDraftResourceWithoutMedia(String title) {
        Long resourceId = insertResource(title, ResourceStatusEnum.DRAFT.getValue(), "video", videoResourceTypeId,
                "resource-" + title.hashCode() + "/cover.jpg", null);
        jdbcTemplate.update(
                "INSERT INTO resourceFile(resourceId, originalFilename, storedFilename, filePath, fileType, fileSize, uploadedAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
                resourceId, "cover.jpg", "cover.jpg", "resource-" + title.hashCode() + "/cover.jpg", "jpg", 3L, LocalDateTime.now()
        );
        return resourceId;
    }

    private Long insertSubmittableDraftResource(String title) {
        Long resourceId = insertResource(title, ResourceStatusEnum.DRAFT.getValue(), "video", videoResourceTypeId,
                "resource-" + title.hashCode() + "/cover.jpg", "resource-" + title.hashCode() + "/video.mp4");
        populateDraftFiles(resourceId, "resource-" + title.hashCode() + "/cover.jpg", "resource-" + title.hashCode() + "/video.mp4");
        return resourceId;
    }

    private Long insertResource(String title,
                                 String status,
                                 String resourceTypeName,
                                 Long resourceTypeKey,
                                 String previewImage,
                                 String mediaUrl) {
        Resource resource = new Resource();
        resource.setContributorId(contributorUserId);
        resource.setTitle(title);
        resource.setDescription("Draft description");
        resource.setCopyright("Copyright");
        resource.setCategoryId(categoryId);
        resource.setResourceTypeId(resourceTypeKey);
        resource.setResourceType(resourceTypeName);
        resource.setPreviewImage(previewImage);
        resource.setMediaUrl(mediaUrl);
        resource.setStatus(status);
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resourceMapper.insert(resource);
        return resource.getId();
    }

    private Long insertPendingResource() {
        Resource resource = new Resource();
        resource.setContributorId(contributorUserId);
        resource.setTitle("Pending");
        resource.setDescription("Pending description");
        resource.setCopyright("Copyright");
        resource.setCategoryId(categoryId);
        resource.setResourceTypeId(resourceTypeId);
        resource.setResourceType("photo");
        resource.setStatus(ResourceStatusEnum.PENDING_REVIEW.getValue());
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resourceMapper.insert(resource);
        return resource.getId();
    }

    private Long insertViewerResource(String title) {
        Resource resource = new Resource();
        resource.setContributorId(viewerUserId);
        resource.setTitle(title);
        resource.setDescription("Other user");
        resource.setCopyright("Copyright");
        resource.setCategoryId(categoryId);
        resource.setResourceTypeId(resourceTypeId);
        resource.setResourceType("photo");
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resourceMapper.insert(resource);
        return resource.getId();
    }

    private void populateDraftFiles(Long resourceId, String previewPath, String mediaPath) {
        jdbcTemplate.update(
                "INSERT INTO resourceFile(resourceId, originalFilename, storedFilename, filePath, fileType, fileSize, uploadedAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
                resourceId, "cover.jpg", "cover.jpg", previewPath, "jpg", 3L, LocalDateTime.now()
        );
        jdbcTemplate.update(
                "INSERT INTO resourceFile(resourceId, originalFilename, storedFilename, filePath, fileType, fileSize, uploadedAt) VALUES (?, ?, ?, ?, ?, ?, ?)",
                resourceId, "video.mp4", "video.mp4", mediaPath, "mp4", 5L, LocalDateTime.now()
        );
    }

    private void seedReferenceData() {
        jdbcTemplate.update("INSERT INTO category(categoryTopic, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "places", "ACTIVE", 0, LocalDateTime.now(), LocalDateTime.now());
        jdbcTemplate.update("INSERT INTO resourceType(typeName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "photo", "ACTIVE", 0, LocalDateTime.now(), LocalDateTime.now());
        jdbcTemplate.update("INSERT INTO resourceType(typeName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "video", "ACTIVE", 0, LocalDateTime.now(), LocalDateTime.now());
        jdbcTemplate.update("INSERT INTO tag(tagName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?)",
                "Temple", "ACTIVE", 0, LocalDateTime.now(), LocalDateTime.now());

        Category category = categoryMapper.selectByTopic("places");
        ResourceType resourceType = resourceTypeMapper.selectActiveByTypeName("photo");
        ResourceType videoType = resourceTypeMapper.selectActiveByTypeName("video");
        Tag tag = tagMapper.selectByName("Temple");

        categoryId = category.getCategoryId();
        resourceTypeId = resourceType.getResourceTypeId();
        videoResourceTypeId = videoType.getResourceTypeId();
        tagId = tag.getTagId();

        AppUser contributor = new AppUser();
        contributor.setName("Contributor");
        contributor.setEmail("contributor@test.local");
        contributor.setPasswordHash(passwordHashService.hash("Contributor123!"));
        contributor.setRole(UserRoleEnum.REGISTERED_VIEWER.getValue());
        contributor.setContributor(true);
        contributor.setBio("Contributor bio");
        contributor.setCreatedAt(LocalDateTime.now());
        contributor.setUpdatedAt(LocalDateTime.now());
        appUserMapper.insert(contributor);
        contributorUserId = contributor.getUserId();

        AppUser viewer = new AppUser();
        viewer.setName("Viewer");
        viewer.setEmail("viewer@test.local");
        viewer.setPasswordHash(passwordHashService.hash("Viewer123!"));
        viewer.setRole(UserRoleEnum.REGISTERED_VIEWER.getValue());
        viewer.setContributor(false);
        viewer.setBio("Viewer bio");
        viewer.setCreatedAt(LocalDateTime.now());
        viewer.setUpdatedAt(LocalDateTime.now());
        appUserMapper.insert(viewer);
        viewerUserId = viewer.getUserId();
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
}
