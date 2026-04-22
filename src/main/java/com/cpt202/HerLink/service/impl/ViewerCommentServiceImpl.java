package com.cpt202.HerLink.service.impl;

import com.cpt202.HerLink.dto.viewer.CommentCreateRequest;
import com.cpt202.HerLink.entity.Comment;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CommentMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.service.ViewerCommentService;
import com.cpt202.HerLink.vo.CommentVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewerCommentServiceImpl implements ViewerCommentService {

    private static final int MAX_COMMENT_LENGTH = 1000;
    private static final int DUPLICATE_COMMENT_WINDOW_SECONDS = 30;

    private final CommentMapper commentMapper;
    private final ResourceMapper resourceMapper;

    public ViewerCommentServiceImpl(CommentMapper commentMapper,
                                    ResourceMapper resourceMapper) {
        this.commentMapper = commentMapper;
        this.resourceMapper = resourceMapper;
    }

    @Override
    public List<CommentVO> listComments(Long resourceId) {
        validateApprovedResource(resourceId);

        List<Comment> comments = commentMapper.selectByResourceId(resourceId);
        if (comments == null) {
            return Collections.emptyList();
        }

        List<CommentVO> commentVOList = new ArrayList<>();
        for (Comment comment : comments) {
            commentVOList.add(buildCommentVO(comment));
        }
        return commentVOList;
    }

    @Override
    @Transactional
    public CommentVO createComment(Long currentUserId, Long resourceId, CommentCreateRequest request) {
        validateApprovedResource(resourceId);
        String content = normalizeCommentContent(request == null ? null : request.getContent());

        Comment latestDuplicateComment = commentMapper.selectLatestByResourceIdAndUserIdAndContent(
                resourceId,
                currentUserId,
                content
        );
        LocalDateTime now = LocalDateTime.now();
        if (latestDuplicateComment != null
                && latestDuplicateComment.getCreatedAt() != null
                && latestDuplicateComment.getCreatedAt().isAfter(now.minusSeconds(DUPLICATE_COMMENT_WINDOW_SECONDS))) {
            throw AppException.conflict("Please wait before submitting the same comment again.");
        }

        Comment comment = new Comment();
        comment.setResourceId(resourceId);
        comment.setUserId(currentUserId);
        comment.setContent(content);
        comment.setCreatedAt(now);
        commentMapper.insert(comment);

        Comment savedComment = commentMapper.selectById(comment.getId());
        if (savedComment == null) {
            throw AppException.notFound("Comment was saved but cannot be reloaded.");
        }

        return buildCommentVO(savedComment);
    }

    private void validateApprovedResource(Long resourceId) {
        if (resourceId == null) {
            throw AppException.badRequest("Resource id is required.");
        }
        if (resourceMapper.selectApprovedById(resourceId) == null) {
            throw AppException.notFound("Approved resource does not exist.");
        }
    }

    private String normalizeCommentContent(String content) {
        String normalizedContent = content == null ? null : content.trim();
        if (normalizedContent == null || normalizedContent.isEmpty()) {
            throw AppException.badRequest("Comment content cannot be empty.");
        }
        if (normalizedContent.length() > MAX_COMMENT_LENGTH) {
            throw AppException.badRequest("Comment content cannot exceed 1000 characters.");
        }
        return normalizedContent;
    }

    private CommentVO buildCommentVO(Comment comment) {
        CommentVO commentVO = new CommentVO();
        commentVO.setId(comment.getId());
        commentVO.setResourceId(comment.getResourceId());
        commentVO.setUserId(comment.getUserId());
        commentVO.setUserName(comment.getUserName());
        commentVO.setContent(comment.getContent());
        commentVO.setCreatedAt(comment.getCreatedAt());
        return commentVO;
    }
}
