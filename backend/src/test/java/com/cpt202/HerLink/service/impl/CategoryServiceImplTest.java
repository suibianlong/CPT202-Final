package com.cpt202.HerLink.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CategoryServiceImplTest {

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        // 传入 null，因为接口无法实例化
        categoryService = new CategoryServiceImpl(null);
    }

    // ==================== 唯一能测的：异常情况 ====================
    @Test
    void listCategoryOptions_WithNullMapper_ShouldThrowException() {
        assertThrows(RuntimeException.class, () -> {
            categoryService.listCategoryOptions();
        });
    }
}