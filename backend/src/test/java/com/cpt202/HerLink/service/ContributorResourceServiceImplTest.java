package com.cpt202.HerLink.service;

import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceFileMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceSubmissionMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.mapper.ResourceVersionMapper;
import com.cpt202.HerLink.mapper.ReviewRecordMapper;
import com.cpt202.HerLink.mapper.TagMapper;
import com.cpt202.HerLink.service.ResourceVersionService;
import com.cpt202.HerLink.service.impl.ContributorResourceServiceImpl;
import com.cpt202.HerLink.util.FileStorageManager;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.Tag;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import com.cpt202.HerLink.dto.resource.ResourceUpdateRequest;
import com.cpt202.HerLink.entity.ResourceTag;
import com.cpt202.HerLink.dto.resource.ResourceSubmitRequest;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.entity.ResourceSubmission;



@ExtendWith(MockitoExtension.class)
class ContributorResourceServiceImplTest {

    @Mock
    private ResourceMapper resourceMapper;

    @Mock
    private ResourceSubmissionMapper resourceSubmissionMapper;

    @Mock
    private ReviewRecordMapper reviewRecordMapper;

    @Mock
    private ResourceTagMapper resourceTagMapper;

    @Mock
    private ResourceTypeMapper resourceTypeMapper;

    @Mock
    private ResourceVersionMapper resourceVersionMapper;

    @Mock
    private ResourceFileMapper resourceFileMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private FileStorageManager fileStorageManager;

    @Mock
    private ResourceVersionService resourceVersionService;

    @InjectMocks
    private ContributorResourceServiceImpl contributorResourceService;

    @Test
    void createDraft_shouldInsertDraftAndReturnDetailVO() {
        // setup
        when(categoryMapper.selectActiveCategories()).thenReturn(java.util.List.of(
                createCategory(1L, "places"),
                createCategory(2L, "traditions"),
                createCategory(3L, "stories"),
                createCategory(4L, "objects"),
                createCategory(5L, "educational materials"),
                createCategory(6L, "other")
        ));
        when(resourceTypeMapper.selectActiveByTypeName("photo")).thenReturn(createResourceType(1L, "photo"));

        Long currentUserId = 1L;

        doAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            resource.setId(100L);
            return 1;
        }).when(resourceMapper).insert(any(Resource.class));

        Resource latestResource = new Resource();
        latestResource.setId(100L);
        latestResource.setContributorId(currentUserId);
        latestResource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        latestResource.setCategoryId(1L);
        latestResource.setCategoryName("places");
        latestResource.setResourceTypeId(1L);
        latestResource.setResourceType("photo");
        latestResource.setCopyright("");
        when(resourceMapper.selectById(100L)).thenReturn(latestResource);
        when(resourceVersionMapper.selectMaxVersionNoByResourceId(100L)).thenReturn(1);
        when(resourceTagMapper.selectTagIdsByResourceId(100L)).thenReturn(java.util.List.of());
        when(resourceTagMapper.selectTagNamesByResourceId(100L)).thenReturn(java.util.List.of());

        // call
        ResourceDetailVO result = contributorResourceService.createDraft(currentUserId);

        // assertion
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(currentUserId, result.getContributorId());
        assertEquals(ResourceStatusEnum.DRAFT.getValue(), result.getStatus());
        assertEquals(1L, result.getCategoryId());
        assertEquals("places", result.getCategoryName());
        assertEquals("photo", result.getResourceType());
        assertEquals("", result.getCopyright());

        ArgumentCaptor<Resource> captor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceMapper).insert(captor.capture());
        verify(resourceMapper).selectById(100L);
        verify(categoryMapper).refreshUsageCount(1L);
        verify(resourceTypeMapper).refreshUsageCount(1L);
        verify(resourceVersionService).saveVersionSnapshot(100L, currentUserId, "create", "Draft created");

        Resource inserted = captor.getValue();
        assertEquals(currentUserId, inserted.getContributorId());
        assertEquals(ResourceStatusEnum.DRAFT.getValue(), inserted.getStatus());
        assertEquals(1L, inserted.getCategoryId());
        assertEquals(1L, inserted.getResourceTypeId());
        assertEquals("photo", inserted.getResourceType());
        assertEquals("", inserted.getCopyright());
        assertNotNull(inserted.getCreatedAt());
        assertNotNull(inserted.getUpdatedAt());
    }

    @Test
    void updateResource_shouldUpdateMetadataAndReplaceTags() {
        // setup
        Long currentUserId = 1L;
        Long resourceId = 10L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(currentUserId);
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCategoryId(1L);
        resource.setResourceTypeId(1L);
        resource.setResourceType("Picture");

        when(resourceMapper.selectByIdForUpdate(resourceId)).thenReturn(resource);
        when(categoryMapper.selectById(2L)).thenReturn(createCategory(2L, "traditions"));
        when(resourceTypeMapper.selectActiveByTypeName("photo")).thenReturn(createResourceType(1L, "photo"));
        when(resourceTagMapper.selectTagIdsByResourceId(resourceId)).thenReturn(List.of(11L, 12L));
        when(tagMapper.selectByIds(List.of(11L, 12L))).thenReturn(List.of(
                createTag(11L, "Temple"),
                createTag(12L, "Festival")
        ));

        Resource latestResource = new Resource();
        latestResource.setId(resourceId);
        latestResource.setContributorId(currentUserId);
        latestResource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        latestResource.setTitle("New Title");
        latestResource.setDescription("New Description");
        latestResource.setCopyright("Museum archive reference");
        latestResource.setCategoryId(2L);
        latestResource.setCategoryName("traditions");
        latestResource.setPlace("Suzhou");
        latestResource.setResourceTypeId(1L);
        latestResource.setResourceType("photo");
        when(resourceMapper.selectById(resourceId)).thenReturn(latestResource);
        when(resourceVersionMapper.selectMaxVersionNoByResourceId(resourceId)).thenReturn(2);
        when(resourceTagMapper.selectTagNamesByResourceId(resourceId)).thenReturn(List.of("Temple", "Festival"));

        ResourceUpdateRequest request = new ResourceUpdateRequest();
        request.setTitle("New Title");
        request.setDescription("New Description");
        request.setCopyright("Museum archive reference");
        request.setCategoryId(2L);
        request.setPlace("Suzhou");
        request.setResourceType("IMAGE");
        request.setTagIds(new java.util.ArrayList<>(Arrays.asList(11L, 11L, null, 12L)));

        // call
        ResourceDetailVO result = contributorResourceService.updateResource(currentUserId, resourceId, request);

        // assertion
        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertEquals("New Description", result.getDescription());
        assertEquals("Museum archive reference", result.getCopyright());
        assertEquals(2L, result.getCategoryId());
        assertEquals("traditions", result.getCategoryName());
        assertEquals("Suzhou", result.getPlace());
        assertEquals("photo", result.getResourceType());

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceMapper).updateById(resourceCaptor.capture());
        verify(resourceMapper).selectById(resourceId);
        verify(resourceTagMapper).deleteByResourceId(resourceId);
        verify(categoryMapper).refreshUsageCount(1L);
        verify(categoryMapper).refreshUsageCount(2L);
        verify(resourceTypeMapper, never()).refreshUsageCount(any());
        verify(tagMapper).refreshUsageCount(11L);
        verify(tagMapper).refreshUsageCount(12L);
        verify(resourceVersionService).saveVersionSnapshot(resourceId, currentUserId, "edit", "Metadata updated");

        Resource updatedResource = resourceCaptor.getValue();
        assertEquals(1L, updatedResource.getResourceTypeId());
        assertEquals("photo", updatedResource.getResourceType());

        ArgumentCaptor<ResourceTag> tagCaptor = ArgumentCaptor.forClass(ResourceTag.class);
        verify(resourceTagMapper, times(2)).insert(tagCaptor.capture());

        java.util.List<ResourceTag> insertedTags = tagCaptor.getAllValues();
        assertEquals(11L, insertedTags.get(0).getTagId());
        assertEquals(12L, insertedTags.get(1).getTagId());
    }

    @Test
    void submitResource_shouldThrowBadRequestWhenMediaFileMissing() {
        // setup
        Long currentUserId = 1L;
        Long resourceId = 20L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(currentUserId);
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setTitle("Title");
        resource.setDescription("Description");
        resource.setCopyright("Archive rights reserved");
        resource.setCategoryId(1L);
        resource.setResourceType("Picture");
        resource.setMediaUrl(null);

        when(resourceMapper.selectByIdForUpdate(resourceId)).thenReturn(resource);
        when(categoryMapper.selectById(1L)).thenReturn(createCategory(1L, "places"));

        // call
        AppException exception = assertThrows(
                AppException.class,
                () -> contributorResourceService.submitResource(currentUserId, resourceId, null)
        );

        // assertion
        assertEquals(400, exception.getStatusCode());
        verify(resourceSubmissionMapper, never()).insert(any());
        verify(resourceMapper, never()).updateById(argThat(updated ->
                ResourceStatusEnum.PENDING_REVIEW.getValue().equals(updated.getStatus())
        ));
    }

    @Test
    void submitResource_shouldInsertSubmissionAndUpdateResourceStatus() {
        // setup
        Long currentUserId = 1L;
        Long resourceId = 30L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(currentUserId);
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setTitle("Title");
        resource.setDescription("Description");
        resource.setCopyright("Archive rights reserved");
        resource.setCategoryId(1L);
        resource.setResourceType("Video");
        resource.setMediaUrl("resource-30/video.mp4");

        when(resourceMapper.selectByIdForUpdate(resourceId)).thenReturn(resource);
        when(categoryMapper.selectById(1L)).thenReturn(createCategory(1L, "places"));
        ResourceSubmission latestSubmission = new ResourceSubmission();
        latestSubmission.setVersionNo(2);
        when(resourceSubmissionMapper.selectLatestByResourceId(resourceId)).thenReturn(latestSubmission);

        ResourceSubmitRequest request = new ResourceSubmitRequest();
        request.setSubmissionNote("Please review");

        // call
        contributorResourceService.submitResource(currentUserId, resourceId, request);

        // assertion
        ArgumentCaptor<ResourceSubmission> submissionCaptor = ArgumentCaptor.forClass(ResourceSubmission.class);
        verify(resourceSubmissionMapper).insert(submissionCaptor.capture());

        ResourceSubmission submission = submissionCaptor.getValue();
        assertEquals(resourceId, submission.getResourceId());
        assertEquals(currentUserId, submission.getSubmittedBy());
        assertEquals(3, submission.getVersionNo());
        assertEquals("Please review", submission.getSubmissionNote());
        assertEquals(ResourceStatusEnum.PENDING_REVIEW.getValue(), submission.getStatusSnapshot());
        assertNotNull(submission.getSubmittedAt());
        assertNotNull(submission.getCreatedAt());

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceMapper).updateById(resourceCaptor.capture());

        Resource updatedResource = resourceCaptor.getValue();
        assertEquals(ResourceStatusEnum.PENDING_REVIEW.getValue(), updatedResource.getStatus());
        assertNotNull(updatedResource.getUpdatedAt());
        verify(resourceVersionService).saveVersionSnapshot(resourceId, currentUserId, "submit", "Submitted for review");
    }

    @Test
    void uploadFiles_shouldReloadResourceAndKeepPreviousFilesAfterSuccess() {
        Long currentUserId = 1L;
        Long resourceId = 40L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(currentUserId);
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCategoryId(1L);
        resource.setPreviewImage("resource-40/old-preview.jpg");
        resource.setMediaUrl("resource-40/old-video.mp4");
        resource.setResourceType("Picture");

        when(resourceMapper.selectByIdForUpdate(resourceId)).thenReturn(resource);
        when(fileStorageManager.storeFile(any(), eq("resource-40")))
                .thenReturn(storedFile("preview.jpg", "resource-40/new-preview.jpg", "jpg", 7L));

        Resource latestResource = new Resource();
        latestResource.setId(resourceId);
        latestResource.setContributorId(currentUserId);
        latestResource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        latestResource.setCategoryId(1L);
        latestResource.setCategoryName("places");
        latestResource.setPreviewImage("resource-40/new-preview.jpg");
        latestResource.setMediaUrl("resource-40/old-video.mp4");
        latestResource.setResourceType("Picture");
        when(resourceMapper.selectById(resourceId)).thenReturn(latestResource);
        when(resourceVersionMapper.selectMaxVersionNoByResourceId(resourceId)).thenReturn(2);
        when(resourceTagMapper.selectTagIdsByResourceId(resourceId)).thenReturn(List.of());
        when(resourceTagMapper.selectTagNamesByResourceId(resourceId)).thenReturn(List.of());

        MockMultipartFile previewImage = new MockMultipartFile(
                "previewImage",
                "preview.jpg",
                "image/jpeg",
                "preview".getBytes()
        );

        ResourceDetailVO result = contributorResourceService.uploadFiles(currentUserId, resourceId, previewImage, null);

        assertEquals("resource-40/new-preview.jpg", result.getPreviewImage());
        verify(resourceMapper).updateById(any(Resource.class));
        verify(resourceMapper).selectById(resourceId);
        verify(resourceFileMapper).insert(argThat(resourceFile ->
                resourceId.equals(resourceFile.getResourceId())
                        && "preview.jpg".equals(resourceFile.getOriginalFilename())
                        && "resource-40/new-preview.jpg".equals(resourceFile.getFilePath())
                        && "jpg".equals(resourceFile.getFileType())
        ));
        verify(fileStorageManager, never()).deleteQuietly("resource-40/old-preview.jpg");
        verify(fileStorageManager, never()).deleteQuietly("resource-40/old-video.mp4");
        verify(resourceVersionService).saveVersionSnapshot(resourceId, currentUserId, "edit", "Files updated");
    }

    @Test
    void uploadFiles_shouldDeleteNewlyStoredFilesWhenUploadFails() {
        Long currentUserId = 1L;
        Long resourceId = 50L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(currentUserId);
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCategoryId(1L);
        resource.setResourceType("Video");

        when(resourceMapper.selectByIdForUpdate(resourceId)).thenReturn(resource);
        when(fileStorageManager.storeFile(any(), eq("resource-50")))
                .thenReturn(storedFile("preview.jpg", "resource-50/new-preview.jpg", "jpg", 7L))
                .thenThrow(AppException.badRequest("Unsupported file type."));

        MockMultipartFile previewImage = new MockMultipartFile(
                "previewImage",
                "preview.jpg",
                "image/jpeg",
                "preview".getBytes()
        );
        MockMultipartFile mediaFile = new MockMultipartFile(
                "mediaFile",
                "media.mp4",
                "video/mp4",
                "media".getBytes()
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> contributorResourceService.uploadFiles(currentUserId, resourceId, previewImage, mediaFile)
        );

        assertEquals(400, exception.getStatusCode());
        verify(fileStorageManager).deleteQuietly("resource-50/new-preview.jpg");
        verify(resourceMapper, never()).updateById(any(Resource.class));
    }

    @Test
    void uploadFiles_shouldDeleteNewlyStoredFilesWhenVersionSaveFails() {
        Long currentUserId = 1L;
        Long resourceId = 60L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(currentUserId);
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCategoryId(1L);
        resource.setResourceType("Picture");

        when(resourceMapper.selectByIdForUpdate(resourceId)).thenReturn(resource);
        when(fileStorageManager.storeFile(any(), eq("resource-60")))
                .thenReturn(storedFile("preview.jpg", "resource-60/new-preview.jpg", "jpg", 7L));

        Resource latestResource = new Resource();
        latestResource.setId(resourceId);
        latestResource.setContributorId(currentUserId);
        latestResource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        latestResource.setCategoryId(1L);
        latestResource.setCategoryName("places");
        latestResource.setPreviewImage("resource-60/new-preview.jpg");
        latestResource.setResourceType("photo");
        when(resourceMapper.selectById(resourceId)).thenReturn(latestResource);
        doThrow(AppException.conflict("Version save failed."))
                .when(resourceVersionService)
                .saveVersionSnapshot(resourceId, currentUserId, "edit", "Files updated");

        MockMultipartFile previewImage = new MockMultipartFile(
                "previewImage",
                "preview.jpg",
                "image/jpeg",
                "preview".getBytes()
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> contributorResourceService.uploadFiles(currentUserId, resourceId, previewImage, null)
        );

        assertEquals(409, exception.getStatusCode());
        verify(fileStorageManager).deleteQuietly("resource-60/new-preview.jpg");
    }

    @Test
    void uploadFiles_shouldRejectNonImagePreviewFile() {
        Long currentUserId = 1L;
        Long resourceId = 70L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(currentUserId);
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCategoryId(1L);
        resource.setResourceType("Picture");

        when(resourceMapper.selectByIdForUpdate(resourceId)).thenReturn(resource);

        MockMultipartFile previewImage = new MockMultipartFile(
                "previewImage",
                "preview.pdf",
                "application/pdf",
                "preview".getBytes()
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> contributorResourceService.uploadFiles(currentUserId, resourceId, previewImage, null)
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("Preview image must be a JPG, JPEG, PNG, or GIF file.", exception.getMessage());
        verify(fileStorageManager, never()).storeFile(any(), any());
    }

    @Test
    void uploadFiles_shouldRejectMediaFileThatDoesNotMatchResourceType() {
        Long currentUserId = 1L;
        Long resourceId = 80L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(currentUserId);
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCategoryId(1L);
        resource.setResourceType("Picture");

        when(resourceMapper.selectByIdForUpdate(resourceId)).thenReturn(resource);

        MockMultipartFile mediaFile = new MockMultipartFile(
                "mediaFile",
                "clip.mp4",
                "video/mp4",
                "media".getBytes()
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> contributorResourceService.uploadFiles(currentUserId, resourceId, null, mediaFile)
        );

        assertEquals(400, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Media file must match the selected resource type."));
        verify(fileStorageManager, never()).storeFile(any(), any());
    }

    @Test
    void submitResource_shouldRejectWhenStoredMediaDoesNotMatchResourceType() {
        Long currentUserId = 1L;
        Long resourceId = 90L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(currentUserId);
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setTitle("Title");
        resource.setDescription("Description");
        resource.setCopyright("Archive rights reserved");
        resource.setCategoryId(1L);
        resource.setResourceType("Picture");
        resource.setMediaUrl("resource-90/video.mp4");

        when(resourceMapper.selectByIdForUpdate(resourceId)).thenReturn(resource);
        when(categoryMapper.selectById(1L)).thenReturn(createCategory(1L, "places"));

        AppException exception = assertThrows(
                AppException.class,
                () -> contributorResourceService.submitResource(currentUserId, resourceId, null)
        );

        assertEquals(400, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Media file must match the selected resource type."));
        verify(resourceSubmissionMapper, never()).insert(any());
    }

    @Test
    void listCategoryOptions_shouldReturnOnlyActiveCategoriesFromMapper() {
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(
                createCategory(20L, "Manuscripts")
        ));

        var result = contributorResourceService.listCategoryOptions();

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).getId());
        assertEquals("Manuscripts", result.get(0).getName());
    }

    @Test
    void listResourceTypeOptions_shouldReturnOnlyActiveResourceTypesFromMapper() {
        when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(List.of(
                createResourceType(30L, "Map")
        ));

        var result = contributorResourceService.listResourceTypeOptions();

        assertEquals(1, result.size());
        assertEquals(30L, result.get(0).getId());
        assertEquals("Map", result.get(0).getName());
    }

    @Test
    void listMyResources_shouldIncludeArchivedOwnedResources() {
        Long currentUserId = 1L;
        Resource archivedResource = new Resource();
        archivedResource.setId(110L);
        archivedResource.setContributorId(currentUserId);
        archivedResource.setTitle("Archived resource");
        archivedResource.setStatus(ResourceStatusEnum.ARCHIVED.getValue());
        archivedResource.setCategoryId(1L);
        archivedResource.setCategoryName("places");
        archivedResource.setResourceType("photo");

        when(resourceMapper.selectMyResources(currentUserId, null, null, null)).thenReturn(List.of(archivedResource));
        when(resourceVersionMapper.selectMaxVersionNoByResourceId(110L)).thenReturn(3);

        var result = contributorResourceService.listMyResources(currentUserId, null);

        assertEquals(1, result.size());
        assertEquals(110L, result.get(0).getId());
        assertEquals(ResourceStatusEnum.ARCHIVED.getValue(), result.get(0).getStatus());
        verify(resourceMapper).selectMyResources(currentUserId, null, null, null);
    }

    private Category createCategory(Long id, String topic) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryTopic(topic);
        category.setStatus("ACTIVE");
        return category;
    }

    private Tag createTag(Long id, String name) {
        Tag tag = new Tag();
        tag.setTagId(id);
        tag.setTagName(name);
        tag.setStatus("ACTIVE");
        return tag;
    }

    private ResourceType createResourceType(Long id, String typeName) {
        ResourceType resourceType = new ResourceType();
        resourceType.setResourceTypeId(id);
        resourceType.setTypeName(typeName);
        resourceType.setStatus("ACTIVE");
        return resourceType;
    }

    private FileStorageManager.StoredFile storedFile(String originalFilename,
                                                     String filePath,
                                                     String fileType,
                                                     long fileSize) {
        FileStorageManager.StoredFile storedFile = new FileStorageManager.StoredFile();
        storedFile.setOriginalFilename(originalFilename);
        storedFile.setStoredFilename(filePath.substring(filePath.lastIndexOf("/") + 1));
        storedFile.setFilePath(filePath);
        storedFile.setFileType(fileType);
        storedFile.setFileSize(fileSize);
        return storedFile;
    }
}
