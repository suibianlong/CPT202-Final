package com.cpt202.HerLink.service.impl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cpt202.HerLink.dto.resource.ResourceSubmitRequest;
import com.cpt202.HerLink.dto.resource.ResourceUpdateRequest;
import com.cpt202.HerLink.exception.AppException;

public class ContributorResourceServiceImplTest {

    private ContributorResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        // 所有 Mapper 传入 null，只测试参数校验、异常抛出逻辑
        service = new ContributorResourceServiceImpl(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null
        );
    }

    // ==================== 能测的方法：边界 / 异常 / 入参校验 ====================

    /**
     * 测试：资源 ID 为 null → 抛异常
     */
    @Test
    void getMyResourceDetail_ResourceIdNull_ShouldThrow() {
        AppException ex = assertThrows(AppException.class, () -> {
            service.getMyResourceDetail(1000L, null);
        });
        assertNotNull(ex.getMessage());
        assertEquals("Resource does not exist.", ex.getMessage());
    }

    /**
     * 测试：更新请求为 null
     */
    @Test
    void updateResource_RequestNull_ShouldThrow() {
        AppException ex = assertThrows(AppException.class, () -> {
            service.updateResource(1L, 1L, null);
        });
        assertEquals("Update request cannot be null.", ex.getMessage());
    }

    /**
     * 测试：同时传 tagIds 和 tagNames → 异常
     */
    @Test
    void updateResource_BothTagsProvided_ShouldThrow() {
        ResourceUpdateRequest req = new ResourceUpdateRequest();
        req.setTagIds(List.of(1L));
        req.setTagNames(List.of("test"));

        AppException ex = assertThrows(AppException.class, () -> {
            service.updateResource(1L, 1L, req);
        });
        assertEquals("Tag ids and tag names cannot be submitted together.", ex.getMessage());
    }

    /**
     * 测试：两个文件都为空 → 异常
     */
    @Test
    void uploadFiles_NoFilesProvided_ShouldThrow() {
        AppException ex = assertThrows(AppException.class, () -> {
            service.uploadFiles(1L, 1L, null, null);
        });
        assertEquals("At least one file must be uploaded.", ex.getMessage());
    }

    /**
     * 测试：提交资源时，资源未找到
     */
    @Test
    void submitResource_ResourceNotExist_ShouldThrow() {
        ResourceSubmitRequest req = new ResourceSubmitRequest();
        AppException ex = assertThrows(AppException.class, () -> {
            service.submitResource(1L, 9999L, req);
        });
        assertEquals("Resource does not exist.", ex.getMessage());
    }

    // ==================== 以下方法完全依赖数据库，单元测试无法执行 ====================
    // createDraft
    // listMyResources
    // listSubmissionHistory
    // listCategoryOptions
    // listResourceTypeOptions
    // listTagOptions

    @Test
    void createDraft_UnsupportedInUnitTest() {
        // 依赖 Mapper，单元测试无法运行
    }

    @Test
    void listMyResources_UnsupportedInUnitTest() {
        // 依赖 Mapper，单元测试无法运行
    }

    @Test
    void listSubmissionHistory_UnsupportedInUnitTest() {
        // 依赖 Mapper，单元测试无法运行
    }

    @Test
    void listCategoryOptions_UnsupportedInUnitTest() {
        // 依赖 Mapper，单元测试无法运行
    }

    @Test
    void listResourceTypeOptions_UnsupportedInUnitTest() {
        // 依赖 Mapper，单元测试无法运行
    }

    @Test
    void listTagOptions_UnsupportedInUnitTest() {
        // 依赖 Mapper，单元测试无法运行
    }
}