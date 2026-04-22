package com.cpt202.HerLink.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

// display the submission history record
public class ResourceSubmissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long resourceId;
    private Integer versionNo;
    private Long submittedBy;
    private LocalDateTime submittedAt;
    private String submissionNote;
    private String statusSnapshot;
    private LocalDateTime createdAt;

    public ResourceSubmissionVO() {
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Long submittedBy) {
        this.submittedBy = submittedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getSubmissionNote() {
        return submissionNote;
    }

    public void setSubmissionNote(String submissionNote) {
        this.submissionNote = submissionNote;
    }

    public String getStatusSnapshot() {
        return statusSnapshot;
    }

    public void setStatusSnapshot(String statusSnapshot) {
        this.statusSnapshot = statusSnapshot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ResourceSubmissionVO{" +
                "submissionId=" + submissionId +
                ", resourceId=" + resourceId +
                ", versionNo=" + versionNo +
                ", submittedBy=" + submittedBy +
                ", submittedAt=" + submittedAt +
                ", submissionNote='" + submissionNote + '\'' +
                ", statusSnapshot='" + statusSnapshot + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}