package com.cpt202.HerLink.util;

import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.vo.CurrentUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class ResourcePermissionChecker {

    private final UserAccessService userAccessService;

    public ResourcePermissionChecker(UserAccessService userAccessService) {
        this.userAccessService = userAccessService;
    }

    public CurrentUserVO requireCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw AppException.unauthorized("Please log in first.");
        }

        Object userIdValue = session.getAttribute(SessionKeys.USER_ID);
        if (userIdValue == null) {
            throw AppException.unauthorized("Please log in first.");
        }

        Long currentUserId = parseLongValue(userIdValue);
        try {
            return userAccessService.getCurrentUserById(currentUserId);
        } catch (AppException exception) {
            if (exception.getStatusCode() == 404) {
                clearLoginSession(request);
                throw AppException.unauthorized("Your session is no longer valid. Please log in again.");
            }
            throw exception;
        }
    }

    public Long requireAuthenticatedUserId(HttpServletRequest request) {
        return requireCurrentUser(request).getUserId();
    }

    public Long requireContributorUserId(HttpServletRequest request) {
        CurrentUserVO currentUser = requireCurrentUser(request);
        if (!currentUser.isContributor()) {
            throw AppException.forbidden("Contributor access requires an approved contributor request.");
        }
        return currentUser.getUserId();
    }

    public Long requireAdminUserId(HttpServletRequest request) {
        return requireAdminUser(request).getUserId();
    }

    public CurrentUserVO requireAdminUser(HttpServletRequest request) {
        CurrentUserVO currentUser = requireCurrentUser(request);
        if (!UserRoleEnum.ADMINISTRATOR.matches(currentUser.getRole())) {
            throw AppException.forbidden("Administrator permission is required.");
        }
        return currentUser;
    }

    public void storeLoginSession(HttpServletRequest request, Long userId) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionKeys.USER_ID, userId);
    }

    public void clearLoginSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private Long parseLongValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        String text = value == null ? "" : value.toString().trim();
        if (text.isEmpty()) {
            throw AppException.unauthorized("Current user session is incomplete.");
        }

        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            throw AppException.unauthorized("Current user identity is invalid.");
        }
    }
}
