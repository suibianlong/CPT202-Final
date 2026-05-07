package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.dto.resource.ResourceQueryRequest;
import com.cpt202.HerLink.dto.resource.ResourceSubmitRequest;
import com.cpt202.HerLink.dto.resource.ResourceUpdateRequest;
import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceFile;
import com.cpt202.HerLink.entity.ResourceSubmission;
import com.cpt202.HerLink.entity.ResourceTag;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.ReviewRecord;
import com.cpt202.HerLink.entity.Tag;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
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
import com.cpt202.HerLink.service.impl.ContributorResourceServiceImpl;
import com.cpt202.HerLink.service.notification.EmailNotificationService;
import com.cpt202.HerLink.util.FileStorageManager;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import com.cpt202.HerLink.vo.ResourceSubmissionVO;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributorResourceServiceImplTest {

    private static final Long USER_ID = 20L;
    private static final Long RESOURCE_ID = 100L;

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
    private ContributorResourceServiceImpl service;

    @Test
    @DisplayName("Create draft succeeds with first active category and photo resource type")
    void createDraft_activeDefaults_returnsDraftDetail() {
        AtomicReference<Resource> inserted = new AtomicReference<>();
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(category(1L, "Places", "ACTIVE")));
        when(resourceTypeMapper.selectActiveByTypeName("photo")).thenReturn(type(2L, "photo"));
        when(resourceMapper.insert(any(Resource.class))).thenAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            resource.setId(RESOURCE_ID);
            inserted.set(resource);
            return 1;
        });
        when(resourceMapper.selectById(RESOURCE_ID)).thenAnswer(invocation -> {
            Resource resource = inserted.get();
            resource.setCategoryName("Places");
            return resource;
        });
        stubDetailLookups(RESOURCE_ID, 1);

        ResourceDetailVO result = service.createDraft(USER_ID);

        assertAll(
                () -> assertEquals(RESOURCE_ID, result.getId()),
                () -> assertEquals(USER_ID, result.getContributorId()),
                () -> assertEquals(ResourceStatusEnum.DRAFT.getValue(), result.getStatus()),
                () -> assertEquals(1L, result.getCategoryId()),
                () -> assertEquals("places", result.getCategoryName()),
                () -> assertEquals("photo", result.getResourceType()),
                () -> assertEquals("", result.getTitle()),
                () -> assertNotNull(inserted.get().getCreatedAt()),
                () -> assertNotNull(inserted.get().getUpdatedAt())
        );
    }

    @Test
    @DisplayName("Create draft rejects missing active category")
    void createDraft_noActiveCategory_throwsConflict() {
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of());

        AppException exception = assertThrows(AppException.class, () -> service.createDraft(USER_ID));

        assertEquals("Cannot create draft because no active category is available.", exception.getMessage());
    }

    @Test
    @DisplayName("Create draft rejects invalid default category without id")
    void createDraft_defaultCategoryWithoutId_throwsConflict() {
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(category(null, "Invalid", "ACTIVE")));

        AppException exception = assertThrows(AppException.class, () -> service.createDraft(USER_ID));

        assertEquals("Cannot create draft because the default category is invalid.", exception.getMessage());
    }

    @Test
    @DisplayName("Create draft rejects missing active resource type")
    void createDraft_noResourceType_throwsConflict() {
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(category(1L, "Places", "ACTIVE")));
        when(resourceTypeMapper.selectActiveByTypeName("photo")).thenReturn(null);
        when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(List.of());

        AppException exception = assertThrows(AppException.class, () -> service.createDraft(USER_ID));

        assertEquals("Cannot create draft because no active resource type is available.", exception.getMessage());
    }

    @Test
    @DisplayName("Create draft falls back to first valid active resource type when photo type missing")
    void createDraft_photoTypeMissing_usesFirstValidFallbackType() {
        AtomicReference<Resource> inserted = new AtomicReference<>();
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(category(1L, "Places", "ACTIVE")));
        when(resourceTypeMapper.selectActiveByTypeName("photo")).thenReturn(null);
        when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(List.of(
                type(null, "invalid"),
                type(8L, "video")
        ));
        when(resourceMapper.insert(any(Resource.class))).thenAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            resource.setId(RESOURCE_ID);
            inserted.set(resource);
            return 1;
        });
        when(resourceMapper.selectById(RESOURCE_ID)).thenAnswer(invocation -> {
            Resource resource = inserted.get();
            resource.setCategoryName("Places");
            return resource;
        });
        stubDetailLookups(RESOURCE_ID, 1);

        ResourceDetailVO result = service.createDraft(USER_ID);

        assertAll(
                () -> assertEquals(RESOURCE_ID, result.getId()),
                () -> assertEquals("video", result.getResourceType()),
                () -> assertEquals(8L, inserted.get().getResourceTypeId())
        );
    }

    @Test
    @DisplayName("Get resource detail rejects missing resource")
    void getMyResourceDetail_missingResource_throwsNotFound() {
        when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(null);

        AppException exception = assertThrows(AppException.class,
                () -> service.getMyResourceDetail(USER_ID, RESOURCE_ID));

        assertEquals("Resource does not exist.", exception.getMessage());
    }

    @Test
    @DisplayName("Get resource detail rejects resource owned by another contributor")
    void getMyResourceDetail_otherOwner_throwsForbidden() {
        when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource(RESOURCE_ID, 999L, ResourceStatusEnum.DRAFT.getValue()));

        AppException exception = assertThrows(AppException.class,
                () -> service.getMyResourceDetail(USER_ID, RESOURCE_ID));

        assertEquals("Current user does not own this resource.", exception.getMessage());
    }

    @Test
    @DisplayName("Update resource rejects null request")
    void updateResource_nullRequest_throwsBadRequest() {
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue()));

        AppException exception = assertThrows(AppException.class,
                () -> service.updateResource(USER_ID, RESOURCE_ID, null));

        assertEquals("Update request cannot be null.", exception.getMessage());
    }

    @Test
    @DisplayName("Update resource rejects simultaneous tag ids and names")
    void updateResource_bothTagIdsAndNames_throwsBadRequest() {
        ResourceUpdateRequest request = new ResourceUpdateRequest();
        request.setTagIds(List.of(1L));
        request.setTagNames(List.of("Festival"));
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue()));

        AppException exception = assertThrows(AppException.class,
                () -> service.updateResource(USER_ID, RESOURCE_ID, request));

        assertEquals("Tag ids and tag names cannot be submitted together.", exception.getMessage());
    }

    @Test
    @DisplayName("Update resource rejects inactive category")
    void updateResource_inactiveCategory_throwsConflict() {
        ResourceUpdateRequest request = new ResourceUpdateRequest();
        request.setCategoryId(2L);
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue()));
        when(categoryMapper.selectById(2L)).thenReturn(category(2L, "Stories", "INACTIVE"));

        AppException exception = assertThrows(AppException.class,
                () -> service.updateResource(USER_ID, RESOURCE_ID, request));

        assertEquals("Selected category is inactive.", exception.getMessage());
    }

    @Test
    @DisplayName("Update resource normalizes metadata and de-duplicates tag ids")
    void updateResource_validMetadata_returnsUpdatedDetail() {
        AtomicReference<Resource> updated = new AtomicReference<>();
        List<ResourceTag> insertedTags = new ArrayList<>();
        Resource existing = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        existing.setCategoryId(1L);
        existing.setResourceTypeId(1L);
        existing.setResourceType("photo");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(existing);
        when(categoryMapper.selectById(2L)).thenReturn(category(2L, "Stories", "ACTIVE"));
        when(resourceTypeMapper.selectActiveByTypeName("video")).thenReturn(type(3L, "video"));
        when(resourceTagMapper.selectTagIdsByResourceId(RESOURCE_ID)).thenReturn(List.of(4L));
        when(tagMapper.selectByIds(List.of(5L, 6L))).thenReturn(List.of(tag(5L, "Temple", "ACTIVE"), tag(6L, "Festival", "ACTIVE")));
        when(resourceTagMapper.insert(any(ResourceTag.class))).thenAnswer(invocation -> {
            insertedTags.add(invocation.getArgument(0));
            return 1;
        });
        when(resourceMapper.updateById(any(Resource.class))).thenAnswer(invocation -> {
            updated.set(invocation.getArgument(0));
            return 1;
        });
        when(resourceMapper.selectById(RESOURCE_ID)).thenAnswer(invocation -> {
            Resource latest = updated.get();
            latest.setCategoryName("Stories");
            return latest;
        });
        stubDetailLookups(RESOURCE_ID, 3);
        when(resourceTagMapper.selectTagIdsByResourceId(RESOURCE_ID)).thenReturn(List.of(4L), List.of(5L, 6L));
        when(resourceTagMapper.selectTagNamesByResourceId(RESOURCE_ID)).thenReturn(List.of("Temple", "Festival"));

        ResourceUpdateRequest request = new ResourceUpdateRequest();
        request.setTitle("New title");
        request.setDescription("New description");
        request.setCopyright("Museum rights");
        request.setPlace("Suzhou");
        request.setCategoryId(2L);
        request.setResourceType("Video");
        request.setTagIds(Arrays.asList(5L, 5L, null, 6L));

        ResourceDetailVO result = service.updateResource(USER_ID, RESOURCE_ID, request);

        assertAll(
                () -> assertEquals("New title", result.getTitle()),
                () -> assertEquals("New description", result.getDescription()),
                () -> assertEquals("Museum rights", result.getCopyright()),
                () -> assertEquals("Suzhou", result.getPlace()),
                () -> assertEquals(2L, result.getCategoryId()),
                () -> assertEquals("stories", result.getCategoryName()),
                () -> assertEquals("video", result.getResourceType()),
                () -> assertEquals(2, insertedTags.size()),
                () -> assertEquals(5L, insertedTags.get(0).getTagId()),
                () -> assertEquals(6L, insertedTags.get(1).getTagId())
        );
    }

    @Test
    @DisplayName("Update resource normalizes comma-split tag names and removes case-insensitive duplicates")
    void updateResource_tagNamesNormalizedAndDeduplicated_returnsUpdatedDetail() {
        AtomicReference<Resource> updated = new AtomicReference<>();
        List<ResourceTag> insertedTags = new ArrayList<>();
        Resource existing = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        existing.setCategoryId(1L);
        existing.setResourceTypeId(1L);
        existing.setResourceType("photo");

        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(existing);
        when(resourceTagMapper.selectTagIdsByResourceId(RESOURCE_ID)).thenReturn(List.of(11L), List.of(5L, 6L));
        when(tagMapper.selectByNameIgnoreCase("Temple")).thenReturn(tag(5L, "Temple", "ACTIVE"));
        when(tagMapper.selectByNameIgnoreCase("Festival")).thenReturn(null);
        when(tagMapper.insert(any(Tag.class))).thenAnswer(invocation -> {
            Tag newTag = invocation.getArgument(0);
            newTag.setTagId(6L);
            return 1;
        });
        when(tagMapper.selectByIds(List.of(5L, 6L))).thenReturn(List.of(
                tag(5L, "Temple", "ACTIVE"),
                tag(6L, "Festival", "ACTIVE")
        ));
        when(resourceTagMapper.insert(any(ResourceTag.class))).thenAnswer(invocation -> {
            insertedTags.add(invocation.getArgument(0));
            return 1;
        });
        when(resourceMapper.updateById(any(Resource.class))).thenAnswer(invocation -> {
            updated.set(invocation.getArgument(0));
            return 1;
        });
        when(resourceMapper.selectById(RESOURCE_ID)).thenAnswer(invocation -> {
            Resource latest = updated.get();
            latest.setCategoryName("Places");
            return latest;
        });
        stubDetailLookups(RESOURCE_ID, 5);
        when(resourceTagMapper.selectTagIdsByResourceId(RESOURCE_ID)).thenReturn(List.of(11L), List.of(5L, 6L));
        when(resourceTagMapper.selectTagNamesByResourceId(RESOURCE_ID)).thenReturn(List.of("Temple", "Festival"));

        ResourceUpdateRequest request = new ResourceUpdateRequest();
        request.setTagNames(Arrays.asList(" Temple , Festival ", "temple", null, "  "));

        ResourceDetailVO result = service.updateResource(USER_ID, RESOURCE_ID, request);

        assertAll(
                () -> assertEquals(2, insertedTags.size()),
                () -> assertEquals(5L, insertedTags.get(0).getTagId()),
                () -> assertEquals(6L, insertedTags.get(1).getTagId()),
                () -> assertEquals(List.of("Temple", "Festival"), result.getTagNames())
        );
    }

    @Test
    @DisplayName("Update resource rejects tag name exceeding max length")
    void updateResource_tagNameTooLong_throwsBadRequest() {
        Resource existing = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(existing);

        ResourceUpdateRequest request = new ResourceUpdateRequest();
        request.setTagNames(List.of("a".repeat(101)));

        AppException exception = assertThrows(AppException.class,
                () -> service.updateResource(USER_ID, RESOURCE_ID, request));

        assertEquals("Tag name cannot exceed 100 characters.", exception.getMessage());
    }

    @Test
    @DisplayName("Update resource rejects existing inactive tag when using tag names")
    void updateResource_inactiveTagName_throwsConflict() {
        Resource existing = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(existing);
        when(tagMapper.selectByNameIgnoreCase("Festival")).thenReturn(tag(99L, "Festival", "INACTIVE"));

        ResourceUpdateRequest request = new ResourceUpdateRequest();
        request.setTagNames(List.of("Festival"));

        AppException exception = assertThrows(AppException.class,
                () -> service.updateResource(USER_ID, RESOURCE_ID, request));

        assertEquals("Tag \"Festival\" exists but is inactive.", exception.getMessage());
    }

    @Test
    @DisplayName("Upload files rejects request without files")
    void uploadFiles_noFiles_throwsBadRequest() {
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue()));

        AppException exception = assertThrows(AppException.class,
                () -> service.uploadFiles(USER_ID, RESOURCE_ID, null, null));

        assertEquals("At least one file must be uploaded.", exception.getMessage());
    }

    @Test
    @DisplayName("Upload files rejects unsupported preview image")
    void uploadFiles_invalidPreview_throwsBadRequest() {
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue()));
        MockMultipartFile preview = new MockMultipartFile("previewImage", "preview.pdf", "application/pdf", "pdf".getBytes());

        AppException exception = assertThrows(AppException.class,
                () -> service.uploadFiles(USER_ID, RESOURCE_ID, preview, null));

        assertEquals("Preview image must be a JPG, JPEG, PNG, or GIF file.", exception.getMessage());
    }

    @Test
    @DisplayName("Upload preview image stores file and returns refreshed detail")
    void uploadFiles_validPreview_returnsUpdatedDetail() {
        AtomicReference<Resource> updated = new AtomicReference<>();
        AtomicReference<ResourceFile> insertedFile = new AtomicReference<>();
        Resource existing = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        existing.setResourceType("photo");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(existing);
        when(fileStorageManager.storeFile(any(), eq("resource-100"))).thenReturn(storedFile("preview.jpg", "resource-100/preview.jpg", "jpg", 9L));
        when(resourceFileMapper.insert(any(ResourceFile.class))).thenAnswer(invocation -> {
            insertedFile.set(invocation.getArgument(0));
            return 1;
        });
        when(resourceMapper.updateById(any(Resource.class))).thenAnswer(invocation -> {
            updated.set(invocation.getArgument(0));
            return 1;
        });
        when(resourceMapper.selectById(RESOURCE_ID)).thenAnswer(invocation -> updated.get());
        stubDetailLookups(RESOURCE_ID, 2);
        MockMultipartFile preview = new MockMultipartFile("previewImage", "preview.jpg", "image/jpeg", "image".getBytes());

        ResourceDetailVO result = service.uploadFiles(USER_ID, RESOURCE_ID, preview, null);

        assertAll(
                () -> assertEquals("resource-100/preview.jpg", result.getPreviewImage()),
                () -> assertEquals("resource-100/preview.jpg", updated.get().getPreviewImage()),
                () -> assertEquals("preview.jpg", insertedFile.get().getOriginalFilename()),
                () -> assertEquals("jpg", insertedFile.get().getFileType()),
                () -> assertEquals(RESOURCE_ID, insertedFile.get().getResourceId())
        );
    }

    @Test
    @DisplayName("Upload files rejects media file not matching selected resource type")
    void uploadFiles_mediaTypeMismatch_throwsBadRequest() {
        Resource existing = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        existing.setResourceType("photo");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(existing);
        MockMultipartFile media = new MockMultipartFile("mediaFile", "movie.mp4", "video/mp4", "video".getBytes());

        AppException exception = assertThrows(AppException.class,
                () -> service.uploadFiles(USER_ID, RESOURCE_ID, null, media));

        assertTrue(exception.getMessage().contains("Media file must match the selected resource type."));
    }

    @Test
    @DisplayName("Submit resource rejects missing title boundary")
    void submitResource_missingTitle_throwsBadRequest() {
        Resource draft = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        draft.setTitle(" ");
        draft.setDescription("Description");
        draft.setCopyright("Rights");
        draft.setCategoryId(1L);
        draft.setResourceType("photo");
        draft.setMediaUrl("resource-100/photo.jpg");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertEquals("Title is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Submit resource rejects missing description")
    void submitResource_missingDescription_throwsBadRequest() {
        Resource draft = submittableDraft();
        draft.setDescription(" ");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertEquals("Description is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Submit resource rejects missing copyright")
    void submitResource_missingCopyright_throwsBadRequest() {
        Resource draft = submittableDraft();
        draft.setCopyright(" ");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertEquals("Copyright is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Submit resource rejects missing category")
    void submitResource_missingCategory_throwsBadRequest() {
        Resource draft = submittableDraft();
        draft.setCategoryId(null);
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertEquals("Category is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Submit resource rejects non-existing category")
    void submitResource_categoryNotFound_throwsBadRequest() {
        Resource draft = submittableDraft();
        draft.setCategoryId(99L);
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);
        when(categoryMapper.selectById(99L)).thenReturn(null);

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertEquals("Selected category does not exist.", exception.getMessage());
    }

    @Test
    @DisplayName("Submit resource rejects inactive category")
    void submitResource_categoryInactive_throwsConflict() {
        Resource draft = submittableDraft();
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "Places", "INACTIVE"));

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertEquals("Selected category is inactive.", exception.getMessage());
    }

    @Test
    @DisplayName("Submit resource rejects missing resource type")
    void submitResource_missingResourceType_throwsBadRequest() {
        Resource draft = submittableDraft();
        draft.setResourceType(" ");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "Places", "ACTIVE"));

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertEquals("Resource type is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Submit resource rejects missing media url")
    void submitResource_missingMediaUrl_throwsBadRequest() {
        Resource draft = submittableDraft();
        draft.setMediaUrl(" ");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "Places", "ACTIVE"));

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertEquals("Media file is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Submit resource rejects stored preview image with non-image type")
    void submitResource_storedPreviewNotImage_throwsBadRequest() {
        Resource draft = submittableDraft();
        draft.setPreviewImage("resource-100/preview.pdf");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "Places", "ACTIVE"));
        when(resourceFileMapper.selectByResourceIdAndFilePath(RESOURCE_ID, "resource-100/preview.pdf"))
                .thenReturn(resourceFileWithPath("resource-100/preview.pdf", "pdf"));

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertEquals("Preview image must be an image file.", exception.getMessage());
    }

    @Test
    @DisplayName("Submit resource rejects stored media not matching known resource type")
    void submitResource_storedMediaTypeMismatchForKnownType_throwsBadRequest() {
        Resource draft = submittableDraft();
        draft.setMediaUrl("resource-100/file.pdf");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "Places", "ACTIVE"));
        when(resourceFileMapper.selectByResourceIdAndFilePath(RESOURCE_ID, "resource-100/file.pdf"))
                .thenReturn(resourceFileWithPath("resource-100/file.pdf", "pdf"));

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertTrue(exception.getMessage().contains("Media file must match the selected resource type."));
    }

    @Test
    @DisplayName("Submit resource creates next submission version and changes status")
    void submitResource_validDraft_updatesStatusAndSubmission() {
        AtomicReference<ResourceSubmission> insertedSubmission = new AtomicReference<>();
        AtomicReference<Resource> updatedResource = new AtomicReference<>();
        Resource draft = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        draft.setTitle("Title");
        draft.setDescription("Description");
        draft.setCopyright("Rights");
        draft.setCategoryId(1L);
        draft.setResourceType("photo");
        draft.setMediaUrl("resource-100/photo.jpg");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "Places", "ACTIVE"));
        when(resourceFileMapper.selectByResourceIdAndFilePath(RESOURCE_ID, "resource-100/photo.jpg")).thenReturn(resourceFile("jpg"));
        ResourceSubmission latest = new ResourceSubmission();
        latest.setVersionNo(2);
        when(resourceSubmissionMapper.selectLatestByResourceId(RESOURCE_ID)).thenReturn(latest);
        when(resourceSubmissionMapper.insert(any(ResourceSubmission.class))).thenAnswer(invocation -> {
            insertedSubmission.set(invocation.getArgument(0));
            return 1;
        });
        when(resourceMapper.updateById(any(Resource.class))).thenAnswer(invocation -> {
            updatedResource.set(invocation.getArgument(0));
            return 1;
        });
        ResourceSubmitRequest request = new ResourceSubmitRequest();
        request.setSubmissionNote("Ready");

        service.submitResource(USER_ID, RESOURCE_ID, request);

        assertAll(
                () -> assertEquals(RESOURCE_ID, insertedSubmission.get().getResourceId()),
                () -> assertEquals(3, insertedSubmission.get().getVersionNo()),
                () -> assertEquals(USER_ID, insertedSubmission.get().getSubmittedBy()),
                () -> assertEquals("Ready", insertedSubmission.get().getSubmissionNote()),
                () -> assertEquals(ResourceStatusEnum.PENDING_REVIEW.getValue(), insertedSubmission.get().getStatusSnapshot()),
                () -> assertEquals(ResourceStatusEnum.PENDING_REVIEW.getValue(), updatedResource.get().getStatus()),
                () -> assertNotNull(updatedResource.get().getUpdatedAt())
        );
    }

    @Test
    @DisplayName("Submit resource rejects unsupported stored media type for unknown resource type")
    void submitResource_unknownResourceTypeWithUnsupportedMedia_throwsBadRequest() {
        Resource draft = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        draft.setTitle("Title");
        draft.setDescription("Description");
        draft.setCopyright("Rights");
        draft.setCategoryId(1L);
        draft.setResourceType("custom-type");
        draft.setMediaUrl("resource-100/file.exe");
        when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(draft);
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "Places", "ACTIVE"));
        when(resourceFileMapper.selectByResourceIdAndFilePath(RESOURCE_ID, "resource-100/file.exe"))
                .thenReturn(resourceFile("exe"));

        AppException exception = assertThrows(AppException.class,
                () -> service.submitResource(USER_ID, RESOURCE_ID, null));

        assertTrue(exception.getMessage().contains("Media file type is not supported."));
    }

    @Test
    @DisplayName("List my resources maps status filter and review feedback flag")
    void listMyResources_validFilter_returnsMappedItems() {
        ResourceQueryRequest request = new ResourceQueryRequest();
        request.setKeyword("wall");
        request.setStatus("approved");
        request.setCategoryId(1L);
        Resource resource = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.APPROVED.getValue());
        resource.setTitle("Wall");
        resource.setCategoryName("Education");
        resource.setResourceType("Picture");
        when(resourceMapper.selectMyResources(USER_ID, "wall", ResourceStatusEnum.APPROVED.getValue(), 1L))
                .thenReturn(List.of(resource));
        when(resourceVersionMapper.selectMaxVersionNoByResourceId(RESOURCE_ID)).thenReturn(4);
        ResourceSubmission submission = new ResourceSubmission();
        submission.setSubmittedAt(LocalDateTime.now().minusHours(1));
        when(resourceSubmissionMapper.selectLatestByResourceId(RESOURCE_ID)).thenReturn(submission);
        ReviewRecord reviewRecord = new ReviewRecord();
        reviewRecord.setFeedbackComment("Please revise");
        when(reviewRecordMapper.selectLatestByResourceId(RESOURCE_ID)).thenReturn(reviewRecord);

        List<ResourceListItemVO> result = service.listMyResources(USER_ID, request);

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals("Wall", result.get(0).getTitle()),
                () -> assertEquals("Approved", result.get(0).getStatus()),
                () -> assertEquals("photo", result.get(0).getResourceType()),
                () -> assertEquals("educational materials", result.get(0).getCategoryName()),
                () -> assertEquals(4, result.get(0).getCurrentVersionNo()),
                () -> assertTrue(result.get(0).getHasReviewFeedback())
        );
    }

    @Test
    @DisplayName("List category options skips null and invalid active categories")
    void listCategoryOptions_mixedRows_returnsValidOptionsOnly() {
        when(categoryMapper.selectActiveCategories()).thenReturn(Arrays.asList(
                null,
                category(null, "Invalid", "ACTIVE"),
                category(1L, "Places", "ACTIVE")));

        List<CategoryTagOptionVO> result = service.listCategoryOptions();

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(1L, result.get(0).getId()),
                () -> assertEquals("Places", result.get(0).getName())
        );
    }

    @Test
    @DisplayName("List resource type options skips null and invalid rows")
    void listResourceTypeOptions_mixedRows_returnsValidOptionsOnly() {
        when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(Arrays.asList(
                null,
                type(null, "invalid"),
                type(9L, "photo")
        ));

        List<CategoryTagOptionVO> result = service.listResourceTypeOptions();

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(9L, result.get(0).getId()),
                () -> assertEquals("photo", result.get(0).getName())
        );
    }

    @Test
    @DisplayName("List resource type options returns empty when mapper returns null")
    void listResourceTypeOptions_mapperReturnsNull_returnsEmptyList() {
        when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(null);

        List<CategoryTagOptionVO> result = service.listResourceTypeOptions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("List tag options returns empty when mapper returns null")
    void listTagOptions_mapperReturnsNull_returnsEmptyList() {
        when(tagMapper.selectActiveTags()).thenReturn(null);

        List<CategoryTagOptionVO> result = service.listTagOptions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("List submission history returns empty list when mapper returns null")
    void listSubmissionHistory_mapperReturnsNull_returnsEmptyList() {
        when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue()));
        when(resourceSubmissionMapper.selectByResourceId(RESOURCE_ID)).thenReturn(null);

        List<ResourceSubmissionVO> result = service.listSubmissionHistory(USER_ID, RESOURCE_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("List submission history maps resource submission fields")
    void listSubmissionHistory_rowsExist_returnsMappedRows() {
        when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue()));
        ResourceSubmission submission = new ResourceSubmission();
        submission.setSubmissionId(1L);
        submission.setResourceId(RESOURCE_ID);
        submission.setVersionNo(2);
        submission.setSubmittedBy(USER_ID);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setSubmissionNote("Note");
        submission.setStatusSnapshot(ResourceStatusEnum.PENDING_REVIEW.getValue());
        submission.setCreatedAt(LocalDateTime.now());
        when(resourceSubmissionMapper.selectByResourceId(RESOURCE_ID)).thenReturn(List.of(submission));

        List<ResourceSubmissionVO> result = service.listSubmissionHistory(USER_ID, RESOURCE_ID);

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(1L, result.get(0).getSubmissionId()),
                () -> assertEquals(2, result.get(0).getVersionNo()),
                () -> assertEquals("Note", result.get(0).getSubmissionNote()),
                () -> assertEquals(ResourceStatusEnum.PENDING_REVIEW.getValue(), result.get(0).getStatusSnapshot())
        );
    }

    @Test
    @DisplayName("Resolve active resource type rejects blank value")
    void resolveActiveResourceType_blank_throwsBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> invokeResolveActiveResourceType("  "));

        assertEquals("Resource type is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Resolve active resource type uses normalized lookup aliases")
    void resolveActiveResourceType_lookupAlias_returnsMatchedType() {
        when(resourceTypeMapper.selectActiveByTypeName("photo")).thenReturn(type(null, "photo"));
        when(resourceTypeMapper.selectActiveByTypeName("Picture")).thenReturn(type(77L, "photo"));

        ResourceType resolved = invokeResolveActiveResourceType("PHOTO_IMAGE");

        assertAll(
                () -> assertNotNull(resolved),
                () -> assertEquals(77L, resolved.getResourceTypeId()),
                () -> assertEquals("photo", resolved.getTypeName())
        );
    }

    @Test
    @DisplayName("Resolve active resource type uses direct match for custom type")
    void resolveActiveResourceType_customDirectMatch_returnsType() {
        when(resourceTypeMapper.selectActiveByTypeName("customType")).thenReturn(type(66L, "customType"));

        ResourceType resolved = invokeResolveActiveResourceType(" customType ");

        assertEquals(66L, resolved.getResourceTypeId());
    }

    @Test
    @DisplayName("Resolve active resource type throws conflict when no active match")
    void resolveActiveResourceType_noMatch_throwsConflict() {
        when(resourceTypeMapper.selectActiveByTypeName("document")).thenReturn(type(null, "document"));

        AppException exception = assertThrows(AppException.class,
                () -> invokeResolveActiveResourceType("document"));

        assertEquals("Selected resource type is unavailable.", exception.getMessage());
    }

    @Test
    @DisplayName("isCurrentResourceFile checks preview and media path correctly")
    void isCurrentResourceFile_coversAllDecisionPaths() {
        Resource resource = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        resource.setPreviewImage("resource-100/preview.jpg");
        resource.setMediaUrl("resource-100/media.mp4");

        assertAll(
                () -> assertFalse(invokeIsCurrentResourceFile("resource-100/preview.jpg", null)),
                () -> assertFalse(invokeIsCurrentResourceFile(null, resource)),
                () -> assertTrue(invokeIsCurrentResourceFile("resource-100/preview.jpg", resource)),
                () -> assertTrue(invokeIsCurrentResourceFile("resource-100/media.mp4", resource)),
                () -> assertFalse(invokeIsCurrentResourceFile("resource-100/other.bin", resource))
        );
    }

    private void stubDetailLookups(Long resourceId, int versionNo) {
        when(resourceVersionMapper.selectMaxVersionNoByResourceId(resourceId)).thenReturn(versionNo);
        when(resourceTagMapper.selectTagIdsByResourceId(resourceId)).thenReturn(List.of());
        when(resourceTagMapper.selectTagNamesByResourceId(resourceId)).thenReturn(List.of());
        when(resourceSubmissionMapper.selectLatestByResourceId(resourceId)).thenReturn(null);
        when(reviewRecordMapper.selectLatestByResourceId(resourceId)).thenReturn(null);
    }

    private Resource resource(Long id, Long contributorId, String status) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setContributorId(contributorId);
        resource.setStatus(status);
        resource.setCategoryId(1L);
        resource.setCategoryName("Places");
        resource.setResourceType("photo");
        return resource;
    }

    private Category category(Long id, String name, String status) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryTopic(name);
        category.setStatus(status);
        return category;
    }

    private ResourceType type(Long id, String name) {
        ResourceType type = new ResourceType();
        type.setResourceTypeId(id);
        type.setTypeName(name);
        type.setStatus("ACTIVE");
        return type;
    }

    private Tag tag(Long id, String name, String status) {
        Tag tag = new Tag();
        tag.setTagId(id);
        tag.setTagName(name);
        tag.setStatus(status);
        return tag;
    }

    private FileStorageManager.StoredFile storedFile(String originalFilename, String path, String type, Long size) {
        FileStorageManager.StoredFile storedFile = new FileStorageManager.StoredFile();
        storedFile.setOriginalFilename(originalFilename);
        storedFile.setStoredFilename(path.substring(path.lastIndexOf('/') + 1));
        storedFile.setFilePath(path);
        storedFile.setFileType(type);
        storedFile.setFileSize(size);
        return storedFile;
    }

    private ResourceFile resourceFile(String type) {
        ResourceFile file = new ResourceFile();
        file.setResourceId(RESOURCE_ID);
        file.setFilePath("resource-100/photo.jpg");
        file.setFileType(type);
        return file;
    }

    private ResourceFile resourceFileWithPath(String path, String type) {
        ResourceFile file = new ResourceFile();
        file.setResourceId(RESOURCE_ID);
        file.setFilePath(path);
        file.setFileType(type);
        return file;
    }

    private Resource submittableDraft() {
        Resource draft = resource(RESOURCE_ID, USER_ID, ResourceStatusEnum.DRAFT.getValue());
        draft.setTitle("Title");
        draft.setDescription("Description");
        draft.setCopyright("Rights");
        draft.setCategoryId(1L);
        draft.setResourceType("photo");
        draft.setMediaUrl("resource-100/photo.jpg");
        return draft;
    }

    private ResourceType invokeResolveActiveResourceType(String resourceType) {
        return (ResourceType) invokePrivateMethod(
                "resolveActiveResourceType",
                new Class<?>[]{String.class},
                resourceType
        );
    }

    private boolean invokeIsCurrentResourceFile(String filePath, Resource resource) {
        return (Boolean) invokePrivateMethod(
                "isCurrentResourceFile",
                new Class<?>[]{String.class, Resource.class},
                filePath,
                resource
        );
    }

    private Object invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = ContributorResourceServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(service, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AssertionError("Private method invocation failed: " + methodName, cause);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to invoke private method: " + methodName, exception);
        }
    }
}
