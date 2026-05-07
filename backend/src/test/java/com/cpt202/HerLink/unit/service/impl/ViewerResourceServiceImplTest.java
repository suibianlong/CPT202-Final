package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.service.impl.ViewerResourceServiceImpl;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewerResourceServiceImplTest {

    @Mock
    private ResourceMapper resourceMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private ResourceTagMapper resourceTagMapper;
    @Mock
    private ResourceTypeMapper resourceTypeMapper;

    @InjectMocks
    private ViewerResourceServiceImpl service;

    @Test
    @DisplayName("List approved resources rejects unsupported sort option")
    void listApprovedResources_invalidSort_throwsBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.listApprovedResources(null, null, null, "created_at"));

        assertAll(
                () -> assertEquals(400, exception.getStatusCode()),
                () -> assertEquals("Unsupported sort option.", exception.getMessage())
        );
    }

    @Test
    @DisplayName("List approved resources returns empty list for unknown resource type filter")
    void listApprovedResources_unknownResourceType_returnsEmptyList() {
        when(resourceTypeMapper.selectActiveByTypeName("custom")).thenReturn(null);

        List<ResourceListItemVO> result = service.listApprovedResources(null, "custom", null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("List approved resources resolves direct custom type match")
    void listApprovedResources_customTypeDirectMatch_returnsMappedRows() {
        LocalDateTime updatedAt = LocalDateTime.now().minusHours(2);
        when(resourceTypeMapper.selectActiveByTypeName("custom type")).thenReturn(type(5L, "custom type"));
        when(resourceMapper.selectApprovedResources(eq("wall"), eq(5L), eq(null), eq("time")))
                .thenReturn(List.of(resource(5L, "Wall", "Approved", "custom type", "History", updatedAt)));

        List<ResourceListItemVO> result = service.listApprovedResources(" wall ", " custom type ", null, " time ");

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(5L, result.get(0).getId()),
                () -> assertEquals("custom type", result.get(0).getResourceType()),
                () -> assertEquals("history", result.get(0).getCategoryName())
        );
    }

    @Test
    @DisplayName("List approved resources maps null mapper result to empty list")
    void listApprovedResources_mapperReturnsNull_returnsEmptyList() {
        when(resourceMapper.selectApprovedResources(isNull(), isNull(), isNull(), isNull())).thenReturn(null);

        List<ResourceListItemVO> result = service.listApprovedResources(null, null, null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("List approved resources normalizes filters and response fields")
    void listApprovedResources_validFilters_returnsMappedRows() {
        ResourceType photoType = type(2L, "photo");
        LocalDateTime updatedAt = LocalDateTime.now().minusHours(1);
        when(resourceTypeMapper.selectActiveByTypeName("photo")).thenReturn(photoType);
        when(resourceMapper.selectApprovedResources(eq("heritage"), eq(2L), eq(9L), eq("title")))
                .thenReturn(List.of(resource(1L, "Wall", " Approved ", "Picture", " Education ", updatedAt)));

        List<ResourceListItemVO> result = service.listApprovedResources(" heritage ", "Picture", 9L, " TITLE ");

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(1L, result.get(0).getId()),
                () -> assertEquals("Wall", result.get(0).getTitle()),
                () -> assertEquals(ResourceStatusEnum.APPROVED.getValue(), result.get(0).getStatus()),
                () -> assertEquals("photo", result.get(0).getResourceType()),
                () -> assertEquals("educational materials", result.get(0).getCategoryName()),
                () -> assertEquals(updatedAt, result.get(0).getUpdatedAt())
        );
    }

    @Test
    @DisplayName("Get approved resource detail rejects missing resource")
    void getApprovedResourceDetail_missingResource_throwsNotFound() {
        when(resourceMapper.selectApprovedById(99L)).thenReturn(null);

        AppException exception = assertThrows(AppException.class, () -> service.getApprovedResourceDetail(99L));

        assertEquals("Approved resource does not exist.", exception.getMessage());
    }

    @Test
    @DisplayName("Get approved resource detail maps tags and resource fields")
    void getApprovedResourceDetail_resourceExists_returnsDetail() {
        Resource resource = resource(1L, "Wall", "Approved", "Video", "Places", LocalDateTime.now());
        resource.setContributorId(88L);
        resource.setDescription("Description");
        resource.setCopyright("Rights");
        resource.setPlace("Suzhou");
        resource.setPreviewImage("preview.jpg");
        resource.setMediaUrl("video.mp4");
        when(resourceMapper.selectApprovedById(1L)).thenReturn(resource);
        when(resourceTagMapper.selectTagIdsByResourceId(1L)).thenReturn(List.of(3L, 4L));
        when(resourceTagMapper.selectTagNamesByResourceId(1L)).thenReturn(List.of("Temple", "Festival"));

        ResourceDetailVO detail = service.getApprovedResourceDetail(1L);

        assertAll(
                () -> assertEquals(1L, detail.getId()),
                () -> assertEquals(88L, detail.getContributorId()),
                () -> assertEquals("Wall", detail.getTitle()),
                () -> assertEquals("Description", detail.getDescription()),
                () -> assertEquals("Rights", detail.getCopyright()),
                () -> assertEquals("Suzhou", detail.getPlace()),
                () -> assertEquals("video", detail.getResourceType()),
                () -> assertEquals(List.of(3L, 4L), detail.getTagIds()),
                () -> assertEquals(List.of("Temple", "Festival"), detail.getTagNames())
        );
    }

    @Test
    @DisplayName("List category options skips null and invalid category rows")
    void listCategoryOptions_mixedRows_returnsOnlyValidOptions() {
        when(categoryMapper.selectActiveCategories()).thenReturn(Arrays.asList(
                category(null, "invalid"),
                category(1L, "Places"),
                null,
                category(2L, "Education")));

        List<CategoryTagOptionVO> result = service.listCategoryOptions();

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals(1L, result.get(0).getId()),
                () -> assertEquals("Places", result.get(0).getName()),
                () -> assertEquals(2L, result.get(1).getId()),
                () -> assertEquals("Education", result.get(1).getName())
        );
    }

    @Test
    @DisplayName("List category options returns empty list when mapper returns null")
    void listCategoryOptions_mapperReturnsNull_returnsEmptyList() {
        when(categoryMapper.selectActiveCategories()).thenReturn(null);

        List<CategoryTagOptionVO> result = service.listCategoryOptions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("List resource type options skips null and invalid rows")
    void listResourceTypeOptions_mixedRows_returnsOnlyValidOptions() {
        when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(Arrays.asList(
                type(null, "invalid"),
                type(1L, "photo"),
                null,
                type(2L, "video")));

        List<CategoryTagOptionVO> result = service.listResourceTypeOptions();

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals(1L, result.get(0).getId()),
                () -> assertEquals("photo", result.get(0).getName()),
                () -> assertEquals(2L, result.get(1).getId()),
                () -> assertEquals("video", result.get(1).getName())
        );
    }

    @Test
    @DisplayName("Unknown non-empty category name is normalized by trimming and lowercasing")
    void getApprovedResourceDetail_unknownCategoryName_normalizesWhitespaceAndCase() {
        Resource resource = resource(7L, "Document", "Approved", "Document", "  Local   Archive  ", LocalDateTime.now());
        when(resourceMapper.selectApprovedById(7L)).thenReturn(resource);
        when(resourceTagMapper.selectTagIdsByResourceId(7L)).thenReturn(List.of());
        when(resourceTagMapper.selectTagNamesByResourceId(7L)).thenReturn(List.of());

        ResourceDetailVO detail = service.getApprovedResourceDetail(7L);

        assertEquals("local archive", detail.getCategoryName());
    }

    @Test
    @DisplayName("Unsupported stored resource type is returned unchanged")
    void getApprovedResourceDetail_unknownResourceType_returnsOriginalType() {
        Resource resource = resource(8L, "Other", "Approved", "custom type", null, LocalDateTime.now());
        when(resourceMapper.selectApprovedById(8L)).thenReturn(resource);
        when(resourceTagMapper.selectTagIdsByResourceId(8L)).thenReturn(List.of());
        when(resourceTagMapper.selectTagNamesByResourceId(8L)).thenReturn(List.of());

        ResourceDetailVO detail = service.getApprovedResourceDetail(8L);

        assertSame("custom type", detail.getResourceType());
    }

    @Test
    @DisplayName("Private category matching supports educational materials alias")
    void privateMatchesCategoryName_supportsEducationalAlias() {
        assertTrue(invokeMatchesCategoryName("educational materials", "Education"));
        assertFalse(invokeMatchesCategoryName("educational materials", "Museum"));
    }

    @Test
    @DisplayName("Private findMatchingCategory returns matching row or null")
    void privateFindMatchingCategory_returnsExpectedResult() {
        List<Category> categories = Arrays.asList(
                null,
                category(null, "Invalid"),
                category(1L, "Education"),
                category(2L, "History")
        );

        Category hit = invokeFindMatchingCategory(categories, "educational materials");
        Category miss = invokeFindMatchingCategory(categories, "unknown");

        assertAll(
                () -> assertNotNull(hit),
                () -> assertEquals(1L, hit.getCategoryId()),
                () -> assertNull(miss)
        );
    }

    private Resource resource(Long id, String title, String status, String resourceType, String categoryName, LocalDateTime updatedAt) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setTitle(title);
        resource.setStatus(status);
        resource.setResourceType(resourceType);
        resource.setCategoryId(9L);
        resource.setCategoryName(categoryName);
        resource.setUpdatedAt(updatedAt);
        return resource;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryTopic(name);
        return category;
    }

    private ResourceType type(Long id, String name) {
        ResourceType type = new ResourceType();
        type.setResourceTypeId(id);
        type.setTypeName(name);
        return type;
    }

    private boolean invokeMatchesCategoryName(String expectedCategoryName, String actualCategoryName) {
        return (Boolean) invokePrivateMethod(
                "matchesCategoryName",
                new Class<?>[]{String.class, String.class},
                expectedCategoryName,
                actualCategoryName
        );
    }

    @SuppressWarnings("unchecked")
    private Category invokeFindMatchingCategory(List<Category> categories, String categoryName) {
        return (Category) invokePrivateMethod(
                "findMatchingCategory",
                new Class<?>[]{List.class, String.class},
                categories,
                categoryName
        );
    }

    private Object invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = ViewerResourceServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
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
