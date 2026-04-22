package com.cpt202.HerLink.vo;

import java.io.Serializable;
import java.util.List;

// display compare result between two versions
public class ResourceVersionCompareVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long resourceId;
    private Integer leftVersionNo;
    private Integer rightVersionNo;
    private List<ResourceVersionDiffItemVO> diffItems;

    public ResourceVersionCompareVO() {
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getLeftVersionNo() {
        return leftVersionNo;
    }

    public void setLeftVersionNo(Integer leftVersionNo) {
        this.leftVersionNo = leftVersionNo;
    }

    public Integer getRightVersionNo() {
        return rightVersionNo;
    }

    public void setRightVersionNo(Integer rightVersionNo) {
        this.rightVersionNo = rightVersionNo;
    }

    public List<ResourceVersionDiffItemVO> getDiffItems() {
        return diffItems;
    }

    public void setDiffItems(List<ResourceVersionDiffItemVO> diffItems) {
        this.diffItems = diffItems;
    }
}
