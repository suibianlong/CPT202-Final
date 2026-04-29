package com.cpt202.HerLink.vo;

import java.time.LocalDateTime;
import java.util.List;

public class FeedbackVO {

    private Long feedbackId;
    private Integer fileNum;
    private LocalDateTime uploadedAt;
    private Long userId;
    private String feedbackType;
    private String description;
    private List<AttachedFileVO> attachments;

    public Long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Integer getFileNum() {
        return fileNum;
    }

    public void setFileNum(Integer fileNum) {
        this.fileNum = fileNum;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<AttachedFileVO> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachedFileVO> attachments) {
        this.attachments = attachments;
    }
}
