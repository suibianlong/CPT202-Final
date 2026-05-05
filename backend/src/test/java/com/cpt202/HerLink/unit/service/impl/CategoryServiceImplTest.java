package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.service.impl.*;
import com.cpt202.HerLink.service.impl.CategoryServiceImpl;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("Should return multiple VO objects when multiple active categories exist")
    void listCategoryOptions_ShouldReturnMultipleVOs_WhenMultipleCategoriesExist() {
        Category category1 = new Category();
        category1.setCategoryId(1L);
        category1.setCategoryTopic("Health");

        Category category2 = new Category();
        category2.setCategoryId(2L);
        category2.setCategoryTopic("Career");

        List<Category> mockList = List.of(category1, category2);
        when(categoryMapper.selectActiveCategories()).thenReturn(mockList);

        List<CategoryTagOptionVO> result = categoryService.listCategoryOptions();

        assertNotNull(result);
        assertEquals(2, result.size());

        CategoryTagOptionVO vo1 = result.get(0);
        assertEquals(1L, vo1.getId());
        assertEquals("Health", vo1.getName());

        CategoryTagOptionVO vo2 = result.get(1);
        assertEquals(2L, vo2.getId());
        assertEquals("Career", vo2.getName());
    }

    @Test
    @DisplayName("Should return empty list when mapper returns empty list")
    void listCategoryOptions_ShouldReturnEmptyList_WhenMapperReturnsEmptyList() {
        when(categoryMapper.selectActiveCategories()).thenReturn(Collections.emptyList());

        List<CategoryTagOptionVO> result = categoryService.listCategoryOptions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should return empty list when mapper returns null")
    void listCategoryOptions_ShouldReturnEmptyList_WhenMapperReturnsNull() {
        when(categoryMapper.selectActiveCategories()).thenReturn(null);

        List<CategoryTagOptionVO> result = categoryService.listCategoryOptions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return single VO when only one category exists")
    void listCategoryOptions_ShouldReturnSingleVO_WhenOnlyOneCategoryExists() {
        Category category = new Category();
        category.setCategoryId(10L);
        category.setCategoryTopic("Single Category");

        when(categoryMapper.selectActiveCategories()).thenReturn(List.of(category));

        List<CategoryTagOptionVO> result = categoryService.listCategoryOptions();

        assertEquals(1, result.size());
        CategoryTagOptionVO vo = result.get(0);
        assertEquals(10L, vo.getId());
        assertEquals("Single Category", vo.getName());
    }
}
