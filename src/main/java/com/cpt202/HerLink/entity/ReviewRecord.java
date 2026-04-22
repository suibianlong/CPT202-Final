package com.cpt202.HerLink.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

// // corresponding to the "review_record" table
public class ReviewRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private String actionDescription;
    private String status;
    private String feedbackComment;
    private Long reviewRecordId;
    private Long resourceId;
    private Long submissionId;
    private Long reviewerId;
    private Integer versionNo;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    public ReviewRecord() {
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    public void setFeedbackComment(String feedbackComment) {
        this.feedbackComment = feedbackComment;
    }

    public Long getReviewRecordId() {
        return reviewRecordId;
    }

    public void setReviewRecordId(Long reviewRecordId) {
        this.reviewRecordId = reviewRecordId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ReviewRecord{" +
                "actionDescription='" + actionDescription + '\'' +
                ", status='" + status + '\'' +
                ", feedbackComment='" + feedbackComment + '\'' +
                ", reviewRecordId=" + reviewRecordId +
                ", resourceId=" + resourceId +
                ", submissionId=" + submissionId +
                ", reviewerId=" + reviewerId +
                ", versionNo=" + versionNo +
                ", reviewedAt=" + reviewedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
