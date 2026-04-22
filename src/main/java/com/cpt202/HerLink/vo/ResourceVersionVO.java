package com.cpt202.HerLink.vo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

// display one resource version record
public class ResourceVersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long versionId;
    private Long resourceId;
    private Integer versionNo;
    private String changeType;
    private String changeSummary;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Map<String, Object> snapshotMap;

    public ResourceVersionVO() {
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

    public Map<String, Object> getSnapshotMap() {
        return snapshotMap;
    }

    public void setSnapshotMap(Map<String, Object> snapshotMap) {
        this.snapshotMap = snapshotMap;
    }
}
