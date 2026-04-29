package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.admin.AdminResourceTypeRequest;
import com.cpt202.HerLink.dto.admin.AdminResourceTypeResponse;
import com.cpt202.HerLink.service.admin.AdminClassificationManagementService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CurrentUserVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resource-types")
public class AdminResourceTypeController {

    private final AdminClassificationManagementService classificationManagementService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public AdminResourceTypeController(AdminClassificationManagementService classificationManagementService,
                                       ResourcePermissionChecker resourcePermissionChecker) {
        this.classificationManagementService = classificationManagementService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @GetMapping
    public List<AdminResourceTypeResponse> getAllResourceTypes(HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.getAllResourceTypes();
    }

    @GetMapping("/active")
    public List<AdminResourceTypeResponse> getActiveResourceTypes(HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.getActiveResourceTypes();
    }

    @PostMapping
    public AdminResourceTypeResponse createResourceType(@RequestBody(required = false) AdminResourceTypeRequest request,
                                                        HttpServletRequest httpServletRequest) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(httpServletRequest);
        return classificationManagementService.createResourceType(request, administratorName(adminUser));
    }

    @PutMapping("/{id}")
    public AdminResourceTypeResponse updateResourceType(@PathVariable Long id,
                                                        @RequestBody(required = false) AdminResourceTypeRequest request,
                                                        HttpServletRequest httpServletRequest) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(httpServletRequest);
        return classificationManagementService.updateResourceType(id, request, administratorName(adminUser));
    }

    @PutMapping("/{id}/deactivate")
    public AdminResourceTypeResponse deactivateResourceType(@PathVariable Long id, HttpServletRequest request) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.deactivateResourceType(id, administratorName(adminUser));
    }

    @PutMapping("/{id}/activate")
    public AdminResourceTypeResponse activateResourceType(@PathVariable Long id, HttpServletRequest request) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.activateResourceType(id, administratorName(adminUser));
    }

    private String administratorName(CurrentUserVO adminUser) {
        if (adminUser.getName() != null && !adminUser.getName().isBlank()) {
            return adminUser.getName();
        }
        return "user_" + adminUser.getUserId();
    }
}
