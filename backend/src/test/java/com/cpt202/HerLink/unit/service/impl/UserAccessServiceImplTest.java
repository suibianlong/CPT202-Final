package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.service.impl.*;
import com.cpt202.HerLink.service.impl.UserAccessServiceImpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import com.cpt202.HerLink.dto.auth.AccountUpdateRequest;
import com.cpt202.HerLink.dto.auth.ContributorRequestSubmitRequest;
import com.cpt202.HerLink.dto.auth.ContributorReviewDecisionRequest;
import com.cpt202.HerLink.dto.auth.LoginRequest;
import com.cpt202.HerLink.dto.auth.RegisterRequest;
import com.cpt202.HerLink.dto.auth.RegisterVerificationCodeRequest;
import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.ContributorRequest;
import com.cpt202.HerLink.enums.ContributorApplicationStatusEnum;
import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.AppUserMapper;
import com.cpt202.HerLink.mapper.ContributorRequestMapper;
import com.cpt202.HerLink.service.admin.AdminOperationHistoryService;
import com.cpt202.HerLink.service.notification.EmailNotificationService;
import com.cpt202.HerLink.util.PasswordHashService;
import com.cpt202.HerLink.vo.ContributorRequestVO;
import com.cpt202.HerLink.vo.CurrentUserVO;

@ExtendWith(MockitoExtension.class)
class UserAccessServiceImplTest {

    @Mock
    private AppUserMapper appUserMapper;
    @Mock
    private ContributorRequestMapper contributorRequestMapper;
    @Mock
    private PasswordHashService passwordHashService;
    @Mock
    private org.springframework.beans.factory.ObjectProvider<JavaMailSender> javaMailSenderProvider;
    @Mock
    private EmailNotificationService emailNotificationService;
    @Mock
    private AdminOperationHistoryService adminOperationHistoryService;


    private UserAccessServiceImpl userAccessService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long ADMIN_USER_ID = 2L;
    private static final Long REQUEST_ID = 100L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_NAME = "testUser";
    private static final String TEST_PASSWORD = "12345678";
    private static final String TEST_HASH = "hashedPassword";
    private static final String VALID_CODE = "123456";
    private static final String INVALID_CODE = "1234";
    private static final String APPLICATION_REASON = "I want to be a contributor";
    private static final String REVIEW_COMMENT = "Approved";

    private AppUser testUser;
    private AppUser adminUser;
    private ContributorRequest pendingRequest;
    private ContributorRequest approvedRequest;

    @BeforeEach
    void setUp() {
        userAccessService = new UserAccessServiceImpl(
        appUserMapper,
        contributorRequestMapper,
        passwordHashService,
        javaMailSenderProvider,
        emailNotificationService,
        adminOperationHistoryService,
        "test-host",          // mailHost（随便填，mock不会用到）
        "test@example.com",   // verificationFromAddress
        10,                   // registerCodeExpireMinutes
        60                    // registerCodeResendSeconds
        );
        testUser = new AppUser();
        testUser.setUserId(TEST_USER_ID);
        testUser.setName(TEST_NAME);
        testUser.setEmail(TEST_EMAIL);
        testUser.setPasswordHash(TEST_HASH);
        testUser.setRole(UserRoleEnum.REGISTERED_VIEWER.getValue());
        testUser.setContributor(false);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        adminUser = new AppUser();
        adminUser.setUserId(ADMIN_USER_ID);
        adminUser.setRole(UserRoleEnum.ADMINISTRATOR.getValue());
        adminUser.setContributor(true);

        pendingRequest = new ContributorRequest();
        pendingRequest.setRequestId(REQUEST_ID);
        pendingRequest.setUserId(TEST_USER_ID);
        pendingRequest.setStatus(ContributorApplicationStatusEnum.PENDING.getValue());
        pendingRequest.setApplicationReason(APPLICATION_REASON);
        pendingRequest.setRequestedAt(LocalDateTime.now());
        pendingRequest.setUpdatedAt(LocalDateTime.now());

        approvedRequest = new ContributorRequest();
        approvedRequest.setRequestId(REQUEST_ID);
        approvedRequest.setUserId(TEST_USER_ID);
        approvedRequest.setStatus(ContributorApplicationStatusEnum.APPROVED.getValue());
        approvedRequest.setReviewedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Register Verification Code Feature Test")
    class SendRegisterVerificationCodeTest {

        @Test
        @DisplayName("Normal case: send verification code with valid email successfully")
        void sendCode_WithValidEmail_Success() {
            RegisterVerificationCodeRequest request = new RegisterVerificationCodeRequest();
            request.setEmail(TEST_EMAIL);
            when(appUserMapper.selectByEmail(TEST_EMAIL)).thenReturn(null);
            when(javaMailSenderProvider.getIfAvailable()).thenReturn(mock(JavaMailSender.class));

            assertDoesNotThrow(() -> userAccessService.sendRegisterVerificationCode(request));
        }

        @Test
        @DisplayName("Exception case: null email throws bad request exception")
        void sendCode_WithNullEmail_ThrowException() {
            RegisterVerificationCodeRequest request = new RegisterVerificationCodeRequest();
            request.setEmail(null);

            AppException exception = assertThrows(AppException.class,
                    () -> userAccessService.sendRegisterVerificationCode(request));

            assertEquals("Please correct the email address.", exception.getMessage());
        }

        @Test
        @DisplayName("Exception case: invalid email format throws bad request exception")
        void sendCode_WithInvalidEmailFormat_ThrowException() {
            RegisterVerificationCodeRequest request = new RegisterVerificationCodeRequest();
            request.setEmail("invalid-email");

            AppException exception = assertThrows(AppException.class,
                    () -> userAccessService.sendRegisterVerificationCode(request));

            assertEquals("Please correct the email address.", exception.getMessage());
        }

        @Test
        @DisplayName("Exception case: email already registered throws conflict exception")
        void sendCode_EmailAlreadyExists_ThrowException() {
            RegisterVerificationCodeRequest request = new RegisterVerificationCodeRequest();
            request.setEmail(TEST_EMAIL);
            when(appUserMapper.selectByEmail(TEST_EMAIL)).thenReturn(testUser);
            // 关键：mock 邮件发送器，避免提前抛邮件配置异常
            when(javaMailSenderProvider.getIfAvailable()).thenReturn(mock(JavaMailSender.class));

            AppException exception = assertThrows(AppException.class,
                    () -> userAccessService.sendRegisterVerificationCode(request));

            assertEquals("The email address is already in use.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("User Register Feature Test")
    class RegisterTest {

        @Test
        @DisplayName("Normal case: valid params and correct code complete register")
        void register_WithValidParams_Success() throws Exception {
            // ==============================
            // 核心：强行把验证码放进 Map（永不报错）
            // ==============================
            Field codesField = UserAccessServiceImpl.class.getDeclaredField("registerVerificationCodes");
            codesField.setAccessible(true);
            Map<String, Object> codesMap = (Map<String, Object>) codesField.get(userAccessService);

            // 获取内部类构造器（私有也能构造）
            Class<?> entryClass = Class.forName("com.cpt202.HerLink.service.impl.UserAccessServiceImpl$RegisterVerificationCodeEntry");
            Constructor<?> constructor = entryClass.getDeclaredConstructor(
                String.class,
                LocalDateTime.class,
                LocalDateTime.class
            );
            constructor.setAccessible(true);

            // 构造正确的验证码对象
            Object validEntry = constructor.newInstance(
                VALID_CODE,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusSeconds(60)
            );

            // 放入Map → 这行直接让 "Please send a verification code first" 永远消失
            codesMap.put(TEST_EMAIL, validEntry);

            // ==============================
            // 正常注册流程
            // ==============================
            RegisterRequest request = new RegisterRequest();
            request.setName(TEST_NAME);
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);
            request.setConfirmPassword(TEST_PASSWORD);
            request.setVerificationCode(VALID_CODE);

            when(appUserMapper.selectByEmail(TEST_EMAIL)).thenReturn(null);
            when(appUserMapper.selectByUsername(TEST_NAME)).thenReturn(null);
            when(passwordHashService.hash(TEST_PASSWORD)).thenReturn(TEST_HASH);

            // 这一行现在 100% 不会再抛异常！
            CurrentUserVO vo = userAccessService.register(request);

            // 断言
            assertNotNull(vo);
            assertEquals(TEST_NAME, vo.getName());
            assertEquals(TEST_EMAIL, vo.getEmail());
            assertEquals(UserRoleEnum.REGISTERED_VIEWER.getApiValue(), vo.getRole());
            assertFalse(vo.isContributor());
        }

        @Test
        @DisplayName("Exception case: password length less than 8 throws validation error")
        void register_PasswordTooShort_ThrowException() {
            RegisterRequest request = new RegisterRequest();
            request.setName(TEST_NAME);
            request.setEmail(TEST_EMAIL);
            request.setPassword("123");
            request.setConfirmPassword("123");
            request.setVerificationCode(VALID_CODE);

            AppException exception = assertThrows(AppException.class, () -> userAccessService.register(request));

            // 断言主消息是表单错误
            assertEquals("Please correct the registration form.", exception.getMessage());
            // 断言错误详情里包含具体提示
            assertTrue(exception.getDetails().contains("Password must be at least 8 characters long."));
        }

        @Test
        @DisplayName("Exception case: password and confirm password not match")
        void register_PasswordNotMatch_ThrowException() {
            RegisterRequest request = new RegisterRequest();
            request.setName(TEST_NAME);
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);
            request.setConfirmPassword("different");
            request.setVerificationCode(VALID_CODE);

            AppException exception = assertThrows(AppException.class, () -> userAccessService.register(request));

            // 正确断言：异常 message 是表单错误，details 里包含密码不匹配
            assertEquals("Please correct the registration form.", exception.getMessage());
            assertTrue(exception.getDetails().contains("The password confirmation does not match."));
        }

        @Test
        @DisplayName("Exception case: verification code format invalid")
        void register_InvalidCodeFormat_ThrowException() {
            RegisterRequest request = new RegisterRequest();
            request.setName(TEST_NAME);
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);
            request.setConfirmPassword(TEST_PASSWORD);
            request.setVerificationCode(INVALID_CODE);

            AppException exception = assertThrows(AppException.class, () -> userAccessService.register(request));

            assertEquals("Please correct the registration form.", exception.getMessage());
            assertTrue(exception.getDetails().contains("Verification code must be a 6-digit number."));
        }
    }
    @Nested
    @DisplayName("User Login Feature Test")
    class LoginTest {

        @Test
        @DisplayName("Normal case: correct email and password login success")
        void login_WithValidCredentials_Success() {
            LoginRequest request = new LoginRequest();
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);

            when(appUserMapper.selectByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(passwordHashService.matches(TEST_PASSWORD, TEST_HASH)).thenReturn(true);
            when(contributorRequestMapper.selectLatestByUserId(TEST_USER_ID)).thenReturn(null);

            CurrentUserVO vo = userAccessService.login(request);

            assertNotNull(vo);
            assertEquals(TEST_USER_ID, vo.getUserId());
            assertEquals(TEST_EMAIL, vo.getEmail());
        }

        @Test
        @DisplayName("Exception case: email not exist throws unauthorized")
        void login_EmailNotExists_ThrowException() {
            LoginRequest request = new LoginRequest();
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);
            when(appUserMapper.selectByEmail(TEST_EMAIL)).thenReturn(null);

            AppException exception = assertThrows(AppException.class, () -> userAccessService.login(request));

            assertEquals("Invalid email or password.", exception.getMessage());
        }

        @Test
        @DisplayName("Exception case: wrong password throws unauthorized")
        void login_WrongPassword_ThrowException() {
            LoginRequest request = new LoginRequest();
            request.setEmail(TEST_EMAIL);
            request.setPassword("wrong");
            when(appUserMapper.selectByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(passwordHashService.matches("wrong", TEST_HASH)).thenReturn(false);

            AppException exception = assertThrows(AppException.class, () -> userAccessService.login(request));

            assertEquals("Invalid email or password.", exception.getMessage());
        }

        @Test
        @DisplayName("Exception case: empty login parameters throws bad request")
        void login_EmptyParams_ThrowException() {
            LoginRequest request = new LoginRequest();
            request.setEmail(null);
            request.setPassword(null);

            AppException exception = assertThrows(AppException.class, () -> userAccessService.login(request));

            assertEquals("Please complete the login form.", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Normal case: get current user by valid user id")
    void getCurrentUserById_ValidId_Success() {
        when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(testUser);
        when(contributorRequestMapper.selectLatestByUserId(TEST_USER_ID)).thenReturn(approvedRequest);

        CurrentUserVO vo = userAccessService.getCurrentUserById(TEST_USER_ID);

        assertNotNull(vo);
        assertEquals(TEST_USER_ID, vo.getUserId());
        assertEquals(ContributorApplicationStatusEnum.APPROVED.getValue(), vo.getContributorStatus());
    }

    @Test
    @DisplayName("Exception case: null user id throws unauthorized exception")
    void getCurrentUserById_NullId_ThrowException() {
        AppException exception = assertThrows(AppException.class, () -> userAccessService.getCurrentUserById(null));

        assertEquals("Please log in first.", exception.getMessage());
    }

    @Nested
    @DisplayName("Account Information Update Test")
    class UpdateAccountTest {

        @Test
        @DisplayName("Normal case: update account with valid parameters")
        void updateAccount_ValidParams_Success() {
            String newName = "newName";
            String newBio = "new bio";
            AccountUpdateRequest request = new AccountUpdateRequest();
            request.setName(newName);
            request.setEmail(TEST_EMAIL);
            request.setBio(newBio);

            when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(testUser);
            when(appUserMapper.selectByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(appUserMapper.selectByUsername(newName)).thenReturn(null);
            when(contributorRequestMapper.selectLatestByUserId(TEST_USER_ID)).thenReturn(null);

            CurrentUserVO vo = userAccessService.updateAccount(TEST_USER_ID, request);

            assertNotNull(vo);
            assertEquals(newName, vo.getName());
            assertEquals(newBio, vo.getBio());
        }

        @Test
        @DisplayName("Boundary case: bio with max length 1000 update success")
        void updateAccount_MaxBioLength_Success() {
            String maxBio = "a".repeat(1000);
            AccountUpdateRequest request = new AccountUpdateRequest();
            request.setName(TEST_NAME);
            request.setEmail(TEST_EMAIL);
            request.setBio(maxBio);

            when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(testUser);
            when(appUserMapper.selectByEmail(TEST_EMAIL)).thenReturn(testUser);
            when(appUserMapper.selectByUsername(TEST_NAME)).thenReturn(testUser);

            assertDoesNotThrow(() -> userAccessService.updateAccount(TEST_USER_ID, request));
        }

        @Test
        @DisplayName("Exception case: bio exceeds max length limit")
        void updateAccount_BioTooLong_ThrowException() {
            String longBio = "a".repeat(1001);
            AccountUpdateRequest request = new AccountUpdateRequest();
            request.setName(TEST_NAME);
            request.setEmail(TEST_EMAIL);
            request.setBio(longBio);

            AppException exception = assertThrows(AppException.class,
                    () -> userAccessService.updateAccount(TEST_USER_ID, request));

            // 断言主消息
            assertEquals("Please correct your account settings.", exception.getMessage());
            // 断言details里包含具体错误
            assertTrue(exception.getDetails().contains("Bio must be 1000 characters or fewer."));
        }

        @Test
        @DisplayName("Exception case: target email occupied by other user")
        void updateAccount_EmailUsedByOthers_ThrowException() {
            String newEmail = "other@example.com";
            AccountUpdateRequest request = new AccountUpdateRequest();
            request.setName(TEST_NAME);
            request.setEmail(newEmail);
            request.setBio(null);

            AppUser otherUser = new AppUser();
            otherUser.setUserId(999L);
            otherUser.setEmail(newEmail);

            when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(testUser);
            when(appUserMapper.selectByEmail(newEmail)).thenReturn(otherUser);

            AppException exception = assertThrows(AppException.class,
                    () -> userAccessService.updateAccount(TEST_USER_ID, request));

            assertEquals("The email address is already in use.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Contributor Application Submit Test")
    class SubmitContributorRequestTest {

        @Test
        @DisplayName("Normal case: registered viewer submit contributor request")
        void submitRequest_ValidUser_Success() {
            ContributorRequestSubmitRequest request = new ContributorRequestSubmitRequest();
            request.setApplicationReason(APPLICATION_REASON);

            when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(testUser);
            when(contributorRequestMapper.selectLatestByUserId(TEST_USER_ID)).thenReturn(null);

            ContributorRequestVO vo = userAccessService.submitContributorRequest(TEST_USER_ID, request);

            assertNotNull(vo);
            assertEquals(TEST_USER_ID, vo.getUserId());
            assertEquals(APPLICATION_REASON, vo.getApplicationReason());
            assertEquals(ContributorApplicationStatusEnum.PENDING.getValue(), vo.getStatus());
        }

        @Test
        @DisplayName("Boundary case: application reason reach max 2000 characters")
        void submitRequest_MaxReasonLength_Success() {
            String maxReason = "a".repeat(2000);
            ContributorRequestSubmitRequest request = new ContributorRequestSubmitRequest();
            request.setApplicationReason(maxReason);

            when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(testUser);
            when(contributorRequestMapper.selectLatestByUserId(TEST_USER_ID)).thenReturn(null);

            assertDoesNotThrow(() -> userAccessService.submitContributorRequest(TEST_USER_ID, request));
        }

        @Test
        @DisplayName("Exception case: application reason exceeds length limit")
        void submitRequest_ReasonTooLong_ThrowException() {
            String longReason = "a".repeat(2001);
            ContributorRequestSubmitRequest request = new ContributorRequestSubmitRequest();
            request.setApplicationReason(longReason);

            AppException exception = assertThrows(AppException.class,
                    () -> userAccessService.submitContributorRequest(TEST_USER_ID, request));

            // 断言主消息
            assertEquals("Please correct your contributor application.", exception.getMessage());
            // 断言details里包含具体错误
            assertTrue(exception.getDetails().contains("Application reason must be 2000 characters or fewer."));
        }

        @Test
        @DisplayName("Exception case: existing pending application cannot resubmit")
        void submitRequest_HasPendingRequest_ThrowException() {
            ContributorRequestSubmitRequest request = new ContributorRequestSubmitRequest();
            request.setApplicationReason(APPLICATION_REASON);

            when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(testUser);
            when(contributorRequestMapper.selectLatestByUserId(TEST_USER_ID)).thenReturn(pendingRequest);

            AppException exception = assertThrows(AppException.class,
                    () -> userAccessService.submitContributorRequest(TEST_USER_ID, request));

            assertEquals("Your existing contributor request is still under review.", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Normal case: admin approve pending contributor application")
    void reviewRequest_ApprovePendingRequest_Success() {
        ContributorReviewDecisionRequest request = new ContributorReviewDecisionRequest();
        request.setDecision(ContributorApplicationStatusEnum.APPROVED.getValue());
        request.setReviewComment(REVIEW_COMMENT);

        when(contributorRequestMapper.selectByIdForUpdate(REQUEST_ID)).thenReturn(pendingRequest);
        when(appUserMapper.selectById(TEST_USER_ID)).thenReturn(testUser);
        when(contributorRequestMapper.selectRequestViewById(REQUEST_ID)).thenReturn(mock(ContributorRequestVO.class));

        ContributorRequestVO vo = userAccessService.reviewContributorRequest(ADMIN_USER_ID, REQUEST_ID, request);

        assertNotNull(vo);
    }

    @Test
    @DisplayName("Exception case: review non-pending contributor request")
    void reviewRequest_NotPendingStatus_ThrowException() {
        ContributorReviewDecisionRequest request = new ContributorReviewDecisionRequest();
        request.setDecision(ContributorApplicationStatusEnum.APPROVED.getValue());

        when(contributorRequestMapper.selectByIdForUpdate(REQUEST_ID)).thenReturn(approvedRequest);

        AppException exception = assertThrows(AppException.class,
                () -> userAccessService.reviewContributorRequest(ADMIN_USER_ID, REQUEST_ID, request));

        assertEquals("Only pending contributor requests can be reviewed.", exception.getMessage());
    }

    @Test
    @DisplayName("Normal case: admin revoke contributor role from normal user")
    void revokeContributor_ValidAdmin_Success() {
        testUser.setContributor(true);
        when(appUserMapper.selectByIdForUpdate(TEST_USER_ID)).thenReturn(testUser);

        ContributorRequestVO vo = userAccessService.revokeContributor(ADMIN_USER_ID, TEST_USER_ID, "admin");

        assertNotNull(vo);
        assertEquals("REVOKED", vo.getStatus());
    }

    @Test
    @DisplayName("Exception case: admin cannot revoke own contributor role")
    void revokeContributor_SelfRevoke_ThrowException() {
        AppException exception = assertThrows(AppException.class,
                () -> userAccessService.revokeContributor(ADMIN_USER_ID, ADMIN_USER_ID, "admin"));

        assertEquals("Administrators cannot revoke their own contributor state through this action.", exception.getMessage());
    }

    @Test
    @DisplayName("Normal case: query pending contributor request list")
    void listPendingRequests_ReturnList() {
        ContributorRequestVO vo = new ContributorRequestVO();
        when(contributorRequestMapper.selectPendingRequestViews()).thenReturn(List.of(vo));

        List<ContributorRequestVO> result = userAccessService.listPendingContributorRequests();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Normal case: no pending request return empty list")
    void listPendingRequests_ReturnEmptyList() {
        when(contributorRequestMapper.selectPendingRequestViews()).thenReturn(Collections.emptyList());

        List<ContributorRequestVO> result = userAccessService.listPendingContributorRequests();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Normal case: get contributor request detail by valid id")
    void getRequestDetail_ValidId_Success() {
        ContributorRequestVO vo = new ContributorRequestVO();
        vo.setRequestId(REQUEST_ID);
        when(contributorRequestMapper.selectRequestViewById(REQUEST_ID)).thenReturn(vo);

        ContributorRequestVO result = userAccessService.getContributorRequestDetail(REQUEST_ID);

        assertNotNull(result);
        assertEquals(REQUEST_ID, result.getRequestId());
    }

    @Test
    @DisplayName("Exception case: query non-exist contributor request detail")
    void getRequestDetail_InvalidId_ThrowException() {
        when(contributorRequestMapper.selectRequestViewById(999L)).thenReturn(null);

        AppException exception = assertThrows(AppException.class,
                () -> userAccessService.getContributorRequestDetail(999L));

        assertEquals("Contributor request does not exist.", exception.getMessage());
    }
}
