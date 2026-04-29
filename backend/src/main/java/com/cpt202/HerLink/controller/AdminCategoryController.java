package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.admin.AdminCategoryRequest;
import com.cpt202.HerLink.dto.admin.AdminCategoryResponse;
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
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final AdminClassificationManagementService classificationManagementService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public AdminCategoryController(AdminClassificationManagementService classificationManagementService,
                                   ResourcePermissionChecker resourcePermissionChecker) {
        this.classificationManagementService = classificationManagementService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @GetMapping
    public List<AdminCategoryResponse> getAllCategories(HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.getAllCategories();
    }

    @GetMapping("/active")
    public List<AdminCategoryResponse> getActiveCategories(HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.getActiveCategories();
    }

    @PostMapping
    public AdminCategoryResponse createCategory(@RequestBody(required = false) AdminCategoryRequest request,
                                                HttpServletRequest httpServletRequest) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(httpServletRequest);
        return classificationManagementService.createCategory(request, administratorName(adminUser));
    }

    @PutMapping("/{id}")
    public AdminCategoryResponse updateCategory(@PathVariable Long id,
                                                @RequestBody(required = false) AdminCategoryRequest request,
                                                HttpServletRequest httpServletRequest) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(httpServletRequest);
        return classificationManagementService.updateCategory(id, request, administratorName(adminUser));
    }

    @PutMapping("/{id}/deactivate")
    public AdminCategoryResponse deactivateCategory(@PathVariable Long id, HttpServletRequest request) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.deactivateCategory(id, administratorName(adminUser));
    }

    @PutMapping("/{id}/activate")
    public AdminCategoryResponse activateCategory(@PathVariable Long id, HttpServletRequest request) {
        CurrentUserVO adminUser = resourcePermissionChecker.requireAdminUser(request);
        return classificationManagementService.activateCategory(id, administratorName(adminUser));
    }

    private String administratorName(CurrentUserVO adminUser) {
        if (adminUser.getName() != null && !adminUser.getName().isBlank()) {
            return adminUser.getName();
        }
        return "user_" + adminUser.getUserId();
    }
}
