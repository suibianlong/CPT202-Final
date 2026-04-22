package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.admin.ClassificationUsageHistoryResponse;
import com.cpt202.HerLink.service.admin.AdminUsageHistoryService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/classifications")
public class AdminClassificationController {

    private final AdminUsageHistoryService usageHistoryService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public AdminClassificationController(AdminUsageHistoryService usageHistoryService,
                                         ResourcePermissionChecker resourcePermissionChecker) {
        this.usageHistoryService = usageHistoryService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @GetMapping("/usage-history")
    public List<ClassificationUsageHistoryResponse> getClassificationUsageHistory(HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return usageHistoryService.getClassificationUsageHistory();
    }
}
