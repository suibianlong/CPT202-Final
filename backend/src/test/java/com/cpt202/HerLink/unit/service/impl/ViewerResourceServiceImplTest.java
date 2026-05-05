package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.service.impl.*;
import com.cpt202.HerLink.service.impl.ViewerResourceServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.Resource;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.enums.ResourceTypeEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;
import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceListItemVO;

@ExtendWith(MockitoExtension.class)
@DisplayName("ViewerResourceServiceImpl Unit Test")
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
    private ViewerResourceServiceImpl viewerResourceService;

    private static final Long RESOURCE_ID = 1L;
    private static final Long CATEGORY_ID = 10L;
    private static final Long TYPE_ID = 20L;

    private Resource buildApprovedResource() {
        Resource resource = new Resource();
        resource.setId(RESOURCE_ID);
        resource.setTitle("Test Title");
        resource.setDescription("Test Desc");
        resource.setPreviewImage("preview.jpg");
        resource.setStatus(ResourceStatusEnum.APPROVED.getValue());
        resource.setResourceType(ResourceTypeEnum.VIDEO.getValue());
        resource.setCategoryId(CATEGORY_ID);
        resource.setCategoryName("Education");
        return resource;
    }

    private Category buildValidCategory() {
        Category category = new Category();
        category.setCategoryId(CATEGORY_ID);
        category.setCategoryTopic("Educational Materials");
        return category;
    }

    private ResourceType buildValidResourceType() {
        ResourceType type = new ResourceType();
        type.setResourceTypeId(TYPE_ID);
        type.setTypeName(ResourceTypeEnum.VIDEO.getValue());
        return type;
    }

    @Nested
    @DisplayName("listApprovedResources: Query Approved Resource List")
    class ListApprovedResources {

        @Test
        @DisplayName("Normal: No filters, return valid resource list")
        void listApprovedResources_Normal_NoFilters() {
            Resource resource = buildApprovedResource();
            when(resourceMapper.selectApprovedResources(isNull(), isNull(), isNull(), isNull()))
                    .thenReturn(List.of(resource));

            List<ResourceListItemVO> result = viewerResourceService.listApprovedResources(null, null, null, null);

            assertEquals(1, result.size());
            assertEquals(RESOURCE_ID, result.get(0).getId());
            assertEquals("Test Title", result.get(0).getTitle());
            assertEquals("educational materials", result.get(0).getCategoryName());
        }

        @Test
        @DisplayName("Normal: With keyword, category and sorting, return matched list")
        void listApprovedResources_Normal_WithAllFilters() {
            // Stub resourceType lookup
            ResourceType mockType = buildValidResourceType();
            when(resourceTypeMapper.selectActiveByTypeName("video")).thenReturn(mockType);
            
            when(resourceMapper.selectApprovedResources(eq("test"), eq(TYPE_ID), eq(CATEGORY_ID), eq("title")))
                    .thenReturn(List.of(buildApprovedResource()));

            List<ResourceListItemVO> result = viewerResourceService.listApprovedResources(
                    "test", "video", CATEGORY_ID, "title");

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Boundary: Blank keyword, process as no keyword")
        void listApprovedResources_Boundary_BlankKeyword() {
            when(resourceMapper.selectApprovedResources(any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            List<ResourceListItemVO> result = viewerResourceService.listApprovedResources(
                    "   ", null, null, null);

            assertNotNull(result);
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Boundary: Mapper returns null, convert to empty list")
        void listApprovedResources_Boundary_MapperReturnsNull() {
            when(resourceMapper.selectApprovedResources(any(), any(), any(), any())).thenReturn(null);

            List<ResourceListItemVO> result = viewerResourceService.listApprovedResources(null, null, null, null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Boundary: Invalid resource type, return empty list")
        void listApprovedResources_Boundary_InvalidResourceType() {
            List<ResourceListItemVO> result = viewerResourceService.listApprovedResources(
                    null, "unknownType", null, null);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Exception: Unsupported sort field, throw bad request exception")
        void listApprovedResources_Exception_InvalidSort() {
            AppException ex = assertThrows(AppException.class,
                    () -> viewerResourceService.listApprovedResources(null, null, null, "invalid"));

            assertEquals("Unsupported sort option.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("getApprovedResourceDetail: Query Approved Resource Detail")
    class GetApprovedResourceDetail {

        @Test
        @DisplayName("Normal: Resource exists, return full detail VO")
        void getApprovedResourceDetail_Normal_Exists() {
            Resource resource = buildApprovedResource();
            when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(resource);
            when(resourceTagMapper.selectTagIdsByResourceId(RESOURCE_ID)).thenReturn(List.of(1L, 2L));
            when(resourceTagMapper.selectTagNamesByResourceId(RESOURCE_ID)).thenReturn(List.of("TagA", "TagB"));

            ResourceDetailVO vo = viewerResourceService.getApprovedResourceDetail(RESOURCE_ID);

            assertEquals(RESOURCE_ID, vo.getId());
            assertEquals("Test Title", vo.getTitle());
            assertEquals("educational materials", vo.getCategoryName());
            assertEquals(2, vo.getTagIds().size());
        }

        @Test
        @DisplayName("Boundary: Resource has no tags, return empty tag lists")
        void getApprovedResourceDetail_Boundary_NoTags() {
            Resource resource = buildApprovedResource();
            when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(resource);
            when(resourceTagMapper.selectTagIdsByResourceId(RESOURCE_ID)).thenReturn(Collections.emptyList());
            when(resourceTagMapper.selectTagNamesByResourceId(RESOURCE_ID)).thenReturn(Collections.emptyList());

            ResourceDetailVO vo = viewerResourceService.getApprovedResourceDetail(RESOURCE_ID);

            assertTrue(vo.getTagIds().isEmpty());
            assertTrue(vo.getTagNames().isEmpty());
        }

        @Test
        @DisplayName("Exception: Resource not found, throw not found exception")
        void getApprovedResourceDetail_Exception_NotFound() {
            when(resourceMapper.selectApprovedById(RESOURCE_ID)).thenReturn(null);

            AppException ex = assertThrows(AppException.class,
                    () -> viewerResourceService.getApprovedResourceDetail(RESOURCE_ID));

            assertEquals("Approved resource does not exist.", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("listCategoryOptions: Query Category Options")
    class ListCategoryOptions {

        @Test
        @DisplayName("Normal: Has valid categories, return option list")
        void listCategoryOptions_Normal_HasCategories() {
            when(categoryMapper.selectActiveCategories()).thenReturn(List.of(buildValidCategory()));

            List<CategoryTagOptionVO> result = viewerResourceService.listCategoryOptions();

            assertEquals(1, result.size());
            assertEquals(CATEGORY_ID, result.get(0).getId());
            assertEquals("Educational Materials", result.get(0).getName());
        }

        @Test
        @DisplayName("Boundary: Mapper returns null, convert to empty list")
        void listCategoryOptions_Boundary_MapperNull() {
            when(categoryMapper.selectActiveCategories()).thenReturn(null);

            List<CategoryTagOptionVO> result = viewerResourceService.listCategoryOptions();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Boundary: Filter null elements in list")
        void listCategoryOptions_Boundary_FilterNullElements() {
            Category valid = buildValidCategory();
            List<Category> categoryList = new ArrayList<>();
            categoryList.add(null);
            categoryList.add(valid);
            categoryList.add(null);

            when(categoryMapper.selectActiveCategories()).thenReturn(categoryList);

            List<CategoryTagOptionVO> result = viewerResourceService.listCategoryOptions();
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Boundary: No available categories, return empty list")
        void listCategoryOptions_Boundary_EmptyList() {
            when(categoryMapper.selectActiveCategories()).thenReturn(Collections.emptyList());

            List<CategoryTagOptionVO> result = viewerResourceService.listCategoryOptions();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("listResourceTypeOptions: Query Resource Type Options")
    class ListResourceTypeOptions {

        @Test
        @DisplayName("Normal: Has valid types, return option list")
        void listResourceTypeOptions_Normal_HasTypes() {
            when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(List.of(buildValidResourceType()));

            List<CategoryTagOptionVO> result = viewerResourceService.listResourceTypeOptions();

            assertEquals(1, result.size());
            assertEquals(TYPE_ID, result.get(0).getId());
            assertEquals(ResourceTypeEnum.VIDEO.getValue(), result.get(0).getName());
        }

        @Test
        @DisplayName("Boundary: Mapper returns null, convert to empty list")
        void listResourceTypeOptions_Boundary_MapperNull() {
            when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(null);

            List<CategoryTagOptionVO> result = viewerResourceService.listResourceTypeOptions();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Boundary: Filter null type objects")
        void listResourceTypeOptions_Boundary_FilterNull() {
            ResourceType valid = buildValidResourceType();
            List<ResourceType> typeList = new ArrayList<>();
            typeList.add(null);
            typeList.add(valid);

            when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(typeList);

            List<CategoryTagOptionVO> result = viewerResourceService.listResourceTypeOptions();
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Boundary: No available types, return empty list")
        void listResourceTypeOptions_Boundary_Empty() {
            when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(Collections.emptyList());

            List<CategoryTagOptionVO> result = viewerResourceService.listResourceTypeOptions();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Private Method Logic (Test via Public Methods)")
    class PrivateMethodsCoverage {

        @Test
        @DisplayName("Boundary: Normalize category name 'education' to 'educational materials'")
        void categoryNameNormalizeTest() {
            Resource resource = new Resource();
            resource.setCategoryName("Education");
            resource.setStatus(ResourceStatusEnum.APPROVED.getValue());
            when(resourceMapper.selectApprovedById(any())).thenReturn(resource);
            when(resourceTagMapper.selectTagIdsByResourceId(any())).thenReturn(Collections.emptyList());

            ResourceDetailVO vo = viewerResourceService.getApprovedResourceDetail(RESOURCE_ID);

            assertEquals("educational materials", vo.getCategoryName());
        }

        @Test
        @DisplayName("Boundary: Normalize resource type matching enum")
        void resourceTypeNormalizeTest() {
            Resource resource = new Resource();
            resource.setResourceType("video");
            resource.setStatus(ResourceStatusEnum.APPROVED.getValue());
            when(resourceMapper.selectApprovedById(any())).thenReturn(resource);

            ResourceDetailVO vo = viewerResourceService.getApprovedResourceDetail(RESOURCE_ID);

            assertEquals(ResourceTypeEnum.VIDEO.getValue(), vo.getResourceType());
        }

        @Test
        @DisplayName("Boundary: Normalize mixed-case sort field")
        void sortByNormalizeTest() {
            when(resourceMapper.selectApprovedResources(any(), any(), any(), any())).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> viewerResourceService.listApprovedResources(null, null, null, "TiTlE"));
            assertDoesNotThrow(() -> viewerResourceService.listApprovedResources(null, null, null, "TIME"));
        }
    }
}
