package com.cpt202.HerLink.util;

import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.vo.CurrentUserVO;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourcePermissionCheckerTest {

    @Mock
    private UserAccessService userAccessService;

    @Test
    void requireContributorUserId_shouldRejectRevokedContributor() {
        ResourcePermissionChecker checker = new ResourcePermissionChecker(userAccessService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionKeys.USER_ID, 7L);

        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(7L);
        currentUser.setContributor(false);
        currentUser.setContributorStatus("APPROVED");
        when(userAccessService.getCurrentUserById(7L)).thenReturn(currentUser);

        AppException exception = assertThrows(AppException.class, () -> checker.requireContributorUserId(request));

        assertEquals(403, exception.getStatusCode());
    }

    @Test
    void requireContributorUserId_shouldAllowActiveContributor() {
        ResourcePermissionChecker checker = new ResourcePermissionChecker(userAccessService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionKeys.USER_ID, 8L);

        CurrentUserVO currentUser = new CurrentUserVO();
        currentUser.setUserId(8L);
        currentUser.setContributor(true);
        when(userAccessService.getCurrentUserById(8L)).thenReturn(currentUser);

        Long currentUserId = checker.requireContributorUserId(request);

        assertEquals(8L, currentUserId);
    }
}
