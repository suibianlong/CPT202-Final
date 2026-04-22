package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.admin.AdminOperationHistoryResponse;
import com.cpt202.HerLink.service.admin.AdminOperationHistoryService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operation-history")
public class AdminOperationHistoryController {

    private final AdminOperationHistoryService operationHistoryService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public AdminOperationHistoryController(AdminOperationHistoryService operationHistoryService,
                                           ResourcePermissionChecker resourcePermissionChecker) {
        this.operationHistoryService = operationHistoryService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @GetMapping
    public List<AdminOperationHistoryResponse> getOperationHistory(@RequestParam(required = false) String module,
                                                                   HttpServletRequest request) {
        resourcePermissionChecker.requireAdminUser(request);
        return operationHistoryService.getOperationHistory(module);
    }
}
