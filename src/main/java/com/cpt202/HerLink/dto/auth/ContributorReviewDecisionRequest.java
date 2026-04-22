package com.cpt202.HerLink.dto.auth;

public class ContributorReviewDecisionRequest {

    private String decision;
    private String reviewComment;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}
