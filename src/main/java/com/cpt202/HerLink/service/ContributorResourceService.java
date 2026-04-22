package com.cpt202.HerLink.service;

import com.cpt202.HerLink.dto.resource.ResourceQueryRequest;
import com.cpt202.HerLink.dto.resource.ResourceSubmitRequest;
import com.cpt202.HerLink.dto.resource.ResourceUpdateRequest;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import com.cpt202.HerLink.vo.ResourceSubmissionVO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ContributorResourceService {

    ResourceDetailVO createDraft(Long currentUserId);

    ResourceDetailVO updateResource(Long currentUserId, Long resourceId, ResourceUpdateRequest request);

    ResourceDetailVO uploadFiles(Long currentUserId, Long resourceId, MultipartFile previewImage, MultipartFile mediaFile);

    ResourceDetailVO getMyResourceDetail(Long currentUserId, Long resourceId);

    List<ResourceListItemVO> listMyResources(Long currentUserId, ResourceQueryRequest request);

    List<ResourceSubmissionVO> listSubmissionHistory(Long currentUserId, Long resourceId);

    void submitResource(Long currentUserId, Long resourceId, ResourceSubmitRequest request);

    List<CategoryTagOptionVO> listCategoryOptions();

    List<CategoryTagOptionVO> listTagOptions();
}
