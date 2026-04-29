package com.cpt202.HerLink.service.notification;

import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.Resource;

public interface EmailNotificationService {

    void notifyContributorApplicationApproved(AppUser user);

    void notifyContributorRoleRevoked(AppUser user);

    void notifyResourcePendingReview(AppUser contributor, Resource resource);

    void notifyResourceApproved(AppUser contributor, String resourceTitle, Long resourceId);

    void notifyResourceRejected(AppUser contributor, String resourceTitle, Long resourceId, String feedbackComment);
}
