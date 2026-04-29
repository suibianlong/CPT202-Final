package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.admin.AdminTagRequest;
import com.cpt202.HerLink.dto.admin.AdminTagResponse;
import com.cpt202.HerLink.dto.admin.TagUsageHistoryResponse;
import com.cpt202.HerLink.service.admin.AdminClassificationManagementService;
import com.cpt202.HerLink.service.admin.AdminUsageHistoryService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CurrentUserVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

    private final AdminClassificationManagementService classificationManagementService;
    private final AdminUsageHistoryService usageHistoryService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public AdminTagController(AdminClassificationManagementService classificationManagementService,
                              AdminUsageHistoryService usageHistoryService,
                              ResourcePermissionChecker resourcePermissionChecker) {
        this.classificationManagementService = classificationManagementService;
        this.usageHistoryService = usageHistoryService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @GetMapping
    public List<AdminTagResponse> getAllTags(HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.getAllTags();
    }

    @GetMapping("/active")
    public List<AdminTagResponse> getActiveTags(HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.getActiveTags();
    }

    @GetMapping("/usage-history")
    public List<TagUsageHistoryResponse> getTagUsageHistory(HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return usageHistoryService.getTagUsageHistory();
    }

    @PutMapping("/{id}")
    public AdminTagResponse updateTag(@PathVariable Long id,
                                      @RequestBody(required = false) AdminTagRequest request,
                                      HttpServletRequest httpServletRequest) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(httpServletRequest);
        return classificationManagementService.updateTag(id, request, administratorName(adminUser));
    }

    @PutMapping("/{id}/deactivate")
    public AdminTagResponse deactivateTag(@PathVariable Long id, HttpServletRequest request) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.deactivateTag(id, administratorName(adminUser));
    }

    @PutMapping("/{id}/activate")
    public AdminTagResponse activateTag(@PathVariable Long id, HttpServletRequest request) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.activateTag(id, administratorName(adminUser));
    }

    private String administratorName(CurrentUserVO adminUser) {
        if (adminUser.getName() != null && !adminUser.getName().isBlank()) {
            return adminUser.getName();
        }
        return "user_" + adminUser.getUserId();
    }
}