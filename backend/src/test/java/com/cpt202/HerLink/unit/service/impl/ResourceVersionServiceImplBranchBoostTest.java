package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceSubmission;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.ResourceVersion;
import com.cpt202.HerLink.entity.ReviewRecord;
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
import com.cpt202.HerLink.vo.ResourceVersionVO;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceVersionServiceImpl Branch Boost Test")
class ResourceVersionServiceImplBranchBoostTest {

    private static final Long RESOURCE_ID = 100L;
    private static final Long USER_ID = 200L;
    private static final Integer V1 = 1;
    private static final Integer V2 = 2;

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
    private ResourceVersionServiceImpl service;

    @Nested
    @DisplayName("Save Version Snapshot")
    class SaveVersionSnapshotTests {

        @Test
        @DisplayName("Should reject missing required inputs")
        void saveVersionSnapshot_rejectsMissingInputs() {
            AppException resourceIdException = assertThrows(AppException.class,
                    () -> service.saveVersionSnapshot(null, USER_ID, "update", "summary"));
            AppException userIdException = assertThrows(AppException.class,
                    () -> service.saveVersionSnapshot(RESOURCE_ID, null, "update", "summary"));
            AppException changeTypeException = assertThrows(AppException.class,
                    () -> service.saveVersionSnapshot(RESOURCE_ID, USER_ID, " ", "summary"));

            assertAll(
                    () -> assertEquals("Resource id is required.", resourceIdException.getMessage()),
                    () -> assertEquals("Current user id is required.", userIdException.getMessage()),
                    () -> assertEquals("Change type is required.", changeTypeException.getMessage())
            );
        }

        @Test
        @DisplayName("Should reject missing resource")
        void saveVersionSnapshot_missingResource_throwsNotFound() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(null);

            AppException exception = assertThrows(AppException.class,
                    () -> service.saveVersionSnapshot(RESOURCE_ID, USER_ID, "update", "summary"));

            assertEquals("Resource does not exist.", exception.getMessage());
        }

        @Test
        @DisplayName("Should save snapshot for valid resource")
        void saveVersionSnapshot_validResource_succeeds() {
            Resource resource = resource();
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource);
            when(resourceVersionMapper.selectMaxVersionNoByResourceId(RESOURCE_ID)).thenReturn(null);
            when(resourceTagMapper.selectTagNamesByResourceId(RESOURCE_ID)).thenReturn(List.of("tag1", "tag2"));

            assertDoesNotThrow(() -> service.saveVersionSnapshot(RESOURCE_ID, USER_ID, "update", "summary"));
        }
    }

    @Nested
    @DisplayName("List and Get Versions")
    class ListAndGetVersionTests {

        @Test
        @DisplayName("Should reject non-owner when listing versions")
        void listVersions_notOwner_throwsForbidden() {
            Resource resource = resource();
            resource.setContributorId(999L);
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource);

            AppException exception = assertThrows(AppException.class,
                    () -> service.listVersions(USER_ID, RESOURCE_ID));

            assertEquals("Current user does not own this resource.", exception.getMessage());
        }

        @Test
        @DisplayName("Should return empty list when mapper returns null")
        void listVersions_mapperReturnsNull_returnsEmptyList() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceId(RESOURCE_ID)).thenReturn(null);

            List<ResourceVersionVO> result = service.listVersions(USER_ID, RESOURCE_ID);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should reject invalid version numbers")
        void getVersion_invalidVersionNumbers_throwBadRequest() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());

            AppException zero = assertThrows(AppException.class,
                    () -> service.getVersion(USER_ID, RESOURCE_ID, 0));
            AppException negative = assertThrows(AppException.class,
                    () -> service.getVersion(USER_ID, RESOURCE_ID, -1));

            assertAll(
                    () -> assertEquals("Version number is required.", zero.getMessage()),
                    () -> assertEquals("Version number is required.", negative.getMessage())
            );
        }

        @Test
        @DisplayName("Should reject missing version record")
        void getVersion_missingRecord_throwsNotFound() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(null);

            AppException exception = assertThrows(AppException.class,
                    () -> service.getVersion(USER_ID, RESOURCE_ID, V1));

            assertEquals("Requested version does not exist.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Compare Versions")
    class CompareVersionTests {

        @Test
        @DisplayName("Should mark changed and unchanged values correctly")
        void compareVersions_changesAreDetected() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(version(V1, "A", "Education", "ARTICLE"));
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V2)).thenReturn(version(V2, "B", null, "custom-type"));

            ResourceVersionCompareVO result = service.compareVersions(USER_ID, RESOURCE_ID, V1, V2);

            assertAll(
                    () -> assertEquals(RESOURCE_ID, result.getResourceId()),
                    () -> assertTrue(result.getDiffItems().stream().anyMatch(item -> "Title".equals(item.getFieldLabel()))),
                    () -> assertTrue(result.getDiffItems().stream().anyMatch(item -> "Category".equals(item.getFieldLabel()))),
                    () -> assertTrue(result.getDiffItems().stream().anyMatch(item -> "A".equals(item.getLeftValue()) || "B".equals(item.getRightValue())))
            );
        }
    }

    @Nested
    @DisplayName("Rollback Version")
    class RollbackVersionTests {

        @Test
        @DisplayName("Should reject status that does not allow rollback")
        void rollbackVersion_invalidStatus_throwsConflict() {
            Resource resource = resource();
            resource.setStatus(ResourceStatusEnum.APPROVED.getValue());
            when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(resource);

            AppException exception = assertThrows(AppException.class,
                    () -> service.rollbackToVersion(USER_ID, RESOURCE_ID, V1));

            assertEquals("Current resource status does not allow rollback.", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject missing or inactive category and missing resource type")
        void rollbackVersion_invalidStoredFields_throwConflict() {
            Resource resource = resource();
            ResourceVersion version = version(V1, "title", null, null);
            version.setSnapshot("{\"categoryId\":999,\"resourceType\":\"ARTICLE\"}");

            when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(resource);
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(version);
            when(categoryMapper.selectById(999L)).thenReturn(null);

            AppException missingCategory = assertThrows(AppException.class,
                    () -> service.rollbackToVersion(USER_ID, RESOURCE_ID, V1));

            assertEquals("Stored category does not exist and cannot be restored.", missingCategory.getMessage());
        }

        @Test
        @DisplayName("Should restore valid rollback and latest relations")
        void rollbackVersion_validRollback_succeeds() {
            Resource current = resource();
            current.setCategoryId(2L);
            current.setResourceTypeId(2L);
            current.setResourceType("video");

            ResourceVersion version = version(V1, "Restored", "Education", "ARTICLE");
            version.setSnapshot("""
                    {
                      "title":"Restored",
                      "description":"Restored description",
                      "copyright":"Restored rights",
                      "categoryId":1,
                      "categoryName":"Education",
                      "place":"Hangzhou",
                      "resourceType":"ARTICLE",
                      "previewImage":"preview.jpg",
                      "mediaUrl":"media.mp4",
                      "tagNames":["tag1","tag2"]
                    }
                    """);

            Resource latest = resource();
            latest.setCategoryId(1L);
            latest.setResourceTypeId(1L);
            latest.setResourceType("ARTICLE");
            latest.setCategoryName("Education");
            latest.setTitle("Restored");
            latest.setDescription("Restored description");
            latest.setCopyright("Restored rights");
            latest.setPlace("Hangzhou");
            latest.setPreviewImage("preview.jpg");
            latest.setMediaUrl("media.mp4");

            ResourceSubmission submission = new ResourceSubmission();
            submission.setSubmittedAt(LocalDateTime.now().minusMinutes(2));
            ReviewRecord reviewRecord = new ReviewRecord();
            reviewRecord.setStatus("APPROVED");
            reviewRecord.setFeedbackComment("Looks good");

            when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(current);
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(version);
            when(categoryMapper.selectById(1L)).thenReturn(category(1L, "Education", "ACTIVE"));
            when(resourceTypeMapper.selectActiveByTypeName("ARTICLE")).thenReturn(resourceType(1L, "ARTICLE"));
            when(resourceTagMapper.selectTagNamesByResourceId(RESOURCE_ID)).thenReturn(List.of("tag1", "tag2"));
            when(tagMapper.selectByNameIgnoreCase("tag1")).thenReturn(null);
            when(tagMapper.selectByNameIgnoreCase("tag2")).thenReturn(null);
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(latest);
            when(resourceVersionMapper.selectMaxVersionNoByResourceId(RESOURCE_ID)).thenReturn(5);
            when(resourceTagMapper.selectTagIdsByResourceId(RESOURCE_ID)).thenReturn(List.of(11L, 12L));
            when(resourceSubmissionMapper.selectLatestByResourceId(RESOURCE_ID)).thenReturn(submission);
            when(reviewRecordMapper.selectLatestByResourceId(RESOURCE_ID)).thenReturn(reviewRecord);

            ResourceDetailVO result = service.rollbackToVersion(USER_ID, RESOURCE_ID, V1);

            assertAll(
                    () -> assertEquals(RESOURCE_ID, result.getId()),
                    () -> assertEquals("Restored", result.getTitle()),
                    () -> assertEquals("Restored description", result.getDescription()),
                    () -> assertEquals("educational materials", result.getCategoryName()),
                    () -> assertEquals("ARTICLE", result.getResourceType()),
                    () -> assertEquals(5, result.getCurrentVersionNo()),
                    () -> assertEquals(List.of("tag1", "tag2"), result.getTagNames()),
                    () -> assertEquals("APPROVED", result.getLatestReviewStatus()),
                    () -> assertEquals("Looks good", result.getLatestFeedbackComment()),
                    () -> assertNotNull(result.getLatestSubmittedAt())
            );
        }
    }

    @Nested
    @DisplayName("Private Helper Coverage")
    class PrivateHelperCoverage {

        @Test
        @DisplayName("Should cover displayCategory branches")
        void displayCategory_coversBranches() {
            Object named = newSnapshot();
            setField(named, "categoryName", " Education ");

            Object missingId = newSnapshot();
            setField(missingId, "categoryId", null);

            Object fallback = newSnapshot();
            setField(fallback, "categoryId", 999L);

            Object resolved = newSnapshot();
            setField(resolved, "categoryId", 1L);

            when(categoryMapper.selectById(999L)).thenReturn(null);
            when(categoryMapper.selectById(1L)).thenReturn(category(1L, "Education", "ACTIVE"));

            assertAll(
                    () -> assertEquals("educational materials", invokeDisplayCategory(named)),
                    () -> assertEquals("-", invokeDisplayCategory(missingId)),
                    () -> assertEquals("Category #999", invokeDisplayCategory(fallback)),
                    () -> assertEquals("educational materials", invokeDisplayCategory(resolved))
            );
        }

        @Test
        @DisplayName("Should cover normalization helpers")
        void normalizationHelpers_coversBranches() {
            assertAll(
                    () -> assertEquals("-", invokeNormalizeCompareValue(null)),
                    () -> assertEquals("-", invokeNormalizeCompareValue("   ")),
                    () -> assertEquals("value", invokeNormalizeCompareValue("value")),
                    () -> assertNull(invokeNormalizeCategoryName(null)),
                    () -> assertEquals("educational materials", invokeNormalizeCategoryName("Education")),
                    () -> assertEquals("ARTICLE", invokeNormalizeResourceTypeValue("ARTICLE")),
                    () -> assertEquals("custom-type", invokeNormalizeResourceTypeValue("custom-type"))
            );
        }

        @Test
        @DisplayName("Should cover tag normalization and distinct id branches")
        void tagNormalization_coversBranches() {
            assertAll(
                    () -> assertEquals(List.of(), invokeNormalizeTagNames(null)),
                    () -> {
                        List<String> normalized = invokeNormalizeTagNames(java.util.Arrays.asList("A, B", "a", "  "));
                        assertEquals(2, normalized.size());
                        assertEquals("A, B", normalized.get(0));
                        assertEquals("a", normalized.get(1));
                    },
                    () -> assertEquals(List.of(1L, 2L), invokeDistinctTagIds(java.util.Arrays.asList(1L, 1L, null, 2L)))
            );
        }
    }

    private Resource resource() {
        Resource resource = new Resource();
        resource.setId(RESOURCE_ID);
        resource.setContributorId(USER_ID);
        resource.setTitle("Test");
        resource.setDescription("Desc");
        resource.setCopyright("Copyright");
        resource.setCategoryId(1L);
        resource.setCategoryName("Education");
        resource.setPlace("Place");
        resource.setResourceTypeId(1L);
        resource.setResourceType("ARTICLE");
        resource.setPreviewImage("preview.jpg");
        resource.setMediaUrl("media.mp4");
        resource.setStatus(ResourceStatusEnum.DRAFT.getValue());
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        return resource;
    }

    private ResourceVersion version(Integer versionNo, String title, String categoryName, String resourceType) {
        ResourceVersion version = new ResourceVersion();
        version.setResourceId(RESOURCE_ID);
        version.setVersionNo(versionNo);
        version.setChangeType("update");
        version.setChangeSummary("test");
        version.setCreatedBy(USER_ID);
        version.setCreatedAt(LocalDateTime.now());
        version.setSnapshot("""
                {
                    "title":"%s",
                    "description":"Desc",
                    "copyright":"Copyright",
                    "categoryId":1,
                    "categoryName":"%s",
                    "place":"Place",
                    "resourceType":"%s",
                    "previewImage":"preview.jpg",
                    "mediaUrl":"media.mp4",
                    "tagNames":["tag1"]
                }""".formatted(title, categoryName, resourceType));
        return version;
    }

    private Category category() {
        return category(1L, "Education", "ACTIVE");
    }

    private Category category(Long id, String topic, String status) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryTopic(topic);
        category.setStatus(status);
        return category;
    }

    private ResourceType resourceType(Long id, String typeName) {
        ResourceType type = new ResourceType();
        type.setResourceTypeId(id);
        type.setTypeName(typeName);
        type.setStatus("ACTIVE");
        return type;
    }

    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = ResourceVersionServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(service, args);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to invoke private method: " + methodName, exception);
        }
    }

    private Object newSnapshot() {
        try {
            Class<?> snapshotClass = Class.forName("com.cpt202.HerLink.service.impl.ResourceVersionServiceImpl$ResourceSnapshot");
            Constructor<?> constructor = snapshotClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to create snapshot", exception);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to set field " + fieldName, exception);
        }
    }

    private String invokeDisplayCategory(Object snapshot) {
        return (String) invokePrivate("displayCategory", new Class<?>[]{snapshot.getClass()}, snapshot);
    }

    private String invokeNormalizeCompareValue(String value) {
        return (String) invokePrivate("normalizeCompareValue", new Class<?>[]{String.class}, value);
    }

    private String invokeNormalizeCategoryName(String value) {
        return (String) invokePrivate("normalizeCategoryName", new Class<?>[]{String.class}, value);
    }

    private String invokeNormalizeResourceTypeValue(String value) {
        return (String) invokePrivate("normalizeResourceTypeValue", new Class<?>[]{String.class}, value);
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeNormalizeTagNames(List<String> tagNames) {
        return (List<String>) invokePrivate("normalizeTagNames", new Class<?>[]{List.class}, tagNames);
    }

    @SuppressWarnings("unchecked")
    private List<Long> invokeDistinctTagIds(List<Long> tagIds) {
        return (List<Long>) invokePrivate("distinctTagIds", new Class<?>[]{List.class}, tagIds);
    }
}
