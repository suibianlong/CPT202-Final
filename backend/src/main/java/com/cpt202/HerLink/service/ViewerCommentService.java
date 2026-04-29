package com.cpt202.HerLink.service;

import com.cpt202.HerLink.dto.viewer.CommentCreateRequest;
import com.cpt202.HerLink.vo.CommentVO;
import java.util.List;

public interface ViewerCommentService {

    List<CommentVO> listComments(Long resourceId);

    CommentVO createComment(Long currentUserId, Long resourceId, CommentCreateRequest request);

    void deleteComment(Long currentUserId, Long resourceId, Long commentId);
}
