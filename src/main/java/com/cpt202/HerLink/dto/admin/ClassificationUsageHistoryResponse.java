package com.cpt202.HerLink.dto.admin;

import java.time.LocalDateTime;

public class ClassificationUsageHistoryResponse {

    private Long classificationId;
    private String name;
    private String kind;
    private Long resourceId;
    private String relatedRecordName;
    private LocalDateTime dateOfUse;

    public Long getClassificationId() {
        return classificationId;
    }

    public void setClassificationId(Long classificationId) {
        this.classificationId = classificationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
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
