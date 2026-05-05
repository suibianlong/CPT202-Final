package com.cpt202.HerLink.controller;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.ContributorRequest;
import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.mapper.AppUserMapper;
import com.cpt202.HerLink.mapper.ContributorRequestMapper;
import com.cpt202.HerLink.util.PasswordHashService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("AdminContributorRequestController integration test")
class AdminContributorRequestControllerIntegrationTest {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("herlink_admin_contributor_request_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("sql/admin-contributor-request-integration-schema.sql")
            .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("HerLink.demo-data-enabled", () -> "false");
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
    private PasswordHashService passwordHashService;

    private Long adminUserId;
    private Long viewerUserId;
    private Long approvedContributorUserId;
    private Long pendingApplicantUserId;
    private Long approvedRequestId;
    private Long pendingRequestId;

    @BeforeEach
    void setUp() {
        cleanupDatabase();
        seedData();
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    @Nested
    @DisplayName("GET /api/admin/contributor-requests/pending")
    class ListPendingRequestsTests {

        @Test
        @DisplayName("Should return pending contributor requests for an admin")
        void shouldReturnPendingContributorRequestsForAdmin() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(get("/api/admin/contributor-requests/pending").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].requestId").value(pendingRequestId))
                    .andExpect(jsonPath("$[0].userId").value(pendingApplicantUserId))
                    .andExpect(jsonPath("$[0].userName").value("Pending Applicant"))
                    .andExpect(jsonPath("$[0].userEmail").value("pending@example.com"))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

            assertEquals("PENDING", contributorRequestMapper.selectLatestByUserId(pendingApplicantUserId).getStatus());
        }

        @Test
        @DisplayName("Should return an empty list when no pending requests exist")
        void shouldReturnEmptyListWhenNoPendingRequestsExist() throws Exception {
            jdbcTemplate.update("DELETE FROM contributorApplication WHERE applicationId = ?", pendingRequestId);
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(get("/api/admin/contributor-requests/pending").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should return 401 when the admin session is missing")
        void shouldReturnUnauthorizedWhenSessionIsMissing() throws Exception {
            mockMvc.perform(get("/api/admin/contributor-requests/pending"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when a non admin user requests pending contributor applications")
        void shouldReturnForbiddenWhenNonAdminRequestsPendingApplications() throws Exception {
            MockHttpSession session = loginAsViewer();

            mockMvc.perform(get("/api/admin/contributor-requests/pending").session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/contributor-requests/{requestId}")
    class GetRequestDetailTests {

        @Test
        @DisplayName("Should return contributor request detail for an admin")
        void shouldReturnContributorRequestDetailForAdmin() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(get("/api/admin/contributor-requests/{requestId}", approvedRequestId).session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(approvedRequestId))
                    .andExpect(jsonPath("$.userId").value(approvedContributorUserId))
                    .andExpect(jsonPath("$.userName").value("Approved Contributor"))
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            assertNotNull(contributorRequestMapper.selectById(approvedRequestId));
        }

        @Test
        @DisplayName("Should return 400 when request id is malformed")
        void shouldReturnBadRequestWhenRequestIdIsMalformed() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(get("/api/admin/contributor-requests/not-a-number").session(session))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when request detail is requested without login")
        void shouldReturnUnauthorizedWhenRequestDetailIsRequestedWithoutLogin() throws Exception {
            mockMvc.perform(get("/api/admin/contributor-requests/{requestId}", approvedRequestId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when a viewer requests request detail")
        void shouldReturnForbiddenWhenViewerRequestsRequestDetail() throws Exception {
            MockHttpSession session = loginAsViewer();

            mockMvc.perform(get("/api/admin/contributor-requests/{requestId}", approvedRequestId).session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."));
        }

        @Test
        @DisplayName("Should return 404 when the contributor request does not exist")
        void shouldReturnNotFoundWhenContributorRequestDoesNotExist() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(get("/api/admin/contributor-requests/{requestId}", 999999L).session(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Contributor request does not exist."));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/contributor-requests/approved-contributors")
    class ListApprovedContributorsTests {

        @Test
        @DisplayName("Should return approved contributors for an admin")
        void shouldReturnApprovedContributorsForAdmin() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(get("/api/admin/contributor-requests/approved-contributors").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(approvedContributorUserId))
                    .andExpect(jsonPath("$[0].userName").value("Approved Contributor"))
                    .andExpect(jsonPath("$[0].status").value("APPROVED"));

            assertTrue(appUserMapper.selectById(approvedContributorUserId).getContributor());
        }

        @Test
        @DisplayName("Should return an empty list when there are no approved contributors")
        void shouldReturnEmptyListWhenThereAreNoApprovedContributors() throws Exception {
            jdbcTemplate.update("UPDATE `user` SET isContributor = 0 WHERE userId = ?", approvedContributorUserId);
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(get("/api/admin/contributor-requests/approved-contributors").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should return 401 when approved contributors are requested without login")
        void shouldReturnUnauthorizedWhenApprovedContributorsAreRequestedWithoutLogin() throws Exception {
            mockMvc.perform(get("/api/admin/contributor-requests/approved-contributors"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when a viewer requests approved contributors")
        void shouldReturnForbiddenWhenViewerRequestsApprovedContributors() throws Exception {
            MockHttpSession session = loginAsViewer();

            mockMvc.perform(get("/api/admin/contributor-requests/approved-contributors").session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/contributor-requests/{requestId}/decision")
    class ReviewRequestTests {

        @Test
        @DisplayName("Should approve a pending contributor request and persist the approval")
        void shouldApprovePendingContributorRequestAndPersistApproval() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(post("/api/admin/contributor-requests/{requestId}/decision", pendingRequestId)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "reviewComment": "Approved after review"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(pendingRequestId))
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.reviewComment").value("Approved after review"))
                    .andExpect(jsonPath("$.reviewedBy").value(adminUserId));

            ContributorRequest reviewed = contributorRequestMapper.selectById(pendingRequestId);
            assertEquals("APPROVED", reviewed.getStatus());
            assertEquals(adminUserId, reviewed.getReviewedBy());
            assertTrue(appUserMapper.selectById(pendingApplicantUserId).getContributor());
        }

        @Test
        @DisplayName("Should reject a pending contributor request and persist the rejection")
        void shouldRejectPendingContributorRequestAndPersistRejection() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(post("/api/admin/contributor-requests/{requestId}/decision", pendingRequestId)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "REJECTED",
                                      "reviewComment": "Needs more experience"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.reviewComment").value("Needs more experience"))
                    .andExpect(jsonPath("$.reviewedBy").value(adminUserId));

            ContributorRequest reviewed = contributorRequestMapper.selectById(pendingRequestId);
            assertEquals("REJECTED", reviewed.getStatus());
            assertTrue(!appUserMapper.selectById(pendingApplicantUserId).getContributor());
        }

        @Test
        @DisplayName("Should return 400 when the decision payload is malformed")
        void shouldReturnBadRequestWhenDecisionPayloadIsMalformed() throws Exception {
            MockHttpSession session = loginAsAdmin();
            ContributorRequest before = contributorRequestMapper.selectById(pendingRequestId);

            mockMvc.perform(post("/api/admin/contributor-requests/{requestId}/decision", pendingRequestId)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest());

            ContributorRequest after = contributorRequestMapper.selectById(pendingRequestId);
            assertEquals(before.getStatus(), after.getStatus());
        }

        @Test
        @DisplayName("Should return 400 when the decision value is invalid")
        void shouldReturnBadRequestWhenDecisionValueIsInvalid() throws Exception {
            MockHttpSession session = loginAsAdmin();
            ContributorRequest before = contributorRequestMapper.selectById(pendingRequestId);

            mockMvc.perform(post("/api/admin/contributor-requests/{requestId}/decision", pendingRequestId)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "INVALID",
                                      "reviewComment": "Unsupported choice"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Decision must be APPROVED or REJECTED."));

            ContributorRequest after = contributorRequestMapper.selectById(pendingRequestId);
            assertEquals(before.getStatus(), after.getStatus());
        }

        @Test
        @DisplayName("Should return 401 when review is requested without login")
        void shouldReturnUnauthorizedWhenReviewIsRequestedWithoutLogin() throws Exception {
            mockMvc.perform(post("/api/admin/contributor-requests/{requestId}/decision", pendingRequestId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "reviewComment": "Approved"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when a viewer tries to review a contributor request")
        void shouldReturnForbiddenWhenViewerTriesToReviewContributorRequest() throws Exception {
            MockHttpSession session = loginAsViewer();

            mockMvc.perform(post("/api/admin/contributor-requests/{requestId}/decision", pendingRequestId)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "reviewComment": "Approved"
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."));
        }

        @Test
        @DisplayName("Should return 404 when the contributor request to review does not exist")
        void shouldReturnNotFoundWhenContributorRequestToReviewDoesNotExist() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(post("/api/admin/contributor-requests/{requestId}/decision", 999999L)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "reviewComment": "Approved"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Contributor request does not exist."));
        }

        @Test
        @DisplayName("Should return 409 when an already reviewed request is reviewed again")
        void shouldReturnConflictWhenAlreadyReviewedRequestIsReviewedAgain() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(post("/api/admin/contributor-requests/{requestId}/decision", approvedRequestId)
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "decision": "APPROVED",
                                      "reviewComment": "Second review attempt"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Only pending contributor requests can be reviewed."));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/contributor-requests/contributors/{userId}/revoke")
    class RevokeContributorTests {

        @Test
        @DisplayName("Should revoke contributor access and persist the admin operation history")
        void shouldRevokeContributorAccessAndPersistAdminOperationHistory() throws Exception {
            MockHttpSession session = loginAsAdmin();
            int historyCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM adminOperationHistory", Integer.class);

            mockMvc.perform(post("/api/admin/contributor-requests/contributors/{userId}/revoke", approvedContributorUserId)
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(approvedContributorUserId))
                    .andExpect(jsonPath("$.userName").value("Approved Contributor"));

            AppUser user = appUserMapper.selectById(approvedContributorUserId);
            assertNotNull(user);
            assertTrue(!user.getContributor());
            Integer historyCountAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM adminOperationHistory", Integer.class);
            assertEquals(historyCountBefore + 1, historyCountAfter);
        }

        @Test
        @DisplayName("Should return 401 when revoke is requested without login")
        void shouldReturnUnauthorizedWhenRevokeIsRequestedWithoutLogin() throws Exception {
            mockMvc.perform(post("/api/admin/contributor-requests/contributors/{userId}/revoke", approvedContributorUserId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Please log in first."));
        }

        @Test
        @DisplayName("Should return 403 when a viewer tries to revoke contributor access")
        void shouldReturnForbiddenWhenViewerTriesToRevokeContributorAccess() throws Exception {
            MockHttpSession session = loginAsViewer();

            mockMvc.perform(post("/api/admin/contributor-requests/contributors/{userId}/revoke", approvedContributorUserId)
                            .session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Administrator permission is required."));
        }

        @Test
        @DisplayName("Should return 400 when the contributor user id is malformed")
        void shouldReturnBadRequestWhenContributorUserIdIsMalformed() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(post("/api/admin/contributor-requests/contributors/not-a-number/revoke").session(session))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when the contributor user does not exist")
        void shouldReturnNotFoundWhenContributorUserDoesNotExist() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(post("/api/admin/contributor-requests/contributors/{userId}/revoke", 999999L)
                            .session(session))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User does not exist."));
        }

        @Test
        @DisplayName("Should return 409 when an administrator tries to revoke their own contributor state")
        void shouldReturnConflictWhenAdministratorTriesToRevokeOwnContributorState() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(post("/api/admin/contributor-requests/contributors/{userId}/revoke", adminUserId)
                            .session(session))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Administrators cannot revoke their own contributor state through this action."));
        }

        @Test
        @DisplayName("Should return 409 when the target user is not currently an approved contributor")
        void shouldReturnConflictWhenTargetUserIsNotApprovedContributor() throws Exception {
            MockHttpSession session = loginAsAdmin();

            mockMvc.perform(post("/api/admin/contributor-requests/contributors/{userId}/revoke", viewerUserId)
                            .session(session))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("This user is not currently an approved contributor."));
        }
    }

    private MockHttpSession loginAsAdmin() throws Exception {
        return login("admin@example.com", "Admin123!");
    }

    private MockHttpSession loginAsViewer() throws Exception {
        return login("viewer@example.com", "Viewer123!");
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

    private void seedData() {
        LocalDateTime now = LocalDateTime.now();

        adminUserId = insertUser("Admin User", "admin@example.com", "Admin123!", UserRoleEnum.ADMINISTRATOR.getValue(), false);
        viewerUserId = insertUser("Viewer User", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER.getValue(), false);
        approvedContributorUserId = insertUser("Approved Contributor", "approved@example.com", "Contributor123!", UserRoleEnum.REGISTERED_VIEWER.getValue(), true);
        pendingApplicantUserId = insertUser("Pending Applicant", "pending@example.com", "Pending123!", UserRoleEnum.REGISTERED_VIEWER.getValue(), false);

        approvedRequestId = insertContributorRequest(approvedContributorUserId, "I already curate heritage resources.", "APPROVED", adminUserId, now.minusDays(2), now.minusDays(1), "Approved");
        pendingRequestId = insertContributorRequest(pendingApplicantUserId, "I would like to contribute local stories.", "PENDING", null, now.minusDays(1), null, null);
    }

    private Long insertUser(String name, String email, String password, String role, boolean contributor) {
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
        return user.getUserId();
    }

    private Long insertContributorRequest(Long userId,
                                          String reason,
                                          String status,
                                          Long reviewedBy,
                                          LocalDateTime submittedAt,
                                          LocalDateTime reviewedAt,
                                          String reviewComment) {
        ContributorRequest request = new ContributorRequest();
        request.setUserId(userId);
        request.setApplicationReason(reason);
        request.setStatus(status);
        request.setRequestedAt(submittedAt);
        request.setReviewedBy(reviewedBy);
        request.setReviewedAt(reviewedAt);
        request.setReviewComment(reviewComment);
        request.setUpdatedAt(reviewedAt == null ? submittedAt : reviewedAt);
        contributorRequestMapper.insert(request);
        return request.getRequestId();
    }

    private void cleanupDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.update("DELETE FROM adminOperationHistory");
        jdbcTemplate.update("DELETE FROM contributorApplication");
        jdbcTemplate.update("DELETE FROM `user`");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
