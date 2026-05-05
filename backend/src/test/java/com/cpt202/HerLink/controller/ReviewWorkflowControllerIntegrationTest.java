package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceFile;
import com.cpt202.HerLink.entity.ResourceSubmission;
import com.cpt202.HerLink.entity.ResourceTag;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.ReviewRecord;
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
import com.cpt202.HerLink.mapper.ReviewRecordMapper;
import com.cpt202.HerLink.mapper.TagMapper;
import com.cpt202.HerLink.util.PasswordHashService;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for {@link ReviewWorkflowController}.
 * This project uses HttpSession-based authentication rather than JWT.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "HerLink.demo-data-enabled=false",
                "HerLink.upload-dir=target/test-uploads",
                "HerLink.frontend-dir=../frontend"
        }
)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("ReviewWorkflowController integration tests")
public class ReviewWorkflowControllerIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("herlink_review_test")
            .withUsername("herlink")
            .withPassword("herlink")
            .withInitScript("sql/review-workflow-controller-test-schema.sql");

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
    private ReviewRecordMapper reviewRecordMapper;

    @Autowired
    private ResourceFileMapper resourceFileMapper;

    @Autowired
    private ResourceTagMapper resourceTagMapper;

    @Autowired
    private PasswordHashService passwordHashService;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM reviewRecord");
        jdbcTemplate.update("DELETE FROM resourceFile");
        jdbcTemplate.update("DELETE FROM resourceTag");
        jdbcTemplate.update("DELETE FROM resourceSubmission");
        jdbcTemplate.update("DELETE FROM resource");
        jdbcTemplate.update("DELETE FROM contributorApplication");
        jdbcTemplate.update("DELETE FROM tag");
        jdbcTemplate.update("DELETE FROM category");
        jdbcTemplate.update("DELETE FROM resourceType");
        jdbcTemplate.update("DELETE FROM `user`");

        jdbcTemplate.execute("ALTER TABLE reviewRecord AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE resourceFile AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE resourceTag AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE resourceSubmission AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE resource AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE contributorApplication AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE tag AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE category AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE resourceType AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE `user` AUTO_INCREMENT = 1");
    }

    @Nested
    @DisplayName("GET /api/reviewer/reviews/pending")
    class GetPendingReviewsTests {

        @Test
        @DisplayName("Should return the latest pending submissions for an authenticated reviewer")
        void shouldReturnPendingReviewsForReviewer() throws Exception {
            AppUser reviewer = persistUser(
                    "reviewer-user",
                    "reviewer@example.com",
                    "Reviewer123!",
                    UserRoleEnum.ADMINISTRATOR
            );
            ResourceGraph olderPending = persistPendingResourceGraph("Older Pending Resource", 1);
            ResourceGraph latestPending = persistPendingResourceGraph("Latest Pending Resource", 2);
            persistReviewedRecord(olderPending, reviewer, "REJECT", ResourceStatusEnum.REJECTED.getValue(), "Old feedback");

            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/pending")
                            .session(reviewerSession)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].submissionId").value(latestPending.latestSubmission().getSubmissionId()))
                    .andExpect(jsonPath("$.items[0].resourceId").value(latestPending.resource().getId()))
                    .andExpect(jsonPath("$.items[0].versionNo").value(latestPending.latestSubmission().getVersionNo()))
                    .andExpect(jsonPath("$.items[0].title").value("Latest Pending Resource"))
                    .andExpect(jsonPath("$.items[0].contributorId").value(latestPending.contributor().getUserId()))
                    .andExpect(jsonPath("$.items[0].contributorName").value(latestPending.contributor().getName()))
                    .andExpect(jsonPath("$.items[0].categoryTopic").value(latestPending.category().getCategoryTopic()))
                    .andExpect(jsonPath("$.items[0].resourceStatus").value("PENDING_REVIEW"))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(10))
                    .andExpect(jsonPath("$.total").value(2))
                    .andExpect(jsonPath("$.emptyMessage").doesNotExist());

            assertThat(reviewRecordRowCount()).isEqualTo(1);
            assertThat(resourceSubmissionMapper.selectLatestByResourceId(latestPending.resource().getId()).getSubmissionId())
                    .isEqualTo(latestPending.latestSubmission().getSubmissionId());
        }

        @Test
        @DisplayName("Should return an empty page and empty message when no pending submissions exist")
        void shouldReturnEmptyPendingReviewPageWhenNoDataExists() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/pending")
                            .session(reviewerSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isEmpty())
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(10))
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.emptyMessage").value("No submissions are currently waiting for review."));

            assertThat(resourceRowCount()).isZero();
            assertThat(reviewRecordRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 401 when pending reviews are requested without login")
        void shouldReturnUnauthorizedWhenPendingReviewsRequestedWithoutLogin() throws Exception {
            mockMvc.perform(get("/api/reviewer/reviews/pending"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/pending"));

            assertThat(resourceRowCount()).isZero();
            assertThat(reviewRecordRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 403 when a non reviewer tries to list pending reviews")
        void shouldReturnForbiddenWhenViewerRequestsPendingReviews() throws Exception {
            persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/pending").session(viewerSession))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/pending"));

            assertThat(resourceRowCount()).isZero();
            assertThat(reviewRecordRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 400 when pagination parameters violate business constraints")
        void shouldReturnBadRequestWhenPaginationIsInvalid() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/pending")
                            .session(reviewerSession)
                            .param("page", "0")
                            .param("pageSize", "51"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid pagination request."))
                    .andExpect(jsonPath("$.details[0]").value("page must be at least 1."))
                    .andExpect(jsonPath("$.details[1]").value("pageSize must be between 1 and 50."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/pending"));

            assertThat(resourceRowCount()).isZero();
            assertThat(reviewRecordRowCount()).isZero();
        }
    }

    @Nested
    @DisplayName("GET /api/reviewer/reviews/submissions/{submissionId}")
    class GetReviewDetailTests {

        @Test
        @DisplayName("Should return submission details together with files tags and history for a reviewer")
        void shouldReturnReviewDetailForReviewer() throws Exception {
            AppUser reviewer = persistUser(
                    "reviewer-user",
                    "reviewer@example.com",
                    "Reviewer123!",
                    UserRoleEnum.ADMINISTRATOR
            );
            ResourceGraph resourceGraph = persistPendingResourceGraph("Community Archive", 2);
            persistReviewedRecord(resourceGraph, reviewer, "REJECT", ResourceStatusEnum.REJECTED.getValue(), "Need more metadata");
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/submissions/{submissionId}", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(resourceGraph.latestSubmission().getSubmissionId()))
                    .andExpect(jsonPath("$.resourceId").value(resourceGraph.resource().getId()))
                    .andExpect(jsonPath("$.versionNo").value(2))
                    .andExpect(jsonPath("$.resourceStatus").value("PENDING_REVIEW"))
                    .andExpect(jsonPath("$.resource.title").value("Community Archive"))
                    .andExpect(jsonPath("$.resource.resourceType").value(resourceGraph.resourceType().getTypeName()))
                    .andExpect(jsonPath("$.resource.files[0].originalFilename").value("archive-v2.pdf"))
                    .andExpect(jsonPath("$.contributor.userId").value(resourceGraph.contributor().getUserId()))
                    .andExpect(jsonPath("$.contributor.username").value(resourceGraph.contributor().getName()))
                    .andExpect(jsonPath("$.category.categoryId").value(resourceGraph.category().getCategoryId()))
                    .andExpect(jsonPath("$.category.categoryTopic").value(resourceGraph.category().getCategoryTopic()))
                    .andExpect(jsonPath("$.submission.submittedBy").value(resourceGraph.contributor().getUserId()))
                    .andExpect(jsonPath("$.submission.submissionNote").value("Submission note for version 2"))
                    .andExpect(jsonPath("$.submission.statusSnapshot").value("PENDING_REVIEW"))
                    .andExpect(jsonPath("$.submission.resubmission").value(true))
                    .andExpect(jsonPath("$.tags[0]").value("museum-2"))
                    .andExpect(jsonPath("$.reviewHistory[0].action").value("REJECT"))
                    .andExpect(jsonPath("$.reviewHistory[0].feedbackComment").value("Need more metadata"))
                    .andExpect(jsonPath("$.reviewHistory[0].contextType").value("PREVIOUS_SUBMISSION"));

            assertThat(resourceTagMapper.selectTagNamesByResourceId(resourceGraph.resource().getId()))
                    .containsExactly("museum-2");
            assertThat(resourceFileMapper.selectByResourceId(resourceGraph.resource().getId())).hasSize(1);
        }

        @Test
        @DisplayName("Should return 404 when submission detail is requested for a non existing submission")
        void shouldReturnNotFoundWhenSubmissionDetailDoesNotExist() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/submissions/9999").session(reviewerSession))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Review submission does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/9999"));

            assertThat(resourceRowCount()).isZero();
            assertThat(reviewRecordRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 401 when submission detail is requested without login")
        void shouldReturnUnauthorizedWhenSubmissionDetailRequestedWithoutLogin() throws Exception {
            ResourceGraph resourceGraph = persistPendingResourceGraph("Detail Without Login", 1);

            mockMvc.perform(get("/api/reviewer/reviews/submissions/{submissionId}", resourceGraph.latestSubmission().getSubmissionId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/%d"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));
        }

        @Test
        @DisplayName("Should return 403 when a non reviewer requests submission detail")
        void shouldReturnForbiddenWhenViewerRequestsSubmissionDetail() throws Exception {
            persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Detail Forbidden Resource", 1);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/submissions/{submissionId}", resourceGraph.latestSubmission().getSubmissionId())
                            .session(viewerSession))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/%d"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));
        }

        @Test
        @DisplayName("Should return 400 when submission id path variable is malformed")
        void shouldReturnBadRequestWhenSubmissionIdIsMalformed() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/submissions/not-a-number").session(reviewerSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/not-a-number"));

            assertThat(resourceRowCount()).isZero();
        }
    }

    @Nested
    @DisplayName("GET /api/reviewer/reviews/{submissionId}")
    class GetReviewDetailAliasTests {

        @Test
        @DisplayName("Should return the same review detail through the alias endpoint")
        void shouldReturnReviewDetailThroughAliasEndpoint() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Alias Detail Resource", 1);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/{submissionId}", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(resourceGraph.latestSubmission().getSubmissionId()))
                    .andExpect(jsonPath("$.resource.title").value("Alias Detail Resource"))
                    .andExpect(jsonPath("$.submission.resubmission").value(false));

            assertThat(resourceSubmissionMapper.selectLatestByResourceId(resourceGraph.resource().getId()).getSubmissionId())
                    .isEqualTo(resourceGraph.latestSubmission().getSubmissionId());
        }
    }

    @Nested
    @DisplayName("GET /api/reviewer/reviews/submissions/{submissionId}/history")
    class GetReviewHistoryTests {

        @Test
        @DisplayName("Should return grouped review history for a resubmitted resource")
        void shouldReturnReviewHistoryForReviewer() throws Exception {
            AppUser reviewer = persistUser(
                    "reviewer-user",
                    "reviewer@example.com",
                    "Reviewer123!",
                    UserRoleEnum.ADMINISTRATOR
            );
            ResourceGraph resourceGraph = persistPendingResourceGraph("History Resource", 2);
            persistReviewedRecord(resourceGraph, reviewer, "REJECT", ResourceStatusEnum.REJECTED.getValue(), "Please revise and resubmit");
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/submissions/{submissionId}/history", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(resourceGraph.latestSubmission().getSubmissionId()))
                    .andExpect(jsonPath("$.resourceId").value(resourceGraph.resource().getId()))
                    .andExpect(jsonPath("$.versionNo").value(2))
                    .andExpect(jsonPath("$.resubmission").value(true))
                    .andExpect(jsonPath("$.sections[0].label").value("Previous Reviews"))
                    .andExpect(jsonPath("$.sections[0].contextType").value("PREVIOUS_SUBMISSION"))
                    .andExpect(jsonPath("$.sections[0].items[0].action").value("REJECT"))
                    .andExpect(jsonPath("$.sections[1].label").value("Current Resubmission"))
                    .andExpect(jsonPath("$.sections[1].contextType").value("CURRENT_SUBMISSION"))
                    .andExpect(jsonPath("$.sections[1].items").isEmpty());

            assertThat(reviewRecordMapper.selectLatestBySubmissionId(resourceGraph.previousSubmission().getSubmissionId()))
                    .isNotNull();
            assertThat(reviewRecordRowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 404 when review history is requested for a non existing submission")
        void shouldReturnNotFoundWhenHistorySubmissionDoesNotExist() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/submissions/404/history").session(reviewerSession))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Review submission does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/404/history"));

            assertThat(reviewRecordRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 401 when review history is requested without login")
        void shouldReturnUnauthorizedWhenHistoryRequestedWithoutLogin() throws Exception {
            ResourceGraph resourceGraph = persistPendingResourceGraph("History Without Login", 1);

            mockMvc.perform(get("/api/reviewer/reviews/submissions/{submissionId}/history", resourceGraph.latestSubmission().getSubmissionId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/%d/history"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));
        }

        @Test
        @DisplayName("Should return 403 when a non reviewer requests review history")
        void shouldReturnForbiddenWhenViewerRequestsHistory() throws Exception {
            persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            ResourceGraph resourceGraph = persistPendingResourceGraph("History Forbidden Resource", 1);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/submissions/{submissionId}/history", resourceGraph.latestSubmission().getSubmissionId())
                            .session(viewerSession))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/%d/history"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));
        }
    }

    @Nested
    @DisplayName("GET /api/reviewer/reviews/{submissionId}/history")
    class GetReviewHistoryAliasTests {

        @Test
        @DisplayName("Should return review history through the alias endpoint")
        void shouldReturnReviewHistoryThroughAliasEndpoint() throws Exception {
            AppUser reviewer = persistUser(
                    "reviewer-user",
                    "reviewer@example.com",
                    "Reviewer123!",
                    UserRoleEnum.ADMINISTRATOR
            );
            ResourceGraph resourceGraph = persistPendingResourceGraph("Alias History Resource", 1);
            persistReviewedRecord(resourceGraph, reviewer, "APPROVE", ResourceStatusEnum.APPROVED.getValue(), "Looks good");
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/reviewer/reviews/{submissionId}/history", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(resourceGraph.latestSubmission().getSubmissionId()))
                    .andExpect(jsonPath("$.sections[0].items[0].action").value("APPROVE"));

            assertThat(reviewRecordRowCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("POST /api/reviewer/reviews/{submissionId}/approve")
    class ApproveSubmissionTests {

        @Test
        @DisplayName("Should approve the latest pending submission and persist the review record")
        void shouldApprovePendingSubmissionSuccessfully() throws Exception {
            AppUser reviewer = persistUser(
                    "reviewer-user",
                    "reviewer@example.com",
                    "Reviewer123!",
                    UserRoleEnum.ADMINISTRATOR
            );
            ResourceGraph resourceGraph = persistPendingResourceGraph("Approve Resource", 1);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(post("/api/reviewer/reviews/{submissionId}/approve", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "feedbackComment": "  Approved for publication.  "
                                    }
                                    """.formatted(
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(resourceGraph.latestSubmission().getSubmissionId()))
                    .andExpect(jsonPath("$.resourceId").value(resourceGraph.resource().getId()))
                    .andExpect(jsonPath("$.versionNo").value(resourceGraph.latestSubmission().getVersionNo()))
                    .andExpect(jsonPath("$.action").value("APPROVE"))
                    .andExpect(jsonPath("$.resourceStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.feedbackComment").value("Approved for publication."))
                    .andExpect(jsonPath("$.removedFromPendingQueue").value(true));

            Resource updatedResource = resourceMapper.selectById(resourceGraph.resource().getId());
            ReviewRecord reviewRecord = reviewRecordMapper.selectLatestBySubmissionId(resourceGraph.latestSubmission().getSubmissionId());

            assertThat(updatedResource.getStatus()).isEqualTo(ResourceStatusEnum.APPROVED.getValue());
            assertThat(updatedResource.getReviewedAt()).isNotNull();
            assertThat(reviewRecord).isNotNull();
            assertThat(reviewRecord.getReviewerId()).isEqualTo(reviewer.getUserId());
            assertThat(reviewRecord.getActionDescription()).isEqualTo("APPROVE");
            assertThat(reviewRecord.getStatus()).isEqualTo(ResourceStatusEnum.APPROVED.getValue());
            assertThat(reviewRecord.getFeedbackComment()).isEqualTo("Approved for publication.");
            assertThat(reviewRecordRowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 401 when approving without login")
        void shouldReturnUnauthorizedWhenApprovingWithoutLogin() throws Exception {
            ResourceGraph resourceGraph = persistPendingResourceGraph("Approve Without Login", 1);
            long beforeReviewRows = reviewRecordRowCount();
            String beforeStatus = resourceMapper.selectById(resourceGraph.resource().getId()).getStatus();

            mockMvc.perform(post("/api/reviewer/reviews/{submissionId}/approve", resourceGraph.latestSubmission().getSubmissionId())
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "feedbackComment": "Approved"
                                    }
                                    """.formatted(
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/%d/approve"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus()).isEqualTo(beforeStatus);
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 403 when a non reviewer tries to approve a submission")
        void shouldReturnForbiddenWhenViewerApprovesSubmission() throws Exception {
            persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Approve Forbidden Resource", 1);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");
            long beforeReviewRows = reviewRecordRowCount();

            mockMvc.perform(post("/api/reviewer/reviews/{submissionId}/approve", resourceGraph.latestSubmission().getSubmissionId())
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "feedbackComment": "Approved"
                                    }
                                    """.formatted(
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/%d/approve"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus())
                    .isEqualTo(ResourceStatusEnum.PENDING_REVIEW.getValue());
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 400 when approve request body is malformed JSON")
        void shouldReturnBadRequestWhenApproveJsonIsMalformed() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Approve Invalid Json", 1);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");
            long beforeReviewRows = reviewRecordRowCount();

            mockMvc.perform(post("/api/reviewer/reviews/{submissionId}/approve", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/%d/approve"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus())
                    .isEqualTo(ResourceStatusEnum.PENDING_REVIEW.getValue());
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 400 when approve request violates business validation")
        void shouldReturnBadRequestWhenApproveBusinessValidationFails() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Approve Validation Resource", 1);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");
            long beforeReviewRows = reviewRecordRowCount();

            mockMvc.perform(post("/api/reviewer/reviews/{submissionId}/approve", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": %d,
                                      "versionNo": 99,
                                      "feedbackComment": "Approved"
                                    }
                                    """.formatted(resourceGraph.resource().getId())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Review decision request is invalid."))
                    .andExpect(jsonPath("$.details[0]").value("versionNo does not match the selected submission."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/%d/approve"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus())
                    .isEqualTo(ResourceStatusEnum.PENDING_REVIEW.getValue());
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 404 when approving a non existing submission")
        void shouldReturnNotFoundWhenApproveSubmissionDoesNotExist() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(post("/api/reviewer/reviews/9999/approve")
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": 1,
                                      "versionNo": 1,
                                      "feedbackComment": "Approved"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Review submission does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/9999/approve"));

            assertThat(reviewRecordRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 409 and leave database unchanged when submission is no longer pending")
        void shouldReturnConflictWhenApproveSubmissionIsNoLongerPending() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            ResourceGraph resourceGraph = persistResourceGraph("Already Approved Resource", 1, ResourceStatusEnum.APPROVED.getValue());
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");
            long beforeReviewRows = reviewRecordRowCount();
            String beforeStatus = resourceMapper.selectById(resourceGraph.resource().getId()).getStatus();

            mockMvc.perform(post("/api/reviewer/reviews/{submissionId}/approve", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "feedbackComment": "Approved"
                                    }
                                    """.formatted(
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("This submission is no longer pending review."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/%d/approve"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus()).isEqualTo(beforeStatus);
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }
    }

    @Nested
    @DisplayName("POST /api/reviewer/reviews/{submissionId}/reject")
    class RejectSubmissionTests {

        @Test
        @DisplayName("Should reject the latest pending submission and store reviewer feedback")
        void shouldRejectPendingSubmissionSuccessfully() throws Exception {
            AppUser reviewer = persistUser(
                    "reviewer-user",
                    "reviewer@example.com",
                    "Reviewer123!",
                    UserRoleEnum.ADMINISTRATOR
            );
            ResourceGraph resourceGraph = persistPendingResourceGraph("Reject Resource", 1);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(post("/api/reviewer/reviews/{submissionId}/reject", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "feedbackComment": "Please add source attribution."
                                    }
                                    """.formatted(
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(resourceGraph.latestSubmission().getSubmissionId()))
                    .andExpect(jsonPath("$.action").value("REJECT"))
                    .andExpect(jsonPath("$.resourceStatus").value("REJECTED"))
                    .andExpect(jsonPath("$.feedbackComment").value("Please add source attribution."));

            Resource updatedResource = resourceMapper.selectById(resourceGraph.resource().getId());
            ReviewRecord reviewRecord = reviewRecordMapper.selectLatestBySubmissionId(resourceGraph.latestSubmission().getSubmissionId());

            assertThat(updatedResource.getStatus()).isEqualTo(ResourceStatusEnum.REJECTED.getValue());
            assertThat(reviewRecord).isNotNull();
            assertThat(reviewRecord.getReviewerId()).isEqualTo(reviewer.getUserId());
            assertThat(reviewRecord.getActionDescription()).isEqualTo("REJECT");
            assertThat(reviewRecord.getStatus()).isEqualTo(ResourceStatusEnum.REJECTED.getValue());
            assertThat(reviewRecord.getFeedbackComment()).isEqualTo("Please add source attribution.");
            assertThat(reviewRecordRowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 400 and keep database unchanged when rejection comment is missing")
        void shouldReturnBadRequestWhenRejectCommentIsMissing() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Reject Validation Resource", 1);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");
            long beforeReviewRows = reviewRecordRowCount();
            String beforeStatus = resourceMapper.selectById(resourceGraph.resource().getId()).getStatus();

            mockMvc.perform(post("/api/reviewer/reviews/{submissionId}/reject", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "feedbackComment": "   "
                                    }
                                    """.formatted(
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Review decision request is invalid."))
                    .andExpect(jsonPath("$.details[0]").value("Rejection comments are required."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/%d/reject"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus()).isEqualTo(beforeStatus);
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 401 when rejecting without login")
        void shouldReturnUnauthorizedWhenRejectingWithoutLogin() throws Exception {
            ResourceGraph resourceGraph = persistPendingResourceGraph("Reject Without Login", 1);
            long beforeReviewRows = reviewRecordRowCount();

            mockMvc.perform(post("/api/reviewer/reviews/{submissionId}/reject", resourceGraph.latestSubmission().getSubmissionId())
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "feedbackComment": "Please revise."
                                    }
                                    """.formatted(
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/%d/reject"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus())
                    .isEqualTo(ResourceStatusEnum.PENDING_REVIEW.getValue());
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 403 when a non reviewer tries to reject a submission")
        void shouldReturnForbiddenWhenViewerRejectsSubmission() throws Exception {
            persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Reject Forbidden Resource", 1);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");
            long beforeReviewRows = reviewRecordRowCount();

            mockMvc.perform(post("/api/reviewer/reviews/{submissionId}/reject", resourceGraph.latestSubmission().getSubmissionId())
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "feedbackComment": "Please revise."
                                    }
                                    """.formatted(
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/%d/reject"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus())
                    .isEqualTo(ResourceStatusEnum.PENDING_REVIEW.getValue());
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 404 when rejecting a non existing submission")
        void shouldReturnNotFoundWhenRejectSubmissionDoesNotExist() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(post("/api/reviewer/reviews/9999/reject")
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "resourceId": 1,
                                      "versionNo": 1,
                                      "feedbackComment": "Please revise."
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Review submission does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/9999/reject"));

            assertThat(reviewRecordRowCount()).isZero();
        }
    }

    @Nested
    @DisplayName("POST /api/reviewer/reviews/submissions/{submissionId}/decision")
    class SubmitDecisionTests {

        @Test
        @DisplayName("Should approve through the unified decision endpoint and persist database changes")
        void shouldApproveThroughUnifiedDecisionEndpoint() throws Exception {
            AppUser reviewer = persistUser(
                    "reviewer-user",
                    "reviewer@example.com",
                    "Reviewer123!",
                    UserRoleEnum.ADMINISTRATOR
            );
            ResourceGraph resourceGraph = persistPendingResourceGraph("Decision Resource", 1);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(post("/api/reviewer/reviews/submissions/{submissionId}/decision", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionId": %d,
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "action": "APPROVE",
                                      "feedbackComment": "Approved via decision endpoint."
                                    }
                                    """.formatted(
                                    resourceGraph.latestSubmission().getSubmissionId(),
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").value(resourceGraph.latestSubmission().getSubmissionId()))
                    .andExpect(jsonPath("$.action").value("APPROVE"))
                    .andExpect(jsonPath("$.resourceStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.feedbackComment").value("Approved via decision endpoint."));

            Resource updatedResource = resourceMapper.selectById(resourceGraph.resource().getId());
            ReviewRecord reviewRecord = reviewRecordMapper.selectLatestBySubmissionId(resourceGraph.latestSubmission().getSubmissionId());

            assertThat(updatedResource.getStatus()).isEqualTo(ResourceStatusEnum.APPROVED.getValue());
            assertThat(reviewRecord).isNotNull();
            assertThat(reviewRecord.getReviewerId()).isEqualTo(reviewer.getUserId());
            assertThat(reviewRecord.getActionDescription()).isEqualTo("APPROVE");
            assertThat(reviewRecord.getFeedbackComment()).isEqualTo("Approved via decision endpoint.");
        }

        @Test
        @DisplayName("Should return 400 and keep database unchanged when unified decision body is invalid")
        void shouldReturnBadRequestWhenUnifiedDecisionBodyIsInvalid() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Decision Validation Resource", 1);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");
            long beforeReviewRows = reviewRecordRowCount();
            String beforeStatus = resourceMapper.selectById(resourceGraph.resource().getId()).getStatus();

            mockMvc.perform(post("/api/reviewer/reviews/submissions/{submissionId}/decision", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionId": %d,
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "feedbackComment": "Missing action."
                                    }
                                    """.formatted(
                                    resourceGraph.latestSubmission().getSubmissionId(),
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Review decision request is invalid."))
                    .andExpect(jsonPath("$.details[0]").value("action is required."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/%d/decision"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus()).isEqualTo(beforeStatus);
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 400 when unified decision JSON is malformed")
        void shouldReturnBadRequestWhenUnifiedDecisionJsonIsMalformed() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Decision Invalid Json Resource", 1);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");
            long beforeReviewRows = reviewRecordRowCount();

            mockMvc.perform(post("/api/reviewer/reviews/submissions/{submissionId}/decision", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/%d/decision"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus())
                    .isEqualTo(ResourceStatusEnum.PENDING_REVIEW.getValue());
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 400 when decision path and body submission ids do not match")
        void shouldReturnBadRequestWhenDecisionSubmissionIdMismatch() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Decision Id Mismatch Resource", 1);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");
            long beforeReviewRows = reviewRecordRowCount();

            mockMvc.perform(post("/api/reviewer/reviews/submissions/{submissionId}/decision", resourceGraph.latestSubmission().getSubmissionId())
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionId": %d,
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "action": "APPROVE",
                                      "feedbackComment": "Approved"
                                    }
                                    """.formatted(
                                    resourceGraph.latestSubmission().getSubmissionId() + 1,
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Review decision request is invalid."))
                    .andExpect(jsonPath("$.details[0]").value("Submission id in path and body must match."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/%d/decision"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus())
                    .isEqualTo(ResourceStatusEnum.PENDING_REVIEW.getValue());
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 401 when unified decision is submitted without login")
        void shouldReturnUnauthorizedWhenUnifiedDecisionSubmittedWithoutLogin() throws Exception {
            ResourceGraph resourceGraph = persistPendingResourceGraph("Decision Without Login", 1);
            long beforeReviewRows = reviewRecordRowCount();

            mockMvc.perform(post("/api/reviewer/reviews/submissions/{submissionId}/decision", resourceGraph.latestSubmission().getSubmissionId())
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionId": %d,
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "action": "APPROVE",
                                      "feedbackComment": "Approved"
                                    }
                                    """.formatted(
                                    resourceGraph.latestSubmission().getSubmissionId(),
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/%d/decision"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus())
                    .isEqualTo(ResourceStatusEnum.PENDING_REVIEW.getValue());
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 403 when a non reviewer submits a unified decision")
        void shouldReturnForbiddenWhenViewerSubmitsUnifiedDecision() throws Exception {
            persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            ResourceGraph resourceGraph = persistPendingResourceGraph("Decision Forbidden Resource", 1);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");
            long beforeReviewRows = reviewRecordRowCount();

            mockMvc.perform(post("/api/reviewer/reviews/submissions/{submissionId}/decision", resourceGraph.latestSubmission().getSubmissionId())
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionId": %d,
                                      "resourceId": %d,
                                      "versionNo": %d,
                                      "action": "APPROVE",
                                      "feedbackComment": "Approved"
                                    }
                                    """.formatted(
                                    resourceGraph.latestSubmission().getSubmissionId(),
                                    resourceGraph.resource().getId(),
                                    resourceGraph.latestSubmission().getVersionNo()
                            )))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/%d/decision"
                            .formatted(resourceGraph.latestSubmission().getSubmissionId())));

            assertThat(resourceMapper.selectById(resourceGraph.resource().getId()).getStatus())
                    .isEqualTo(ResourceStatusEnum.PENDING_REVIEW.getValue());
            assertThat(reviewRecordRowCount()).isEqualTo(beforeReviewRows);
        }

        @Test
        @DisplayName("Should return 404 when unified decision targets a non existing submission")
        void shouldReturnNotFoundWhenUnifiedDecisionSubmissionDoesNotExist() throws Exception {
            persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(post("/api/reviewer/reviews/submissions/9999/decision")
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "submissionId": 9999,
                                      "resourceId": 1,
                                      "versionNo": 1,
                                      "action": "APPROVE",
                                      "feedbackComment": "Approved"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Review submission does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/reviewer/reviews/submissions/9999/decision"));

            assertThat(reviewRecordRowCount()).isZero();
        }
    }

    private AppUser persistUser(String username, String email, String rawPassword, UserRoleEnum role) {
        LocalDateTime now = LocalDateTime.now();
        AppUser user = new AppUser();
        user.setName(username);
        user.setEmail(email.toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordHashService.hash(rawPassword));
        user.setRole(role.getValue());
        user.setContributor(false);
        user.setBio(username + " bio");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        appUserMapper.insert(user);
        return user;
    }

    private Category persistCategory(String topic) {
        Category category = new Category();
        category.setCategoryTopic(topic);
        category.setStatus("ACTIVE");
        category.setUsageCount(0);
        categoryMapper.insert(category);
        return category;
    }

    private ResourceType persistResourceType(String typeName) {
        ResourceType resourceType = new ResourceType();
        resourceType.setTypeName(typeName);
        resourceType.setStatus("ACTIVE");
        resourceType.setUsageCount(0);
        resourceTypeMapper.insert(resourceType);
        return resourceType;
    }

    private Tag persistTag(String tagName) {
        Tag tag = new Tag();
        tag.setTagName(tagName);
        tag.setStatus("ACTIVE");
        tag.setUsageCount(0);
        tagMapper.insert(tag);
        return tag;
    }

    private ResourceGraph persistPendingResourceGraph(String title, int latestVersionNo) {
        return persistResourceGraph(title, latestVersionNo, ResourceStatusEnum.PENDING_REVIEW.getValue());
    }

    private ResourceGraph persistResourceGraph(String title, int latestVersionNo, String resourceStatus) {
        LocalDateTime now = LocalDateTime.now();
        AppUser contributor = persistUser(
                "contributor-" + UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8) + "@example.com",
                "Contributor123!",
                UserRoleEnum.REGISTERED_VIEWER
        );
        Category category = persistCategory("category-" + latestVersionNo);
        ResourceType resourceType = persistResourceType("type-" + latestVersionNo);
        Tag tag = persistTag("museum-" + latestVersionNo);

        Resource resource = new Resource();
        resource.setContributorId(contributor.getUserId());
        resource.setTitle(title);
        resource.setDescription("Description for " + title);
        resource.setCopyright("Contributor-owned");
        resource.setCategoryId(category.getCategoryId());
        resource.setPlace("Liverpool");
        resource.setPreviewImage("/images/" + latestVersionNo + ".png");
        resource.setMediaUrl("/media/" + latestVersionNo + ".pdf");
        resource.setStatus(resourceStatus);
        resource.setReviewedAt(ResourceStatusEnum.PENDING_REVIEW.getValue().equals(resourceStatus) ? null : now.minusHours(1));
        resource.setCreatedAt(now.minusDays(5));
        resource.setUpdatedAt(now.minusMinutes(10));
        resource.setArchivedAt(null);
        resource.setResourceTypeId(resourceType.getResourceTypeId());
        resourceMapper.insert(resource);

        ResourceSubmission previousSubmission = null;
        for (int versionNo = 1; versionNo <= latestVersionNo; versionNo++) {
            ResourceSubmission submission = new ResourceSubmission();
            submission.setResourceId(resource.getId());
            submission.setVersionNo(versionNo);
            submission.setSubmittedBy(contributor.getUserId());
            submission.setSubmittedAt(now.minusDays(latestVersionNo - versionNo + 1L));
            submission.setSubmissionNote("Submission note for version " + versionNo);
            submission.setStatusSnapshot(versionNo == latestVersionNo
                    ? resourceStatus
                    : ResourceStatusEnum.REJECTED.getValue());
            submission.setCreatedAt(now.minusDays(latestVersionNo - versionNo + 1L));
            resourceSubmissionMapper.insert(submission);
            if (versionNo == latestVersionNo - 1) {
                previousSubmission = submission;
            }
        }

        ResourceSubmission latestSubmission = resourceSubmissionMapper.selectLatestByResourceId(resource.getId());

        ResourceFile resourceFile = new ResourceFile();
        resourceFile.setResourceId(resource.getId());
        resourceFile.setOriginalFilename("archive-v" + latestVersionNo + ".pdf");
        resourceFile.setStoredFilename("archive-v" + latestVersionNo + "-stored.pdf");
        resourceFile.setFilePath("/uploads/archive-v" + latestVersionNo + ".pdf");
        resourceFile.setFileType("application/pdf");
        resourceFile.setFileSize(1024L + latestVersionNo);
        resourceFile.setUploadedAt(now.minusHours(2));
        resourceFileMapper.insert(resourceFile);

        ResourceTag resourceTag = new ResourceTag();
        resourceTag.setResourceId(resource.getId());
        resourceTag.setTagId(tag.getTagId());
        resourceTagMapper.insert(resourceTag);

        return new ResourceGraph(contributor, category, resourceType, resource, previousSubmission, latestSubmission);
    }

    private ReviewRecord persistReviewedRecord(ResourceGraph resourceGraph,
                                              AppUser reviewer,
                                              String actionDescription,
                                              String status,
                                              String feedbackComment) {
        LocalDateTime reviewedAt = LocalDateTime.now().minusDays(1);
        ResourceSubmission targetSubmission = resourceGraph.previousSubmission() != null
                ? resourceGraph.previousSubmission()
                : resourceGraph.latestSubmission();

        ReviewRecord reviewRecord = new ReviewRecord();
        reviewRecord.setResourceId(resourceGraph.resource().getId());
        reviewRecord.setSubmissionId(targetSubmission.getSubmissionId());
        reviewRecord.setVersionNo(targetSubmission.getVersionNo());
        reviewRecord.setReviewerId(reviewer.getUserId());
        reviewRecord.setActionDescription(actionDescription);
        reviewRecord.setStatus(status);
        reviewRecord.setFeedbackComment(feedbackComment);
        reviewRecord.setReviewedAt(reviewedAt);
        reviewRecord.setCreatedAt(reviewedAt);
        reviewRecordMapper.insert(reviewRecord);
        return reviewRecord;
    }

    private MockHttpSession loginAndGetSession(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    private long resourceRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resource", Long.class);
        return count == null ? 0L : count;
    }

    private long reviewRecordRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reviewRecord", Long.class);
        return count == null ? 0L : count;
    }

    private record ResourceGraph(AppUser contributor,
                                 Category category,
                                 ResourceType resourceType,
                                 Resource resource,
                                 ResourceSubmission previousSubmission,
                                 ResourceSubmission latestSubmission) {
    }
}
