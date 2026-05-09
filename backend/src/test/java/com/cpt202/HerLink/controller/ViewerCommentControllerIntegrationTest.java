package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Comment;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.mapper.AppUserMapper;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.CommentMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for {@link ViewerCommentController}.
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
@DisplayName("ViewerCommentController integration tests")
public class ViewerCommentControllerIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("herlink_viewer_comment_test")
            .withUsername("herlink")
            .withPassword("herlink")
            .withInitScript("sql/contributor-resource-history-integration-schema.sql");

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
    private ResourceMapper resourceMapper;

    @Autowired
    private CommentMapper commentMapper;

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
        jdbcTemplate.update("DELETE FROM `comment`");
        jdbcTemplate.update("DELETE FROM resource");
        jdbcTemplate.update("DELETE FROM contributorApplication");
        jdbcTemplate.update("DELETE FROM category");
        jdbcTemplate.update("DELETE FROM resourceType");
        jdbcTemplate.update("DELETE FROM `user`");

        jdbcTemplate.execute("ALTER TABLE `comment` AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE resource AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE contributorApplication AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE category AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE resourceType AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE `user` AUTO_INCREMENT = 1");
    }

    @Nested
    @DisplayName("GET /api/viewer/resources/{resourceId}/comments")
    class ListCommentsTests {

        @Test
        @DisplayName("Should return comments for an authenticated user on an approved resource")
        void shouldReturnCommentsForApprovedResource() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            AppUser commenter = persistUser("commenter-user", "commenter@example.com", "Commenter123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), commenter);
            Comment olderComment = persistComment(approvedResource.getId(), commenter.getUserId(), "Older comment", LocalDateTime.now().minusMinutes(10));
            Comment latestComment = persistComment(approvedResource.getId(), viewer.getUserId(), "Most recent comment", LocalDateTime.now().minusMinutes(1));
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(get("/api/viewer/resources/{resourceId}/comments", approvedResource.getId())
                            .session(viewerSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(latestComment.getId()))
                    .andExpect(jsonPath("$[0].resourceId").value(approvedResource.getId()))
                    .andExpect(jsonPath("$[0].userId").value(viewer.getUserId()))
                    .andExpect(jsonPath("$[0].userName").value("viewer-user"))
                    .andExpect(jsonPath("$[0].content").value("Most recent comment"))
                    .andExpect(jsonPath("$[1].id").value(olderComment.getId()))
                    .andExpect(jsonPath("$[1].userName").value("commenter-user"))
                    .andExpect(jsonPath("$[1].content").value("Older comment"));

            assertThat(commentMapper.selectByResourceId(approvedResource.getId())).hasSize(2);
            assertThat(commentRowCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return an empty array when an approved resource has no comments")
        void shouldReturnEmptyListWhenApprovedResourceHasNoComments() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), viewer);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(get("/api/viewer/resources/{resourceId}/comments", approvedResource.getId())
                            .session(viewerSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 401 when listing comments without login")
        void shouldReturnUnauthorizedWhenListingCommentsWithoutLogin() throws Exception {
            AppUser contributor = persistUser("contributor-user", "contributor@example.com", "Contributor123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), contributor);

            mockMvc.perform(get("/api/viewer/resources/{resourceId}/comments", approvedResource.getId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments".formatted(approvedResource.getId())));

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 400 when list comments resource id path variable is malformed")
        void shouldReturnBadRequestWhenListCommentsResourceIdIsMalformed() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(get("/api/viewer/resources/not-a-number/comments").session(viewerSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/not-a-number/comments"));

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 404 when listing comments for a non approved or missing resource")
        void shouldReturnNotFoundWhenListingCommentsForMissingApprovedResource() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(get("/api/viewer/resources/999/comments").session(viewerSession))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Approved resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/999/comments"));
        }
    }

    @Nested
    @DisplayName("POST /api/viewer/resources/{resourceId}/comments")
    class CreateCommentTests {

        @Test
        @DisplayName("Should create a comment on an approved resource and persist it")
        void shouldCreateCommentSuccessfully() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            AppUser contributor = persistUser("contributor-user", "contributor@example.com", "Contributor123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), contributor);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(post("/api/viewer/resources/{resourceId}/comments", approvedResource.getId())
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "  This archive is very useful.  "
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.resourceId").value(approvedResource.getId()))
                    .andExpect(jsonPath("$.userId").value(viewer.getUserId()))
                    .andExpect(jsonPath("$.userName").value("viewer-user"))
                    .andExpect(jsonPath("$.content").value("This archive is very useful."));

            Comment savedComment = commentMapper.selectByResourceId(approvedResource.getId()).get(0);
            assertThat(savedComment.getUserId()).isEqualTo(viewer.getUserId());
            assertThat(savedComment.getContent()).isEqualTo("This archive is very useful.");
            assertThat(commentRowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 400 when create comment resource id path variable is malformed")
        void shouldReturnBadRequestWhenCreateCommentResourceIdIsMalformed() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(post("/api/viewer/resources/not-a-number/comments")
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "This archive is useful."
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/not-a-number/comments"));

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 401 when creating a comment without login")
        void shouldReturnUnauthorizedWhenCreatingCommentWithoutLogin() throws Exception {
            AppUser contributor = persistUser("contributor-user", "contributor@example.com", "Contributor123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), contributor);

            mockMvc.perform(post("/api/viewer/resources/{resourceId}/comments", approvedResource.getId())
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "This archive is useful."
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments".formatted(approvedResource.getId())));

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 400 when create comment request JSON is malformed")
        void shouldReturnBadRequestWhenCreateCommentJsonIsMalformed() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            AppUser contributor = persistUser("contributor-user", "contributor@example.com", "Contributor123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), contributor);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(post("/api/viewer/resources/{resourceId}/comments", approvedResource.getId())
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments".formatted(approvedResource.getId())));

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 400 and keep database unchanged when comment content field is missing")
        void shouldReturnBadRequestWhenCommentContentFieldIsMissing() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            AppUser contributor = persistUser("contributor-user", "contributor@example.com", "Contributor123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), contributor);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(post("/api/viewer/resources/{resourceId}/comments", approvedResource.getId())
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Comment content cannot be empty."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments".formatted(approvedResource.getId())));

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 400 and keep database unchanged when comment content is blank")
        void shouldReturnBadRequestWhenCommentContentIsBlank() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            AppUser contributor = persistUser("contributor-user", "contributor@example.com", "Contributor123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), contributor);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(post("/api/viewer/resources/{resourceId}/comments", approvedResource.getId())
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "   "
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Comment content cannot be empty."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments".formatted(approvedResource.getId())));

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 400 and keep database unchanged when comment content exceeds the max length")
        void shouldReturnBadRequestWhenCommentContentTooLong() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            AppUser contributor = persistUser("contributor-user", "contributor@example.com", "Contributor123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), contributor);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(post("/api/viewer/resources/{resourceId}/comments", approvedResource.getId())
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "%s"
                                    }
                                    """.formatted("x".repeat(1001))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Comment content cannot exceed 1000 characters."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments".formatted(approvedResource.getId())));

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 404 when creating a comment for a non approved or missing resource")
        void shouldReturnNotFoundWhenCreatingCommentForMissingApprovedResource() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(post("/api/viewer/resources/999/comments")
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "This archive is useful."
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Approved resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/999/comments"));

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 409 and keep database unchanged when the same comment is resubmitted too quickly")
        void shouldReturnConflictWhenSubmittingDuplicateCommentTooQuickly() throws Exception {
            AppUser viewer = persistUser("viewer-user", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), viewer);
            persistComment(approvedResource.getId(), viewer.getUserId(), "Repeated comment", LocalDateTime.now().minusSeconds(5));
            MockHttpSession viewerSession = loginAndGetSession("viewer@example.com", "Viewer123!");
            long beforeCommentCount = commentRowCount();

            mockMvc.perform(post("/api/viewer/resources/{resourceId}/comments", approvedResource.getId())
                            .session(viewerSession)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "content": "Repeated comment"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Please wait before submitting the same comment again."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments".formatted(approvedResource.getId())));

            assertThat(commentRowCount()).isEqualTo(beforeCommentCount);
        }
    }

    @Nested
    @DisplayName("DELETE /api/viewer/resources/{resourceId}/comments/{commentId}")
    class DeleteCommentTests {

        @Test
        @DisplayName("Should delete a comment when the current user owns it")
        void shouldDeleteCommentWhenCurrentUserOwnsIt() throws Exception {
            AppUser owner = persistUser("owner-user", "owner@example.com", "Owner123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), owner);
            Comment comment = persistComment(approvedResource.getId(), owner.getUserId(), "Owner comment", LocalDateTime.now().minusMinutes(1));
            MockHttpSession ownerSession = loginAndGetSession("owner@example.com", "Owner123!");

            mockMvc.perform(delete("/api/viewer/resources/{resourceId}/comments/{commentId}", approvedResource.getId(), comment.getId())
                            .session(ownerSession))
                    .andExpect(status().isNoContent());

            assertThat(commentMapper.selectById(comment.getId())).isNull();
            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should delete a comment when the current user is an administrator")
        void shouldDeleteCommentWhenCurrentUserIsAdministrator() throws Exception {
            AppUser owner = persistUser("owner-user", "owner@example.com", "Owner123!", UserRoleEnum.REGISTERED_VIEWER);
            AppUser reviewer = persistUser("reviewer-user", "reviewer@example.com", "Reviewer123!", UserRoleEnum.ADMINISTRATOR);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), owner);
            Comment comment = persistComment(approvedResource.getId(), owner.getUserId(), "Comment to remove", LocalDateTime.now().minusMinutes(1));
            MockHttpSession reviewerSession = loginAndGetSession("reviewer@example.com", "Reviewer123!");

            mockMvc.perform(delete("/api/viewer/resources/{resourceId}/comments/{commentId}", approvedResource.getId(), comment.getId())
                            .session(reviewerSession))
                    .andExpect(status().isNoContent());

            assertThat(commentMapper.selectById(comment.getId())).isNull();
            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 401 when deleting a comment without login")
        void shouldReturnUnauthorizedWhenDeletingWithoutLogin() throws Exception {
            AppUser owner = persistUser("owner-user", "owner@example.com", "Owner123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), owner);
            Comment comment = persistComment(approvedResource.getId(), owner.getUserId(), "Owner comment", LocalDateTime.now().minusMinutes(1));
            long beforeCommentCount = commentRowCount();

            mockMvc.perform(delete("/api/viewer/resources/{resourceId}/comments/{commentId}", approvedResource.getId(), comment.getId()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments/%d".formatted(approvedResource.getId(), comment.getId())));

            assertThat(commentRowCount()).isEqualTo(beforeCommentCount);
        }

        @Test
        @DisplayName("Should return 400 when comment id path variable is malformed")
        void shouldReturnBadRequestWhenCommentIdIsMalformed() throws Exception {
            AppUser owner = persistUser("owner-user", "owner@example.com", "Owner123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), owner);
            MockHttpSession ownerSession = loginAndGetSession("owner@example.com", "Owner123!");

            mockMvc.perform(delete("/api/viewer/resources/{resourceId}/comments/not-a-number", approvedResource.getId())
                            .session(ownerSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments/not-a-number".formatted(approvedResource.getId())));
        }

        @Test
        @DisplayName("Should return 400 and keep database unchanged when the comment belongs to another resource")
        void shouldReturnBadRequestWhenCommentDoesNotBelongToResource() throws Exception {
            AppUser owner = persistUser("owner-user", "owner@example.com", "Owner123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource firstResource = persistResource("Approved Resource A", ResourceStatusEnum.APPROVED.getValue(), owner);
            Resource secondResource = persistResource("Approved Resource B", ResourceStatusEnum.APPROVED.getValue(), owner);
            Comment comment = persistComment(firstResource.getId(), owner.getUserId(), "Comment on first resource", LocalDateTime.now().minusMinutes(1));
            MockHttpSession ownerSession = loginAndGetSession("owner@example.com", "Owner123!");
            long beforeCommentCount = commentRowCount();

            mockMvc.perform(delete("/api/viewer/resources/{resourceId}/comments/{commentId}", secondResource.getId(), comment.getId())
                            .session(ownerSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Comment does not belong to this resource."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments/%d".formatted(secondResource.getId(), comment.getId())));

            assertThat(commentRowCount()).isEqualTo(beforeCommentCount);
            assertThat(commentMapper.selectById(comment.getId())).isNotNull();
        }

        @Test
        @DisplayName("Should return 403 when a non owner non administrator deletes a comment")
        void shouldReturnForbiddenWhenDeletingOthersCommentWithoutPermission() throws Exception {
            AppUser owner = persistUser("owner-user", "owner@example.com", "Owner123!", UserRoleEnum.REGISTERED_VIEWER);
            AppUser anotherViewer = persistUser("another-user", "another@example.com", "Another123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), owner);
            Comment comment = persistComment(approvedResource.getId(), owner.getUserId(), "Protected comment", LocalDateTime.now().minusMinutes(1));
            MockHttpSession anotherViewerSession = loginAndGetSession("another@example.com", "Another123!");
            long beforeCommentCount = commentRowCount();

            mockMvc.perform(delete("/api/viewer/resources/{resourceId}/comments/{commentId}", approvedResource.getId(), comment.getId())
                            .session(anotherViewerSession))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.message").value("You do not have permission to delete this comment."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments/%d".formatted(approvedResource.getId(), comment.getId())));

            assertThat(commentRowCount()).isEqualTo(beforeCommentCount);
        }

        @Test
        @DisplayName("Should return 404 when deleting a comment for a missing approved resource")
        void shouldReturnNotFoundWhenDeletingCommentForMissingApprovedResource() throws Exception {
            AppUser owner = persistUser("owner-user", "owner@example.com", "Owner123!", UserRoleEnum.REGISTERED_VIEWER);
            MockHttpSession ownerSession = loginAndGetSession("owner@example.com", "Owner123!");

            mockMvc.perform(delete("/api/viewer/resources/999/comments/1").session(ownerSession))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Approved resource does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/999/comments/1"));

            assertThat(commentRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 404 when the comment does not exist")
        void shouldReturnNotFoundWhenCommentDoesNotExist() throws Exception {
            AppUser owner = persistUser("owner-user", "owner@example.com", "Owner123!", UserRoleEnum.REGISTERED_VIEWER);
            Resource approvedResource = persistResource("Approved Resource", ResourceStatusEnum.APPROVED.getValue(), owner);
            MockHttpSession ownerSession = loginAndGetSession("owner@example.com", "Owner123!");

            mockMvc.perform(delete("/api/viewer/resources/{resourceId}/comments/999", approvedResource.getId())
                            .session(ownerSession))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Comment does not exist."))
                    .andExpect(jsonPath("$.path").value("/api/viewer/resources/%d/comments/999".formatted(approvedResource.getId())));

            assertThat(commentRowCount()).isZero();
        }
    }

    private AppUser persistUser(String name, String email, String rawPassword, UserRoleEnum role) {
        LocalDateTime now = LocalDateTime.now();
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email.toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordHashService.hash(rawPassword));
        user.setRole(role.getValue());
        user.setContributor(false);
        user.setBio(name + " bio");
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

    private Resource persistResource(String title, String status, AppUser contributor) {
        LocalDateTime now = LocalDateTime.now();
        Category category = persistCategory("category-" + title.hashCode());
        ResourceType resourceType = persistResourceType("type-" + Math.abs(title.hashCode()));

        Resource resource = new Resource();
        resource.setContributorId(contributor.getUserId());
        resource.setTitle(title);
        resource.setDescription("Description for " + title);
        resource.setCopyright("Contributor-owned");
        resource.setCategoryId(category.getCategoryId());
        resource.setPlace("Liverpool");
        resource.setPreviewImage("/images/" + Math.abs(title.hashCode()) + ".png");
        resource.setMediaUrl("/media/" + Math.abs(title.hashCode()) + ".pdf");
        resource.setStatus(status);
        resource.setReviewedAt(ResourceStatusEnum.APPROVED.getValue().equals(status) ? now.minusHours(1) : null);
        resource.setCreatedAt(now.minusDays(5));
        resource.setUpdatedAt(now.minusMinutes(5));
        resource.setArchivedAt(null);
        resource.setResourceTypeId(resourceType.getResourceTypeId());
        resourceMapper.insert(resource);
        return resource;
    }

    private Comment persistComment(Long resourceId, Long userId, String content, LocalDateTime createdAt) {
        Comment comment = new Comment();
        comment.setResourceId(resourceId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreatedAt(createdAt);
        commentMapper.insert(comment);
        return commentMapper.selectById(comment.getId());
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

    private long commentRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `comment`", Long.class);
        return count == null ? 0L : count;
    }
}
