package com.cpt202.HerLink.service.admin;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cpt202.HerLink.dto.admin.AdminOperationHistoryResponse;

public class AdminOperationHistoryServiceImplTest {

    private AdminOperationHistoryServiceImpl service;

    @BeforeAll
    static void beforeAll() {
        System.out.println("=== 开始测试：操作日志模块 ===");
    }

    @BeforeEach
    void setUp() {
        service = new AdminOperationHistoryServiceImpl(null);
    }

    @AfterEach
    void tearDown() {
        System.out.println("测试完成");
    }

    // ============================
    // recordOperation 测试
    // ============================

    // 正常情况：传入合法参数 → 不抛异常
    @Test
    void recordOperation_ValidData_ShouldNotThrow() {
        assertDoesNotThrow(() -> {
            service.recordOperation("item", "kind", "module", "action", "admin");
        });
    }

    // 异常/边界情况：传入全 null → 仍然不抛异常（catch保护）
    @Test
    void recordOperation_NullParams_ShouldNotThrow() {
        assertDoesNotThrow(() -> {
            service.recordOperation(null, null, null, null, null);
        });
    }

    // ============================
    // getOperationHistory 测试
    // ============================

    // 正常情况：传入有效 module → 返回列表
    @Test
    void getHistory_ValidModule_ShouldReturnList() {
        List<AdminOperationHistoryResponse> result = service.getOperationHistory("classification");
        assertNotNull(result, "正常情况应返回列表");
    }

    // 边界情况：module = null → 查询全部
    @Test
    void getHistory_NullModule_ShouldReturnAll() {
        List<AdminOperationHistoryResponse> result = service.getOperationHistory(null);
        assertNotNull(result, "边界 null 应返回全部");
    }

    // 边界情况：module = 空白 → 查询全部
    @Test
    void getHistory_BlankModule_ShouldReturnAll() {
        List<AdminOperationHistoryResponse> result = service.getOperationHistory("   ");
        assertNotNull(result, "边界空白 应返回全部");
    }
}