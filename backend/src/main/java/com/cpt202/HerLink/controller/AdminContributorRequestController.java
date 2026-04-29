package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.auth.ContributorReviewDecisionRequest;
import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.ContributorRequestVO;
import com.cpt202.HerLink.vo.CurrentUserVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contributor-requests")
public class AdminContributorRequestController {

    private final UserAccessService userAccessService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public AdminContributorRequestController(UserAccessService userAccessService,
                                             ResourcePermissionChecker resourcePermissionChecker) {
        this.userAccessService = userAccessService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @GetMapping("/pending")
    public List<ContributorRequestVO> listPendingRequests(HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUserId(request);
        return userAccessService.listPendingContributorRequests();
    }

    @GetMapping("/{requestId}")
    public ContributorRequestVO getRequestDetail(@PathVariable Long requestId, HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUserId(request);
        return userAccessService.getContributorRequestDetail(requestId);
    }

    @GetMapping("/approved-contributors")
    public List<ContributorRequestVO> listApprovedContributors(HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUserId(request);
        return userAccessService.listApprovedContributors();
    }

    @PostMapping("/{requestId}/decision")
    public ContributorRequestVO reviewRequest(@PathVariable Long requestId,
                                              @RequestBody ContributorReviewDecisionRequest request,
                                              HttpServletRequest httpServletRequest) {
        Long adminUserId = resourcePermissionChecker.requireAdminUserId(httpServletRequest);
        return userAccessService.reviewContributorRequest(adminUserId, requestId, request);
    }

    @PostMapping("/contributors/{userId}/revoke")
    public ContributorRequestVO revokeContributor(@PathVariable Long userId,
                                                  HttpServletRequest httpServletRequest) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(httpServletRequest);
        return userAccessService.revokeContributor(
                adminUser.getUserId(),
                userId,
                administratorName(adminUser)
        );
    }

    private String administratorName(CurrentUserVO adminUser) {
        if (adminUser.getName() != null && !adminUser.getName().isBlank()) {
            return adminUser.getName();
        }
        return "user_" + adminUser.getUserId();
    }
}
