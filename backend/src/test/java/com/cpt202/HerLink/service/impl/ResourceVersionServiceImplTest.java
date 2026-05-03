package com.cpt202.HerLink.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cpt202.HerLink.exception.AppException;

public class ResourceVersionServiceImplTest {

    private ResourceVersionServiceImpl service;

    @BeforeEach
    void setUp() {
        // 只初始化，mapper 传 null
        service = new ResourceVersionServiceImpl(
                null, null, null, null, null,
                null, null, null
        );
    }

    // ========================== PUBLIC 方法中能测的：参数异常 / 边界 ==========================

    @Test
    void saveVersionSnapshot_ResourceIdNull_ShouldThrow() {
        AppException ex = assertThrows(AppException.class, () -> {
            service.saveVersionSnapshot(null, 1L, "edit", "test");
        });
        assertEquals("Resource id is required.", ex.getMessage());
    }

    @Test
    void saveVersionSnapshot_UserIdNull_ShouldThrow() {
        AppException ex = assertThrows(AppException.class, () -> {
            service.saveVersionSnapshot(1L, null, "edit", "test");
        });
        assertEquals("Current user id is required.", ex.getMessage());
    }

    @Test
    void saveVersionSnapshot_ChangeTypeBlank_ShouldThrow() {
        AppException ex = assertThrows(AppException.class, () -> {
            service.saveVersionSnapshot(1L, 1L, null, "test");
        });
        assertEquals("Change type is required.", ex.getMessage());
    }

    // ========================== 以下 PUBLIC 方法依赖 mapper → 单元测试无法运行 ==========================

    @Test
    void listVersions_UnableToTestInUnitTest() {
        // 依赖数据库，无法测试
    }

    @Test
    void getVersion_UnableToTestInUnitTest() {
        // 依赖数据库，无法测试
    }

    @Test
    void compareVersions_UnableToTestInUnitTest() {
        // 依赖数据库，无法测试
    }

    @Test
    void rollbackToVersion_UnableToTestInUnitTest() {
        // 依赖数据库，无法测试
    }
}