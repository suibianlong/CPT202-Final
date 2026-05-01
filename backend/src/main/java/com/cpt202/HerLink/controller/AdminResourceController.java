package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.admin.AdminResourceLifecycleResponse;
import com.cpt202.HerLink.dto.admin.ResourceLifecycleRow;
import com.cpt202.HerLink.service.admin.AdminResourceLifecycleService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CurrentUserVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public List<ResourceLifecycleRow> listResources(@RequestParam(required = false) String status,
                                                    HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return lifecycleService.listResources(status);
    }

    @PostMapping("/{resourceId}/archive")
    public AdminResourceLifecycleResponse archiveResource(@PathVariable Long resourceId, HttpServletRequest request) {
        CurrentUserVO currentUser = resourcePermissionChecker.requireAdminUser(request);
        return lifecycleService.archiveResource(resourceId, resolveAdministratorName(currentUser));
    }

    private String resolveAdministratorName(CurrentUserVO currentUser) {
        if (currentUser == null) {
            return "admin";
        }
        if (currentUser.getName() != null && !currentUser.getName().isBlank()) {
            return currentUser.getName();
        }
        if (currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) {
            return currentUser.getEmail();
        }
        return currentUser.getUserId() == null ? "admin" : "admin#" + currentUser.getUserId();
    }
}
