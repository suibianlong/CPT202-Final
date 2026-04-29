package com.cpt202.HerLink.entity;

import java.io.Serializable;

// corresponding to the "resource_tag" table
public class ResourceTag implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private Long resourceId;
    private Long tagId;

    public ResourceTag() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    @Override
    public String toString() {
        return "ResourceTag{" +
                "id=" + id +
                ", resourceId=" + resourceId +
                ", tagId=" + tagId +
                '}';
    }
}
