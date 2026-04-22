package com.cpt202.HerLink.service;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceSubmission;
import com.cpt202.HerLink.entity.ResourceTag;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.ResourceVersion;
import com.cpt202.HerLink.entity.Tag;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceSubmissionMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.mapper.ResourceVersionMapper;
import com.cpt202.HerLink.mapper.ReviewRecordMapper;
import com.cpt202.HerLink.mapper.TagMapper;
import com.cpt202.HerLink.service.impl.ResourceVersionServiceImpl;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceVersionCompareVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceVersionServiceImplTest {

    @Mock
    private ResourceMapper resourceMapper;

    @Mock
    private ResourceVersionMapper resourceVersionMapper;

    @Mock
    private ResourceTagMapper resourceTagMapper;

    @Mock
    private ResourceSubmissionMapper resourceSubmissionMapper;

    @Mock
    private ReviewRecordMapper reviewRecordMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private ResourceTypeMapper resourceTypeMapper;

    @InjectMocks
    private ResourceVersionServiceImpl resourceVersionService;

    @Test
    void saveVersionSnapshot_shouldInsertNextVersionSnapshot() {
        Long resourceId = 10L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(1L);
        resource.setTitle("Ancient Temple");
        resource.setDescription("Historic notes");
        resource.setCopyright("Museum archive");
        resource.setCategoryId(2L);
        resource.setCategoryName("traditions");
        resource.setPlace("Suzhou");
        resource.setPreviewImage("resource-10/preview.jpg");
        resource.setMediaUrl("resource-10/video.mp4");
        resource.setResourceType("Picture");

        when(resourceMapper.selectById(resourceId)).thenReturn(resource);
        when(resourceVersionMapper.selectMaxVersionNoByResourceId(resourceId)).thenReturn(2);
        when(resourceTagMapper.selectTagNamesByResourceId(resourceId)).thenReturn(List.of("Temple", "Festival"));

        resourceVersionService.saveVersionSnapshot(resourceId, 1L, "edit", "Metadata updated");

        ArgumentCaptor<ResourceVersion> captor = ArgumentCaptor.forClass(ResourceVersion.class);
        verify(resourceVersionMapper).insert(captor.capture());

        ResourceVersion inserted = captor.getValue();
        assertEquals(resourceId, inserted.getResourceId());
        assertEquals(3, inserted.getVersionNo());
        assertEquals("edit", inserted.getChangeType());
        assertEquals("Metadata updated", inserted.getChangeSummary());
        assertTrue(inserted.getSnapshot().contains("\"title\":\"Ancient Temple\""));
        assertTrue(inserted.getSnapshot().contains("\"resourceType\":\"photo\""));
        assertTrue(inserted.getSnapshot().contains("\"tagNames\":[\"Temple\",\"Festival\"]"));
    }

    @Test
    void compareVersions_shouldRejectNonOwner() {
        Resource resource = new Resource();
        resource.setId(20L);
        resource.setContributorId(2L);

        when(resourceMapper.selectById(20L)).thenReturn(resource);

        AppException exception = assertThrows(
                AppException.class,
                () -> resourceVersionService.compareVersions(1L, 20L, 1, 2)
        );

        assertEquals(403, exception.getStatusCode());
        verify(resourceVersionMapper, never()).selectByResourceIdAndVersionNo(eq(20L), anyInt());
    }

    @Test
    void compareVersions_shouldReturnDiffItems() {
        Resource resource = new Resource();
        resource.setId(30L);
        resource.setContributorId(1L);

        ResourceVersion leftVersion = new ResourceVersion();
        leftVersion.setResourceId(30L);
        leftVersion.setVersionNo(1);
        leftVersion.setSnapshot("""
                {"title":"Old Title","description":"Old Description","copyright":"Museum archive","categoryId":1,"categoryName":"places","place":"Suzhou","resourceType":"Picture","previewImage":"resource-30/old.jpg","mediaUrl":"resource-30/old.mp4","tagNames":["Temple"]}
                """);

        ResourceVersion rightVersion = new ResourceVersion();
        rightVersion.setResourceId(30L);
        rightVersion.setVersionNo(2);
        rightVersion.setSnapshot("""
                {"title":"New Title","description":"Old Description","copyright":"Museum archive","categoryId":2,"categoryName":"traditions","place":"Hangzhou","resourceType":"Video","previewImage":"resource-30/new.jpg","mediaUrl":"resource-30/new.mp4","tagNames":["Temple","Festival"]}
                """);

        when(resourceMapper.selectById(30L)).thenReturn(resource);
        when(resourceVersionMapper.selectByResourceIdAndVersionNo(30L, 1)).thenReturn(leftVersion);
        when(resourceVersionMapper.selectByResourceIdAndVersionNo(30L, 2)).thenReturn(rightVersion);

        ResourceVersionCompareVO result = resourceVersionService.compareVersions(1L, 30L, 1, 2);

        assertEquals(30L, result.getResourceId());
        assertEquals(9, result.getDiffItems().size());
        assertEquals("Title", result.getDiffItems().get(0).getFieldLabel());
        assertEquals("Old Title", result.getDiffItems().get(0).getLeftValue());
        assertEquals("New Title", result.getDiffItems().get(0).getRightValue());
        assertTrue(result.getDiffItems().get(0).getChanged());
        assertEquals("traditions", result.getDiffItems().get(3).getRightValue());
        assertEquals("photo", result.getDiffItems().get(5).getLeftValue());
        assertEquals("video", result.getDiffItems().get(5).getRightValue());
    }

    @Test
    void rollbackToVersion_shouldRestoreMetadataAndWriteRollbackVersion() {
        Long currentUserId = 1L;
        Long resourceId = 40L;

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setContributorId(currentUserId);
        resource.setStatus(ResourceStatusEnum.REJECTED.getValue());
        resource.setTitle("Current Title");
        resource.setDescription("Current Description");
        resource.setCopyright("Current Copyright");
        resource.setCategoryId(5L);
        resource.setPlace("Current Place");
        resource.setPreviewImage("resource-40/current-preview.jpg");
        resource.setMediaUrl("resource-40/current-video.mp4");
        resource.setResourceTypeId(1L);
        resource.setResourceType("Picture");

        ResourceVersion targetVersion = new ResourceVersion();
        targetVersion.setResourceId(resourceId);
        targetVersion.setVersionNo(2);
        targetVersion.setSnapshot("""
                {"title":"Restored Title","description":"Restored Description","copyright":"Restored Copyright","categoryId":2,"categoryName":"traditions","place":"Suzhou","resourceType":"Video","previewImage":"resource-40/legacy-preview.jpg","mediaUrl":"resource-40/legacy-video.mp4","tagNames":["Temple"]}
                """);

        Resource latestResource = new Resource();
        latestResource.setId(resourceId);
        latestResource.setContributorId(currentUserId);
        latestResource.setStatus(ResourceStatusEnum.REJECTED.getValue());
        latestResource.setTitle("Restored Title");
        latestResource.setDescription("Restored Description");
        latestResource.setCopyright("Restored Copyright");
        latestResource.setCategoryId(2L);
        latestResource.setCategoryName("traditions");
        latestResource.setPlace("Suzhou");
        latestResource.setPreviewImage("resource-40/legacy-preview.jpg");
        latestResource.setMediaUrl("resource-40/legacy-video.mp4");
        latestResource.setResourceTypeId(2L);
        latestResource.setResourceType("video");

        ResourceSubmission latestSubmission = new ResourceSubmission();
        latestSubmission.setSubmittedAt(java.time.LocalDateTime.now());

        when(resourceMapper.selectByIdForUpdate(resourceId)).thenReturn(resource);
        when(resourceVersionMapper.selectByResourceIdAndVersionNo(resourceId, 2)).thenReturn(targetVersion);
        when(categoryMapper.selectById(2L)).thenReturn(createCategory(2L, "traditions"));
        when(resourceTypeMapper.selectActiveByTypeName("video")).thenReturn(createResourceType(2L, "video"));
        when(resourceTagMapper.selectTagIdsByResourceId(resourceId)).thenReturn(List.of(9L));
        when(tagMapper.selectByNameIgnoreCase("Temple")).thenReturn(createTag(7L, "Temple"));
        when(resourceMapper.selectById(resourceId)).thenReturn(latestResource);
        when(resourceTagMapper.selectTagNamesByResourceId(resourceId)).thenReturn(List.of("Temple"));
        when(resourceVersionMapper.selectMaxVersionNoByResourceId(resourceId)).thenReturn(3, 4);
        when(resourceSubmissionMapper.selectLatestByResourceId(resourceId)).thenReturn(latestSubmission);

        ResourceDetailVO result = resourceVersionService.rollbackToVersion(currentUserId, resourceId, 2);

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceMapper).updateById(resourceCaptor.capture());
        Resource updatedResource = resourceCaptor.getValue();

        assertEquals("Restored Title", updatedResource.getTitle());
        assertEquals("Restored Description", updatedResource.getDescription());
        assertEquals("Restored Copyright", updatedResource.getCopyright());
        assertEquals(2L, updatedResource.getCategoryId());
        assertEquals("Suzhou", updatedResource.getPlace());
        assertEquals(2L, updatedResource.getResourceTypeId());
        assertEquals("video", updatedResource.getResourceType());
        assertEquals("resource-40/legacy-preview.jpg", updatedResource.getPreviewImage());
        assertEquals("resource-40/legacy-video.mp4", updatedResource.getMediaUrl());

        verify(categoryMapper).refreshUsageCount(5L);
        verify(categoryMapper).refreshUsageCount(2L);
        verify(resourceTypeMapper).refreshUsageCount(1L);
        verify(resourceTypeMapper).refreshUsageCount(2L);
        verify(resourceTagMapper).deleteByResourceId(resourceId);

        ArgumentCaptor<ResourceTag> tagCaptor = ArgumentCaptor.forClass(ResourceTag.class);
        verify(resourceTagMapper).insert(tagCaptor.capture());
        assertEquals(7L, tagCaptor.getValue().getTagId());
        verify(tagMapper).refreshUsageCount(9L);
        verify(tagMapper).refreshUsageCount(7L);

        ArgumentCaptor<ResourceVersion> versionCaptor = ArgumentCaptor.forClass(ResourceVersion.class);
        verify(resourceVersionMapper).insert(versionCaptor.capture());
        assertEquals(4, versionCaptor.getValue().getVersionNo());
        assertEquals("rollback", versionCaptor.getValue().getChangeType());
        assertEquals("Restored version V2", versionCaptor.getValue().getChangeSummary());
        assertEquals("resource-40/legacy-preview.jpg", result.getPreviewImage());
        assertEquals("resource-40/legacy-video.mp4", result.getMediaUrl());
        assertEquals(4, result.getCurrentVersionNo());
    }

    @Test
    void rollbackToVersion_shouldRejectPendingReviewResource() {
        Resource resource = new Resource();
        resource.setId(50L);
        resource.setContributorId(1L);
        resource.setStatus(ResourceStatusEnum.PENDING_REVIEW.getValue());

        when(resourceMapper.selectByIdForUpdate(50L)).thenReturn(resource);

        AppException exception = assertThrows(
                AppException.class,
                () -> resourceVersionService.rollbackToVersion(1L, 50L, 2)
        );

        assertEquals(409, exception.getStatusCode());
        verify(resourceVersionMapper, never()).selectByResourceIdAndVersionNo(eq(50L), anyInt());
    }

    private Category createCategory(Long id, String topic) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryTopic(topic);
        category.setStatus("ACTIVE");
        return category;
    }

    private Tag createTag(Long id, String tagName) {
        Tag tag = new Tag();
        tag.setTagId(id);
        tag.setTagName(tagName);
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
}
