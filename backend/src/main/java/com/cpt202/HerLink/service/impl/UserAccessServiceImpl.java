package com.cpt202.HerLink.service.impl;

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
import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.service.admin.AdminOperationHistoryService;
import com.cpt202.HerLink.service.notification.EmailNotificationService;
import com.cpt202.HerLink.util.PasswordHashService;
import com.cpt202.HerLink.vo.ContributorRequestVO;
import com.cpt202.HerLink.vo.CurrentUserVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccessServiceImpl implements UserAccessService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int MAX_APPLICATION_REASON_LENGTH = 2000;
    private static final int MAX_BIO_LENGTH = 1000;

    private final AppUserMapper appUserMapper;
    private final ContributorRequestMapper contributorRequestMapper;
    private final PasswordHashService passwordHashService;
    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final EmailNotificationService emailNotificationService;
    private final AdminOperationHistoryService adminOperationHistoryService;
    private final Map<String, RegisterVerificationCodeEntry> registerVerificationCodes = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final String mailHost;
    private final String verificationFromAddress;
    private final int registerCodeExpireMinutes;
    private final int registerCodeResendSeconds;

    @Autowired
    public UserAccessServiceImpl(AppUserMapper appUserMapper,
                                 ContributorRequestMapper contributorRequestMapper,
                                 PasswordHashService passwordHashService,
                                 ObjectProvider<JavaMailSender> javaMailSenderProvider,
                                 EmailNotificationService emailNotificationService,
                                 AdminOperationHistoryService adminOperationHistoryService,
                                 @Value("${spring.mail.host:}") String mailHost,
                                 @Value("${HerLink.register-verification.from-address:}") String verificationFromAddress,
                                 @Value("${HerLink.register-verification.code-expire-minutes:10}") int registerCodeExpireMinutes,
                                 @Value("${HerLink.register-verification.resend-interval-seconds:60}") int registerCodeResendSeconds) {
        this.appUserMapper = appUserMapper;
        this.contributorRequestMapper = contributorRequestMapper;
        this.passwordHashService = passwordHashService;
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.emailNotificationService = emailNotificationService;
        this.adminOperationHistoryService = adminOperationHistoryService;
        this.mailHost = mailHost == null ? "" : mailHost.trim();
        this.verificationFromAddress = verificationFromAddress == null ? "" : verificationFromAddress.trim();
        this.registerCodeExpireMinutes = registerCodeExpireMinutes;
        this.registerCodeResendSeconds = registerCodeResendSeconds;
    }

    public UserAccessServiceImpl(AppUserMapper appUserMapper,
                                 ContributorRequestMapper contributorRequestMapper,
                                 PasswordHashService passwordHashService,
                                 ObjectProvider<JavaMailSender> javaMailSenderProvider,
                                 @Value("${spring.mail.host:}") String mailHost,
                                 @Value("${HerLink.register-verification.from-address:}") String verificationFromAddress,
                                 @Value("${HerLink.register-verification.code-expire-minutes:10}") int registerCodeExpireMinutes,
                                 @Value("${HerLink.register-verification.resend-interval-seconds:60}") int registerCodeResendSeconds) {
        this(
                appUserMapper,
                contributorRequestMapper,
                passwordHashService,
                javaMailSenderProvider,
                null,
                null,
                mailHost,
                verificationFromAddress,
                registerCodeExpireMinutes,
                registerCodeResendSeconds
        );
    }

    @Override
    public void sendRegisterVerificationCode(RegisterVerificationCodeRequest request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());

        validateRegisterVerificationCodeRequestInput(email);
        ensureRegisterVerificationMailIsConfigured();

        if (appUserMapper.selectByEmail(email) != null) {
            throw AppException.conflict("The email address is already in use.");
        }

        purgeExpiredRegisterVerificationCodes();

        LocalDateTime now = LocalDateTime.now();
        RegisterVerificationCodeEntry existingEntry = registerVerificationCodes.get(email);
        if (existingEntry != null && existingEntry.getAvailableResendAt().isAfter(now)) {
            throw AppException.conflict(
                    "Please wait " + registerCodeResendSeconds + " seconds before requesting another verification code."
            );
        }

        String verificationCode = generateVerificationCode();
        RegisterVerificationCodeEntry verificationEntry = new RegisterVerificationCodeEntry(
                verificationCode,
                now.plusMinutes(registerCodeExpireMinutes),
                now.plusSeconds(registerCodeResendSeconds)
        );

        try {
            sendRegisterVerificationEmail(email, verificationCode);
            registerVerificationCodes.put(email, verificationEntry);
        } catch (MailException exception) {
            throw AppException.badRequest("Unable to send the verification email right now. Please try again later.");
        }
    }

    @Override
    @Transactional
    public CurrentUserVO register(RegisterRequest request) {
        String name = trimToNull(request == null ? null : request.getName());
        String email = normalizeEmail(request == null ? null : request.getEmail());
        String password = request == null ? null : request.getPassword();
        String confirmPassword = request == null ? null : request.getConfirmPassword();
        String verificationCode = trimToNull(request == null ? null : request.getVerificationCode());

        validateRegistrationInput(name, email, password, confirmPassword, verificationCode);

        if (appUserMapper.selectByEmail(email) != null) {
            throw AppException.conflict("The email address is already in use.");
        }
        if (appUserMapper.selectByUsername(name) != null) {
            throw AppException.conflict("The username is already in use.");
        }

        consumeRegisterVerificationCode(email, verificationCode);

        LocalDateTime now = LocalDateTime.now();
        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordHashService.hash(password));
        user.setRole(UserRoleEnum.REGISTERED_VIEWER.getValue());
        user.setContributor(Boolean.FALSE);
        user.setBio(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        try {
            appUserMapper.insert(user);
        } catch (DataIntegrityViolationException exception) {
            throw translateUserConflictException(exception);
        }

        return buildCurrentUserVO(user, null);
    }

    @Override
    public CurrentUserVO login(LoginRequest request) {
        String email = normalizeEmail(request == null ? null : request.getEmail());
        String password = request == null ? null : request.getPassword();
        validateLoginInput(email, password);

        AppUser user = appUserMapper.selectByEmail(email);
        if (user == null || !passwordHashService.matches(password, user.getPasswordHash())) {
            throw AppException.unauthorized("Invalid email or password.");
        }

        ContributorRequest latestRequest = contributorRequestMapper.selectLatestByUserId(user.getUserId());
        return buildCurrentUserVO(user, latestRequest);
    }

    @Override
    public CurrentUserVO getCurrentUserById(Long userId) {
        AppUser user = loadUser(userId);
        ContributorRequest latestRequest = contributorRequestMapper.selectLatestByUserId(userId);
        return buildCurrentUserVO(user, latestRequest);
    }

    @Override
    @Transactional
    public CurrentUserVO updateAccount(Long userId, AccountUpdateRequest request) {
        String name = trimToNull(request == null ? null : request.getName());
        String email = normalizeEmail(request == null ? null : request.getEmail());
        String bio = trimToNull(request == null ? null : request.getBio());
        validateAccountUpdateInput(name, email, bio);

        AppUser existingUser = loadUser(userId);
        AppUser userByEmail = appUserMapper.selectByEmail(email);
        if (userByEmail != null && !Objects.equals(userByEmail.getUserId(), userId)) {
            throw AppException.conflict("The email address is already in use.");
        }
        AppUser userByUsername = appUserMapper.selectByUsername(name);
        if (userByUsername != null && !Objects.equals(userByUsername.getUserId(), userId)) {
            throw AppException.conflict("The username is already in use.");
        }

        existingUser.setName(name);
        existingUser.setEmail(email);
        existingUser.setBio(bio);
        existingUser.setUpdatedAt(LocalDateTime.now());
        try {
            appUserMapper.updateBasicInfo(existingUser);
        } catch (DataIntegrityViolationException exception) {
            throw translateUserConflictException(exception);
        }

        ContributorRequest latestRequest = contributorRequestMapper.selectLatestByUserId(userId);
        return buildCurrentUserVO(existingUser, latestRequest);
    }

    @Override
    @Transactional
    public ContributorRequestVO submitContributorRequest(Long userId, ContributorRequestSubmitRequest request) {
        String applicationReason = trimToNull(request == null ? null : request.getApplicationReason());
        validateContributorRequestInput(applicationReason);

        AppUser user = loadUser(userId);
        if (!UserRoleEnum.REGISTERED_VIEWER.matches(user.getRole())) {
            throw AppException.forbidden("Only registered viewers can submit contributor requests.");
        }

        ContributorRequest latestRequest = contributorRequestMapper.selectLatestByUserId(userId);
        if (latestRequest != null) {
            String latestStatus = latestRequest.getStatus();
            if (ContributorApplicationStatusEnum.PENDING.getValue().equalsIgnoreCase(latestStatus)) {
                throw AppException.conflict("Your existing contributor request is still under review.");
            }
            if (ContributorApplicationStatusEnum.APPROVED.getValue().equalsIgnoreCase(latestStatus)
                    && Boolean.TRUE.equals(user.getContributor())) {
                throw AppException.conflict("You are already an approved contributor.");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ContributorRequest contributorRequest = new ContributorRequest();
        contributorRequest.setUserId(userId);
        contributorRequest.setApplicationReason(applicationReason);
        contributorRequest.setStatus(ContributorApplicationStatusEnum.PENDING.getValue());
        contributorRequest.setRequestedAt(now);
        contributorRequest.setUpdatedAt(now);
        contributorRequestMapper.insert(contributorRequest);

        return buildContributorRequestVO(contributorRequest, user);
    }

    @Override
    public ContributorRequestVO getMyLatestContributorRequest(Long userId) {
        AppUser user = loadUser(userId);
        ContributorRequest latestRequest = contributorRequestMapper.selectLatestByUserId(userId);
        if (latestRequest == null) {
            return null;
        }
        return buildContributorRequestVO(latestRequest, user);
    }

    @Override
    public List<ContributorRequestVO> listPendingContributorRequests() {
        return contributorRequestMapper.selectPendingRequestViews();
    }

    @Override
    public ContributorRequestVO getContributorRequestDetail(Long requestId) {
        if (requestId == null) {
            throw AppException.badRequest("Contributor request id is required.");
        }
        ContributorRequestVO requestView = contributorRequestMapper.selectRequestViewById(requestId);
        if (requestView == null) {
            throw AppException.notFound("Contributor request does not exist.");
        }
        return requestView;
    }

    @Override
    public List<ContributorRequestVO> listApprovedContributors() {
        return contributorRequestMapper.selectApprovedContributorViews();
    }

    @Override
    @Transactional
    public ContributorRequestVO reviewContributorRequest(Long adminUserId,
                                                         Long requestId,
                                                         ContributorReviewDecisionRequest request) {
        String decision = normalizeDecision(request == null ? null : request.getDecision());
        String reviewComment = trimToNull(request == null ? null : request.getReviewComment());

        if (!ContributorApplicationStatusEnum.APPROVED.getValue().equals(decision)
                && !ContributorApplicationStatusEnum.REJECTED.getValue().equals(decision)) {
            throw AppException.badRequest("Decision must be APPROVED or REJECTED.");
        }

        ContributorRequest contributorRequest = contributorRequestMapper.selectByIdForUpdate(requestId);
        if (contributorRequest == null) {
            throw AppException.notFound("Contributor request does not exist.");
        }

        if (!ContributorApplicationStatusEnum.PENDING.getValue().equalsIgnoreCase(contributorRequest.getStatus())) {
            throw AppException.conflict("Only pending contributor requests can be reviewed.");
        }

        LocalDateTime now = LocalDateTime.now();
        contributorRequest.setStatus(decision);
        contributorRequest.setReviewedAt(now);
        contributorRequest.setReviewedBy(adminUserId);
        contributorRequest.setReviewComment(reviewComment);
        contributorRequest.setUpdatedAt(now);
        contributorRequestMapper.updateReviewDecision(contributorRequest);
        appUserMapper.updateContributorFlag(
                contributorRequest.getUserId(),
                ContributorApplicationStatusEnum.APPROVED.getValue().equals(decision),
                now
        );

        ContributorRequestVO requestView = contributorRequestMapper.selectRequestViewById(requestId);
        if (ContributorApplicationStatusEnum.APPROVED.getValue().equals(decision)) {
            notifyContributorApplicationApproved(contributorRequest.getUserId());
        }
        if (requestView != null) {
            return requestView;
        }

        AppUser applicant = loadUser(contributorRequest.getUserId());
        return buildContributorRequestVO(contributorRequest, applicant);
    }

    @Override
    @Transactional
    public ContributorRequestVO revokeContributor(Long adminUserId, Long contributorUserId, String administrator) {
        if (adminUserId == null) {
            throw AppException.unauthorized("Please log in first.");
        }
        if (contributorUserId == null) {
            throw AppException.badRequest("Contributor user id is required.");
        }
        if (Objects.equals(adminUserId, contributorUserId)) {
            throw AppException.conflict("Administrators cannot revoke their own contributor state through this action.");
        }

        AppUser user = appUserMapper.selectByIdForUpdate(contributorUserId);
        if (user == null) {
            throw AppException.notFound("User does not exist.");
        }
        if (UserRoleEnum.ADMINISTRATOR.matches(user.getRole())) {
            throw AppException.conflict("Administrator or reviewer accounts cannot be demoted by contributor revoke.");
        }
        if (!Boolean.TRUE.equals(user.getContributor())) {
            throw AppException.conflict("This user is not currently an approved contributor.");
        }

        LocalDateTime now = LocalDateTime.now();
        appUserMapper.updateContributorFlag(contributorUserId, false, now);
        user.setContributor(Boolean.FALSE);
        user.setUpdatedAt(now);

        recordContributorRevokeOperation(user, administrator);
        notifyContributorRoleRevoked(user);

        ContributorRequest latestRequest = contributorRequestMapper.selectLatestByUserId(contributorUserId);
        if (latestRequest == null) {
            ContributorRequestVO response = new ContributorRequestVO();
            response.setUserId(user.getUserId());
            response.setUserName(user.getName());
            response.setUserEmail(user.getEmail());
            response.setStatus("REVOKED");
            response.setUpdatedAt(now);
            return response;
        }
        ContributorRequestVO requestView = contributorRequestMapper.selectRequestViewById(latestRequest.getRequestId());
        return requestView == null ? buildContributorRequestVO(latestRequest, user) : requestView;
    }

    private AppUser loadUser(Long userId) {
        if (userId == null) {
            throw AppException.unauthorized("Please log in first.");
        }

        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw AppException.notFound("Current user does not exist.");
        }
        return user;
    }

    private void notifyContributorApplicationApproved(Long userId) {
        if (emailNotificationService == null) {
            return;
        }
        AppUser user = appUserMapper.selectById(userId);
        emailNotificationService.notifyContributorApplicationApproved(user);
    }

    private void notifyContributorRoleRevoked(AppUser user) {
        if (emailNotificationService != null) {
            emailNotificationService.notifyContributorRoleRevoked(user);
        }
    }

    private void recordContributorRevokeOperation(AppUser user, String administrator) {
        if (adminOperationHistoryService == null || user == null) {
            return;
        }
        String itemName = user.getName() == null || user.getName().isBlank()
                ? "user_" + user.getUserId()
                : user.getName();
        String operator = administrator == null || administrator.isBlank()
                ? "admin"
                : administrator;
        adminOperationHistoryService.recordOperation(
                itemName,
                "Contributor",
                "contributor",
                "Revoked",
                operator
        );
    }

    private void validateRegistrationInput(String name,
                                           String email,
                                           String password,
                                           String confirmPassword,
                                           String verificationCode) {
        List<String> details = new ArrayList<>();

        if (name == null) {
            details.add("Name is required.");
        }
        if (email == null) {
            details.add("Email is required.");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            details.add("Please enter a valid email address.");
        }
        if (password == null || password.isBlank()) {
            details.add("Password is required.");
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            details.add("Password must be at least 8 characters long.");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            details.add("Please confirm your password.");
        } else if (password != null && !password.equals(confirmPassword)) {
            details.add("The password confirmation does not match.");
        }
        if (verificationCode == null) {
            details.add("Verification code is required.");
        } else if (!verificationCode.matches("^\\d{" + VERIFICATION_CODE_LENGTH + "}$")) {
            details.add("Verification code must be a 6-digit number.");
        }

        if (!details.isEmpty()) {
            throw AppException.badRequest("Please correct the registration form.", details);
        }
    }

    private void validateRegisterVerificationCodeRequestInput(String email) {
        List<String> details = new ArrayList<>();

        if (email == null) {
            details.add("Email is required.");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            details.add("Please enter a valid email address.");
        }

        if (!details.isEmpty()) {
            throw AppException.badRequest("Please correct the email address.", details);
        }
    }

    private void validateLoginInput(String email, String password) {
        List<String> details = new ArrayList<>();

        if (email == null) {
            details.add("Email is required.");
        }
        if (password == null || password.isBlank()) {
            details.add("Password is required.");
        }

        if (!details.isEmpty()) {
            throw AppException.badRequest("Please complete the login form.", details);
        }
    }

    private void validateAccountUpdateInput(String name, String email, String bio) {
        List<String> details = new ArrayList<>();

        if (name == null) {
            details.add("Name is required.");
        }
        if (email == null) {
            details.add("Email is required.");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            details.add("Please enter a valid email address.");
        }
        if (bio != null && bio.length() > MAX_BIO_LENGTH) {
            details.add("Bio must be " + MAX_BIO_LENGTH + " characters or fewer.");
        }

        if (!details.isEmpty()) {
            throw AppException.badRequest("Please correct your account settings.", details);
        }
    }

    private void validateContributorRequestInput(String applicationReason) {
        List<String> details = new ArrayList<>();

        if (applicationReason == null) {
            details.add("Application reason is required.");
        } else if (applicationReason.length() > MAX_APPLICATION_REASON_LENGTH) {
            details.add("Application reason must be " + MAX_APPLICATION_REASON_LENGTH + " characters or fewer.");
        }

        if (!details.isEmpty()) {
            throw AppException.badRequest("Please correct your contributor application.", details);
        }
    }

    private void ensureRegisterVerificationMailIsConfigured() {
        if (mailHost.isBlank()) {
            throw AppException.badRequest("Email verification is not configured yet. Please contact the administrator.");
        }

        if (javaMailSenderProvider.getIfAvailable() == null) {
            throw AppException.badRequest("Email verification is not available right now. Please try again later.");
        }
    }

    private void sendRegisterVerificationEmail(String email, String verificationCode) {
        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            throw AppException.badRequest("Email verification is not available right now. Please try again later.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (!verificationFromAddress.isBlank()) {
            message.setFrom(verificationFromAddress);
        }
        message.setTo(email);
        message.setSubject("HerLink registration verification code");
        message.setText(
                "Your HerLink registration verification code is: " + verificationCode + System.lineSeparator()
                        + System.lineSeparator()
                        + "This code will expire in " + registerCodeExpireMinutes + " minutes." + System.lineSeparator()
                        + "If you did not request this code, please ignore this email."
        );
        javaMailSender.send(message);
    }

    private void consumeRegisterVerificationCode(String email, String verificationCode) {
        purgeExpiredRegisterVerificationCodes();

        RegisterVerificationCodeEntry verificationEntry = registerVerificationCodes.get(email);
        if (verificationEntry == null) {
            throw AppException.badRequest("Please send a verification code first.");
        }

        if (verificationEntry.getExpiresAt().isBefore(LocalDateTime.now())) {
            registerVerificationCodes.remove(email);
            throw AppException.badRequest("The verification code has expired. Please request a new one.");
        }

        if (!verificationEntry.getCode().equals(verificationCode)) {
            throw AppException.badRequest("The verification code is incorrect.");
        }

        registerVerificationCodes.remove(email);
    }

    private void purgeExpiredRegisterVerificationCodes() {
        LocalDateTime now = LocalDateTime.now();
        registerVerificationCodes.entrySet().removeIf(entry -> entry.getValue().getExpiresAt().isBefore(now));
    }

    private String generateVerificationCode() {
        int upperBound = (int) Math.pow(10, VERIFICATION_CODE_LENGTH);
        return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", secureRandom.nextInt(upperBound));
    }

    private String normalizeEmail(String email) {
        String trimmed = trimToNull(email);
        if (trimmed == null) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String normalizeDecision(String decision) {
        String trimmed = trimToNull(decision);
        if (trimmed == null) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AppException translateUserConflictException(RuntimeException exception) {
        String message = exception.getMessage();
        if (message != null) {
            String normalizedMessage = message.toLowerCase(Locale.ROOT);
            if (normalizedMessage.contains("username")) {
                return AppException.conflict("The username is already in use.");
            }
            if (normalizedMessage.contains("email")) {
                return AppException.conflict("The email address is already in use.");
            }
            if (normalizedMessage.contains("duplicate") || normalizedMessage.contains("unique")) {
                return AppException.conflict("The account information is already in use.");
            }
        }

        throw exception;
    }

    private CurrentUserVO buildCurrentUserVO(AppUser user, ContributorRequest latestRequest) {
        CurrentUserVO currentUserVO = new CurrentUserVO();
        currentUserVO.setUserId(user.getUserId());
        currentUserVO.setName(user.getName());
        currentUserVO.setEmail(user.getEmail());
        currentUserVO.setBio(user.getBio());
        try {
            currentUserVO.setRole(UserRoleEnum.fromValue(user.getRole()).getApiValue());
        } catch (IllegalArgumentException exception) {
            currentUserVO.setRole(user.getRole());
        }
        currentUserVO.setLatestContributorRequestId(latestRequest == null ? null : latestRequest.getRequestId());

        String contributorStatus = latestRequest == null
                ? (Boolean.TRUE.equals(user.getContributor())
                ? ContributorApplicationStatusEnum.APPROVED.getValue()
                : "NONE")
                : latestRequest.getStatus();
        currentUserVO.setContributorStatus(contributorStatus);
        currentUserVO.setContributor(Boolean.TRUE.equals(user.getContributor()));

        return currentUserVO;
    }

    private ContributorRequestVO buildContributorRequestVO(ContributorRequest contributorRequest, AppUser user) {
        ContributorRequestVO contributorRequestVO = new ContributorRequestVO();
        contributorRequestVO.setRequestId(contributorRequest.getRequestId());
        contributorRequestVO.setUserId(contributorRequest.getUserId());
        contributorRequestVO.setReviewedBy(contributorRequest.getReviewedBy());
        contributorRequestVO.setUserName(user.getName());
        contributorRequestVO.setUserEmail(user.getEmail());
        contributorRequestVO.setApplicationReason(contributorRequest.getApplicationReason());
        contributorRequestVO.setStatus(contributorRequest.getStatus());
        contributorRequestVO.setReviewComment(contributorRequest.getReviewComment());
        contributorRequestVO.setRequestedAt(contributorRequest.getRequestedAt());
        contributorRequestVO.setReviewedAt(contributorRequest.getReviewedAt());
        contributorRequestVO.setUpdatedAt(contributorRequest.getUpdatedAt());
        return contributorRequestVO;
    }

    private static class RegisterVerificationCodeEntry {

        private final String code;
        private final LocalDateTime expiresAt;
        private final LocalDateTime availableResendAt;

        private RegisterVerificationCodeEntry(String code,
                                              LocalDateTime expiresAt,
                                              LocalDateTime availableResendAt) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.availableResendAt = availableResendAt;
        }

        public String getCode() {
            return code;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public LocalDateTime getAvailableResendAt() {
            return availableResendAt;
        }
    }
}
