package com.cpt202.HerLink.service.admin;

import com.cpt202.HerLink.dto.admin.AdminCategoryRequest;
import com.cpt202.HerLink.dto.admin.AdminCategoryResponse;
import com.cpt202.HerLink.dto.admin.AdminResourceTypeRequest;
import com.cpt202.HerLink.dto.admin.AdminResourceTypeResponse;
import com.cpt202.HerLink.dto.admin.AdminTagRequest;
import com.cpt202.HerLink.dto.admin.AdminTagResponse;
import com.cpt202.HerLink.dto.admin.ClassificationStatus;
import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.Tag;
import com.cpt202.HerLink.enums.ResourceTypeEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.mapper.TagMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminClassificationManagementServiceImpl implements AdminClassificationManagementService {

    private static final String CLASSIFICATION_MODULE = "classification";
    private static final String TAG_MODULE = "tag";
    private static final int MAX_CATEGORY_TOPIC_LENGTH = 50;
    private static final int MAX_RESOURCE_TYPE_LENGTH = 50;
    private static final int MAX_TAG_NAME_LENGTH = 100;

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ResourceTypeMapper resourceTypeMapper;
    private final AdminOperationHistoryService operationHistoryService;

    public AdminClassificationManagementServiceImpl(CategoryMapper categoryMapper,
                                                    TagMapper tagMapper,
                                                    ResourceTypeMapper resourceTypeMapper,
                                                    AdminOperationHistoryService operationHistoryService) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.resourceTypeMapper = resourceTypeMapper;
        this.operationHistoryService = operationHistoryService;
    }

    @Override
    public List<AdminCategoryResponse> getAllCategories() {
        return mapCategories(categoryMapper.selectAllCategories());
    }

    @Override
    public List<AdminCategoryResponse> getActiveCategories() {
        return mapCategories(categoryMapper.selectByStatus(ClassificationStatus.ACTIVE.name()));
    }

    @Override
    @Transactional
    public AdminCategoryResponse createCategory(AdminCategoryRequest request, String administrator) {
        String categoryTopic = normalizeName(
                request == null ? null : request.categoryTopic(),
                "categoryTopic",
                MAX_CATEGORY_TOPIC_LENGTH
        );
        validateUniqueCategoryTopic(categoryTopic, null);

        Category category = new Category();
        category.setCategoryTopic(categoryTopic);
        category.setStatus(ClassificationStatus.ACTIVE.name());
        category.setUsageCount(0);
        categoryMapper.insert(category);

        AdminCategoryResponse created = mapCategory(loadCategory(category.getCategoryId()));
        recordClassification(created.categoryTopic(), "Topic", "Created", administrator);
        return created;
    }

    @Override
    @Transactional
    public AdminCategoryResponse updateCategory(Long categoryId, AdminCategoryRequest request, String administrator) {
        loadCategory(categoryId);
        String categoryTopic = normalizeName(
                request == null ? null : request.categoryTopic(),
                "categoryTopic",
                MAX_CATEGORY_TOPIC_LENGTH
        );
        validateUniqueCategoryTopic(categoryTopic, categoryId);

        int updatedRows = categoryMapper.updateTopic(categoryId, categoryTopic, LocalDateTime.now());
        if (updatedRows == 0) {
            throw AppException.notFound("Category does not exist.");
        }

        AdminCategoryResponse updated = mapCategory(loadCategory(categoryId));
        recordClassification(updated.categoryTopic(), "Topic", "Updated", administrator);
        return updated;
    }

    @Override
    @Transactional
    public AdminCategoryResponse deactivateCategory(Long categoryId, String administrator) {
        return updateCategoryStatus(categoryId, ClassificationStatus.INACTIVE, "Deactivated", administrator);
    }

    @Override
    @Transactional
    public AdminCategoryResponse activateCategory(Long categoryId, String administrator) {
        return updateCategoryStatus(categoryId, ClassificationStatus.ACTIVE, "Activated", administrator);
    }

    @Override
    public List<AdminTagResponse> getAllTags() {
        return mapTags(tagMapper.selectAllTags());
    }

    @Override
    public List<AdminTagResponse> getActiveTags() {
        return mapTags(tagMapper.selectByStatus(ClassificationStatus.ACTIVE.name()));
    }

    @Override
    @Transactional
    public AdminTagResponse createTag(AdminTagRequest request, String administrator) {
        String tagName = normalizeName(
                request == null ? null : request.tagName(),
                "tagName",
                MAX_TAG_NAME_LENGTH
        );
        validateUniqueTagName(tagName, null);

        Tag tag = new Tag();
        tag.setTagName(tagName);
        tag.setStatus(ClassificationStatus.ACTIVE.name());
        tag.setUsageCount(0);
        tagMapper.insert(tag);

        AdminTagResponse created = mapTag(loadTag(tag.getTagId()));
        recordTag(created.tagName(), "Created", administrator);
        return created;
    }

    @Override
    @Transactional
    public AdminTagResponse updateTag(Long tagId, AdminTagRequest request, String administrator) {
        loadTag(tagId);
        String tagName = normalizeName(
                request == null ? null : request.tagName(),
                "tagName",
                MAX_TAG_NAME_LENGTH
        );
        validateUniqueTagName(tagName, tagId);

        int updatedRows = tagMapper.updateTagName(tagId, tagName, LocalDateTime.now());
        if (updatedRows == 0) {
            throw AppException.notFound("Tag does not exist.");
        }

        AdminTagResponse updated = mapTag(loadTag(tagId));
        recordTag(updated.tagName(), "Updated", administrator);
        return updated;
    }

    @Override
    @Transactional
    public AdminTagResponse deactivateTag(Long tagId, String administrator) {
        return updateTagStatus(tagId, ClassificationStatus.INACTIVE, "Deactivated", administrator);
    }

    @Override
    @Transactional
    public AdminTagResponse activateTag(Long tagId, String administrator) {
        return updateTagStatus(tagId, ClassificationStatus.ACTIVE, "Activated", administrator);
    }

    @Override
    public List<AdminResourceTypeResponse> getAllResourceTypes() {
        return mapResourceTypes(resourceTypeMapper.selectAllResourceTypes());
    }

    @Override
    public List<AdminResourceTypeResponse> getActiveResourceTypes() {
        return mapResourceTypes(resourceTypeMapper.selectByStatus(ClassificationStatus.ACTIVE.name()));
    }

    @Override
    @Transactional
    public AdminResourceTypeResponse createResourceType(AdminResourceTypeRequest request, String administrator) {
        String typeName = normalizeName(
                request == null ? null : request.typeName(),
                "typeName",
                MAX_RESOURCE_TYPE_LENGTH
        );
        validateUniqueResourceType(typeName, null);

        ResourceType resourceType = new ResourceType();
        resourceType.setTypeName(typeName);
        resourceType.setStatus(ClassificationStatus.ACTIVE.name());
        resourceType.setUsageCount(0);
        resourceTypeMapper.insert(resourceType);

        AdminResourceTypeResponse created = mapResourceType(loadResourceType(resourceType.getResourceTypeId()));
        recordClassification(created.typeName(), "Type", "Created", administrator);
        return created;
    }

    @Override
    @Transactional
    public AdminResourceTypeResponse updateResourceType(Long resourceTypeId,
                                                        AdminResourceTypeRequest request,
                                                        String administrator) {
        ResourceType existing = loadResourceType(resourceTypeId);
        String typeName = normalizeName(
                request == null ? null : request.typeName(),
                "typeName",
                MAX_RESOURCE_TYPE_LENGTH
        );
        validateUniqueResourceType(typeName, resourceTypeId);
        validateResourceTypeCompatibility(existing, typeName);

        int updatedRows = resourceTypeMapper.updateTypeName(resourceTypeId, typeName, LocalDateTime.now());
        if (updatedRows == 0) {
            throw AppException.notFound("Resource type does not exist.");
        }

        AdminResourceTypeResponse updated = mapResourceType(loadResourceType(resourceTypeId));
        recordClassification(updated.typeName(), "Type", "Updated", administrator);
        return updated;
    }

    @Override
    @Transactional
    public AdminResourceTypeResponse deactivateResourceType(Long resourceTypeId, String administrator) {
        return updateResourceTypeStatus(resourceTypeId, ClassificationStatus.INACTIVE, "Deactivated", administrator);
    }

    @Override
    @Transactional
    public AdminResourceTypeResponse activateResourceType(Long resourceTypeId, String administrator) {
        return updateResourceTypeStatus(resourceTypeId, ClassificationStatus.ACTIVE, "Activated", administrator);
    }

    private AdminCategoryResponse updateCategoryStatus(Long categoryId,
                                                       ClassificationStatus status,
                                                       String action,
                                                       String administrator) {
        Category existing = loadCategory(categoryId);
        if (status.name().equalsIgnoreCase(existing.getStatus())) {
            return mapCategory(existing);
        }

        int updatedRows = categoryMapper.updateStatus(categoryId, status.name(), LocalDateTime.now());
        if (updatedRows == 0) {
            throw AppException.notFound("Category does not exist.");
        }

        AdminCategoryResponse updated = mapCategory(loadCategory(categoryId));
        recordClassification(updated.categoryTopic(), "Topic", action, administrator);
        return updated;
    }

    private AdminTagResponse updateTagStatus(Long tagId,
                                             ClassificationStatus status,
                                             String action,
                                             String administrator) {
        Tag existing = loadTag(tagId);
        if (status.name().equalsIgnoreCase(existing.getStatus())) {
            return mapTag(existing);
        }

        int updatedRows = tagMapper.updateStatus(tagId, status.name(), LocalDateTime.now());
        if (updatedRows == 0) {
            throw AppException.notFound("Tag does not exist.");
        }

        AdminTagResponse updated = mapTag(loadTag(tagId));
        recordTag(updated.tagName(), action, administrator);
        return updated;
    }

    private AdminResourceTypeResponse updateResourceTypeStatus(Long resourceTypeId,
                                                               ClassificationStatus status,
                                                               String action,
                                                               String administrator) {
        ResourceType existing = loadResourceType(resourceTypeId);
        if (status.name().equalsIgnoreCase(existing.getStatus())) {
            return mapResourceType(existing);
        }

        int updatedRows = resourceTypeMapper.updateStatus(resourceTypeId, status.name(), LocalDateTime.now());
        if (updatedRows == 0) {
            throw AppException.notFound("Resource type does not exist.");
        }

        AdminResourceTypeResponse updated = mapResourceType(loadResourceType(resourceTypeId));
        recordClassification(updated.typeName(), "Type", action, administrator);
        return updated;
    }

    private Category loadCategory(Long categoryId) {
        if (categoryId == null) {
            throw AppException.badRequest("Category id is required.");
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw AppException.notFound("Category does not exist.");
        }
        return category;
    }

    private Tag loadTag(Long tagId) {
        if (tagId == null) {
            throw AppException.badRequest("Tag id is required.");
        }
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw AppException.notFound("Tag does not exist.");
        }
        return tag;
    }

    private ResourceType loadResourceType(Long resourceTypeId) {
        if (resourceTypeId == null) {
            throw AppException.badRequest("Resource type id is required.");
        }
        ResourceType resourceType = resourceTypeMapper.selectById(resourceTypeId);
        if (resourceType == null) {
            throw AppException.notFound("Resource type does not exist.");
        }
        return resourceType;
    }

    private void validateUniqueCategoryTopic(String categoryTopic, Long excludedCategoryId) {
        if (count(categoryMapper.countByTopicIgnoreCase(categoryTopic, excludedCategoryId)) > 0) {
            throw AppException.conflict("categoryTopic already exists.");
        }
        if (count(resourceTypeMapper.countByTypeNameIgnoreCase(categoryTopic, null)) > 0) {
            throw AppException.conflict("categoryTopic conflicts with an existing resource type.");
        }
    }

    private void validateUniqueTagName(String tagName, Long excludedTagId) {
        if (count(tagMapper.countByNameIgnoreCase(tagName, excludedTagId)) > 0) {
            throw AppException.conflict("tagName already exists.");
        }
    }

    private void validateUniqueResourceType(String typeName, Long excludedResourceTypeId) {
        if (count(resourceTypeMapper.countByTypeNameIgnoreCase(typeName, excludedResourceTypeId)) > 0) {
            throw AppException.conflict("typeName already exists.");
        }
        if (count(categoryMapper.countByTopicIgnoreCase(typeName, null)) > 0) {
            throw AppException.conflict("typeName conflicts with an existing category topic.");
        }
    }

    private void validateResourceTypeCompatibility(ResourceType existing, String newTypeName) {
        Integer usageCount = existing.getUsageCount();
        if (usageCount == null || usageCount == 0) {
            return;
        }

        try {
            ResourceTypeEnum.fromValue(newTypeName);
        } catch (IllegalArgumentException exception) {
            throw AppException.conflict(
                    "Resource types already used by resources must keep a name supported by the current resource metadata flow."
            );
        }
    }

    private String normalizeName(String value, String fieldName, int maxLength) {
        List<String> details = new ArrayList<>();
        if (value == null || value.isBlank()) {
            details.add(fieldName + " cannot be blank.");
        }

        String normalized = value == null ? null : value.trim().replaceAll("\\s+", " ");
        if (normalized != null && normalized.length() > maxLength) {
            details.add(fieldName + " cannot exceed " + maxLength + " characters.");
        }

        if (!details.isEmpty()) {
            throw AppException.badRequest(fieldName + " is invalid.", details);
        }
        return normalized;
    }

    private int count(Integer value) {
        return value == null ? 0 : value;
    }

    private List<AdminCategoryResponse> mapCategories(List<Category> categories) {
        List<AdminCategoryResponse> responses = new ArrayList<>();
        if (categories == null) {
            return responses;
        }
        for (Category category : categories) {
            responses.add(mapCategory(category));
        }
        return responses;
    }

    private AdminCategoryResponse mapCategory(Category category) {
        return new AdminCategoryResponse(
                category.getCategoryId(),
                category.getCategoryTopic(),
                ClassificationStatus.fromDatabaseValue(category.getStatus()),
                category.getUsageCount(),
                category.getCreatedAt(),
                category.getLastUpdatedAt()
        );
    }

    private List<AdminTagResponse> mapTags(List<Tag> tags) {
        List<AdminTagResponse> responses = new ArrayList<>();
        if (tags == null) {
            return responses;
        }
        for (Tag tag : tags) {
            responses.add(mapTag(tag));
        }
        return responses;
    }

    private AdminTagResponse mapTag(Tag tag) {
        return new AdminTagResponse(
                tag.getTagId(),
                tag.getTagName(),
                ClassificationStatus.fromDatabaseValue(tag.getStatus()),
                tag.getUsageCount(),
                tag.getCreatedAt(),
                tag.getLastUpdatedAt()
        );
    }

    private List<AdminResourceTypeResponse> mapResourceTypes(List<ResourceType> resourceTypes) {
        List<AdminResourceTypeResponse> responses = new ArrayList<>();
        if (resourceTypes == null) {
            return responses;
        }
        for (ResourceType resourceType : resourceTypes) {
            responses.add(mapResourceType(resourceType));
        }
        return responses;
    }

    private AdminResourceTypeResponse mapResourceType(ResourceType resourceType) {
        return new AdminResourceTypeResponse(
                resourceType.getResourceTypeId(),
                resourceType.getTypeName(),
                ClassificationStatus.fromDatabaseValue(resourceType.getStatus()),
                resourceType.getUsageCount(),
                resourceType.getCreatedAt(),
                resourceType.getLastUpdatedAt()
        );
    }

    private void recordClassification(String itemName, String kind, String action, String administrator) {
        operationHistoryService.recordOperation(itemName, kind, CLASSIFICATION_MODULE, action, administrator);
    }

    private void recordTag(String itemName, String action, String administrator) {
        operationHistoryService.recordOperation(itemName, "Tag", TAG_MODULE, action, administrator);
    }
}
