package com.cpt202.HerLink.vo;

import java.io.Serializable;

// display one compare row between two resource versions
public class ResourceVersionDiffItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fieldName;
    private String fieldLabel;
    private String leftValue;
    private String rightValue;
    private Boolean changed;

    public ResourceVersionDiffItemVO() {
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(String leftValue) {
        this.leftValue = leftValue;
    }

    public String getRightValue() {
        return rightValue;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }

    public Boolean getChanged() {
        return changed;
    }

    public void setChanged(Boolean changed) {
        this.changed = changed;
    }
}
