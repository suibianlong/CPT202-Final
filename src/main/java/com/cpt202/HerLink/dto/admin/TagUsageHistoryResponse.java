package com.cpt202.HerLink.dto.admin;

import java.time.LocalDateTime;

public class TagUsageHistoryResponse {

    private Long tagId;
    private String tagName;
    private Long resourceId;
    private String relatedRecordName;
    private LocalDateTime dateOfUse;

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getRelatedRecordName() {
        return relatedRecordName;
    }

    public void setRelatedRecordName(String relatedRecordName) {
        this.relatedRecordName = relatedRecordName;
    }

    public LocalDateTime getDateOfUse() {
        return dateOfUse;
    }

    public void setDateOfUse(LocalDateTime dateOfUse) {
        this.dateOfUse = dateOfUse;
    }
}
