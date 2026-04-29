package com.cpt202.HerLink.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

// corresponding to the "resource_submission" table
public class ResourceSubmission implements Serializable {

    private static final long serialVersionUID = 1L;

    private String submissionNote;
    private String statusSnapshot;
    private Long submissionId;
    private Long resourceId;
    private Long submittedBy;
    private Integer versionNo;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;

    public ResourceSubmission() {
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

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Long submittedBy) {
        this.submittedBy = submittedBy;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ResourceSubmission{" +
                "submissionNote='" + submissionNote + '\'' +
                ", statusSnapshot='" + statusSnapshot + '\'' +
                ", submissionId=" + submissionId +
                ", resourceId=" + resourceId +
                ", submittedBy=" + submittedBy +
                ", versionNo=" + versionNo +
                ", submittedAt=" + submittedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
