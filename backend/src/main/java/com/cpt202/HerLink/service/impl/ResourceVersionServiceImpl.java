package com.cpt202.HerLink.service.impl;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceSubmission;
import com.cpt202.HerLink.entity.ResourceTag;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.ResourceVersion;
import com.cpt202.HerLink.entity.ReviewRecord;
import com.cpt202.HerLink.entity.Tag;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.enums.ResourceTypeEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceSubmissionMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.mapper.ResourceVersionMapper;
import com.cpt202.HerLink.mapper.ReviewRecordMapper;
import com.cpt202.HerLink.mapper.TagMapper;
import com.cpt202.HerLink.service.ResourceVersionService;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceVersionCompareVO;
import com.cpt202.HerLink.vo.ResourceVersionDiffItemVO;
import com.cpt202.HerLink.vo.ResourceVersionVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ResourceVersionServiceImpl implements ResourceVersionService {

    private static final int MAX_TAG_NAME_LENGTH = 100;
    private static final TypeReference<LinkedHashMap<String, Object>> SNAPSHOT_MAP_TYPE = new TypeReference<>() {
    };

    private final ResourceMapper resourceMapper;
    private final ResourceVersionMapper resourceVersionMapper;
    private final ResourceTagMapper resourceTagMapper;
    private final ResourceSubmissionMapper resourceSubmissionMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ResourceTypeMapper resourceTypeMapper;
    private final ObjectMapper objectMapper;

    public ResourceVersionServiceImpl(ResourceMapper resourceMapper,
                                      ResourceVersionMapper resourceVersionMapper,
                                      ResourceTagMapper resourceTagMapper,
                                      ResourceSubmissionMapper resourceSubmissionMapper,
                                      ReviewRecordMapper reviewRecordMapper,
                                      CategoryMapper categoryMapper,
                                      TagMapper tagMapper,
                                      ResourceTypeMapper resourceTypeMapper) {
        this.resourceMapper = resourceMapper;
        this.resourceVersionMapper = resourceVersionMapper;
        this.resourceTagMapper = resourceTagMapper;
        this.resourceSubmissionMapper = resourceSubmissionMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.resourceTypeMapper = resourceTypeMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void saveVersionSnapshot(Long resourceId, Long userId, String changeType, String changeSummary) {
        if (resourceId == null) {
            throw AppException.badRequest("Resource id is required.");
        }
        if (userId == null) {
            throw AppException.badRequest("Current user id is required.");
        }
        if (changeType == null || changeType.isBlank()) {
            throw AppException.badRequest("Change type is required.");
        }

        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw AppException.notFound("Resource does not exist.");
        }

        ResourceVersion resourceVersion = new ResourceVersion();
        resourceVersion.setResourceId(resourceId);
        resourceVersion.setVersionNo(resolveNextVersionNo(resourceId));
        resourceVersion.setSnapshot(serializeSnapshot(buildSnapshot(resource)));
        resourceVersion.setChangeType(changeType.trim().toLowerCase());
        resourceVersion.setChangeSummary(changeSummary);
        resourceVersion.setCreatedBy(userId);
        resourceVersion.setCreatedAt(LocalDateTime.now());
        resourceVersionMapper.insert(resourceVersion);
    }

    @Override
    public List<ResourceVersionVO> listVersions(Long currentUserId, Long resourceId) {
        loadOwnedResource(currentUserId, resourceId);

        List<ResourceVersion> versionList = resourceVersionMapper.selectByResourceId(resourceId);
        if (versionList == null) {
            return Collections.emptyList();
        }

        List<ResourceVersionVO> versionVOList = new ArrayList<>();
        for (ResourceVersion resourceVersion : versionList) {
            versionVOList.add(buildVersionVO(resourceVersion, false));
        }

        return versionVOList;
    }

    @Override
    public ResourceVersionVO getVersion(Long currentUserId, Long resourceId, Integer versionNo) {
        loadOwnedResource(currentUserId, resourceId);
        ResourceVersion resourceVersion = loadResourceVersion(resourceId, versionNo);
        return buildVersionVO(resourceVersion, true);
    }

    @Override
    public ResourceVersionCompareVO compareVersions(Long currentUserId, Long resourceId, Integer leftVersionNo, Integer rightVersionNo) {
        loadOwnedResource(currentUserId, resourceId);

        ResourceVersion leftVersion = loadResourceVersion(resourceId, leftVersionNo);
        ResourceVersion rightVersion = loadResourceVersion(resourceId, rightVersionNo);
        ResourceSnapshot leftSnapshot = deserializeSnapshot(leftVersion.getSnapshot());
        ResourceSnapshot rightSnapshot = deserializeSnapshot(rightVersion.getSnapshot());

        ResourceVersionCompareVO compareVO = new ResourceVersionCompareVO();
        compareVO.setResourceId(resourceId);
        compareVO.setLeftVersionNo(leftVersionNo);
        compareVO.setRightVersionNo(rightVersionNo);
        compareVO.setDiffItems(List.of(
                createDiffItem("title", "Title", leftSnapshot.getTitle(), rightSnapshot.getTitle()),
                createDiffItem("description", "Description", leftSnapshot.getDescription(), rightSnapshot.getDescription()),
                createDiffItem("copyright", "Copyright", leftSnapshot.getCopyright(), rightSnapshot.getCopyright()),
                createDiffItem("category", "Category", displayCategory(leftSnapshot), displayCategory(rightSnapshot)),
                createDiffItem("place", "Place", leftSnapshot.getPlace(), rightSnapshot.getPlace()),
                createDiffItem(
                        "resourceType",
                        "Resource Type",
                        normalizeResourceTypeValue(leftSnapshot.getResourceType()),
                        normalizeResourceTypeValue(rightSnapshot.getResourceType())
                ),
                createDiffItem("previewImage", "Preview Image", leftSnapshot.getPreviewImage(), rightSnapshot.getPreviewImage()),
                createDiffItem("mediaUrl", "Media File", leftSnapshot.getMediaUrl(), rightSnapshot.getMediaUrl()),
                createDiffItem("tagNames", "Tags", formatTagNames(leftSnapshot.getTagNames()), formatTagNames(rightSnapshot.getTagNames()))
        ));
        return compareVO;
    }

    @Override
    @Transactional
    public ResourceDetailVO rollbackToVersion(Long currentUserId, Long resourceId, Integer versionNo) {
        Resource resource = loadOwnedResource(currentUserId, resourceId, true);
        validateRollbackStatus(resource);

        ResourceVersion resourceVersion = loadResourceVersion(resourceId, versionNo);
        ResourceSnapshot snapshot = deserializeSnapshot(resourceVersion.getSnapshot());

        Long oldCategoryId = resource.getCategoryId();
        Long oldResourceTypeId = resource.getResourceTypeId();
        Long restoredCategoryId = validateRollbackCategory(snapshot.getCategoryId()).getCategoryId();
        ResourceType restoredResourceType = validateRollbackResourceType(snapshot.getResourceType());

        resource.setTitle(defaultText(snapshot.getTitle()));
        resource.setDescription(defaultText(snapshot.getDescription()));
        resource.setCopyright(defaultText(snapshot.getCopyright()));
        resource.setCategoryId(restoredCategoryId);
        resource.setPlace(snapshot.getPlace());
        resource.setResourceTypeId(restoredResourceType.getResourceTypeId());
        resource.setResourceType(restoredResourceType.getTypeName());
        resource.setPreviewImage(snapshot.getPreviewImage());
        resource.setMediaUrl(snapshot.getMediaUrl());
        resource.setUpdatedAt(LocalDateTime.now());
        resourceMapper.updateById(resource);
        refreshCategoryUsageCounts(oldCategoryId, restoredCategoryId);
        refreshResourceTypeUsageCounts(oldResourceTypeId, resource.getResourceTypeId());

        replaceResourceTagsByName(resourceId, snapshot.getTagNames());

        Resource latestResource = resourceMapper.selectById(resourceId);
        if (latestResource == null) {
            throw AppException.notFound("Resource was restored but cannot be reloaded.");
        }

        saveVersionSnapshot(resourceId, currentUserId, "rollback", "Restored version V" + versionNo);
        return buildResourceDetailVO(latestResource);
    }

    private Resource loadOwnedResource(Long currentUserId, Long resourceId) {
        return loadOwnedResource(currentUserId, resourceId, false);
    }

    private Resource loadOwnedResource(Long currentUserId, Long resourceId, boolean forUpdate) {
        Resource resource = forUpdate
                ? resourceMapper.selectByIdForUpdate(resourceId)
                : resourceMapper.selectById(resourceId);

        if (resource == null) {
            throw AppException.notFound("Resource does not exist.");
        }
        if (!Objects.equals(resource.getContributorId(), currentUserId)) {
            throw AppException.forbidden("Current user does not own this resource.");
        }

        return resource;
    }

    private ResourceVersion loadResourceVersion(Long resourceId, Integer versionNo) {
        if (versionNo == null || versionNo <= 0) {
            throw AppException.badRequest("Version number is required.");
        }

        ResourceVersion resourceVersion = resourceVersionMapper.selectByResourceIdAndVersionNo(resourceId, versionNo);
        if (resourceVersion == null) {
            throw AppException.notFound("Requested version does not exist.");
        }

        return resourceVersion;
    }

    private int resolveNextVersionNo(Long resourceId) {
        Integer maxVersionNo = resourceVersionMapper.selectMaxVersionNoByResourceId(resourceId);
        return maxVersionNo == null ? 1 : maxVersionNo + 1;
    }

    private ResourceSnapshot buildSnapshot(Resource resource) {
        ResourceSnapshot snapshot = new ResourceSnapshot();
        snapshot.setTitle(resource.getTitle());
        snapshot.setDescription(resource.getDescription());
        snapshot.setCopyright(resource.getCopyright());
        snapshot.setCategoryId(resource.getCategoryId());
        snapshot.setCategoryName(normalizeCategoryName(resource.getCategoryName()));
        snapshot.setPlace(resource.getPlace());
        snapshot.setResourceType(normalizeResourceTypeValue(resource.getResourceType()));
        snapshot.setPreviewImage(resource.getPreviewImage());
        snapshot.setMediaUrl(resource.getMediaUrl());
        snapshot.setTagNames(resourceTagMapper.selectTagNamesByResourceId(resource.getId()));
        return snapshot;
    }

    private String serializeSnapshot(ResourceSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new AppException(
                    500,
                    "Failed to save resource version snapshot.",
                    buildSnapshotErrorDetails(exception)
            );
        }
    }

    private ResourceSnapshot deserializeSnapshot(String snapshotText) {
        try {
            return objectMapper.readValue(snapshotText, ResourceSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw new AppException(
                    500,
                    "Failed to read resource version snapshot.",
                    buildSnapshotErrorDetails(exception)
            );
        }
    }

    private Map<String, Object> deserializeSnapshotMap(String snapshotText) {
        try {
            return objectMapper.readValue(snapshotText, SNAPSHOT_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new AppException(
                    500,
                    "Failed to read resource version snapshot.",
                    buildSnapshotErrorDetails(exception)
            );
        }
    }

    private List<String> buildSnapshotErrorDetails(JsonProcessingException exception) {
        String detail = exception.getOriginalMessage();
        if (detail == null || detail.isBlank()) {
            detail = exception.getMessage();
        }
        if (detail == null || detail.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(detail);
    }

    private ResourceVersionVO buildVersionVO(ResourceVersion resourceVersion, boolean includeSnapshot) {
        ResourceVersionVO versionVO = new ResourceVersionVO();
        versionVO.setVersionId(resourceVersion.getVersionId());
        versionVO.setResourceId(resourceVersion.getResourceId());
        versionVO.setVersionNo(resourceVersion.getVersionNo());
        versionVO.setChangeType(resourceVersion.getChangeType());
        versionVO.setChangeSummary(resourceVersion.getChangeSummary());
        versionVO.setCreatedBy(resourceVersion.getCreatedBy());
        versionVO.setCreatedAt(resourceVersion.getCreatedAt());

        if (includeSnapshot) {
            versionVO.setSnapshotMap(deserializeSnapshotMap(resourceVersion.getSnapshot()));
        }

        return versionVO;
    }

    private ResourceVersionDiffItemVO createDiffItem(String fieldName,
                                                     String fieldLabel,
                                                     String leftValue,
                                                     String rightValue) {
        ResourceVersionDiffItemVO diffItemVO = new ResourceVersionDiffItemVO();
        String normalizedLeftValue = normalizeCompareValue(leftValue);
        String normalizedRightValue = normalizeCompareValue(rightValue);

        diffItemVO.setFieldName(fieldName);
        diffItemVO.setFieldLabel(fieldLabel);
        diffItemVO.setLeftValue(normalizedLeftValue);
        diffItemVO.setRightValue(normalizedRightValue);
        diffItemVO.setChanged(!Objects.equals(normalizedLeftValue, normalizedRightValue));
        return diffItemVO;
    }

    private String normalizeCompareValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private String displayCategory(ResourceSnapshot snapshot) {
        if (snapshot.getCategoryName() != null && !snapshot.getCategoryName().isBlank()) {
            return normalizeCategoryName(snapshot.getCategoryName());
        }

        if (snapshot.getCategoryId() == null) {
            return "-";
        }

        Category category = categoryMapper.selectById(snapshot.getCategoryId());
        return category == null
                ? "Category #" + snapshot.getCategoryId()
                : normalizeCategoryName(category.getCategoryTopic());
    }

    private void validateRollbackStatus(Resource resource) {
        String status = resource.getStatus();
        if (!ResourceStatusEnum.DRAFT.getValue().equals(status)
                && !ResourceStatusEnum.REJECTED.getValue().equals(status)) {
            throw AppException.conflict("Current resource status does not allow rollback.");
        }
    }

    private Category validateRollbackCategory(Long categoryId) {
        if (categoryId == null) {
            throw AppException.conflict("Stored category is missing and cannot be restored.");
        }

        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw AppException.conflict("Stored category does not exist and cannot be restored.");
        }
        if (!"ACTIVE".equalsIgnoreCase(category.getStatus())) {
            throw AppException.conflict("Stored category is inactive and cannot be restored.");
        }

        return category;
    }

    private ResourceType validateRollbackResourceType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            throw AppException.conflict("Stored resource type is missing and cannot be restored.");
        }

        try {
            ResourceTypeEnum normalizedResourceType = ResourceTypeEnum.fromValue(resourceType);
            for (String lookupValue : normalizedResourceType.getLookupValues()) {
                ResourceType matchedResourceType = resourceTypeMapper.selectActiveByTypeName(lookupValue);
                if (matchedResourceType != null && matchedResourceType.getResourceTypeId() != null) {
                    return matchedResourceType;
                }
            }
        } catch (IllegalArgumentException exception) {
            // Custom admin-managed resource types are restored by exact active type name above.
        }

        ResourceType directMatch = resourceTypeMapper.selectActiveByTypeName(resourceType.trim());
        if (directMatch != null && directMatch.getResourceTypeId() != null) {
            return directMatch;
        }

        throw AppException.conflict("Stored resource type is unavailable and cannot be restored.");
    }

    private void replaceResourceTagsByName(Long resourceId, List<String> tagNames) {
        List<Long> resolvedTagIds = new ArrayList<>();

        for (String tagName : normalizeTagNames(tagNames)) {
            Tag existingTag = tagMapper.selectByNameIgnoreCase(tagName);
            if (existingTag != null && existingTag.getTagId() != null) {
                if (!"ACTIVE".equalsIgnoreCase(existingTag.getStatus())) {
                    throw AppException.conflict("Tag \"" + existingTag.getTagName() + "\" exists but is inactive.");
                }
                resolvedTagIds.add(existingTag.getTagId());
                continue;
            }

            Tag newTag = new Tag();
            newTag.setTagName(tagName);
            newTag.setStatus("ACTIVE");
            newTag.setUsageCount(0);
            tagMapper.insert(newTag);

            if (newTag.getTagId() != null) {
                resolvedTagIds.add(newTag.getTagId());
            }
        }

        replaceResourceTags(resourceId, resolvedTagIds);
    }

    private void replaceResourceTags(Long resourceId, List<Long> tagIds) {
        List<Long> oldTagIds = resourceTagMapper.selectTagIdsByResourceId(resourceId);
        if (oldTagIds == null) {
            oldTagIds = Collections.emptyList();
        }

        resourceTagMapper.deleteByResourceId(resourceId);

        for (Long tagId : distinctTagIds(tagIds)) {
            ResourceTag resourceTag = new ResourceTag();
            resourceTag.setResourceId(resourceId);
            resourceTag.setTagId(tagId);
            resourceTagMapper.insert(resourceTag);
        }

        refreshTagUsageCounts(oldTagIds, tagIds);
    }

    private List<Long> distinctTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> distinctTagIds = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();

        for (Long tagId : tagIds) {
            if (tagId == null || !seen.add(tagId)) {
                continue;
            }
            distinctTagIds.add(tagId);
        }

        return distinctTagIds;
    }

    private List<String> normalizeTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalizedTagNames = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String tagName : tagNames) {
            String normalizedTagName = defaultText(tagName).trim();
            if (normalizedTagName.isEmpty()) {
                continue;
            }
            if (normalizedTagName.length() > MAX_TAG_NAME_LENGTH) {
                throw AppException.badRequest("Tag name cannot exceed 100 characters.");
            }

            String normalizedKey = normalizedTagName.toLowerCase();
            if (!seen.add(normalizedKey)) {
                continue;
            }

            normalizedTagNames.add(normalizedTagName);
        }

        return normalizedTagNames;
    }

    private void refreshCategoryUsageCounts(Long oldCategoryId, Long newCategoryId) {
        if (Objects.equals(oldCategoryId, newCategoryId)) {
            return;
        }

        if (oldCategoryId != null) {
            categoryMapper.refreshUsageCount(oldCategoryId);
        }
        if (newCategoryId != null) {
            categoryMapper.refreshUsageCount(newCategoryId);
        }
    }

    private void refreshResourceTypeUsageCounts(Long oldResourceTypeId, Long newResourceTypeId) {
        if (Objects.equals(oldResourceTypeId, newResourceTypeId)) {
            return;
        }

        if (oldResourceTypeId != null) {
            resourceTypeMapper.refreshUsageCount(oldResourceTypeId);
        }
        if (newResourceTypeId != null) {
            resourceTypeMapper.refreshUsageCount(newResourceTypeId);
        }
    }

    private void refreshTagUsageCounts(List<Long> oldTagIds, List<Long> newTagIds) {
        Set<Long> affectedTagIds = new LinkedHashSet<>();
        if (oldTagIds != null) {
            affectedTagIds.addAll(oldTagIds);
        }
        if (newTagIds != null) {
            affectedTagIds.addAll(newTagIds);
        }

        for (Long affectedTagId : affectedTagIds) {
            if (affectedTagId != null) {
                tagMapper.refreshUsageCount(affectedTagId);
            }
        }
    }

    private String formatTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return "-";
        }

        return String.join(", ", tagNames);
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
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
        resourceDetailVO.setResourceType(normalizeResourceTypeValue(resource.getResourceType()));
        resourceDetailVO.setCurrentVersionNo(resourceVersionMapper.selectMaxVersionNoByResourceId(resource.getId()));

        List<Long> tagIds = resourceTagMapper.selectTagIdsByResourceId(resource.getId());
        resourceDetailVO.setTagIds(tagIds);
        resourceDetailVO.setTagNames(resourceTagMapper.selectTagNamesByResourceId(resource.getId()));

        ResourceSubmission latestSubmission = resourceSubmissionMapper.selectLatestByResourceId(resource.getId());
        if (latestSubmission != null) {
            resourceDetailVO.setLatestSubmittedAt(latestSubmission.getSubmittedAt());
        }

        ReviewRecord latestReviewRecord = reviewRecordMapper.selectLatestByResourceId(resource.getId());
        if (latestReviewRecord != null) {
            resourceDetailVO.setLatestReviewStatus(latestReviewRecord.getStatus());
            resourceDetailVO.setLatestFeedbackComment(latestReviewRecord.getFeedbackComment());
        }

        return resourceDetailVO;
    }

    private String normalizeCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return categoryName;
        }

        String normalizedCategoryName = categoryName.trim().replaceAll("\\s+", " ").toLowerCase();
        if ("education".equals(normalizedCategoryName)) {
            return "educational materials";
        }
        return normalizedCategoryName;
    }

    private String normalizeResourceTypeValue(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return resourceType;
        }

        try {
            return ResourceTypeEnum.fromValue(resourceType).getValue();
        } catch (IllegalArgumentException exception) {
            return resourceType;
        }
    }

    private static final class ResourceSnapshot {

        private String title;
        private String description;
        private String copyright;
        private Long categoryId;
        private String categoryName;
        private String place;
        private String resourceType;
        private String previewImage;
        private String mediaUrl;
        private List<String> tagNames;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCopyright() {
            return copyright;
        }

        public void setCopyright(String copyright) {
            this.copyright = copyright;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getPlace() {
            return place;
        }

        public void setPlace(String place) {
            this.place = place;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getPreviewImage() {
            return previewImage;
        }

        public void setPreviewImage(String previewImage) {
            this.previewImage = previewImage;
        }

        public String getMediaUrl() {
            return mediaUrl;
        }

        public void setMediaUrl(String mediaUrl) {
            this.mediaUrl = mediaUrl;
        }

        public List<String> getTagNames() {
            return tagNames;
        }

        public void setTagNames(List<String> tagNames) {
            this.tagNames = tagNames;
        }
    }
}
