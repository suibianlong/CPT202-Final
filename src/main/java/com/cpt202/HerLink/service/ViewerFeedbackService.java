package com.cpt202.HerLink.service;

import com.cpt202.HerLink.dto.viewer.FeedbackCreateRequest;
import com.cpt202.HerLink.vo.FeedbackVO;
import java.util.List;

public interface ViewerFeedbackService {

    FeedbackVO createFeedback(Long currentUserId, FeedbackCreateRequest request);

    List<FeedbackVO> listMyFeedback(Long currentUserId);
}
