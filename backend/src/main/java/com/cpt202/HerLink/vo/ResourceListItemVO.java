package com.cpt202.HerLink.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

// display brief information
public class ResourceListItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private String previewImage;
    private String status;
    private String resourceType;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime updatedAt;
    private Integer currentVersionNo;
    private LocalDateTime lastSubmittedAt;
    private Boolean hasReviewFeedback;

    public ResourceListItemVO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPreviewImage() {
        return previewImage;
    }

    public void setPreviewImage(String previewImage) {
        this.previewImage = previewImage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCurrentVersionNo() {
        return currentVersionNo;
    }

    public void setCurrentVersionNo(Integer currentVersionNo) {
        this.currentVersionNo = currentVersionNo;
    }

    public LocalDateTime getLastSubmittedAt() {
        return lastSubmittedAt;
    }

    public void setLastSubmittedAt(LocalDateTime lastSubmittedAt) {
        this.lastSubmittedAt = lastSubmittedAt;
    }

    public Boolean getHasReviewFeedback() {
        return hasReviewFeedback;
    }

    public void setHasReviewFeedback(Boolean hasReviewFeedback) {
        this.hasReviewFeedback = hasReviewFeedback;
    }

    @Override
    public String toString() {
        return "ResourceListItemVO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", previewImage='" + previewImage + '\'' +
                ", status='" + status + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", categoryId=" + categoryId +
                ", categoryName='" + categoryName + '\'' +
                ", updatedAt=" + updatedAt +
                ", currentVersionNo=" + currentVersionNo +
                ", lastSubmittedAt=" + lastSubmittedAt +
                ", hasReviewFeedback=" + hasReviewFeedback +
                '}';
    }
}
