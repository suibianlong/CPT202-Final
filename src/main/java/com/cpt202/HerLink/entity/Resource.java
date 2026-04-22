package com.cpt202.HerLink.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

// corresponding to the "resource" table
public class Resource implements Serializable {

    private static final long serialVersionUID = 1L;
    private String title;
    private String description;
    private String copyright;
    private String place;
    private String previewImage;
    private String mediaUrl;
    private String status;
    private String resourceType;
    private String categoryName;
    private Long id;
    private Long contributorId;
    private Long categoryId;
    private Long resourceTypeId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime archivedAt;

    public Resource() {
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

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getPreviewImage() {
        return previewImage;
    }

    public void setPreviewImage(String previewImage) {
        this.previewImage = previewImage;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContributorId() {
        return contributorId;
    }

    public void setContributorId(Long contributorId) {
        this.contributorId = contributorId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getResourceTypeId() {
        return resourceTypeId;
    }

    public void setResourceTypeId(Long resourceTypeId) {
        this.resourceTypeId = resourceTypeId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", place='" + place + '\'' +
                ", previewImage='" + previewImage + '\'' +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", status='" + status + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", id=" + id +
                ", contributorId=" + contributorId +
                ", categoryId=" + categoryId +
                ", resourceTypeId=" + resourceTypeId +
                ", reviewedAt=" + reviewedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", archivedAt=" + archivedAt +
                '}';
    }
}
