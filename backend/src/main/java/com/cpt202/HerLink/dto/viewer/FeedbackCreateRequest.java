package com.cpt202.HerLink.dto.viewer;

import org.springframework.web.multipart.MultipartFile;

public class FeedbackCreateRequest {

    private String feedbackType;
    private String description;
    private MultipartFile[] files;

    public String getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MultipartFile[] getFiles() {
        return files;
    }

    public void setFiles(MultipartFile[] files) {
        this.files = files;
    }
}
