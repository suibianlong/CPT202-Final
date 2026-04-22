package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.viewer.CommentCreateRequest;
import com.cpt202.HerLink.service.ViewerCommentService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.CommentVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/viewer/resources/{resourceId}/comments")
public class ViewerCommentController {

    private final ViewerCommentService viewerCommentService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public ViewerCommentController(ViewerCommentService viewerCommentService,
                                   ResourcePermissionChecker resourcePermissionChecker) {
        this.viewerCommentService = viewerCommentService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @GetMapping
    public List<CommentVO> listComments(@PathVariable Long resourceId,
                                        HttpServletRequest request) {
        resourcePermissionChecker.requireAuthenticatedUserId(request);
        return viewerCommentService.listComments(resourceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentVO createComment(@PathVariable Long resourceId,
                                   @RequestBody CommentCreateRequest request,
                                   HttpServletRequest httpServletRequest) {
        Long currentUserId = resourcePermissionChecker.requireAuthenticatedUserId(httpServletRequest);
        return viewerCommentService.createComment(currentUserId, resourceId, request);
    }
}
