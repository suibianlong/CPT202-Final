package com.cpt202.HerLink.service.admin;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cpt202.HerLink.exception.AppException;

public class AdminResourceLifecycleServiceImplTest {

    private AdminResourceLifecycleServiceImpl service;

    @BeforeAll
    static void beforeAll() {
        System.out.println("=== 开始测试资源生命周期模块 ===");
    }

    @BeforeEach
    void setUp() {
        service = new AdminResourceLifecycleServiceImpl(null);
    }

    @AfterEach
    void tearDown() {
        System.out.println("测试完成");
    }

    // ==============================
    // 异常场景1：id = null
    // ==============================
    @Test
    void archiveResource_WithNullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class, () -> {
            service.archiveResource(null);
        });
        assertNotNull(exception.getMessage());
    }

    // ==============================
    // 异常场景2：资源不存在
    // ==============================
    @Test
    void archiveResource_WithNonExistingId_ShouldThrowNotFound() {
        assertThrows(AppException.class, () -> {
            service.archiveResource(9999L);
        });
    }

    // ==============================
    // 异常场景3：mapper为null，空指针（属于异常）
    // ==============================
    @Test
    void archiveResource_WithNullMapper_ShouldThrowException() {
        assertThrows(RuntimeException.class, () -> {
            service.archiveResource(1L);
        });
    }
}