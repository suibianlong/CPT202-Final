package com.cpt202.HerLink.service.notification;

import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationServiceImpl.class);

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final String mailHost;
    private final String fromAddress;

    public EmailNotificationServiceImpl(ObjectProvider<JavaMailSender> javaMailSenderProvider,
                                        @Value("${spring.mail.host:}") String mailHost,
                                        @Value("${HerLink.notification.from-address:${HerLink.register-verification.from-address:}}")
                                        String fromAddress) {
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.mailHost = mailHost == null ? "" : mailHost.trim();
        this.fromAddress = fromAddress == null ? "" : fromAddress.trim();
    }

    @Override
    public void notifyContributorApplicationApproved(AppUser user) {
        String userLabel = buildUserLabel(user);
        sendNotification(
                user,
                "Contributor Application Approved",
                "Hello " + userLabel + "," + System.lineSeparator()
                        + System.lineSeparator()
                        + "Your contributor application has been approved." + System.lineSeparator()
                        + "You can now use contributor features to create, edit, and submit resources."
        );
    }

    @Override
    public void notifyContributorRoleRevoked(AppUser user) {
        String userLabel = buildUserLabel(user);
        sendNotification(
                user,
                "Contributor Role Revoked",
                "Hello " + userLabel + "," + System.lineSeparator()
                        + System.lineSeparator()
                        + "Your contributor role has been revoked by an administrator." + System.lineSeparator()
                        + "Your account is still available as a registered user."
        );
    }

    @Override
    public void notifyResourcePendingReview(AppUser contributor, Resource resource) {
        sendNotification(
                contributor,
                "Resource Status Updated",
                "Hello " + buildUserLabel(contributor) + "," + System.lineSeparator()
                        + System.lineSeparator()
                        + "Your resource \"" + buildResourceLabel(resource == null ? null : resource.getTitle(),
                        resource == null ? null : resource.getId()) + "\" is now pending review."
        );
    }

    @Override
    public void notifyResourceApproved(AppUser contributor, String resourceTitle, Long resourceId) {
        sendNotification(
                contributor,
                "Resource Approved",
                "Hello " + buildUserLabel(contributor) + "," + System.lineSeparator()
                        + System.lineSeparator()
                        + "Your resource \"" + buildResourceLabel(resourceTitle, resourceId)
                        + "\" has been approved."
        );
    }

    @Override
    public void notifyResourceRejected(AppUser contributor, String resourceTitle, Long resourceId, String feedbackComment) {
        String feedback = feedbackComment == null || feedbackComment.isBlank()
                ? "No feedback was provided."
                : feedbackComment.trim();
        sendNotification(
                contributor,
                "Resource Rejected",
                "Hello " + buildUserLabel(contributor) + "," + System.lineSeparator()
                        + System.lineSeparator()
                        + "Your resource \"" + buildResourceLabel(resourceTitle, resourceId)
                        + "\" has been rejected." + System.lineSeparator()
                        + System.lineSeparator()
                        + "Feedback: " + feedback
        );
    }

    private void sendNotification(AppUser user, String subject, String text) {
        if (user == null) {
            LOGGER.warn("Email notification skipped because the user record is missing. Subject: {}", subject);
            return;
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            LOGGER.warn("Email notification skipped because user {} has no email. Subject: {}", user.getUserId(), subject);
            return;
        }
        if (mailHost.isBlank()) {
            LOGGER.warn("Email notification skipped because mail host is not configured. Subject: {}", subject);
            return;
        }

        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            LOGGER.warn("Email notification skipped because JavaMailSender is unavailable. Subject: {}", subject);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (!fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(text);

        try {
            javaMailSender.send(message);
        } catch (MailException exception) {
            LOGGER.warn("Email notification could not be sent to {}. Subject: {}", user.getEmail(), subject, exception);
        }
    }

    private String buildUserLabel(AppUser user) {
        if (user == null) {
            return "user";
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName().trim();
        }
        return "user " + user.getUserId();
    }

    private String buildResourceLabel(String resourceTitle, Long resourceId) {
        if (resourceTitle != null && !resourceTitle.isBlank()) {
            return resourceTitle.trim();
        }
        return "resource " + resourceId;
    }
}
