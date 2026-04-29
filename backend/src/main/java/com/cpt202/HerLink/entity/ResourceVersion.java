package com.cpt202.HerLink.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

// corresponding to the "resourceVersion" table
public class ResourceVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long versionId;
    private Long resourceId;
    private Integer versionNo;
    private String snapshot;
    private String changeType;
    private String changeSummary;
    private Long createdBy;
    private LocalDateTime createdAt;

    public ResourceVersion() {
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
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

    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ResourceVersion{" +
                "versionId=" + versionId +
                ", resourceId=" + resourceId +
                ", versionNo=" + versionNo +
                ", snapshot='" + snapshot + '\'' +
                ", changeType='" + changeType + '\'' +
                ", changeSummary='" + changeSummary + '\'' +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                '}';
    }
}
