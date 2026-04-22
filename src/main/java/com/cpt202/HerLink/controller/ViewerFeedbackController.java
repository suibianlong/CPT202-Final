package com.cpt202.HerLink.controller;

import com.cpt202.HerLink.dto.viewer.FeedbackCreateRequest;
import com.cpt202.HerLink.service.ViewerFeedbackService;
import com.cpt202.HerLink.util.ResourcePermissionChecker;
import com.cpt202.HerLink.vo.FeedbackVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/viewer/feedback")
public class ViewerFeedbackController {

    private final ViewerFeedbackService viewerFeedbackService;
    private final ResourcePermissionChecker resourcePermissionChecker;

    public ViewerFeedbackController(ViewerFeedbackService viewerFeedbackService,
                                    ResourcePermissionChecker resourcePermissionChecker) {
        this.viewerFeedbackService = viewerFeedbackService;
        this.resourcePermissionChecker = resourcePermissionChecker;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackVO createFeedback(@ModelAttribute FeedbackCreateRequest request,
                                     HttpServletRequest httpServletRequest) {
        Long currentUserId = resourcePermissionChecker.requireAuthenticatedUserId(httpServletRequest);
        return viewerFeedbackService.createFeedback(currentUserId, request);
    }

    @GetMapping("/mine")
    public List<FeedbackVO> listMyFeedback(HttpServletRequest request) {
        Long currentUserId = resourcePermissionChecker.requireAuthenticatedUserId(request);
        return viewerFeedbackService.listMyFeedback(currentUserId);
    }
}
