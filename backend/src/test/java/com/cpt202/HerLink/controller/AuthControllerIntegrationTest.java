package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.ContributorRequest;
import com.cpt202.HerLink.enums.ContributorApplicationStatusEnum;
import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.mapper.AppUserMapper;
import com.cpt202.HerLink.mapper.ContributorRequestMapper;
import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.util.PasswordHashService;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for {@link AuthController}.
 * This project uses MyBatis and HttpSession-based authentication rather than JWT.
 * AuthController has no role-restricted endpoints, so 403 scenarios are not applicable here.
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
@DisplayName("AuthController integration tests")
class AuthControllerIntegrationTest {

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("(\\d{6})");

    @Container
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("herlink_test")
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

    @Autowired
    private UserAccessService userAccessService;

    @MockBean
    private JavaMailSender javaMailSender;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.mail.host", () -> "mail.test.local");
        registry.add("HerLink.register-verification.from-address", () -> "noreply@test.local");
        registry.add("HerLink.notification.from-address", () -> "noreply@test.local");
    }

    @BeforeEach
    void setUp() throws Exception {
        reset(javaMailSender);
        clearRegisterVerificationCodes();
        jdbcTemplate.update("DELETE FROM contributorApplication");
        jdbcTemplate.update("DELETE FROM `user`");
        jdbcTemplate.execute("ALTER TABLE contributorApplication AUTO_INCREMENT = 1");
        jdbcTemplate.execute("ALTER TABLE `user` AUTO_INCREMENT = 1");
    }

    @Nested
    @DisplayName("POST /api/auth/register-verification-code")
    class RegisterVerificationCodeTests {

        @Test
        @DisplayName("Should send a verification email and keep database unchanged for a valid email")
        void shouldSendVerificationEmailWhenRequestIsValid() throws Exception {
            assertThat(userRowCount()).isZero();

            mockMvc.perform(post("/api/auth/register-verification-code")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "new.user@example.com"
                                    }
                                    """))
                    .andExpect(status().isNoContent());

            SimpleMailMessage sentMail = captureSentMail();
            assertThat(sentMail.getTo()).containsExactly("new.user@example.com");
            assertThat(sentMail.getSubject()).isEqualTo("HerLink registration verification code");
            assertThat(extractVerificationCode(sentMail.getText())).matches("\\d{6}");
            assertThat(registerVerificationCodeStore()).containsKey("new.user@example.com");
            assertThat(userRowCount()).isZero();
            assertThat(contributorApplicationRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 400 when the email format is invalid")
        void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
            mockMvc.perform(post("/api/auth/register-verification-code")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "not-an-email"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please correct the email address."))
                    .andExpect(jsonPath("$.details[0]").value("Please enter a valid email address."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register-verification-code"));

            assertThat(registerVerificationCodeStore()).isEmpty();
            assertThat(userRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 409 when the email is already registered")
        void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
            persistUser("Existing User", "existing@example.com", "Password123!", UserRoleEnum.REGISTERED_VIEWER, false, null);

            mockMvc.perform(post("/api/auth/register-verification-code")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "existing@example.com"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("The email address is already in use."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register-verification-code"));

            assertThat(registerVerificationCodeStore()).isEmpty();
            assertThat(userRowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 409 when requesting another code before the resend window expires")
        void shouldReturnConflictWhenVerificationCodeIsRequestedTooSoon() throws Exception {
            mockMvc.perform(post("/api/auth/register-verification-code")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "resend@example.com"
                                    }
                                    """))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/auth/register-verification-code")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "resend@example.com"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("Please wait 60 seconds before requesting another verification code."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register-verification-code"));

            assertThat(registerVerificationCodeStore()).hasSize(1);
            assertThat(userRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 400 when the verification email cannot be sent")
        void shouldReturnBadRequestWhenMailSendingFails() throws Exception {
            doThrow(new MailSendException("SMTP unavailable"))
                    .when(javaMailSender).send(any(SimpleMailMessage.class));

            mockMvc.perform(post("/api/auth/register-verification-code")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "mail.failure@example.com"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Unable to send the verification email right now. Please try again later."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register-verification-code"));

            assertThat(registerVerificationCodeStore()).isEmpty();
            assertThat(userRowCount()).isZero();
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should create a registered viewer account when the verification code is valid")
        void shouldRegisterNewUserWhenVerificationCodeIsValid() throws Exception {
            String verificationCode = requestVerificationCode("register.user@example.com");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Register User",
                                      "email": "register.user@example.com",
                                      "password": "Viewer123!",
                                      "confirmPassword": "Viewer123!",
                                      "verificationCode": "%s"
                                    }
                                    """.formatted(verificationCode)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").isNumber())
                    .andExpect(jsonPath("$.name").value("Register User"))
                    .andExpect(jsonPath("$.email").value("register.user@example.com"))
                    .andExpect(jsonPath("$.role").value("REGISTERED_VIEWER"))
                    .andExpect(jsonPath("$.contributorStatus").value("NONE"))
                    .andExpect(jsonPath("$.contributor").value(false));

            AppUser savedUser = appUserMapper.selectByEmail("register.user@example.com");
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getName()).isEqualTo("Register User");
            assertThat(savedUser.getRole()).isEqualTo(UserRoleEnum.REGISTERED_VIEWER.getValue());
            assertThat(savedUser.getContributor()).isFalse();
            assertThat(savedUser.getPasswordHash()).isNotEqualTo("Viewer123!");
            assertThat(passwordHashService.matches("Viewer123!", savedUser.getPasswordHash())).isTrue();
            assertThat(contributorRequestMapper.selectLatestByUserId(savedUser.getUserId())).isNull();
            assertThat(registerVerificationCodeStore()).isEmpty();
            assertThat(userRowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 400 when the registration body is malformed")
        void shouldReturnBadRequestWhenRegistrationJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/auth/register"));

            assertThat(userRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 400 when required registration fields are missing or malformed")
        void shouldReturnBadRequestWhenRegistrationInputIsInvalid() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": " ",
                                      "email": "bad-email",
                                      "password": "short",
                                      "confirmPassword": "",
                                      "verificationCode": "12"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please correct the registration form."))
                    .andExpect(jsonPath("$.details[0]").value("Name is required."))
                    .andExpect(jsonPath("$.details[1]").value("Please enter a valid email address."))
                    .andExpect(jsonPath("$.details[2]").value("Password must be at least 8 characters long."))
                    .andExpect(jsonPath("$.details[3]").value("Please confirm your password."))
                    .andExpect(jsonPath("$.details[4]").value("Verification code must be a 6-digit number."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register"));

            assertThat(userRowCount()).isZero();
        }

        @Test
        @DisplayName("Should return 400 and keep the database unchanged when the verification code is wrong")
        void shouldReturnBadRequestWhenVerificationCodeIsIncorrect() throws Exception {
            requestVerificationCode("wrong.code@example.com");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Wrong Code User",
                                      "email": "wrong.code@example.com",
                                      "password": "Viewer123!",
                                      "confirmPassword": "Viewer123!",
                                      "verificationCode": "999999"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("The verification code is incorrect."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register"));

            assertThat(appUserMapper.selectByEmail("wrong.code@example.com")).isNull();
            assertThat(userRowCount()).isZero();
            assertThat(registerVerificationCodeStore()).containsKey("wrong.code@example.com");
        }

        @Test
        @DisplayName("Should return 409 and keep the existing row unchanged when the email is already registered")
        void shouldReturnConflictWhenRegistrationEmailAlreadyExists() throws Exception {
            AppUser existingUser = persistUser(
                    "Existing User",
                    "duplicate@example.com",
                    "Password123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    "existing bio"
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Another User",
                                      "email": "duplicate@example.com",
                                      "password": "Viewer123!",
                                      "confirmPassword": "Viewer123!",
                                      "verificationCode": "123456"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("The email address is already in use."))
                    .andExpect(jsonPath("$.path").value("/api/auth/register"));

            AppUser savedUser = appUserMapper.selectByEmail("duplicate@example.com");
            assertThat(savedUser).extracting(
                    AppUser::getUserId,
                    AppUser::getName,
                    AppUser::getEmail,
                    AppUser::getBio
            ).containsExactly(existingUser.getUserId(), "Existing User", "duplicate@example.com", "existing bio");
            assertThat(userRowCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should create a session and return current user details when credentials are valid")
        void shouldLoginSuccessfullyWhenCredentialsAreValid() throws Exception {
            AppUser contributor = persistUser(
                    "Approved Contributor",
                    "contributor@example.com",
                    "Contributor123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    true,
                    "Contributor bio"
            );
            ContributorRequest request = persistContributorRequest(
                    contributor,
                    ContributorApplicationStatusEnum.APPROVED.getValue(),
                    "Approved to contribute."
            );

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "contributor@example.com",
                                      "password": "Contributor123!"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(contributor.getUserId()))
                    .andExpect(jsonPath("$.latestContributorRequestId").value(request.getRequestId()))
                    .andExpect(jsonPath("$.name").value("Approved Contributor"))
                    .andExpect(jsonPath("$.email").value("contributor@example.com"))
                    .andExpect(jsonPath("$.role").value("REGISTERED_VIEWER"))
                    .andExpect(jsonPath("$.contributorStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.contributor").value(true))
                    .andReturn();

            MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
            assertThat(session).isNotNull();
            assertThat(userRowCount()).isEqualTo(1);
            assertThat(appUserMapper.selectById(contributor.getUserId())).isNotNull();
        }

        @Test
        @DisplayName("Should return 400 when the login body is malformed")
        void shouldReturnBadRequestWhenLoginJsonIsMalformed() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"));
        }

        @Test
        @DisplayName("Should return 400 when the login form is incomplete")
        void shouldReturnBadRequestWhenLoginFormIsIncomplete() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "",
                                      "password": " "
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please complete the login form."))
                    .andExpect(jsonPath("$.details[0]").value("Email is required."))
                    .andExpect(jsonPath("$.details[1]").value("Password is required."))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"));
        }

        @Test
        @DisplayName("Should return 401 and keep persisted data unchanged when the password is wrong")
        void shouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {
            AppUser viewer = persistUser(
                    "Viewer User",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );

            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "viewer@example.com",
                                      "password": "WrongPassword123!"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid email or password."))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"));

            AppUser savedUser = appUserMapper.selectById(viewer.getUserId());
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getEmail()).isEqualTo("viewer@example.com");
            assertThat(userRowCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Should invalidate the current session without changing database rows")
        void shouldLogoutSuccessfullyWhenSessionExists() throws Exception {
            AppUser viewer = persistUser(
                    "Viewer User",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            MockHttpSession session = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(post("/api/auth/logout").session(session))
                    .andExpect(status().isNoContent());

            assertThat(appUserMapper.selectById(viewer.getUserId())).isNotNull();
            assertThat(userRowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should still return 204 when logout is called without a session")
        void shouldReturnNoContentWhenLogoutIsCalledWithoutSession() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isNoContent());

            assertThat(userRowCount()).isZero();
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class CurrentUserTests {

        @Test
        @DisplayName("Should return the authenticated user loaded from the database")
        void shouldReturnCurrentUserWhenSessionIsValid() throws Exception {
            AppUser contributor = persistUser(
                    "Approved Contributor",
                    "contributor@example.com",
                    "Contributor123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    true,
                    "Contributor bio"
            );
            ContributorRequest request = persistContributorRequest(
                    contributor,
                    ContributorApplicationStatusEnum.APPROVED.getValue(),
                    "Approved to contribute."
            );
            MockHttpSession session = loginAndGetSession("contributor@example.com", "Contributor123!");

            mockMvc.perform(get("/api/auth/me").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(contributor.getUserId()))
                    .andExpect(jsonPath("$.latestContributorRequestId").value(request.getRequestId()))
                    .andExpect(jsonPath("$.name").value("Approved Contributor"))
                    .andExpect(jsonPath("$.email").value("contributor@example.com"))
                    .andExpect(jsonPath("$.bio").value("Contributor bio"))
                    .andExpect(jsonPath("$.role").value("REGISTERED_VIEWER"))
                    .andExpect(jsonPath("$.contributorStatus").value("APPROVED"))
                    .andExpect(jsonPath("$.contributor").value(true));
        }

        @Test
        @DisplayName("Should return 401 when no session is provided")
        void shouldReturnUnauthorizedWhenSessionIsMissing() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/auth/me"));
        }

        @Test
        @DisplayName("Should return 401 when the session points to a user that no longer exists")
        void shouldReturnUnauthorizedWhenSessionUserNoLongerExists() throws Exception {
            AppUser viewer = persistUser(
                    "Viewer User",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            MockHttpSession session = loginAndGetSession("viewer@example.com", "Viewer123!");
            jdbcTemplate.update("DELETE FROM `user` WHERE userId = ?", viewer.getUserId());

            mockMvc.perform(get("/api/auth/me").session(session))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Your session is no longer valid. Please log in again."))
                    .andExpect(jsonPath("$.path").value("/api/auth/me"));

            assertThat(userRowCount()).isZero();
        }
    }

    @Nested
    @DisplayName("PUT /api/auth/account")
    class UpdateAccountTests {

        @Test
        @DisplayName("Should update the authenticated user account and persist the new values")
        void shouldUpdateAccountWhenRequestIsValid() throws Exception {
            AppUser viewer = persistUser(
                    "Viewer User",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    "Old bio"
            );
            MockHttpSession session = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(put("/api/auth/account")
                            .session(session)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Updated Viewer",
                                      "email": "updated.viewer@example.com",
                                      "bio": "Updated bio"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(viewer.getUserId()))
                    .andExpect(jsonPath("$.name").value("Updated Viewer"))
                    .andExpect(jsonPath("$.email").value("updated.viewer@example.com"))
                    .andExpect(jsonPath("$.bio").value("Updated bio"))
                    .andExpect(jsonPath("$.role").value("REGISTERED_VIEWER"))
                    .andExpect(jsonPath("$.contributorStatus").value("NONE"))
                    .andExpect(jsonPath("$.contributor").value(false));

            AppUser updatedUser = appUserMapper.selectById(viewer.getUserId());
            assertThat(updatedUser).extracting(
                    AppUser::getName,
                    AppUser::getEmail,
                    AppUser::getBio,
                    AppUser::getContributor
            ).containsExactly("Updated Viewer", "updated.viewer@example.com", "Updated bio", false);
            assertThat(userRowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 400 when the update request body is malformed")
        void shouldReturnBadRequestWhenUpdateJsonIsMalformed() throws Exception {
            MockHttpSession session = loginAndGetSession(
                    persistUser("Viewer User", "viewer@example.com", "Viewer123!", UserRoleEnum.REGISTERED_VIEWER, false, null)
                            .getEmail(),
                    "Viewer123!"
            );

            mockMvc.perform(put("/api/auth/account")
                            .session(session)
                            .contentType(APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.path").value("/api/auth/account"));
        }

        @Test
        @DisplayName("Should return 401 when updating the account without login")
        void shouldReturnUnauthorizedWhenUpdatingWithoutLogin() throws Exception {
            mockMvc.perform(put("/api/auth/account")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Updated Viewer",
                                      "email": "updated.viewer@example.com",
                                      "bio": "Updated bio"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Please log in first."))
                    .andExpect(jsonPath("$.path").value("/api/auth/account"));
        }

        @Test
        @DisplayName("Should return 400 and keep the row unchanged when account settings are invalid")
        void shouldReturnBadRequestWhenAccountSettingsAreInvalid() throws Exception {
            AppUser viewer = persistUser(
                    "Viewer User",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    "Old bio"
            );
            MockHttpSession session = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(put("/api/auth/account")
                            .session(session)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": " ",
                                      "email": "bad-email",
                                      "bio": "%s"
                                    }
                                    """.formatted("x".repeat(1001))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value("Please correct your account settings."))
                    .andExpect(jsonPath("$.details[0]").value("Name is required."))
                    .andExpect(jsonPath("$.details[1]").value("Please enter a valid email address."))
                    .andExpect(jsonPath("$.details[2]").value("Bio must be 1000 characters or fewer."))
                    .andExpect(jsonPath("$.path").value("/api/auth/account"));

            AppUser unchangedUser = appUserMapper.selectById(viewer.getUserId());
            assertThat(unchangedUser).extracting(
                    AppUser::getName,
                    AppUser::getEmail,
                    AppUser::getBio
            ).containsExactly("Viewer User", "viewer@example.com", "Old bio");
        }

        @Test
        @DisplayName("Should return 409 and keep both rows unchanged when the new email is already used by another account")
        void shouldReturnConflictWhenNewEmailAlreadyExists() throws Exception {
            AppUser viewer = persistUser(
                    "Viewer User",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    "Viewer bio"
            );
            AppUser existingUser = persistUser(
                    "Existing User",
                    "existing@example.com",
                    "Existing123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    "Existing bio"
            );
            MockHttpSession session = loginAndGetSession("viewer@example.com", "Viewer123!");

            mockMvc.perform(put("/api/auth/account")
                            .session(session)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Viewer User",
                                      "email": "existing@example.com",
                                      "bio": "Changed bio"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409))
                    .andExpect(jsonPath("$.message").value("The email address is already in use."))
                    .andExpect(jsonPath("$.path").value("/api/auth/account"));

            assertThat(appUserMapper.selectById(viewer.getUserId()))
                    .extracting(AppUser::getName, AppUser::getEmail, AppUser::getBio)
                    .containsExactly("Viewer User", "viewer@example.com", "Viewer bio");
            assertThat(appUserMapper.selectById(existingUser.getUserId()))
                    .extracting(AppUser::getName, AppUser::getEmail, AppUser::getBio)
                    .containsExactly("Existing User", "existing@example.com", "Existing bio");
            assertThat(userRowCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return 401 when the logged-in account has been deleted")
        void shouldReturnUnauthorizedWhenCurrentUserNoLongerExists() throws Exception {
            AppUser viewer = persistUser(
                    "Viewer User",
                    "viewer@example.com",
                    "Viewer123!",
                    UserRoleEnum.REGISTERED_VIEWER,
                    false,
                    null
            );
            MockHttpSession session = loginAndGetSession("viewer@example.com", "Viewer123!");
            jdbcTemplate.update("DELETE FROM `user` WHERE userId = ?", viewer.getUserId());

            mockMvc.perform(put("/api/auth/account")
                            .session(session)
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Updated Viewer",
                                      "email": "updated.viewer@example.com",
                                      "bio": "Updated bio"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(401))
                    .andExpect(jsonPath("$.message").value("Your session is no longer valid. Please log in again."))
                    .andExpect(jsonPath("$.path").value("/api/auth/account"));

            assertThat(userRowCount()).isZero();
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

    private ContributorRequest persistContributorRequest(AppUser user, String status, String reviewComment) {
        LocalDateTime now = LocalDateTime.now();
        ContributorRequest request = new ContributorRequest();
        request.setUserId(user.getUserId());
        request.setApplicationReason("Application reason for " + user.getName());
        request.setStatus(status);
        request.setRequestedAt(now.minusDays(1));
        request.setReviewedAt(now);
        request.setReviewedBy(user.getUserId());
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

    private String requestVerificationCode(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register-verification-code")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s"
                                }
                                """.formatted(email)))
                .andExpect(status().isNoContent());

        return extractVerificationCode(captureSentMail().getText());
    }

    private SimpleMailMessage captureSentMail() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());
        return messageCaptor.getValue();
    }

    private String extractVerificationCode(String text) {
        assertThat(text).isNotBlank();
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(text);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> registerVerificationCodeStore() throws Exception {
        Object target = AopTestUtils.getTargetObject(userAccessService);
        return (Map<String, Object>) ReflectionTestUtils.getField(target, "registerVerificationCodes");
    }

    private void clearRegisterVerificationCodes() throws Exception {
        registerVerificationCodeStore().clear();
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
