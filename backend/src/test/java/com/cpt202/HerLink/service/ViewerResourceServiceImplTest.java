package com.cpt202.HerLink.service;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceMapper;
import com.cpt202.HerLink.mapper.ResourceTagMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.service.impl.ViewerResourceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private ViewerResourceServiceImpl viewerResourceService;

    @Test
    void listCategoryOptions_shouldReturnActiveCategoriesWithoutRequiredStandardSet() {
        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(createCategory(10L, "Manuscripts")));

        var result = viewerResourceService.listCategoryOptions();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals("Manuscripts", result.get(0).getName());
    }

    @Test
    void listResourceTypeOptions_shouldReturnActiveResourceTypes() {
        when(resourceTypeMapper.selectActiveResourceTypes()).thenReturn(List.of(createResourceType(11L, "Map")));

        var result = viewerResourceService.listResourceTypeOptions();

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getId());
        assertEquals("Map", result.get(0).getName());
    }

    private Category createCategory(Long id, String topic) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryTopic(topic);
        category.setStatus("ACTIVE");
        return category;
    }

    private ResourceType createResourceType(Long id, String typeName) {
        ResourceType resourceType = new ResourceType();
        resourceType.setResourceTypeId(id);
        resourceType.setTypeName(typeName);
        resourceType.setStatus("ACTIVE");
        return resourceType;
    }
}
