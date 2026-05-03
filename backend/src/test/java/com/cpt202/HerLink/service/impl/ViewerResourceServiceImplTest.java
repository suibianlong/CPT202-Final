package com.cpt202.HerLink.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cpt202.HerLink.exception.AppException;

public class ViewerResourceServiceImplTest {

    private ViewerResourceServiceImpl resourceService;

    @BeforeEach
    void setUp() {
        // 所有 Mapper 传入 null，只测试【纯参数校验】
        resourceService = new ViewerResourceServiceImpl(
                null,  // ResourceMapper
                null,  // CategoryMapper
                null,  // ResourceTagMapper
                null   // ResourceTypeMapper
        );
    }

    // ========================== 能测：排序字段非法、资源ID空值等纯逻辑 ==========================

    /**
     * 测试：非法的 sortBy 参数 → 抛出异常
     * 仅测试参数校验，不依赖数据库
     */
    @Test
    void listApprovedResources_InvalidSortBy_ShouldThrowBadRequest() {
        // 仅传非法排序字段，触发参数校验异常
        AppException exception = assertThrows(AppException.class, () -> {
            resourceService.listApprovedResources(
                    null,
                    null,
                    null,
                    "invalid-sort-value"
            );
        });
        assertEquals("Unsupported sort option.", exception.getMessage());
    }

    /**
     * 测试：资源 ID 为 null → 抛出异常
     */
    @Test
    void getApprovedResourceDetail_ResourceIdNull_ShouldThrow() {
        AppException exception = assertThrows(AppException.class, () ->
                resourceService.getApprovedResourceDetail(null)
        );
        assertEquals("Approved resource does not exist.", exception.getMessage());
    }

    // ========================== 以下 PUBLIC 方法 100% 依赖数据库 → 单元测试无法运行 ==========================
    // 一运行就调用 mapper → null → 空指针异常，必须留到【集成测试】

    @Test
    void listApprovedResources_QueryAndPersistence_UnTestable() {
        // 依赖资源查询 → 无法测试
    }

    @Test
    void listCategoryOptions_UnTestable_InUnitTest() {
        // 依赖 categoryMapper → 无法测试
    }

    @Test
    void listResourceTypeOptions_UnTestable_InUnitTest() {
        // 依赖 resourceTypeMapper → 无法测试
    }
}