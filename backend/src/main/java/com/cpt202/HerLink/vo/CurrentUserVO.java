package com.cpt202.HerLink.vo;

public class CurrentUserVO {

    private Long userId;
    private Long latestContributorRequestId;
    private String name;
    private String email;
    private String bio;
    private String role;
    private String contributorStatus;
    private boolean contributor;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLatestContributorRequestId() {
        return latestContributorRequestId;
    }

    public void setLatestContributorRequestId(Long latestContributorRequestId) {
        this.latestContributorRequestId = latestContributorRequestId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContributorStatus() {
        return contributorStatus;
    }

    public void setContributorStatus(String contributorStatus) {
        this.contributorStatus = contributorStatus;
    }

    public boolean isContributor() {
        return contributor;
    }

    public void setContributor(boolean contributor) {
        this.contributor = contributor;
    }
}
