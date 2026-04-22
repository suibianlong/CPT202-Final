package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.resource.ResourceQueryRequest;
import com.cpt202.HerLink.dto.resource.ResourceSubmitRequest;
import com.cpt202.HerLink.dto.resource.ResourceUpdateRequest;
import com.cpt202.HerLink.service.ContributorResourceService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/contributor/resources")
public class ContributorResourceController {

    private final ContributorResourceService contributorResourceService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public ContributorResourceController(ContributorResourceService contributorResourceService,
                                         ResourcePermissionChecker resourcePermissionChecker) {
        this.contributorResourceService = contributorResourceService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    // create draft
    @PostMapping
    public ResourceDetailVO createDraft(HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return contributorResourceService.createDraft(currentUserId);
    }

    // update resource
    @PutMapping("/{resourceId}")
    public ResourceDetailVO updateResource(@PathVariable Long resourceId,
                                           @RequestBody ResourceUpdateRequest request,
                                           HttpServletRequest httpServletRequest) {
        Long currentUserId = getCurrentUserId(httpServletRequest);
        return contributorResourceService.updateResource(currentUserId, resourceId, request);
    }

    // upload files
    @PostMapping("/{resourceId}/files")
    public ResourceDetailVO uploadFiles(@PathVariable Long resourceId,
                                        @RequestPart(value = "previewImage", required = false) MultipartFile previewImage,
                                        @RequestPart(value = "mediaFile", required = false) MultipartFile mediaFile,
                                        HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return contributorResourceService.uploadFiles(currentUserId, resourceId, previewImage, mediaFile);
    }

    // get my resource list
    @GetMapping("/my")
    public List<ResourceListItemVO> listMyResources(ResourceQueryRequest request, HttpServletRequest httpServletRequest) {
        Long currentUserId = getCurrentUserId(httpServletRequest);
        return contributorResourceService.listMyResources(currentUserId, request);
    }

    // get my specific resource detail
    @GetMapping("/{resourceId}")
    public ResourceDetailVO getMyResourceDetail(@PathVariable Long resourceId, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        return contributorResourceService.getMyResourceDetail(currentUserId, resourceId);
    }

    // submit resource for review
    @PostMapping("/{resourceId}/submit")
    public void submitResource(@PathVariable Long resourceId,
                               @RequestBody(required = false) ResourceSubmitRequest request,
                               HttpServletRequest httpServletRequest) {
        Long currentUserId = getCurrentUserId(httpServletRequest);
        contributorResourceService.submitResource(currentUserId, resourceId, request);
    }

    // get category options
    @GetMapping("/category-options")
    public List<CategoryTagOptionVO> listCategoryOptions() {
        return contributorResourceService.listCategoryOptions();
    }

    // get tag options
    @GetMapping("/tag-options")
    public List<CategoryTagOptionVO> listTagOptions() {
        return contributorResourceService.listTagOptions();
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        return resourcePermissionChecker.requireContributorUserId(request);
    }
}
