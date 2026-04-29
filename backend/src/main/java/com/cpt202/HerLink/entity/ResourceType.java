package com.cpt202.HerLink.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

// corresponding to the "resourceType" table
public class ResourceType implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long resourceTypeId;
    private String typeName;
    private String status;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;

    public ResourceType() {
    }

    public Long getResourceTypeId() {
        return resourceTypeId;
    }

    public void setResourceTypeId(Long resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    @Override
    public String toString() {
        return "ResourceType{" +
                "resourceTypeId=" + resourceTypeId +
                ", typeName='" + typeName + '\'' +
                ", status='" + status + '\'' +
                ", usageCount=" + usageCount +
                ", createdAt=" + createdAt +
                ", lastUpdatedAt=" + lastUpdatedAt +
                '}';
    }
}
