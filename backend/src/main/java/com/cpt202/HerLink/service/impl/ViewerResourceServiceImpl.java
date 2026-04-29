package com.cpt202.HerLink.service.impl;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.enums.ResourceTypeEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.service.ViewerResourceService;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ViewerResourceServiceImpl implements ViewerResourceService {

    private final ResourceMapper resourceMapper;
    private final CategoryMapper categoryMapper;
    private final ResourceTagMapper resourceTagMapper;
    private final ResourceTypeMapper resourceTypeMapper;

    public ViewerResourceServiceImpl(ResourceMapper resourceMapper,
                                     CategoryMapper categoryMapper,
                                     ResourceTagMapper resourceTagMapper,
                                     ResourceTypeMapper resourceTypeMapper) {
        this.resourceMapper = resourceMapper;
        this.categoryMapper = categoryMapper;
        this.resourceTagMapper = resourceTagMapper;
        this.resourceTypeMapper = resourceTypeMapper;
    }

    @Override
    public List<ResourceListItemVO> listApprovedResources(String keyword,
                                                          String resourceType,
                                                          Long categoryId,
                                                          String sortBy) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        boolean hasResourceTypeFilter = resourceType != null && !resourceType.isBlank();
        Long normalizedResourceTypeId = normalizeResourceTypeId(resourceType);
        String normalizedSortBy = normalizeSortBy(sortBy);
        if (hasResourceTypeFilter && normalizedResourceTypeId == null) {
            return Collections.emptyList();
        }

        List<Resource> resources = resourceMapper.selectApprovedResources(
                normalizedKeyword,
                normalizedResourceTypeId,
                categoryId,
                normalizedSortBy
        );

        if (resources == null) {
            resources = Collections.emptyList();
        }

        List<ResourceListItemVO> itemVOList = new ArrayList<>();
        for (Resource resource : resources) {
            ResourceListItemVO itemVO = new ResourceListItemVO();
            itemVO.setId(resource.getId());
            itemVO.setTitle(resource.getTitle());
            itemVO.setDescription(resource.getDescription());
            itemVO.setPreviewImage(resource.getPreviewImage());
            itemVO.setStatus(ResourceStatusEnum.fromValue(resource.getStatus()).getValue());
            itemVO.setResourceType(normalizeResourceTypeForResponse(resource.getResourceType()));
            itemVO.setCategoryId(resource.getCategoryId());
            itemVO.setCategoryName(normalizeCategoryName(resource.getCategoryName()));
            itemVO.setUpdatedAt(resource.getUpdatedAt());
            itemVOList.add(itemVO);
        }

        return itemVOList;
    }

    @Override
    public ResourceDetailVO getApprovedResourceDetail(Long resourceId) {
        Resource resource = resourceMapper.selectApprovedById(resourceId);
        if (resource == null) {
            throw AppException.notFound("Approved resource does not exist.");
        }

        return buildResourceDetailVO(resource);
    }

    @Override
    public List<CategoryTagOptionVO> listCategoryOptions() {
        List<Category> activeCategories = categoryMapper.selectActiveCategories();
        if (activeCategories == null) {
            activeCategories = Collections.emptyList();
        }

        List<CategoryTagOptionVO> optionVOList = new ArrayList<>();
        for (Category category : activeCategories) {
            if (category == null || category.getCategoryId() == null) {
                continue;
            }
            CategoryTagOptionVO optionVO = new CategoryTagOptionVO();
            optionVO.setId(category.getCategoryId());
            optionVO.setName(category.getCategoryTopic());
            optionVOList.add(optionVO);
        }

        return optionVOList;
    }

    @Override
    public List<CategoryTagOptionVO> listResourceTypeOptions() {
        List<ResourceType> resourceTypeList = resourceTypeMapper.selectActiveResourceTypes();
        if (resourceTypeList == null) {
            resourceTypeList = Collections.emptyList();
        }

        List<CategoryTagOptionVO> optionVOList = new ArrayList<>();
        for (ResourceType resourceType : resourceTypeList) {
            if (resourceType == null || resourceType.getResourceTypeId() == null) {
                continue;
            }
            CategoryTagOptionVO optionVO = new CategoryTagOptionVO();
            optionVO.setId(resourceType.getResourceTypeId());
            optionVO.setName(resourceType.getTypeName());
            optionVOList.add(optionVO);
        }

        return optionVOList;
    }

    private ResourceDetailVO buildResourceDetailVO(Resource resource) {
        ResourceDetailVO resourceDetailVO = new ResourceDetailVO();
        resourceDetailVO.setId(resource.getId());
        resourceDetailVO.setContributorId(resource.getContributorId());
        resourceDetailVO.setTitle(resource.getTitle());
        resourceDetailVO.setDescription(resource.getDescription());
        resourceDetailVO.setCopyright(resource.getCopyright());
        resourceDetailVO.setCategoryId(resource.getCategoryId());
        resourceDetailVO.setCategoryName(normalizeCategoryName(resource.getCategoryName()));
        resourceDetailVO.setPlace(resource.getPlace());
        resourceDetailVO.setPreviewImage(resource.getPreviewImage());
        resourceDetailVO.setMediaUrl(resource.getMediaUrl());
        resourceDetailVO.setStatus(ResourceStatusEnum.fromValue(resource.getStatus()).getValue());
        resourceDetailVO.setReviewedAt(resource.getReviewedAt());
        resourceDetailVO.setCreatedAt(resource.getCreatedAt());
        resourceDetailVO.setUpdatedAt(resource.getUpdatedAt());
        resourceDetailVO.setArchivedAt(resource.getArchivedAt());
        resourceDetailVO.setResourceType(normalizeResourceTypeForResponse(resource.getResourceType()));
        resourceDetailVO.setTagIds(resourceTagMapper.selectTagIdsByResourceId(resource.getId()));
        resourceDetailVO.setTagNames(resourceTagMapper.selectTagNamesByResourceId(resource.getId()));
        return resourceDetailVO;
    }

    private Long normalizeResourceTypeId(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return null;
        }

        ResourceTypeEnum normalizedResourceType = tryNormalizeResourceType(resourceType);
        if (normalizedResourceType != null) {
            for (String lookupValue : normalizedResourceType.getLookupValues()) {
                ResourceType matchedResourceType = resourceTypeMapper.selectActiveByTypeName(lookupValue);
                if (matchedResourceType != null && matchedResourceType.getResourceTypeId() != null) {
                    return matchedResourceType.getResourceTypeId();
                }
            }
        }

        ResourceType directMatch = resourceTypeMapper.selectActiveByTypeName(resourceType.trim());
        if (directMatch != null && directMatch.getResourceTypeId() != null) {
            return directMatch.getResourceTypeId();
        }
        return null;
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return null;
        }

        String normalized = sortBy.trim().toLowerCase();
        if (Objects.equals(normalized, "title") || Objects.equals(normalized, "time")) {
            return normalized;
        }
        throw AppException.badRequest("Unsupported sort option.");
    }

    private Category findMatchingCategory(List<Category> categoryList, String categoryName) {
        for (Category category : categoryList) {
            if (category == null || category.getCategoryId() == null) {
                continue;
            }
            if (matchesCategoryName(categoryName, category.getCategoryTopic())) {
                return category;
            }
        }
        return null;
    }

    private boolean matchesCategoryName(String expectedCategoryName, String actualCategoryName) {
        String normalizedExpectedCategoryName = normalize(expectedCategoryName);
        String normalizedActualCategoryName = normalize(actualCategoryName);

        if (Objects.equals(normalizedExpectedCategoryName, normalizedActualCategoryName)) {
            return true;
        }

        return Objects.equals(normalizedExpectedCategoryName, "educational materials")
                && Objects.equals(normalizedActualCategoryName, "education");
    }

    private String normalizeCategoryName(String categoryName) {
        String normalizedCategoryName = normalize(categoryName);
        if ("education".equals(normalizedCategoryName)) {
            return "educational materials";
        }
        return normalizedCategoryName.isEmpty() ? categoryName : normalizedCategoryName;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private ResourceTypeEnum tryNormalizeResourceType(String resourceType) {
        try {
            return ResourceTypeEnum.fromValue(resourceType);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String normalizeResourceTypeForResponse(String resourceType) {
        ResourceTypeEnum normalizedResourceType = tryNormalizeResourceType(resourceType);
        if (normalizedResourceType != null) {
            return normalizedResourceType.getValue();
        }
        return resourceType;
    }
}
