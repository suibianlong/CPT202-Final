package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.service.ViewerResourceService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/viewer/resources")
public class ViewerResourceController {

    private final ViewerResourceService viewerResourceService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public ViewerResourceController(ViewerResourceService viewerResourceService,
                                    ResourcePermissionChecker resourcePermissionChecker) {
        this.viewerResourceService = viewerResourceService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @GetMapping
    public List<ResourceListItemVO> listApprovedResources(@RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String type,
                                                          @RequestParam(required = false) Long categoryId,
                                                          @RequestParam(required = false) String sortBy,
                                                          HttpServletRequest request) {
        resourcePermissionChecker.requireAuthenticatedUserId(request);
        return viewerResourceService.listApprovedResources(keyword, type, categoryId, sortBy);
    }

    @GetMapping("/{resourceId}")
    public ResourceDetailVO getApprovedResourceDetail(@PathVariable Long resourceId,
                                                      HttpServletRequest request) {
        resourcePermissionChecker.requireAuthenticatedUserId(request);
        return viewerResourceService.getApprovedResourceDetail(resourceId);
    }

    @GetMapping("/category-options")
    public List<CategoryTagOptionVO> listCategoryOptions(HttpServletRequest request) {
        resourcePermissionChecker.requireAuthenticatedUserId(request);
        return viewerResourceService.listCategoryOptions();
    }

    @GetMapping("/resource-type-options")
    public List<CategoryTagOptionVO> listResourceTypeOptions(HttpServletRequest request) {
        resourcePermissionChecker.requireAuthenticatedUserId(request);
        return viewerResourceService.listResourceTypeOptions();
    }
}
