package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.service.impl.*;
import com.cpt202.HerLink.service.impl.ResourceVersionServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.ResourceVersion;
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
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceVersionCompareVO;
import com.cpt202.HerLink.vo.ResourceVersionVO;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceVersionServiceImpl Unit Test")
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

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ResourceVersionServiceImpl resourceVersionService;

    private static final Long RESOURCE_ID = 100L;
    private static final Long USER_ID = 200L;
    private static final Integer V1 = 1;
    private static final Integer V2 = 2;

    // ==================== Utility Method ====================
    private Resource resource() {
        Resource r = new Resource();
        r.setId(RESOURCE_ID);
        r.setContributorId(USER_ID);
        r.setTitle("Test");
        r.setDescription("Desc");
        r.setCopyright("Copyright");
        r.setCategoryId(1L);
        r.setPlace("Place");
        r.setResourceTypeId(1L);
        r.setResourceType("ARTICLE");
        r.setPreviewImage("preview.jpg");
        r.setMediaUrl("media.mp4");
        r.setStatus(ResourceStatusEnum.DRAFT.getValue());
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }

    private ResourceVersion version(Integer version) {
        ResourceVersion v = new ResourceVersion();
        v.setResourceId(RESOURCE_ID);
        v.setVersionNo(version);
        v.setChangeType("update");
        v.setChangeSummary("test");
        v.setCreatedBy(USER_ID);
        v.setCreatedAt(LocalDateTime.now());
        v.setSnapshot("""
                {
                    "title":"Test",
                    "description":"Desc",
                    "copyright":"Copyright",
                    "categoryId":1,
                    "categoryName":"Test",
                    "place":"Place",
                    "resourceType":"ARTICLE",
                    "previewImage":"preview.jpg",
                    "mediaUrl":"media.mp4",
                    "tagNames":["tag1"]
                }""");
        return v;
    }

    private Category category() {
        Category c = new Category();
        c.setCategoryId(1L);
        c.setStatus("ACTIVE");
        return c;
    }

    private ResourceType resourceType() {
        ResourceType t = new ResourceType();
        t.setResourceTypeId(1L);
        t.setTypeName("ARTICLE");
        t.setStatus("ACTIVE");
        return t;
    }

    // ============================================================
    @Nested
    @DisplayName("saveVersionSnapshot Method Test")
    class SaveVersionSnapshot {

        @Test
        @DisplayName("Normal case: save snapshot successfully")
        void save_normal() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectMaxVersionNoByResourceId(RESOURCE_ID)).thenReturn(0);
            when(resourceTagMapper.selectTagNamesByResourceId(RESOURCE_ID)).thenReturn(List.of("tag1"));

            assertDoesNotThrow(() ->
                    resourceVersionService.saveVersionSnapshot(RESOURCE_ID, USER_ID, "update", "test")
            );
        }

        @Test
        @DisplayName("Exception case: resourceId is null")
        void save_resourceId_null() {
            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.saveVersionSnapshot(null, USER_ID, "update", "test"));
            assertEquals("Resource id is required.", e.getMessage());
        }

        @Test
        @DisplayName("Exception case: userId is null")
        void save_userId_null() {
            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.saveVersionSnapshot(RESOURCE_ID, null, "update", "test"));
            assertEquals("Current user id is required.", e.getMessage());
        }

        @Test
        @DisplayName("Exception case: changeType is blank")
        void save_changeType_blank() {
            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.saveVersionSnapshot(RESOURCE_ID, USER_ID, "   ", "test"));
            assertEquals("Change type is required.", e.getMessage());
        }

        @Test
        @DisplayName("Boundary case: target resource not found")
        void save_resource_not_found() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(null);
            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.saveVersionSnapshot(RESOURCE_ID, USER_ID, "update", "test"));
            assertEquals("Resource does not exist.", e.getMessage());
        }
    }

    @Nested
    @DisplayName("listVersions Method Test")
    class ListVersions {

        @Test
        @DisplayName("Normal case: return version list normally")
        void list_normal() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceId(RESOURCE_ID)).thenReturn(List.of(version(V1)));

            List<ResourceVersionVO> list = resourceVersionService.listVersions(USER_ID, RESOURCE_ID);
            assertEquals(1, list.size());
            assertEquals(V1, list.get(0).getVersionNo());
        }

        @Test
        @DisplayName("Boundary case: no version record, return empty list")
        void list_empty() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceId(RESOURCE_ID)).thenReturn(null);

            List<ResourceVersionVO> list = resourceVersionService.listVersions(USER_ID, RESOURCE_ID);
            assertTrue(list.isEmpty());
        }

        @Test
        @DisplayName("Exception case: current user has no permission")
        void list_forbidden() {
            Resource r = resource();
            r.setContributorId(999L);
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(r);

            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.listVersions(USER_ID, RESOURCE_ID));
            assertEquals("Current user does not own this resource.", e.getMessage());
        }
    }

    @Nested
    @DisplayName("getVersion Method Test")
    class GetVersion {

        @Test
        @DisplayName("Normal case: get single version detail")
        void get_normal() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(version(V1));

            ResourceVersionVO vo = resourceVersionService.getVersion(USER_ID, RESOURCE_ID, V1);
            assertEquals(V1, vo.getVersionNo());
            assertNotNull(vo.getSnapshotMap());
        }

        @Test
        @DisplayName("Boundary case: version number equals 0")
        void get_version_zero() {
            // 关键：先mock resource，让loadOwnedResource通过
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());

            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.getVersion(USER_ID, RESOURCE_ID, 0));
            assertEquals("Version number is required.", e.getMessage());
        }

        @Test
        @DisplayName("Boundary case: version number is negative")
        void get_version_negative() {
            // 关键：先mock resource，让loadOwnedResource通过
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());

            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.getVersion(USER_ID, RESOURCE_ID, -5));
            assertEquals("Version number is required.", e.getMessage());
        }

        @Test
        @DisplayName("Boundary case: target version not exists")
        void get_version_not_found() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(null);

            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.getVersion(USER_ID, RESOURCE_ID, V1));
            assertEquals("Requested version does not exist.", e.getMessage());
        }
    }

    @Nested
    @DisplayName("compareVersions Method Test")
    class CompareVersions {

        @Test
        @DisplayName("Normal case: two versions have identical content")
        void compare_same() {
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(version(V1));
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V2)).thenReturn(version(V2));

            ResourceVersionCompareVO vo = resourceVersionService.compareVersions(USER_ID, RESOURCE_ID, V1, V2);
            long changed = vo.getDiffItems().stream().filter(d -> d.getChanged()).count();
            assertEquals(0, changed);
        }

        @Test
        @DisplayName("Boundary case: title content different, mark as changed")
        void compare_title_different() {
            ResourceVersion v1 = version(V1);
            v1.setSnapshot("{\"title\":\"A\"}");
            ResourceVersion v2 = version(V2);
            v2.setSnapshot("{\"title\":\"B\"}");

            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(v1);
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V2)).thenReturn(v2);

            ResourceVersionCompareVO vo = resourceVersionService.compareVersions(USER_ID, RESOURCE_ID, V1, V2);
            assertTrue(vo.getDiffItems().get(0).getChanged());
        }

        @Test
        @DisplayName("Boundary case: null or blank value display as '-'")
        void compare_values_null() {
            ResourceVersion v1 = version(V1);
            v1.setSnapshot("{\"title\":null}");
            ResourceVersion v2 = version(V2);
            v2.setSnapshot("{\"title\":\"\"}");

            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(v1);
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V2)).thenReturn(v2);

            ResourceVersionCompareVO vo = resourceVersionService.compareVersions(USER_ID, RESOURCE_ID, V1, V2);
            assertEquals("-", vo.getDiffItems().get(0).getLeftValue());
            assertEquals("-", vo.getDiffItems().get(0).getRightValue());
        }
    }

    @Nested
    @DisplayName("rollbackToVersion Method Test")
    class RollbackToVersion {

        @Test
        @DisplayName("Normal case: rollback to target version successfully")
        void rollback_normal() {
            Resource r = resource();
            when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(r);
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(version(V1));
            when(categoryMapper.selectById(anyLong())).thenReturn(category());
            when(resourceTypeMapper.selectActiveByTypeName(any())).thenReturn(resourceType());
            when(resourceTagMapper.selectTagNamesByResourceId(RESOURCE_ID)).thenReturn(List.of("tag1"));
            when(tagMapper.selectByNameIgnoreCase(any())).thenReturn(null);
            when(resourceMapper.selectById(RESOURCE_ID)).thenReturn(r);

            ResourceDetailVO vo = resourceVersionService.rollbackToVersion(USER_ID, RESOURCE_ID, V1);
            assertNotNull(vo);
            assertEquals(RESOURCE_ID, vo.getId());
        }

        @Test
        @DisplayName("Boundary case: published status cannot rollback")
        void rollback_status_published() {
            Resource r = resource();
            r.setStatus(ResourceStatusEnum.APPROVED.getValue());
            when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(r);

            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.rollbackToVersion(USER_ID, RESOURCE_ID, V1));
            assertEquals("Current resource status does not allow rollback.", e.getMessage());
        }

        @Test
        @DisplayName("Boundary case: target category not exists, rollback failed")
        void rollback_category_missing() {
            Resource r = resource();
            ResourceVersion v = version(V1);
            v.setSnapshot("{\"categoryId\":999,\"resourceType\":\"ARTICLE\"}");

            when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(r);
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(v);
            when(categoryMapper.selectById(999L)).thenReturn(null);

            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.rollbackToVersion(USER_ID, RESOURCE_ID, V1));
            assertEquals("Stored category does not exist and cannot be restored.", e.getMessage());
        }

        @Test
        @DisplayName("Boundary case: tag name over max length limit")
        void rollback_tag_too_long() {
            String longTag = "a".repeat(101);
            ResourceVersion v = version(V1);
            // 关键：快照必须包含 categoryId，否则会提前抛分类异常
            v.setSnapshot("{\"categoryId\":1, \"resourceType\":\"ARTICLE\", \"tagNames\":[\""+longTag+"\"]}");

            when(resourceMapper.selectByIdForUpdate(RESOURCE_ID)).thenReturn(resource());
            when(resourceVersionMapper.selectByResourceIdAndVersionNo(RESOURCE_ID, V1)).thenReturn(v);
            when(categoryMapper.selectById(anyLong())).thenReturn(category());
            when(resourceTypeMapper.selectActiveByTypeName(any())).thenReturn(resourceType());

            AppException e = assertThrows(AppException.class,
                    () -> resourceVersionService.rollbackToVersion(USER_ID, RESOURCE_ID, V1));
            assertEquals("Tag name cannot exceed 100 characters.", e.getMessage());
        }
    }
}
