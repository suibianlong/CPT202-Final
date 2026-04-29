package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.admin.AdminResourceLifecycleResponse;
import com.cpt202.HerLink.service.admin.AdminResourceLifecycleService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resources")
public class AdminResourceController {

    private final AdminResourceLifecycleService lifecycleService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public AdminResourceController(AdminResourceLifecycleService lifecycleService,
                                   ResourcePermissionChecker resourcePermissionChecker) {
        this.lifecycleService = lifecycleService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @PostMapping("/{resourceId}/archive")
    public AdminResourceLifecycleResponse archiveResource(@PathVariable Long resourceId, HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return lifecycleService.archiveResource(resourceId);
    }
}
