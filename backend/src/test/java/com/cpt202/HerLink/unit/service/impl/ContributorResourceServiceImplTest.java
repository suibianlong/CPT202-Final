package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.service.impl.*;
import com.cpt202.HerLink.service.impl.ContributorResourceServiceImpl;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.cpt202.HerLink.dto.resource.ResourceQueryRequest;
import com.cpt202.HerLink.dto.resource.ResourceSubmitRequest;
import com.cpt202.HerLink.dto.resource.ResourceUpdateRequest;
import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceType;
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
import com.cpt202.HerLink.service.ResourceVersionService;
import com.cpt202.HerLink.service.notification.EmailNotificationService;
import com.cpt202.HerLink.util.FileStorageManager;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import com.cpt202.HerLink.vo.ResourceSubmissionVO;

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
    @Mock
    private AppUserMapper appUserMapper;
    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private ContributorResourceServiceImpl contributorResourceService;

    private static final Long TEST_USER_ID = 100L;
    private static final Long TEST_RESOURCE_ID = 1L;
    private static final Long TEST_CATEGORY_ID = 10L;
    private static final Long TEST_TYPE_ID = 20L;

    // ========================== createDraft ==========================
    @Test
    @DisplayName("Create draft: Normal case - Successfully create draft resource")
    void createDraft_Normal_Success() {
        Category category = new Category();
        category.setCategoryId(TEST_CATEGORY_ID);
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(category));

        ResourceType type = new ResourceType();
        type.setResourceTypeId(TEST_TYPE_ID);
        type.setTypeName(ResourceTypeEnum.IMAGE.getValue());
        when(resourceTypeMapper.selectActiveByTypeName(any())).thenReturn(type);

        Resource res = new Resource();
        res.setId(TEST_RESOURCE_ID);
        res.setContributorId(TEST_USER_ID);
        res.setStatus(ResourceStatusEnum.DRAFT.getValue());
        when(resourceMapper.insert(any())).thenAnswer(i -> {
            Resource arg = i.getArgument(0);
            arg.setId(TEST_RESOURCE_ID);
            return 1;
        });
        when(resourceMapper.selectById(TEST_RESOURCE_ID)).thenReturn(res);

        ResourceDetailVO vo = contributorResourceService.createDraft(TEST_USER_ID);

        assertNotNull(vo);
        assertEquals(TEST_RESOURCE_ID, vo.getId());
        assertEquals(TEST_USER_ID, vo.getContributorId());
        assertEquals(ResourceStatusEnum.DRAFT.getValue(), vo.getStatus());
    }

    @Test
    @DisplayName("Create draft: Boundary - No available category, throw exception")
    void createDraft_Boundary_NoCategory_Throw() {
        when(categoryMapper.selectActiveCategories()).thenReturn(Collections.emptyList());
        AppException ex = assertThrows(AppException.class, () -> contributorResourceService.createDraft(TEST_USER_ID));
        assertEquals("Cannot create draft because no active category is available.", ex.getMessage());
    }

    @Test
    @DisplayName("Create draft: Exception - Draft created but cannot be reloaded")
    void createDraft_Exception_CannotReload_Throw() {
        Category category = new Category();
        category.setCategoryId(TEST_CATEGORY_ID);
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(category));

        ResourceType type = new ResourceType();
        type.setResourceTypeId(TEST_TYPE_ID);
        when(resourceTypeMapper.selectActiveByTypeName(any())).thenReturn(type);

        when(resourceMapper.insert(any())).thenAnswer(i -> {
            Resource arg = i.getArgument(0);
            arg.setId(TEST_RESOURCE_ID);
            return 1;
        });
        when(resourceMapper.selectById(TEST_RESOURCE_ID)).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () -> contributorResourceService.createDraft(TEST_USER_ID));
        assertEquals("Draft was created but cannot be reloaded.", ex.getMessage());
    }

    // ========================== updateResource ==========================
    @Test
    @DisplayName("Update resource: Exception - Both tagIds and tagNames provided, throw exception")
    void updateResource_Exception_BothTags_Throw() {
        Resource res = buildOwnedResource();
        when(resourceMapper.selectByIdForUpdate(TEST_RESOURCE_ID)).thenReturn(res);

        ResourceUpdateRequest req = new ResourceUpdateRequest();
        req.setTagIds(List.of(1L));
        req.setTagNames(List.of("test"));

        AppException ex = assertThrows(AppException.class,
            () -> contributorResourceService.updateResource(TEST_USER_ID, TEST_RESOURCE_ID, req));
        assertEquals("Tag ids and tag names cannot be submitted together.", ex.getMessage());
    }

    @Test
    @DisplayName("Update resource: Exception - Request is null, throw exception")
    void updateResource_Exception_RequestNull_Throw() {
        Resource res = buildOwnedResource();
        when(resourceMapper.selectByIdForUpdate(TEST_RESOURCE_ID)).thenReturn(res);

        AppException ex = assertThrows(AppException.class,
            () -> contributorResourceService.updateResource(TEST_USER_ID, TEST_RESOURCE_ID, null));
        assertEquals("Update request cannot be null.", ex.getMessage());
    }

    @Test
    @DisplayName("Update resource: Boundary - Empty update, return valid detail")
    void updateResource_Boundary_EmptyUpdate_Success() {
        Resource res = buildOwnedResource();
        when(resourceMapper.selectByIdForUpdate(TEST_RESOURCE_ID)).thenReturn(res);
        when(resourceMapper.selectById(TEST_RESOURCE_ID)).thenReturn(res);

        ResourceUpdateRequest req = new ResourceUpdateRequest();
        ResourceDetailVO vo = contributorResourceService.updateResource(TEST_USER_ID, TEST_RESOURCE_ID, req);

        assertNotNull(vo);
        assertEquals(TEST_RESOURCE_ID, vo.getId());
    }

    @Test
    @DisplayName("Update resource: Normal case - Update title and category successfully")
    void updateResource_Normal_Success() {
        Resource res = buildOwnedResource();
        when(resourceMapper.selectByIdForUpdate(TEST_RESOURCE_ID)).thenReturn(res);

        // 关键修复：给 Category 设置 ACTIVE 状态
        Category activeCategory = new Category();
        activeCategory.setStatus("ACTIVE"); // 加上这一行！
        when(categoryMapper.selectById(TEST_CATEGORY_ID)).thenReturn(activeCategory);

        when(resourceMapper.selectById(TEST_RESOURCE_ID)).thenReturn(res);

        ResourceUpdateRequest req = new ResourceUpdateRequest();
        req.setTitle("Hi");
        req.setCategoryId(TEST_CATEGORY_ID);

        ResourceDetailVO vo = contributorResourceService.updateResource(TEST_USER_ID, TEST_RESOURCE_ID, req);
        assertEquals("Hi", vo.getTitle());
        assertEquals(TEST_CATEGORY_ID, vo.getCategoryId());
    }

    // ========================== uploadFiles ==========================
    @Test
    @DisplayName("Upload files: Exception - No file uploaded, throw exception")
    void uploadFiles_Exception_NoFile_Throw() {
        Resource res = buildOwnedResource();
        when(resourceMapper.selectByIdForUpdate(TEST_RESOURCE_ID)).thenReturn(res);

        AppException ex = assertThrows(AppException.class,
            () -> contributorResourceService.uploadFiles(TEST_USER_ID, TEST_RESOURCE_ID, null, null));
        assertEquals("At least one file must be uploaded.", ex.getMessage());
    }

    @Test
    @DisplayName("Upload files: Boundary - Upload empty file, throw exception")
    void uploadFiles_Boundary_EmptyFile_Throw() {
        Resource res = buildOwnedResource();
        when(resourceMapper.selectByIdForUpdate(TEST_RESOURCE_ID)).thenReturn(res);

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
            () -> contributorResourceService.uploadFiles(TEST_USER_ID, TEST_RESOURCE_ID, file, null));
        assertEquals("At least one file must be uploaded.", ex.getMessage());
    }

    @Test
    @DisplayName("Upload files: Normal case - Upload preview image successfully")
    void uploadFiles_Normal_Image_Success() {
        Resource res = buildOwnedResource();
        when(resourceMapper.selectByIdForUpdate(TEST_RESOURCE_ID)).thenReturn(res);

        MultipartFile img = mock(MultipartFile.class);
        when(img.isEmpty()).thenReturn(false);
        when(img.getOriginalFilename()).thenReturn("a.jpg");
        when(img.getContentType()).thenReturn("image/jpeg");

        FileStorageManager.StoredFile stored = mock(FileStorageManager.StoredFile.class);
        when(stored.getFilePath()).thenReturn("p/a.jpg");
        when(fileStorageManager.storeFile(any(), any())).thenReturn(stored);
        when(resourceMapper.selectById(TEST_RESOURCE_ID)).thenReturn(res);

        ResourceDetailVO vo = contributorResourceService.uploadFiles(TEST_USER_ID, TEST_RESOURCE_ID, img, null);
        assertNotNull(vo);
        assertEquals(TEST_RESOURCE_ID, vo.getId());
    }

    // ========================== listMyResources ==========================
    @Test
    @DisplayName("List my resources: Boundary - No resources, return empty list")
    void listMyResources_Boundary_Empty_ReturnEmpty() {
        when(resourceMapper.selectMyResources(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        List<ResourceListItemVO> list = contributorResourceService.listMyResources(TEST_USER_ID, null);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("List my resources: Normal case - Query with keyword, return resource list")
    void listMyResources_Normal_WithKeyword_ReturnList() {
        Resource r = new Resource();
        r.setId(TEST_RESOURCE_ID);
        r.setTitle("Test");
        // 关键修复：设置 status 字段，避免 fromValue 传入 null
        r.setStatus(ResourceStatusEnum.DRAFT.getValue()); 

        when(resourceMapper.selectMyResources(any(), any(), any(), any())).thenReturn(List.of(r));

        ResourceQueryRequest req = new ResourceQueryRequest();
        req.setKeyword("test");

        List<ResourceListItemVO> list = contributorResourceService.listMyResources(TEST_USER_ID, req);
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals("Test", list.get(0).getTitle());
    }

    // ========================== listSubmissionHistory ==========================
    @Test
    @DisplayName("List submission history: Boundary - No records, return empty list")
    void listSubmissionHistory_Boundary_Empty_ReturnEmpty() {
        when(resourceMapper.selectById(TEST_RESOURCE_ID)).thenReturn(buildOwnedResource());
        when(resourceSubmissionMapper.selectByResourceId(TEST_RESOURCE_ID)).thenReturn(Collections.emptyList());

        List<ResourceSubmissionVO> list = contributorResourceService.listSubmissionHistory(TEST_USER_ID, TEST_RESOURCE_ID);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("List submission history: Exception - No permission, throw forbidden exception")
    void listSubmissionHistory_Exception_NotOwner_Throw() {
        Resource r = new Resource();
        r.setId(TEST_RESOURCE_ID);
        r.setContributorId(999L);
        when(resourceMapper.selectById(TEST_RESOURCE_ID)).thenReturn(r);

        AppException ex = assertThrows(AppException.class,
            () -> contributorResourceService.listSubmissionHistory(TEST_USER_ID, TEST_RESOURCE_ID));
        assertEquals("Current user does not own this resource.", ex.getMessage());
    }

    // ========================== getMyResourceDetail ==========================
    @Test
    @DisplayName("Get resource detail: Exception - Resource not found, throw exception")
    void getMyResourceDetail_Exception_NotFound_Throw() {
        when(resourceMapper.selectById(TEST_RESOURCE_ID)).thenReturn(null);
        AppException ex = assertThrows(AppException.class,
            () -> contributorResourceService.getMyResourceDetail(TEST_USER_ID, TEST_RESOURCE_ID));
        assertEquals("Resource does not exist.", ex.getMessage());
    }

    @Test
    @DisplayName("Get resource detail: Exception - No permission, throw forbidden exception")
    void getMyResourceDetail_Exception_Forbidden_Throw() {
        Resource r = new Resource();
        r.setId(TEST_RESOURCE_ID);
        r.setContributorId(999L);
        when(resourceMapper.selectById(TEST_RESOURCE_ID)).thenReturn(r);

        AppException ex = assertThrows(AppException.class,
            () -> contributorResourceService.getMyResourceDetail(TEST_USER_ID, TEST_RESOURCE_ID));
        assertEquals("Current user does not own this resource.", ex.getMessage());
    }

    @Test
    @DisplayName("Get resource detail: Normal case - Return full resource detail VO")
    void getMyResourceDetail_Normal_Success() {
        Resource r = buildOwnedResource();
        when(resourceMapper.selectById(TEST_RESOURCE_ID)).thenReturn(r);
        when(resourceTagMapper.selectTagIdsByResourceId(TEST_RESOURCE_ID)).thenReturn(Collections.emptyList());

        ResourceDetailVO vo = contributorResourceService.getMyResourceDetail(TEST_USER_ID, TEST_RESOURCE_ID);
        assertNotNull(vo);
        assertEquals(TEST_RESOURCE_ID, vo.getId());
        assertEquals(TEST_USER_ID, vo.getContributorId());
    }

    // ========================== submitResource ==========================
    @Test
    @DisplayName("Submit resource: Exception - Title is empty, throw exception")
    void submitResource_Exception_EmptyTitle_Throw() {
        Resource r = buildOwnedResource();
        r.setTitle("");
        r.setDescription("d");
        r.setCopyright("c");
        r.setCategoryId(TEST_CATEGORY_ID);
        r.setResourceType("image");
        r.setMediaUrl("m.jpg");
        when(resourceMapper.selectByIdForUpdate(TEST_RESOURCE_ID)).thenReturn(r);
    
        AppException ex = assertThrows(AppException.class,
            () -> contributorResourceService.submitResource(TEST_USER_ID, TEST_RESOURCE_ID, new ResourceSubmitRequest()));
        assertEquals("Title is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Submit resource: Boundary - First submission, status changes to pending review")
    void submitResource_Boundary_FirstSubmission_Version1() {
        Resource r = buildValidSubmissionResource();
        when(resourceMapper.selectByIdForUpdate(TEST_RESOURCE_ID)).thenReturn(r);
        
        // 修复：设置状态为 ACTIVE
        Category activeCategory = new Category();
        activeCategory.setStatus("ACTIVE");
        when(categoryMapper.selectById(TEST_CATEGORY_ID)).thenReturn(activeCategory);
        
        when(resourceSubmissionMapper.selectLatestByResourceId(TEST_RESOURCE_ID)).thenReturn(null);

        contributorResourceService.submitResource(TEST_USER_ID, TEST_RESOURCE_ID, new ResourceSubmitRequest());

        assertEquals(ResourceStatusEnum.PENDING_REVIEW.getValue(), r.getStatus());
    }

    // ========================== listCategoryOptions ==========================
    @Test
    @DisplayName("List category options: Boundary - No categories, return empty list")
    void listCategoryOptions_Boundary_Empty_ReturnEmpty() {
        when(categoryMapper.selectActiveCategories()).thenReturn(Collections.emptyList());
        List<CategoryTagOptionVO> list = contributorResourceService.listCategoryOptions();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("List category options: Exception - Category id is null, skip invalid item")
    void listCategoryOptions_Exception_NullId_Skip() {
        Category c = new Category();
        c.setCategoryId(null);
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(c));

        List<CategoryTagOptionVO> list = contributorResourceService.listCategoryOptions();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("List category options: Normal case - Return valid category options")
    void listCategoryOptions_Normal_Success() {
        Category c = new Category();
        c.setCategoryId(TEST_CATEGORY_ID);
        c.setCategoryTopic("Art");
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(c));

        List<CategoryTagOptionVO> list = contributorResourceService.listCategoryOptions();
        assertEquals(1, list.size());
        assertEquals(TEST_CATEGORY_ID, list.get(0).getId());
        assertEquals("Art", list.get(0).getName());
    }

    // ========================== listResourceTypeOptions ==========================
    @Test
    @DisplayName("List resource type options: Boundary - No types, return empty list")
    void listResourceTypeOptions_Boundary_Empty_ReturnEmpty() {
        when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(Collections.emptyList());
        List<CategoryTagOptionVO> list = contributorResourceService.listResourceTypeOptions();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("List resource type options: Exception - Type id is null, skip invalid item")
    void listResourceTypeOptions_Exception_NullId_Skip() {
        ResourceType t = new ResourceType();
        t.setResourceTypeId(null);
        when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(List.of(t));

        List<CategoryTagOptionVO> list = contributorResourceService.listResourceTypeOptions();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("List resource type options: Normal case - Return valid type options")
    void listResourceTypeOptions_Normal_Success() {
        ResourceType t = new ResourceType();
        t.setResourceTypeId(TEST_TYPE_ID);
        t.setTypeName("Video");
        when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(List.of(t));

        List<CategoryTagOptionVO> list = contributorResourceService.listResourceTypeOptions();
        assertEquals(1, list.size());
        assertEquals(TEST_TYPE_ID, list.get(0).getId());
        assertEquals("Video", list.get(0).getName());
    }

    // ========================== listTagOptions ==========================
    @Test
    @DisplayName("List tag options: Boundary - No tags, return empty list")
    void listTagOptions_Boundary_Empty_ReturnEmpty() {
        when(tagMapper.selectActiveTags()).thenReturn(Collections.emptyList());
        List<CategoryTagOptionVO> list = contributorResourceService.listTagOptions();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("List tag options: Exception - Mapper returns null, convert to empty list")
    void listTagOptions_Exception_MapperNull_ReturnEmpty() {
        when(tagMapper.selectActiveTags()).thenReturn(null);
        List<CategoryTagOptionVO> list = contributorResourceService.listTagOptions();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("List tag options: Normal case - Return valid tag options")
    void listTagOptions_Normal_Success() {
        Tag t = new Tag();
        t.setTagId(1L);
        t.setTagName("Java");
        when(tagMapper.selectActiveTags()).thenReturn(List.of(t));

        List<CategoryTagOptionVO> list = contributorResourceService.listTagOptions();
        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getId());
        assertEquals("Java", list.get(0).getName());
    }

    // ========================== Helper methods ==========================
    private Resource buildOwnedResource() {
        Resource r = new Resource();
        r.setId(TEST_RESOURCE_ID);
        r.setContributorId(TEST_USER_ID);
        r.setStatus(ResourceStatusEnum.DRAFT.getValue());
        return r;
    }

    private Resource buildValidSubmissionResource() {
        Resource r = buildOwnedResource();
        r.setTitle("Valid");
        r.setDescription("Valid");
        r.setCopyright("Valid");
        r.setCategoryId(TEST_CATEGORY_ID);
        r.setResourceType("image");
        r.setMediaUrl("m.jpg");
        r.setPreviewImage("p.jpg");
        return r;
    }
}
