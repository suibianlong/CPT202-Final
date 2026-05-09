package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.ContributorRequest;
import com.cpt202.HerLink.enums.ContributorApplicationStatusEnum;
import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.mapper.AppUserMapper;
import com.cpt202.HerLink.mapper.ContributorRequestMapper;
import com.cpt202.HerLink.util.PasswordHashService;
import java.time.LocalDateTime;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for {@link ContributorRequestController}.
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
@TestMethodOrder(MethodName.class)
@DisplayName("ContributorRequestController integration tests")
public class ContributorRequestControllerIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("herlink_contributor_request_test")
            .withUsername("herlink")
            .withPassword("herlink")
            .withInitScript("sql/auth-controller-test-schema.sql");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppUserMapper appUserMapper;

    @Autowired
    private ContributorRequestMapper contributorRequestMapper;

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
        jdbcTemplate.update("DELETE FROM contributorApplication");
        jdbcTemplate.update("DELETE FROM `user`");
        jdbcTemplate.execute("ALTER TABLE contributorApplication AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE `user` AUTO_INCREMENT = 1");
    }

    @Nested
    @DisplayName("POST /api/contributor-requests")
    class SubmitContributorRequestTests {

        @Test
        @DisplayName("Should create a pending contributor request for a logged in registered viewer")
        void shouldCreatePendingContributorRequestForRegisteredViewer() throws Exception {
            AppUser viewer = persistUser(
                    "viewer-user",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    "Viewer bio"
            );
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(post("/api/contributor-requests")
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I want to contribute verified archive materials."
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.requestId").isNumber())
                    .andExpect(jsonPath("$.userId").value(viewer.getUserId()))
                    .andExpect(jsonPath("$.userName").value("viewer-user"))
                    .andExpect(jsonPath("$.userEmail").value("viewer@example.com"))
                    .andExpect(jsonPath("$.applicationReason").value("I want to contribute verified archive materials."))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            ContributorRequest savedRequest = contributorRequestMapper.selectLatestByUserId(viewer.getUserId());
            assertThat(savedRequest).isNotNull();
            assertThat(savedRequest.getUserId()).isEqualTo(viewer.getUserId());
            assertThat(savedRequest.getApplicationReason()).isEqualTo("I want to contribute verified archive materials.");
            assertThat(savedRequest.getStatus()).isEqualTo(ContributorApplicationStatusEnum.PENDING.getValue());
            assertThat(savedRequest.getReviewedAt()).isNull();
            assertThat(savedRequest.getReviewedBy()).isNull();
            assertThat(contributorApplicationRowCount()).isEqualTo(1);
            assertThat(appUserMapper.selectById(viewer.getUserId()).getContributor()).isFalse();
        }

        @Test
        @DisplayName("Should return 401 when submitting a contributor request without login")
        void shouldReturnUnauthorizedWhenSubmittingWithoutLogin() throws Exception {
            mockMvc.perform(post("/api/contributor-requests")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I would like to contribute."
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));

            assertThat(contributorApplicationRowCount()).isZero();
            assertThat(userRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 403 when a reviewer account submits a contributor request")
        void shouldReturnForbiddenWhenReviewerSubmitsContributorRequest() throws Exception {
            persistUser(
                    "reviewer-user",
                    "reviewer@example.com",
                    "Reviewer123!",
                    UserRoleEnum.ADMINISTRATOR,
                    false,
                    "Reviewer bio"
            );
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");
            long beforeRequestCount = contributorApplicationRowCount();

            mockMvc.perform(post("/api/contributor-requests")
                            .session(reviewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I would like to contribute."
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("Only registered viewers can submit contributor requests."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));

            assertThat(contributorApplicationRowCount()).isEqualTo(beforeRequestCount);
            assertThat(userRowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 400 when contributor request JSON is malformed")
        void shouldReturnBadRequestWhenSubmitJsonIsMalformed() throws Exception {
            persistUser(
                    "viewer-user",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");
            long beforeRequestCount = contributorApplicationRowCount();

            mockMvc.perform(post("/api/contributor-requests")
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));

            assertThat(contributorApplicationRowCount()).isEqualTo(beforeRequestCount);
        }

        @Test
        @DisplayName("Should return 400 and keep database unchanged when application reason is missing")
        void shouldReturnBadRequestWhenApplicationReasonIsMissing() throws Exception {
            AppUser viewer = persistUser(
                    "viewer-user",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");
            long beforeRequestCount = contributorApplicationRowCount();

            mockMvc.perform(post("/api/contributor-requests")
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "   "
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please correct your contributor application."))
                    .andExpect(jsonPath("$.details[0]").value("Application reason is required."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));

            assertThat(contributorRequestMapper.selectLatestByUserId(viewer.getUserId())).isNull();
            assertThat(contributorApplicationRowCount()).isEqualTo(beforeRequestCount);
        }

        @Test
        @DisplayName("Should return 400 and keep database unchanged when application reason exceeds the max length")
        void shouldReturnBadRequestWhenApplicationReasonTooLong() throws Exception {
            AppUser viewer = persistUser(
                    "viewer-user",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");
            long beforeRequestCount = contributorApplicationRowCount();

            mockMvc.perform(post("/api/contributor-requests")
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "%s"
                                    }
                                    """.formatted("x".repeat(2001))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please correct your contributor application."))
                    .andExpect(jsonPath("$.details[0]").value("Application reason must be 2000 characters or fewer."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));

            assertThat(contributorRequestMapper.selectLatestByUserId(viewer.getUserId())).isNull();
            assertThat(contributorApplicationRowCount()).isEqualTo(beforeRequestCount);
        }

        @Test
        @DisplayName("Should return 409 when the user already has a pending contributor request")
        void shouldReturnConflictWhenPendingContributorRequestAlreadyExists() throws Exception {
            AppUser viewer = persistUser(
                    "viewer-user",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            persistContributorRequest(
                    viewer,
                    "Existing pending request.",
                    ContributorApplicationStatusEnum.PENDING.getValue(),
                    null,
                    null,
                    null
            );
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");
            long beforeRequestCount = contributorApplicationRowCount();

            mockMvc.perform(post("/api/contributor-requests")
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I would like to contribute."
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Your existing contributor request is still under review."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));

            assertThat(contributorApplicationRowCount()).isEqualTo(beforeRequestCount);
            assertThat(contributorRequestMapper.selectLatestByUserId(viewer.getUserId()).getApplicationReason())
                    .isEqualTo("Existing pending request.");
        }

        @Test
        @DisplayName("Should return 409 when an approved contributor submits another request")
        void shouldReturnConflictWhenApprovedContributorSubmitsAgain() throws Exception {
            AppUser viewer = persistUser(
                    "approved-contributor",
                    "contributor@example.com",
                    "Contributor123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    true,
                    "Contributor bio"
            );
            persistContributorRequest(
                    viewer,
                    "Approved already.",
                    ContributorApplicationStatusEnum.APPROVED.getValue(),
                    "Approved already.",
                    viewer.getUserId(),
                    LocalDateTime.now().minusDays(1)
            );
            MockHttpSession viewerSession = loginAndGetSession("contributor@example.com", "Contributor123!");
            long beforeRequestCount = contributorApplicationRowCount();

            mockMvc.perform(post("/api/contributor-requests")
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I want to apply again."
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("You are already an approved contributor."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));

            assertThat(contributorApplicationRowCount()).isEqualTo(beforeRequestCount);
            assertThat(appUserMapper.selectById(viewer.getUserId()).getContributor()).isTrue();
        }

        @Test
        @DisplayName("Should allow a new submission after the previous request was rejected")
        void shouldAllowNewSubmissionAfterRejectedRequest() throws Exception {
            AppUser viewer = persistUser(
                    "viewer-user",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            persistContributorRequest(
                    viewer,
                    "Please improve the application.",
                    ContributorApplicationStatusEnum.REJECTED.getValue(),
                    "Please improve the application.",
                    viewer.getUserId(),
                    LocalDateTime.now().minusDays(2)
            );
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(post("/api/contributor-requests")
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I have updated experience details and evidence."
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(viewer.getUserId()))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.applicationReason").value("I have updated experience details and evidence."));

            ContributorRequest latestRequest = contributorRequestMapper.selectLatestByUserId(viewer.getUserId());
            assertThat(latestRequest).isNotNull();
            assertThat(latestRequest.getStatus()).isEqualTo(ContributorApplicationStatusEnum.PENDING.getValue());
            assertThat(latestRequest.getApplicationReason()).isEqualTo("I have updated experience details and evidence.");
            assertThat(contributorApplicationRowCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return 401 when the session user has been deleted before submitting a contributor request")
        void shouldReturnUnauthorizedWhenSubmittingWithDeletedSessionUser() throws Exception {
            AppUser viewer = persistUser(
                    "viewer-user",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");
            jdbcTemplate.update("DELETE FROM `user` WHERE userId = ?", viewer.getUserId());

            mockMvc.perform(post("/api/contributor-requests")
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "applicationReason": "I would like to contribute."
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Your session is no longer valid. Please log in again."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests"));

            assertThat(userRowCount()).isZero();
            assertThat(contributorApplicationRowCount()).isZero();
        }
    }

    @Nested
    @DisplayName("GET /api/contributor-requests/me")
    class GetMyLatestContributorRequestTests {

        @Test
        @DisplayName("Should return the latest contributor request for the logged in user")
        void shouldReturnLatestContributorRequestForCurrentUser() throws Exception {
            AppUser viewer = persistUser(
                    "viewer-user",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    "Viewer bio"
            );
            persistContributorRequest(
                    viewer,
                    "Older rejected reason.",
                    ContributorApplicationStatusEnum.REJECTED.getValue(),
                    "Older request review.",
                    viewer.getUserId(),
                    LocalDateTime.now().minusDays(2)
            );
            ContributorRequest latestRequest = persistContributorRequest(
                    viewer,
                    "Latest pending reason.",
                    ContributorApplicationStatusEnum.PENDING.getValue(),
                    null,
                    null,
                    null
            );
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(get("/api/contributor-requests/me").session(viewerSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(latestRequest.getRequestId()))
                    .andExpect(jsonPath("$.userId").value(viewer.getUserId()))
                    .andExpect(jsonPath("$.userName").value("viewer-user"))
                    .andExpect(jsonPath("$.userEmail").value("viewer@example.com"))
                    .andExpect(jsonPath("$.applicationReason").value("Latest pending reason."))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            assertThat(contributorRequestMapper.selectLatestByUserId(viewer.getUserId()).getRequestId())
                    .isEqualTo(latestRequest.getRequestId());
            assertThat(contributorApplicationRowCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return 200 with an empty body when the user has no contributor request")
        void shouldReturnEmptyBodyWhenNoContributorRequestExists() throws Exception {
            persistUser(
                    "viewer-user",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(get("/api/contributor-requests/me").session(viewerSession))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));

            assertThat(contributorApplicationRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 401 when reading the latest contributor request without login")
        void shouldReturnUnauthorizedWhenReadingLatestRequestWithoutLogin() throws Exception {
            mockMvc.perform(get("/api/contributor-requests/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests/me"));

            assertThat(contributorApplicationRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 401 when the session points to a user that has been deleted")
        void shouldReturnUnauthorizedWhenCurrentUserNoLongerExists() throws Exception {
            AppUser viewer = persistUser(
                    "viewer-user",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");
            jdbcTemplate.update("DELETE FROM `user` WHERE userId = ?", viewer.getUserId());

            mockMvc.perform(get("/api/contributor-requests/me").session(viewerSession))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Your session is no longer valid. Please log in again."))
                    .andExpect(jsonPath("$.path").value("/api/contributor-requests/me"));

            assertThat(userRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return the latest request even when the logged in account is a reviewer")
        void shouldReturnLatestRequestForReviewerAccountWhenHistoryExists() throws Exception {
            AppUser reviewer = persistUser(
                    "reviewer-user",
                    "reviewer@example.com",
                    "Reviewer123!",
                    UserRoleEnum.ADMINISTRATOR,
                    false,
                    null
            );
            ContributorRequest latestRequest = persistContributorRequest(
                    reviewer,
                    "Historical reviewer reason.",
                    ContributorApplicationStatusEnum.REJECTED.getValue(),
                    "Historical reviewer request.",
                    reviewer.getUserId(),
                    LocalDateTime.now().minusDays(1)
            );
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(get("/api/contributor-requests/me").session(reviewerSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(latestRequest.getRequestId()))
                    .andExpect(jsonPath("$.userId").value(reviewer.getUserId()))
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.reviewComment").value("Historical reviewer request."));

            assertThat(contributorApplicationRowCount()).isEqualTo(1);
        }
    }

    private AppUser persistUser(String name,
                                String email,
                                String rawPassword,
                                UserRoleEnum role,
                                boolean contributor,
                                String bio) {
        LocalDateTime now = LocalDateTime.now();
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email.toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordHashService.hash(rawPassword));
        user.setRole(role.getValue());
        user.setContributor(contributor);
        user.setBio(bio);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        appUserMapper.insert(user);
        return user;
    }

    private ContributorRequest persistContributorRequest(AppUser user,
                                                         String applicationReason,
                                                         String status,
                                                         String reviewComment,
                                                         Long reviewedBy,
                                                         LocalDateTime reviewedAt) {
        LocalDateTime now = LocalDateTime.now();
        ContributorRequest request = new ContributorRequest();
        request.setUserId(user.getUserId());
        request.setApplicationReason(applicationReason);
        request.setStatus(status);
        request.setRequestedAt(reviewedAt == null ? now : reviewedAt.minusHours(1));
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(reviewedAt);
        request.setReviewComment(reviewComment);
        request.setUpdatedAt(now);
        contributorRequestMapper.insert(request);
        return request;
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

    private long userRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `user`", Long.class);
        return count == null ? 0L : count;
    }

    private long contributorApplicationRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contributorApplication", Long.class);
        return count == null ? 0L : count;
    }
}
