package com.cpt202.HerLink.service;

import com.cpt202.HerLink.dto.auth.AccountUpdateRequest;
import com.cpt202.HerLink.dto.auth.ContributorReviewDecisionRequest;
import com.cpt202.HerLink.dto.auth.ContributorRequestSubmitRequest;
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
import com.cpt202.HerLink.service.impl.UserAccessServiceImpl;
import com.cpt202.HerLink.util.PasswordHashService;
import com.cpt202.HerLink.vo.ContributorRequestVO;
import com.cpt202.HerLink.vo.CurrentUserVO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class UserAccessServiceImplTest {

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("\\b(\\d{6})\\b");

    @Mock
    private AppUserMapper appUserMapper;

    @Mock
    private ContributorRequestMapper contributorRequestMapper;

    @Mock
    private ObjectProvider<JavaMailSender> javaMailSenderProvider;

    @Mock
    private JavaMailSender javaMailSender;

    private PasswordHashService passwordHashService;
    private UserAccessServiceImpl userAccessService;

    @BeforeEach
    void setUp() {
        passwordHashService = new PasswordHashService();
        userAccessService = new UserAccessServiceImpl(
                appUserMapper,
                contributorRequestMapper,
                passwordHashService,
                javaMailSenderProvider,
                "smtp.example.com",
                "noreply@example.com",
                10,
                60
        );
    }

    @Test
    void register_shouldCreateRegisteredViewerWithHashedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setName("New Viewer");
        request.setEmail("viewer@example.com");
        request.setPassword("Viewer123!");
        request.setConfirmPassword("Viewer123!");

        when(appUserMapper.selectByEmail("viewer@example.com")).thenReturn(null);
        when(appUserMapper.selectByUsername("New Viewer")).thenReturn(null);
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        org.mockito.Mockito.doAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setUserId(10L);
            return 1;
        }).when(appUserMapper).insert(any(AppUser.class));

        RegisterVerificationCodeRequest verificationCodeRequest = new RegisterVerificationCodeRequest();
        verificationCodeRequest.setEmail("viewer@example.com");
        userAccessService.sendRegisterVerificationCode(verificationCodeRequest);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(mailCaptor.capture());
        request.setVerificationCode(extractVerificationCode(mailCaptor.getValue().getText()));

        CurrentUserVO result = userAccessService.register(request);

        assertNotNull(result);
        assertEquals(10L, result.getUserId());
        assertEquals("New Viewer", result.getName());
        assertEquals("viewer@example.com", result.getEmail());
        assertEquals(UserRoleEnum.REGISTERED_VIEWER.getApiValue(), result.getRole());
        assertEquals("NONE", result.getContributorStatus());
        assertFalse(result.isContributor());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserMapper).insert(captor.capture());
        AppUser inserted = captor.getValue();
        assertEquals(UserRoleEnum.REGISTERED_VIEWER.getValue(), inserted.getRole());
        assertTrue(passwordHashService.matches("Viewer123!", inserted.getPasswordHash()));
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Existing");
        request.setEmail("viewer@example.com");
        request.setPassword("Viewer123!");
        request.setConfirmPassword("Viewer123!");
        request.setVerificationCode("123456");

        AppUser existing = new AppUser();
        existing.setUserId(1L);
        when(appUserMapper.selectByEmail("viewer@example.com")).thenReturn(existing);

        AppException exception = assertThrows(AppException.class, () -> userAccessService.register(request));

        assertEquals(409, exception.getStatusCode());
        verify(appUserMapper, never()).insert(any(AppUser.class));
    }

    @Test
    void register_shouldRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Existing");
        request.setEmail("viewer@example.com");
        request.setPassword("Viewer123!");
        request.setConfirmPassword("Viewer123!");
        request.setVerificationCode("123456");

        AppUser existing = new AppUser();
        existing.setUserId(2L);
        when(appUserMapper.selectByEmail("viewer@example.com")).thenReturn(null);
        when(appUserMapper.selectByUsername("Existing")).thenReturn(existing);

        AppException exception = assertThrows(AppException.class, () -> userAccessService.register(request));

        assertEquals(409, exception.getStatusCode());
        assertEquals("The username is already in use.", exception.getMessage());
        verify(appUserMapper, never()).insert(any(AppUser.class));
    }

    @Test
    void register_shouldTranslateDuplicateUsernameConstraint() {
        RegisterRequest request = new RegisterRequest();
        request.setName("New Viewer");
        request.setEmail("viewer@example.com");
        request.setPassword("Viewer123!");
        request.setConfirmPassword("Viewer123!");

        when(appUserMapper.selectByEmail("viewer@example.com")).thenReturn(null);
        when(appUserMapper.selectByUsername("New Viewer")).thenReturn(null);
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        doThrow(new DuplicateKeyException("Duplicate entry 'New Viewer' for key 'user.username'"))
                .when(appUserMapper)
                .insert(any(AppUser.class));

        RegisterVerificationCodeRequest verificationCodeRequest = new RegisterVerificationCodeRequest();
        verificationCodeRequest.setEmail("viewer@example.com");
        userAccessService.sendRegisterVerificationCode(verificationCodeRequest);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(mailCaptor.capture());
        request.setVerificationCode(extractVerificationCode(mailCaptor.getValue().getText()));

        AppException exception = assertThrows(AppException.class, () -> userAccessService.register(request));

        assertEquals(409, exception.getStatusCode());
        assertEquals("The username is already in use.", exception.getMessage());
    }

    @Test
    void login_shouldRejectInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("viewer@example.com");
        request.setPassword("WrongPass123!");

        AppUser user = new AppUser();
        user.setUserId(3L);
        user.setEmail("viewer@example.com");
        user.setPasswordHash(passwordHashService.hash("Viewer123!"));
        when(appUserMapper.selectByEmail("viewer@example.com")).thenReturn(user);

        AppException exception = assertThrows(AppException.class, () -> userAccessService.login(request));

        assertEquals(401, exception.getStatusCode());
    }

    @Test
    void updateAccount_shouldRejectDuplicateEmail() {
        AppUser currentUser = new AppUser();
        currentUser.setUserId(3L);
        currentUser.setName("Viewer");
        currentUser.setEmail("viewer@example.com");
        currentUser.setRole(UserRoleEnum.REGISTERED_VIEWER.getValue());

        AppUser otherUser = new AppUser();
        otherUser.setUserId(4L);
        otherUser.setEmail("other@example.com");

        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setName("Viewer Updated");
        request.setEmail("other@example.com");

        when(appUserMapper.selectById(3L)).thenReturn(currentUser);
        when(appUserMapper.selectByEmail("other@example.com")).thenReturn(otherUser);

        AppException exception = assertThrows(AppException.class, () -> userAccessService.updateAccount(3L, request));

        assertEquals(409, exception.getStatusCode());
        verify(appUserMapper, never()).updateBasicInfo(any(AppUser.class));
    }

    @Test
    void updateAccount_shouldRejectDuplicateUsername() {
        AppUser currentUser = new AppUser();
        currentUser.setUserId(3L);
        currentUser.setName("Viewer");
        currentUser.setEmail("viewer@example.com");
        currentUser.setRole(UserRoleEnum.REGISTERED_VIEWER.getValue());

        AppUser otherUser = new AppUser();
        otherUser.setUserId(4L);
        otherUser.setName("Taken Name");

        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setName("Taken Name");
        request.setEmail("viewer@example.com");

        when(appUserMapper.selectById(3L)).thenReturn(currentUser);
        when(appUserMapper.selectByEmail("viewer@example.com")).thenReturn(currentUser);
        when(appUserMapper.selectByUsername("Taken Name")).thenReturn(otherUser);

        AppException exception = assertThrows(AppException.class, () -> userAccessService.updateAccount(3L, request));

        assertEquals(409, exception.getStatusCode());
        assertEquals("The username is already in use.", exception.getMessage());
        verify(appUserMapper, never()).updateBasicInfo(any(AppUser.class));
    }

    @Test
    void updateAccount_shouldTranslateDuplicateEmailConstraint() {
        AppUser currentUser = new AppUser();
        currentUser.setUserId(3L);
        currentUser.setName("Viewer");
        currentUser.setEmail("viewer@example.com");
        currentUser.setRole(UserRoleEnum.REGISTERED_VIEWER.getValue());

        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setName("Viewer Updated");
        request.setEmail("viewer-updated@example.com");

        when(appUserMapper.selectById(3L)).thenReturn(currentUser);
        when(appUserMapper.selectByEmail("viewer-updated@example.com")).thenReturn(null);
        when(appUserMapper.selectByUsername("Viewer Updated")).thenReturn(null);
        doThrow(new DuplicateKeyException("Duplicate entry 'viewer-updated@example.com' for key 'user.email'"))
                .when(appUserMapper)
                .updateBasicInfo(any(AppUser.class));

        AppException exception = assertThrows(AppException.class, () -> userAccessService.updateAccount(3L, request));

        assertEquals(409, exception.getStatusCode());
        assertEquals("The email address is already in use.", exception.getMessage());
    }

    @Test
    void submitContributorRequest_shouldRejectWhenPendingAlreadyExists() {
        AppUser currentUser = new AppUser();
        currentUser.setUserId(3L);
        currentUser.setRole(UserRoleEnum.REGISTERED_VIEWER.getValue());
        currentUser.setName("Viewer");
        currentUser.setEmail("viewer@example.com");

        ContributorRequest latestRequest = new ContributorRequest();
        latestRequest.setRequestId(11L);
        latestRequest.setStatus(ContributorApplicationStatusEnum.PENDING.getValue());

        when(appUserMapper.selectById(3L)).thenReturn(currentUser);
        when(contributorRequestMapper.selectLatestByUserId(3L)).thenReturn(latestRequest);

        ContributorRequestSubmitRequest request = new ContributorRequestSubmitRequest();
        request.setApplicationReason("I want to contribute documented local heritage materials.");

        AppException exception = assertThrows(
                AppException.class,
                () -> userAccessService.submitContributorRequest(3L, request)
        );

        assertEquals(409, exception.getStatusCode());
        verify(contributorRequestMapper, never()).insert(any(ContributorRequest.class));
    }

    @Test
    void submitContributorRequest_shouldRequireApplicationReason() {
        ContributorRequestSubmitRequest request = new ContributorRequestSubmitRequest();
        request.setApplicationReason(" ");

        AppException exception = assertThrows(
                AppException.class,
                () -> userAccessService.submitContributorRequest(3L, request)
        );

        assertEquals(400, exception.getStatusCode());
        verify(appUserMapper, never()).selectById(any());
        verify(contributorRequestMapper, never()).insert(any(ContributorRequest.class));
    }

    @Test
    void reviewContributorRequest_shouldApprovePendingRequest() {
        ContributorReviewDecisionRequest request = new ContributorReviewDecisionRequest();
        request.setDecision("APPROVED");
        request.setReviewComment("Approved for contributor access.");

        ContributorRequest pendingRequest = new ContributorRequest();
        pendingRequest.setRequestId(20L);
        pendingRequest.setUserId(4L);
        pendingRequest.setStatus(ContributorApplicationStatusEnum.PENDING.getValue());
        pendingRequest.setRequestedAt(LocalDateTime.now().minusDays(1));
        pendingRequest.setUpdatedAt(LocalDateTime.now().minusDays(1));

        ContributorRequestVO requestView = new ContributorRequestVO();
        requestView.setRequestId(20L);
        requestView.setUserId(4L);
        requestView.setUserName("Pending Applicant");
        requestView.setUserEmail("pending@example.com");
        requestView.setStatus(ContributorApplicationStatusEnum.APPROVED.getValue());

        when(contributorRequestMapper.selectByIdForUpdate(20L)).thenReturn(pendingRequest);
        when(contributorRequestMapper.selectRequestViewById(20L)).thenReturn(requestView);

        ContributorRequestVO result = userAccessService.reviewContributorRequest(1L, 20L, request);

        assertEquals(ContributorApplicationStatusEnum.APPROVED.getValue(), result.getStatus());
        ArgumentCaptor<ContributorRequest> captor = ArgumentCaptor.forClass(ContributorRequest.class);
        verify(contributorRequestMapper).updateReviewDecision(captor.capture());
        assertEquals(ContributorApplicationStatusEnum.APPROVED.getValue(), captor.getValue().getStatus());
        assertEquals(1L, captor.getValue().getReviewedBy());
        assertEquals("Approved for contributor access.", captor.getValue().getReviewComment());
    }

    private String extractVerificationCode(String mailText) {
        assertNotNull(mailText);
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(mailText);
        assertTrue(matcher.find(), "Expected verification code in email body.");
        return matcher.group(1);
    }
}
