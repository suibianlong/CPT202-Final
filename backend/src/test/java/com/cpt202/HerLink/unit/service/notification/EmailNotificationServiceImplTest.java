package com.cpt202.HerLink.unit.service.notification;

import com.cpt202.HerLink.service.notification.*;
import com.cpt202.HerLink.service.notification.EmailNotificationServiceImpl;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.Resource;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailNotificationServiceImpl Test")
class EmailNotificationServiceImplTest {

    @Mock
    private ObjectProvider<JavaMailSender> javaMailSenderProvider;

    @Mock
    private JavaMailSender javaMailSender;

    private EmailNotificationServiceImpl emailService;

    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    private static final String TEST_HOST = "smtp.test.com";
    private static final String TEST_FROM = "no-reply@herlink.com";
    private static final String USER_EMAIL = "user@test.com";
    private static final Long USER_ID = 1L;
    private static final String USER_NAME = "Test User";
    private static final Long RES_ID = 100L;
    private static final String RES_TITLE = "Test Resource";

    @BeforeEach
    void setUp() {
        emailService = new EmailNotificationServiceImpl(javaMailSenderProvider, TEST_HOST, TEST_FROM);
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);

        logger = (Logger) LoggerFactory.getLogger(EmailNotificationServiceImpl.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    private List<ILoggingEvent> getLogs() {
        return listAppender.list;
    }

    @Nested
    @DisplayName("notifyContributorApplicationApproved")
    class NotifyContributorApplicationApproved {

        @Test
        @DisplayName("Should send successfully with valid user and no logs")
        void validUser_SendSuccess_NoLogs() {
            emailService.notifyContributorApplicationApproved(user());
            assertTrue(getLogs().isEmpty());
        }

        @Test
        @DisplayName("Should log warning when user is null")
        void userNull_LogWarning() {
            emailService.notifyContributorApplicationApproved(null);
            assertEquals(1, getLogs().size());
            ILoggingEvent log = getLogs().get(0);
            assertEquals("WARN", log.getLevel().toString());
            assertTrue(log.getMessage().contains("user record is missing"));
        }

        @Test
        @DisplayName("Should log warning when user email is blank")
        void emptyEmail_LogWarning() {
            emailService.notifyContributorApplicationApproved(userWithEmptyEmail());
            assertEquals(1, getLogs().size());
            assertTrue(getLogs().get(0).getMessage().contains("no email"));
        }
    }

    @Nested
    @DisplayName("notifyContributorRoleRevoked")
    class NotifyContributorRoleRevoked {

        @Test
        @DisplayName("Should send successfully with valid user and no logs")
        void validUser_SendSuccess_NoLogs() {
            emailService.notifyContributorRoleRevoked(user());
            assertTrue(getLogs().isEmpty());
        }

        @Test
        @DisplayName("Should log warning when user is null")
        void userNull_LogWarning() {
            emailService.notifyContributorRoleRevoked(null);
            assertEquals(1, getLogs().size());
        }
    }

    @Nested
    @DisplayName("notifyResourcePendingReview")
    class NotifyResourcePendingReview {

        @Test
        @DisplayName("Should send successfully with valid user and no logs")
        void validUser_SendSuccess_NoLogs() {
            emailService.notifyResourcePendingReview(user(), resource());
            assertTrue(getLogs().isEmpty());
        }

        @Test
        @DisplayName("Should log warning when user is null")
        void userNull_LogWarning() {
            emailService.notifyResourcePendingReview(null, resource());
            assertEquals(1, getLogs().size());
        }

        @Test
        @DisplayName("Should send successfully when resource is null")
        void resourceNull_SendSuccess_NoLogs() {
            emailService.notifyResourcePendingReview(user(), null);
            assertTrue(getLogs().isEmpty());
        }
    }

    @Nested
    @DisplayName("notifyResourceApproved")
    class NotifyResourceApproved {

        @Test
        @DisplayName("Should send successfully with valid user and no logs")
        void validUser_SendSuccess_NoLogs() {
            emailService.notifyResourceApproved(user(), RES_TITLE, RES_ID);
            assertTrue(getLogs().isEmpty());
        }

        @Test
        @DisplayName("Should log warning when user is null")
        void userNull_LogWarning() {
            emailService.notifyResourceApproved(null, RES_TITLE, RES_ID);
            assertEquals(1, getLogs().size());
        }
    }

    @Nested
    @DisplayName("notifyResourceRejected")
    class NotifyResourceRejected {

        @Test
        @DisplayName("Should send successfully with valid user and no logs")
        void validUser_SendSuccess_NoLogs() {
            emailService.notifyResourceRejected(user(), RES_TITLE, RES_ID, "Good");
            assertTrue(getLogs().isEmpty());
        }

        @Test
        @DisplayName("Should log warning when user is null")
        void userNull_LogWarning() {
            emailService.notifyResourceRejected(null, RES_TITLE, RES_ID, null);
            assertEquals(1, getLogs().size());
        }
    }

    @Nested
    @DisplayName("Global Mail Sending Exceptions and Edge Cases")
    class GlobalMailExceptions {

        @Test
        @DisplayName("Should log failure when sending fails")
        void mailException_LogFailure() {
            // Create an anonymous subclass instance of MailException
            MailException mailException = new MailException("Test send failed") {};

            // Force the specification of the SimpleMailMessage type to eliminate method ambiguity
            doThrow(mailException)
                .when(javaMailSender)
                .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

            emailService.notifyContributorApplicationApproved(user());

            assertEquals(1, getLogs().size());
            assertTrue(getLogs().get(0).getMessage().contains("could not be sent"));
        }

        @Test
        @DisplayName("Should log warning when mail host is blank")
        void hostBlank_LogNotConfigured() {
            EmailNotificationServiceImpl svc = new EmailNotificationServiceImpl(javaMailSenderProvider, "", TEST_FROM);
            svc.notifyContributorApplicationApproved(user());
            assertTrue(getLogs().stream().anyMatch(log -> log.getMessage().contains("mail host is not configured")));
        }

        @Test
        @DisplayName("Should log warning when JavaMailSender is unavailable")
        void senderUnavailable_LogUnavailable() {
            when(javaMailSenderProvider.getIfAvailable()).thenReturn(null);
            emailService.notifyContributorApplicationApproved(user());
            assertTrue(getLogs().stream().anyMatch(log -> log.getMessage().contains("JavaMailSender is unavailable")));
        }

        @Test
        @DisplayName("Should send without from address when configuration is blank")
        void blankFromAddress_SendWithoutFromField() {
            EmailNotificationServiceImpl serviceWithoutFrom = new EmailNotificationServiceImpl(
                    javaMailSenderProvider,
                    TEST_HOST,
                    "   "
            );

            final SimpleMailMessage[] capturedMessage = new SimpleMailMessage[1];
            doAnswer(invocation -> {
                capturedMessage[0] = invocation.getArgument(0);
                return null;
            }).when(javaMailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));

            serviceWithoutFrom.notifyContributorApplicationApproved(user());

            assertNotNull(capturedMessage[0]);
            assertEquals(USER_EMAIL, capturedMessage[0].getTo()[0]);
            assertEquals(null, capturedMessage[0].getFrom());
        }
    }

    @Nested
    @DisplayName("buildUserLabel")
    class BuildUserLabel {

        @Test
        @DisplayName("Should return user name when name is present")
        void userWithName_ReturnName() {
            AppUser user = new AppUser();
            user.setName(USER_NAME);
            assertEquals(USER_NAME, invokeBuildUserLabel(user));
        }

        @Test
        @DisplayName("Should return user ID when name is missing")
        void userWithoutName_ReturnUserId() {
            AppUser user = new AppUser();
            user.setUserId(USER_ID);
            user.setName(null);
            assertEquals("user 1", invokeBuildUserLabel(user));
        }

        @Test
        @DisplayName("Should return user ID when name is blank")
        void userWithBlankName_ReturnUserId() {
            AppUser user = new AppUser();
            user.setUserId(USER_ID);
            user.setName("   ");
            assertEquals("user 1", invokeBuildUserLabel(user));
        }

        @Test
        @DisplayName("Should return default 'user' when user is null")
        void nullUser_ReturnDefault() {
            assertEquals("user", invokeBuildUserLabel(null));
        }
    }

    @Nested
    @DisplayName("buildResourceLabel")
    class BuildResourceLabel {

        @Test
        @DisplayName("Should return title when title is present")
        void resourceWithTitle_ReturnTitle() {
            assertEquals(RES_TITLE, invokeBuildResourceLabel(RES_TITLE, RES_ID));
        }

        @Test
        @DisplayName("Should return resource ID when title is missing")
        void resourceWithoutTitle_ReturnResourceId() {
            assertEquals("resource 100", invokeBuildResourceLabel(null, RES_ID));
        }

        @Test
        @DisplayName("Should return resource ID when title is blank")
        void resourceWithBlankTitle_ReturnResourceId() {
            assertEquals("resource 100", invokeBuildResourceLabel("   ", RES_ID));
        }
    }

    private String invokeBuildUserLabel(AppUser user) {
        try {
            var method = EmailNotificationServiceImpl.class.getDeclaredMethod("buildUserLabel", AppUser.class);
            method.setAccessible(true);
            return (String) method.invoke(emailService, user);
        } catch (Exception e) {
            fail("Failed to invoke buildUserLabel", e);
            return null;
        }
    }

    private String invokeBuildResourceLabel(String title, Long id) {
        try {
            var method = EmailNotificationServiceImpl.class.getDeclaredMethod("buildResourceLabel", String.class, Long.class);
            method.setAccessible(true);
            return (String) method.invoke(emailService, title, id);
        } catch (Exception e) {
            fail("Failed to invoke buildResourceLabel", e);
            return null;
        }
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setUserId(USER_ID);
        user.setName(USER_NAME);
        user.setEmail(USER_EMAIL);
        return user;
    }

    private AppUser userWithEmptyEmail() {
        AppUser user = new AppUser();
        user.setUserId(USER_ID);
        user.setEmail("");
        return user;
    }

    private Resource resource() {
        Resource resource = new Resource();
        resource.setId(RES_ID);
        resource.setTitle(RES_TITLE);
        return resource;
    }
}
