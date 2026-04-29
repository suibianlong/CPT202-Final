package com.cpt202.HerLink.dto.resource;

import java.io.Serializable;

// rollback confirmation request
public class ResourceVersionRollbackRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean confirmed;

    public ResourceVersionRollbackRequest() {
    }

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }
}
