package com.cpt202.HerLink.service;

import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import java.util.List;

public interface ViewerResourceService {

    List<ResourceListItemVO> listApprovedResources(String keyword,
                                                   String resourceType,
                                                   Long categoryId,
                                                   String sortBy);

    ResourceDetailVO getApprovedResourceDetail(Long resourceId);

    List<CategoryTagOptionVO> listCategoryOptions();
}
