package com.cpt202.HerLink.service.impl;

import com.cpt202.HerLink.dto.resource.ResourceQueryRequest;
import com.cpt202.HerLink.dto.resource.ResourceSubmitRequest;
import com.cpt202.HerLink.dto.resource.ResourceUpdateRequest;
import com.cpt202.HerLink.entity.AppUser;
import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceFile;
import com.cpt202.HerLink.entity.ResourceSubmission;
import com.cpt202.HerLink.entity.ResourceTag;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.ReviewRecord;
import com.cpt202.HerLink.entity.Tag;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.enums.ResourceTypeEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.AppUserMapper;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceFileMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceSubmissionMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.mapper.ResourceVersionMapper;
import com.cpt202.HerLink.mapper.ReviewRecordMapper;
import com.cpt202.HerLink.mapper.TagMapper;
import com.cpt202.HerLink.service.ContributorResourceService;
import com.cpt202.HerLink.service.ResourceVersionService;
import com.cpt202.HerLink.service.notification.EmailNotificationService;
import com.cpt202.HerLink.util.FileStorageManager;
import com.cpt202.HerLink.util.FileTypeValidator;
import com.cpt202.HerLink.util.ResourceStatusValidator;
import com.cpt202.HerLink.util.TagIdNormalizer;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import com.cpt202.HerLink.vo.ResourceSubmissionVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContributorResourceServiceImpl implements ContributorResourceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContributorResourceServiceImpl.class);
    private static final int MAX_TAG_NAME_LENGTH = 100;

    private final ResourceMapper resourceMapper;
    private final ResourceSubmissionMapper resourceSubmissionMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ResourceTagMapper resourceTagMapper;
    private final ResourceTypeMapper resourceTypeMapper;
    private final ResourceVersionMapper resourceVersionMapper;
    private final ResourceFileMapper resourceFileMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final FileStorageManager fileStorageManager;
    private final ResourceVersionService resourceVersionService;
    private final AppUserMapper appUserMapper;
    private final EmailNotificationService emailNotificationService;

    public ContributorResourceServiceImpl(ResourceMapper resourceMapper,
                                          ResourceSubmissionMapper resourceSubmissionMapper,
                                          ReviewRecordMapper reviewRecordMapper,
                                          ResourceTagMapper resourceTagMapper,
                                          ResourceTypeMapper resourceTypeMapper,
                                          ResourceVersionMapper resourceVersionMapper,
                                          ResourceFileMapper resourceFileMapper,
                                          CategoryMapper categoryMapper,
                                          TagMapper tagMapper,
                                          FileStorageManager fileStorageManager,
                                          ResourceVersionService resourceVersionService,
                                          AppUserMapper appUserMapper,
                                          EmailNotificationService emailNotificationService) {
        this.resourceMapper = resourceMapper;
        this.resourceSubmissionMapper = resourceSubmissionMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.resourceTagMapper = resourceTagMapper;
        this.resourceTypeMapper = resourceTypeMapper;
        this.resourceVersionMapper = resourceVersionMapper;
        this.resourceFileMapper = resourceFileMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.fileStorageManager = fileStorageManager;
        this.resourceVersionService = resourceVersionService;
        this.appUserMapper = appUserMapper;
        this.emailNotificationService = emailNotificationService;
    }

    @Override
    @Transactional
    public ResourceDetailVO createDraft(Long currentUserId) {
        Resource resource = new Resource();
        LocalDateTime now = LocalDateTime.now();
        Category defaultCategory = loadDefaultDraftCategory();
        ResourceType defaultResourceType = resolveDraftResourceType();

        resource.setContributorId(currentUserId);
        resource.setTitle("");
        resource.setDescription("");
        resource.setCopyright("");
        resource.setCategoryId(defaultCategory.getCategoryId());
        resource.setResourceTypeId(defaultResourceType.getResourceTypeId());
        resource.setResourceType(defaultResourceType.getTypeName());
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);

        resourceMapper.insert(resource);
        categoryMapper.refreshUsageCount(resource.getCategoryId());
        if (resource.getResourceTypeId() != null) {
            resourceTypeMapper.refreshUsageCount(resource.getResourceTypeId());
        }
        Resource latestResource = resourceMapper.selectById(resource.getId());
        if (latestResource == null) {
            throw AppException.notFound("Draft was created but cannot be reloaded.");
        }

        resourceVersionService.saveVersionSnapshot(resource.getId(), currentUserId, "create", "Draft created");
        return buildResourceDetailVO(latestResource);
    }

    @Override
    @Transactional
    public ResourceDetailVO updateResource(Long currentUserId, Long resourceId, ResourceUpdateRequest request) {
        Resource resource = loadOwnedResource(currentUserId, resourceId, true);
        validateEditableStatus(resource);

        if (request == null) {
            throw AppException.badRequest("Update request cannot be null.");
        }
        if (request.getTagIds() != null && request.getTagNames() != null) {
            throw AppException.badRequest("Tag ids and tag names cannot be submitted together.");
        }

        Long oldCategoryId = resource.getCategoryId();
        Long oldResourceTypeId = resource.getResourceTypeId();

        if (request.getTitle() != null) {
            resource.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getCopyright() != null) {
            resource.setCopyright(request.getCopyright());
        }
        if (request.getCategoryId() != null) {
            validateCategoryId(request.getCategoryId());
            resource.setCategoryId(request.getCategoryId());
        }
        if (request.getPlace() != null) {
            resource.setPlace(request.getPlace());
        }
        if (request.getResourceType() != null && !request.getResourceType().isBlank()) {
            ResourceType resourceType = resolveActiveResourceType(request.getResourceType());
            resource.setResourceTypeId(resourceType.getResourceTypeId());
            resource.setResourceType(resourceType.getTypeName());
        }

        resource.setUpdatedAt(LocalDateTime.now());
        resourceMapper.updateById(resource);
        refreshCategoryUsageCounts(oldCategoryId, resource.getCategoryId());
        refreshResourceTypeUsageCounts(oldResourceTypeId, resource.getResourceTypeId());

        if (request.getTagNames() != null) {
            replaceResourceTagsByName(resourceId, request.getTagNames());
        } else if (request.getTagIds() != null) {
            replaceResourceTags(resourceId, request.getTagIds());
        }

        Resource latestResource = resourceMapper.selectById(resourceId);
        if (latestResource == null) {
            throw AppException.notFound("Resource was updated but cannot be reloaded.");
        }

        resourceVersionService.saveVersionSnapshot(
                resourceId,
                currentUserId,
                ResourceStatusEnum.REJECTED.getValue().equals(resource.getStatus()) ? "revision" : "edit",
                ResourceStatusEnum.REJECTED.getValue().equals(resource.getStatus())
                        ? "Revision after rejection"
                        : "Metadata updated"
        );
        return buildResourceDetailVO(latestResource);
    }

    @Override
    @Transactional
    public ResourceDetailVO uploadFiles(Long currentUserId, Long resourceId, MultipartFile previewImage, MultipartFile mediaFile) {
        Resource resource = loadOwnedResource(currentUserId, resourceId, true);
        validateEditableStatus(resource);

        boolean hasPreviewImage = previewImage != null && !previewImage.isEmpty();
        boolean hasMediaFile = mediaFile != null && !mediaFile.isEmpty();

        if (!hasPreviewImage && !hasMediaFile) {
            throw AppException.badRequest("At least one file must be uploaded.");
        }

        if (hasPreviewImage) {
            validatePreviewImageFile(previewImage);
        }
        if (hasMediaFile) {
            validateMediaFile(resource, mediaFile);
        }

        String folderName = "resource-" + resourceId;
        List<String> newlyStoredPaths = new ArrayList<>();
        String oldPreviewImage = resource.getPreviewImage();
        String oldMediaUrl = resource.getMediaUrl();
        Resource latestResource;

        try {
            if (hasPreviewImage) {
                FileStorageManager.StoredFile storedPreviewImage = fileStorageManager.storeFile(previewImage, folderName);
                if (storedPreviewImage != null) {
                    newlyStoredPaths.add(storedPreviewImage.getFilePath());
                    resource.setPreviewImage(storedPreviewImage.getFilePath());
                    insertResourceFile(resourceId, storedPreviewImage);
                }
            }

            if (hasMediaFile) {
                FileStorageManager.StoredFile storedMediaFile = fileStorageManager.storeFile(mediaFile, folderName);
                if (storedMediaFile != null) {
                    newlyStoredPaths.add(storedMediaFile.getFilePath());
                    resource.setMediaUrl(storedMediaFile.getFilePath());
                    insertResourceFile(resourceId, storedMediaFile);
                }
            }

            resource.setUpdatedAt(LocalDateTime.now());
            resourceMapper.updateById(resource);

            latestResource = resourceMapper.selectById(resourceId);
            if (latestResource == null) {
                throw AppException.notFound("Resource files were updated but the resource cannot be reloaded.");
            }

            resourceVersionService.saveVersionSnapshot(
                    resourceId,
                    currentUserId,
                    ResourceStatusEnum.REJECTED.getValue().equals(resource.getStatus()) ? "revision" : "edit",
                    ResourceStatusEnum.REJECTED.getValue().equals(resource.getStatus())
                            ? "Files revised after rejection"
                            : "Files updated"
            );

            if (hasPreviewImage) {
                cleanupReplacedFile(resourceId, oldPreviewImage, latestResource);
            }
            if (hasMediaFile) {
                cleanupReplacedFile(resourceId, oldMediaUrl, latestResource);
            }
        } catch (RuntimeException exception) {
            for (String storedPath : newlyStoredPaths) {
                fileStorageManager.deleteQuietly(storedPath);
            }
            throw exception;
        }

        return buildResourceDetailVO(latestResource);
    }

    @Override
    public List<ResourceListItemVO> listMyResources(Long currentUserId, ResourceQueryRequest request) {
        String keyword = null;
        String status = null;
        Long categoryId = null;

        if (request != null) {
            keyword = request.getKeyword();
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                status = ResourceStatusEnum.fromValue(request.getStatus()).getValue();
            }
            categoryId = request.getCategoryId();
        }

        List<Resource> resourceList = resourceMapper.selectMyResources(currentUserId, keyword, status, categoryId);
        if (resourceList == null) {
            resourceList = Collections.emptyList();
        }

        List<ResourceListItemVO> resourceListItemVOList = new ArrayList<>();
        for (Resource resource : resourceList) {
            ResourceListItemVO resourceListItemVO = new ResourceListItemVO();
            resourceListItemVO.setId(resource.getId());
            resourceListItemVO.setTitle(resource.getTitle());
            resourceListItemVO.setPreviewImage(resource.getPreviewImage());
            resourceListItemVO.setStatus(ResourceStatusEnum.fromValue(resource.getStatus()).getValue());
            resourceListItemVO.setResourceType(normalizeResourceTypeForResponse(resource.getResourceType()));
            resourceListItemVO.setCategoryId(resource.getCategoryId());
            resourceListItemVO.setCategoryName(normalizeCategoryName(resource.getCategoryName()));
            resourceListItemVO.setUpdatedAt(resource.getUpdatedAt());
            resourceListItemVO.setCurrentVersionNo(resourceVersionMapper.selectMaxVersionNoByResourceId(resource.getId()));

            ResourceSubmission latestSubmission = resourceSubmissionMapper.selectLatestByResourceId(resource.getId());
            if (latestSubmission != null) {
                resourceListItemVO.setLastSubmittedAt(latestSubmission.getSubmittedAt());
            }

            ReviewRecord latestReviewRecord = reviewRecordMapper.selectLatestByResourceId(resource.getId());
            resourceListItemVO.setHasReviewFeedback(
                    latestReviewRecord != null
                            && latestReviewRecord.getFeedbackComment() != null
                            && !latestReviewRecord.getFeedbackComment().isBlank()
            );
            resourceListItemVOList.add(resourceListItemVO);
        }

        return resourceListItemVOList;
    }

    @Override
    public List<ResourceSubmissionVO> listSubmissionHistory(Long currentUserId, Long resourceId) {
        loadOwnedResource(currentUserId, resourceId);

        List<ResourceSubmission> submissionList = resourceSubmissionMapper.selectByResourceId(resourceId);
        if (submissionList == null) {
            return Collections.emptyList();
        }

        List<ResourceSubmissionVO> submissionVOList = new ArrayList<>();
        for (ResourceSubmission submission : submissionList) {
            ResourceSubmissionVO submissionVO = new ResourceSubmissionVO();
            submissionVO.setSubmissionId(submission.getSubmissionId());
            submissionVO.setResourceId(submission.getResourceId());
            submissionVO.setVersionNo(submission.getVersionNo());
            submissionVO.setSubmittedBy(submission.getSubmittedBy());
            submissionVO.setSubmittedAt(submission.getSubmittedAt());
            submissionVO.setSubmissionNote(submission.getSubmissionNote());
            submissionVO.setStatusSnapshot(submission.getStatusSnapshot());
            submissionVO.setCreatedAt(submission.getCreatedAt());
            submissionVOList.add(submissionVO);
        }

        return submissionVOList;
    }

    @Override
    public ResourceDetailVO getMyResourceDetail(Long currentUserId, Long resourceId) {
        Resource resource = loadOwnedResource(currentUserId, resourceId);
        return buildResourceDetailVO(resource);
    }

    @Override
    @Transactional
    public void submitResource(Long currentUserId, Long resourceId, ResourceSubmitRequest request) {
        Resource resource = loadOwnedResource(currentUserId, resourceId, true);
        validateSubmittableResource(resource);

        ResourceSubmission latestSubmission = resourceSubmissionMapper.selectLatestByResourceId(resourceId);
        int nextVersionNo = latestSubmission == null || latestSubmission.getVersionNo() == null
                ? 1
                : latestSubmission.getVersionNo() + 1;

        LocalDateTime now = LocalDateTime.now();

        ResourceSubmission resourceSubmission = new ResourceSubmission();
        resourceSubmission.setResourceId(resourceId);
        resourceSubmission.setVersionNo(nextVersionNo);
        resourceSubmission.setSubmittedBy(currentUserId);
        resourceSubmission.setSubmittedAt(now);
        resourceSubmission.setCreatedAt(now);
        if (request != null) {
            resourceSubmission.setSubmissionNote(request.getSubmissionNote());
        }

        resourceSubmission.setStatusSnapshot(ResourceStatusEnum.PENDING_REVIEW.getValue());
        resourceSubmissionMapper.insert(resourceSubmission);

        resource.setStatus(ResourceStatusEnum.PENDING_REVIEW.getValue());
        resource.setUpdatedAt(now);
        resourceMapper.updateById(resource);
        resourceVersionService.saveVersionSnapshot(resourceId, currentUserId, "submit", "Submitted for review");
        notifyResourcePendingReview(currentUserId, resource);
    }

    @Override
    public List<CategoryTagOptionVO> listCategoryOptions() {
        return mapCategoryOptions(loadActiveCategories());
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

    @Override
    public List<CategoryTagOptionVO> listTagOptions() {
        List<Tag> tagList = tagMapper.selectActiveTags();
        if (tagList == null) {
            tagList = Collections.emptyList();
        }

        List<CategoryTagOptionVO> optionVOList = new ArrayList<>();
        for (Tag tag : tagList) {
            CategoryTagOptionVO optionVO = new CategoryTagOptionVO();
            optionVO.setId(tag.getTagId());
            optionVO.setName(tag.getTagName());
            optionVOList.add(optionVO);
        }

        return optionVOList;
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

    private void notifyResourcePendingReview(Long contributorId, Resource resource) {
        if (emailNotificationService == null || appUserMapper == null) {
            return;
        }
        AppUser contributor = appUserMapper.selectById(contributorId);
        emailNotificationService.notifyResourcePendingReview(contributor, resource);
    }

    private Category loadDefaultDraftCategory() {
        List<Category> categoryList = loadActiveCategories();
        if (categoryList.isEmpty()) {
            throw AppException.conflict("Cannot create draft because no active category is available.");
        }

        Category defaultCategory = categoryList.get(0);
        if (defaultCategory.getCategoryId() == null) {
            throw AppException.conflict("Cannot create draft because the default category is invalid.");
        }

        return defaultCategory;
    }

    private List<Category> loadActiveCategories() {
        List<Category> categoryList = categoryMapper.selectActiveCategories();
        if (categoryList == null) {
            return Collections.emptyList();
        }
        return categoryList;
    }

    private List<CategoryTagOptionVO> mapCategoryOptions(List<Category> categoryList) {
        List<CategoryTagOptionVO> optionVOList = new ArrayList<>();
        for (Category category : categoryList) {
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

    private ResourceType resolveDraftResourceType() {
        ResourceType photoType = resourceTypeMapper.selectActiveByTypeName(ResourceTypeEnum.IMAGE.getValue());
        if (photoType != null && photoType.getResourceTypeId() != null) {
            return photoType;
        }

        List<ResourceType> resourceTypeList = resourceTypeMapper.selectActiveResourceTypes();
        if (resourceTypeList == null) {
            resourceTypeList = Collections.emptyList();
        }
        for (ResourceType resourceType : resourceTypeList) {
            if (resourceType != null && resourceType.getResourceTypeId() != null) {
                return resourceType;
            }
        }

        throw AppException.conflict("Cannot create draft because no active resource type is available.");
    }

    private ResourceType resolveActiveResourceType(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            throw AppException.badRequest("Resource type is required.");
        }

        ResourceTypeEnum normalizedResourceType = tryNormalizeResourceType(resourceType);
        if (normalizedResourceType != null) {
            for (String lookupValue : normalizedResourceType.getLookupValues()) {
                ResourceType matchedResourceType = resourceTypeMapper.selectActiveByTypeName(lookupValue);
                if (matchedResourceType != null && matchedResourceType.getResourceTypeId() != null) {
                    return matchedResourceType;
                }
            }
        }

        ResourceType directMatch = resourceTypeMapper.selectActiveByTypeName(resourceType.trim());
        if (directMatch != null && directMatch.getResourceTypeId() != null) {
            return directMatch;
        }

        throw AppException.conflict("Selected resource type is unavailable.");
    }

    private void cleanupReplacedFile(Long resourceId, String filePath, Resource latestResource) {
        if (filePath == null || filePath.isBlank() || isCurrentResourceFile(filePath, latestResource)) {
            return;
        }

        resourceFileMapper.deleteByResourceIdAndFilePath(resourceId, filePath);
        try {
            fileStorageManager.delete(filePath);
        } catch (AppException exception) {
            LOGGER.warn(
                    "Failed to delete replaced file for resource {} at {}: {}",
                    resourceId,
                    filePath,
                    exception.getMessage()
            );
        }
    }

    private boolean isCurrentResourceFile(String filePath, Resource resource) {
        if (resource == null || filePath == null) {
            return false;
        }
        return filePath.equals(resource.getPreviewImage()) || filePath.equals(resource.getMediaUrl());
    }

    private void validateEditableStatus(Resource resource) {
        ResourceStatusValidator.assertEditable(resource);
    }

    private void validateSubmittableResource(Resource resource) {
        ResourceStatusValidator.assertSubmittable(resource);

        if (resource.getTitle() == null || resource.getTitle().isBlank()) {
            throw AppException.badRequest("Title is required.");
        }
        if (resource.getDescription() == null || resource.getDescription().isBlank()) {
            throw AppException.badRequest("Description is required.");
        }
        if (resource.getCopyright() == null || resource.getCopyright().isBlank()) {
            throw AppException.badRequest("Copyright is required.");
        }
        if (resource.getCategoryId() == null) {
            throw AppException.badRequest("Category is required.");
        }
        validateCategoryId(resource.getCategoryId());
        if (resource.getResourceType() == null || resource.getResourceType().isBlank()) {
            throw AppException.badRequest("Resource type is required.");
        }
        if (resource.getMediaUrl() == null || resource.getMediaUrl().isBlank()) {
            throw AppException.badRequest("Media file is required.");
        }
        validateStoredPreviewImage(resource);
        validateStoredMediaFile(resource);
    }

    private void validatePreviewImageFile(MultipartFile previewImage) {
        if (previewImage == null || previewImage.isEmpty()) {
            return;
        }

        if (!FileTypeValidator.isPreviewImageSupported(previewImage.getOriginalFilename(), previewImage.getContentType())) {
            throw AppException.badRequest("Preview image must be a JPG, JPEG, PNG, or GIF file.");
        }
    }

    private void validateMediaFile(Resource resource, MultipartFile mediaFile) {
        if (mediaFile == null || mediaFile.isEmpty()) {
            return;
        }
        if (resource.getResourceType() == null || resource.getResourceType().isBlank()) {
            throw AppException.badRequest("Please select Resource Type before uploading the media file.");
        }

        ResourceTypeEnum normalizedResourceType = tryNormalizeResourceType(resource.getResourceType());
        if (normalizedResourceType == null) {
            if (!FileTypeValidator.isSupported(mediaFile.getOriginalFilename())) {
                throw AppException.badRequest("Media file type is not supported. Allowed formats: "
                        + FileTypeValidator.describeSupportedFileTypes() + ".");
            }
            return;
        }
        if (!FileTypeValidator.isMediaFileSupported(
                mediaFile.getOriginalFilename(),
                mediaFile.getContentType(),
                normalizedResourceType.getValue()
        )) {
            throw AppException.badRequest(
                    "Media file must match the selected resource type. Allowed formats: "
                            + FileTypeValidator.describeMediaFileTypes(normalizedResourceType.getValue()) + "."
            );
        }
    }

    private void validateStoredPreviewImage(Resource resource) {
        if (resource.getPreviewImage() == null || resource.getPreviewImage().isBlank()) {
            return;
        }

        String fileType = resolveStoredFileType(resource.getId(), resource.getPreviewImage());
        if (!FileTypeValidator.isPreviewImageFileType(fileType)) {
            throw AppException.badRequest("Preview image must be an image file.");
        }
    }

    private void validateStoredMediaFile(Resource resource) {
        ResourceTypeEnum normalizedResourceType = tryNormalizeResourceType(resource.getResourceType());
        String fileType = resolveStoredFileType(resource.getId(), resource.getMediaUrl());

        if (normalizedResourceType == null) {
            if (!FileTypeValidator.isSupportedFileType(fileType)) {
                throw AppException.badRequest("Media file type is not supported. Allowed formats: "
                        + FileTypeValidator.describeSupportedFileTypes() + ".");
            }
            return;
        }

        if (!FileTypeValidator.isMediaFileTypeSupported(fileType, normalizedResourceType.getValue())) {
            throw AppException.badRequest(
                    "Media file must match the selected resource type. Allowed formats: "
                            + FileTypeValidator.describeMediaFileTypes(normalizedResourceType.getValue()) + "."
            );
        }
    }

    private String resolveStoredFileType(Long resourceId, String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }

        ResourceFile resourceFile = resourceFileMapper.selectByResourceIdAndFilePath(resourceId, filePath);
        if (resourceFile != null && resourceFile.getFileType() != null && !resourceFile.getFileType().isBlank()) {
            return resourceFile.getFileType();
        }

        return FileTypeValidator.getNormalizedExtension(filePath);
    }

    private void insertResourceFile(Long resourceId, FileStorageManager.StoredFile storedFile) {
        if (storedFile == null) {
            return;
        }

        ResourceFile resourceFile = new ResourceFile();
        resourceFile.setResourceId(resourceId);
        resourceFile.setOriginalFilename(storedFile.getOriginalFilename());
        resourceFile.setStoredFilename(storedFile.getStoredFilename());
        resourceFile.setFilePath(storedFile.getFilePath());
        resourceFile.setFileType(storedFile.getFileType());
        resourceFile.setFileSize(storedFile.getFileSize());
        resourceFile.setUploadedAt(LocalDateTime.now());
        resourceFileMapper.insert(resourceFile);
    }

    private void replaceResourceTags(Long resourceId, List<Long> tagIds) {
        List<Long> oldTagIds = resourceTagMapper.selectTagIdsByResourceId(resourceId);
        if (oldTagIds == null) {
            oldTagIds = Collections.emptyList();
        }

        List<Long> distinctTagIds = validateTagIds(tagIds);
        resourceTagMapper.deleteByResourceId(resourceId);

        for (Long tagId : distinctTagIds) {
            ResourceTag resourceTag = new ResourceTag();
            resourceTag.setResourceId(resourceId);
            resourceTag.setTagId(tagId);
            resourceTagMapper.insert(resourceTag);
        }

        refreshTagUsageCounts(oldTagIds, distinctTagIds);
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

    private Category validateCategoryId(Long categoryId) {
        if (categoryId == null) {
            throw AppException.badRequest("Category is required.");
        }

        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw AppException.badRequest("Selected category does not exist.");
        }

        if (!"ACTIVE".equalsIgnoreCase(category.getStatus())) {
            throw AppException.conflict("Selected category is inactive.");
        }

        return category;
    }

    private List<Long> validateTagIds(List<Long> tagIds) {
        List<Long> distinctTagIds = TagIdNormalizer.distinctNonNull(tagIds);
        if (distinctTagIds.isEmpty()) {
            return distinctTagIds;
        }

        List<Tag> tags = tagMapper.selectByIds(distinctTagIds);
        if (tags == null) {
            tags = Collections.emptyList();
        }
        Map<Long, Tag> tagMap = tags.stream()
                .collect(Collectors.toMap(Tag::getTagId, Function.identity()));

        List<String> errors = new ArrayList<>();
        for (Long tagId : distinctTagIds) {
            Tag tag = tagMap.get(tagId);
            if (tag == null) {
                errors.add("Tag id " + tagId + " does not exist.");
                continue;
            }
            if (!"ACTIVE".equalsIgnoreCase(tag.getStatus())) {
                errors.add("Tag id " + tagId + " is inactive.");
            }
        }

        if (!errors.isEmpty()) {
            throw AppException.badRequest("Invalid tag selection.", errors);
        }

        return distinctTagIds;
    }

    private List<String> normalizeTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalizedTagNames = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();

        for (String tagName : tagNames) {
            if (tagName == null) {
                continue;
            }

            String[] segments = tagName.split(",");
            for (String segment : segments) {
                String normalizedTagName = segment.trim();
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
