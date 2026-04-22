package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.resource.ResourceVersionRollbackRequest;
import com.cpt202.HerLink.service.ContributorResourceService;
import com.cpt202.HerLink.service.ResourceVersionService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceSubmissionVO;
import com.cpt202.HerLink.vo.ResourceVersionCompareVO;
import com.cpt202.HerLink.vo.ResourceVersionVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contributor/resources")
public class ContributorResourceHistoryController {

    private final ContributorResourceService contributorResourceService;
    private final ResourceVersionService resourceVersionService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public ContributorResourceHistoryController(ContributorResourceService contributorResourceService,
                                                ResourceVersionService resourceVersionService,
                                                ResourcePermissionChecker resourcePermissionChecker) {
        this.contributorResourceService = contributorResourceService;
        this.resourceVersionService = resourceVersionService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @GetMapping("/{resourceId}/submissions")
    public List<ResourceSubmissionVO> listSubmissionHistory(@PathVariable Long resourceId,
                                                            HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return contributorResourceService.listSubmissionHistory(currentUserId, resourceId);
    }

    @GetMapping("/{resourceId}/versions")
    public List<ResourceVersionVO> listVersions(@PathVariable Long resourceId,
                                                HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return resourceVersionService.listVersions(currentUserId, resourceId);
    }

    @GetMapping("/{resourceId}/versions/{versionNo}")
    public ResourceVersionVO getVersion(@PathVariable Long resourceId,
                                        @PathVariable Integer versionNo,
                                        HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return resourceVersionService.getVersion(currentUserId, resourceId, versionNo);
    }

    @GetMapping("/{resourceId}/versions/compare")
    public ResourceVersionCompareVO compareVersions(@PathVariable Long resourceId,
                                                    @RequestParam("v1") Integer leftVersionNo,
                                                    @RequestParam("v2") Integer rightVersionNo,
                                                    HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return resourceVersionService.compareVersions(currentUserId, resourceId, leftVersionNo, rightVersionNo);
    }

    @PostMapping("/{resourceId}/versions/{versionNo}/rollback")
    public ResourceDetailVO rollbackToVersion(@PathVariable Long resourceId,
                                              @PathVariable Integer versionNo,
                                              @RequestBody(required = false) ResourceVersionRollbackRequest requestBody,
                                              HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return resourceVersionService.rollbackToVersion(currentUserId, resourceId, versionNo);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        return resourcePermissionChecker.requireContributorUserId(request);
    }
}
