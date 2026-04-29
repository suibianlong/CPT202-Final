package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.auth.ContributorRequestSubmitRequest;
import com.cpt202.HerLink.service.UserAccessService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.ContributorRequestVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contributor-requests")
public class ContributorRequestController {

    private final UserAccessService userAccessService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public ContributorRequestController(UserAccessService userAccessService,
                                        ResourcePermissionChecker resourcePermissionChecker) {
        this.userAccessService = userAccessService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContributorRequestVO submitContributorRequest(@RequestBody ContributorRequestSubmitRequest request,
                                                        HttpServletRequest httpServletRequest) {
        Long currentUserId = resourcePermissionChecker.requireAuthenticatedUserId(httpServletRequest);
        return userAccessService.submitContributorRequest(currentUserId, request);
    }

    @GetMapping("/me")
    public ContributorRequestVO getMyLatestContributorRequest(HttpServletRequest request) {
        Long currentUserId = resourcePermissionChecker.requireAuthenticatedUserId(request);
        return userAccessService.getMyLatestContributorRequest(currentUserId);
    }
}
