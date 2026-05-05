package com.cpt202.HerLink.service.admin;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cpt202.HerLink.dto.admin.AdminCategoryRequest;
import com.cpt202.HerLink.dto.admin.AdminCategoryResponse;
import com.cpt202.HerLink.dto.admin.AdminResourceTypeRequest;
import com.cpt202.HerLink.dto.admin.AdminTagRequest;
import com.cpt202.HerLink.dto.admin.ClassificationStatus;
import com.cpt202.HerLink.exception.AppException;

public class AdminClassificationManagementServiceImplTest {

    private AdminClassificationManagementServiceImpl service;

    // ==================== 测试生命周期 ====================
    @BeforeAll
    static void beforeAll() {
        System.out.println("=== 开始分类管理模块全量测试 ===");
    }

    @BeforeEach
    void setUp() {
        // 模拟注入（测试环境，无真实数据库依赖）
        service = new AdminClassificationManagementServiceImpl(
                null, null, null, null
        );
    }

    @AfterEach
    void tearDown() {
        System.out.println("单条测试方法执行完成");
    }

    // ==================== 1. Category 测试 ====================
    // === Create ===
    @Test
    void createCategory_WithValidData_ShouldSucceed() {
        var req = new AdminCategoryRequest("Valid Topic");
        var res = service.createCategory(req, "admin");
        assertNotNull(res);
    }

    @Test
    void createCategory_WithMaxLengthName_ShouldSucceed() {
        String maxName = "A".repeat(50);
        var req = new AdminCategoryRequest(maxName);
        var res = service.createCategory(req, "admin");
        assertNotNull(res);
    }

    @Test
    void createCategory_WithOverMaxLength_ShouldThrow() {
        String longName = "A".repeat(51);
        var req = new AdminCategoryRequest(longName);
        assertThrows(AppException.class, () -> service.createCategory(req, "admin"));
    }

    @Test
    void createCategory_WithBlankName_ShouldThrow() {
        var req = new AdminCategoryRequest("");
        assertThrows(AppException.class, () -> service.createCategory(req, "admin"));
    }

    @Test
    void createCategory_WithNullRequest_ShouldThrow() {
        assertThrows(AppException.class, () -> service.createCategory(null, "admin"));
    }

    // === Update ===
    @Test
    void updateCategory_WithValidData_ShouldSucceed() {
        var req = new AdminCategoryRequest("Updated Topic");
        var res = service.updateCategory(1L, req, "admin");
        assertNotNull(res);
    }

    @Test
    void updateCategory_WithMaxLengthName_ShouldSucceed() {
        String maxName = "A".repeat(50);
        var req = new AdminCategoryRequest(maxName);
        var res = service.updateCategory(1L, req, "admin");
        assertNotNull(res);
    }

    @Test
    void updateCategory_WithOverMaxLength_ShouldThrow() {
        String longName = "A".repeat(51);
        var req = new AdminCategoryRequest(longName);
        assertThrows(AppException.class, () -> service.updateCategory(1L, req, "admin"));
    }

    @Test
    void updateCategory_WithNullId_ShouldThrow() {
        var req = new AdminCategoryRequest("Test");
        assertThrows(AppException.class, () -> service.updateCategory(null, req, "admin"));
    }

    // ==================== 【新增】ID 不存在测试 ====================
    @Test
    void updateCategory_WithNonExistingId_ShouldThrowNotFound() {
        var req = new AdminCategoryRequest("Valid Name");
        assertThrows(AppException.class, () -> service.updateCategory(9999L, req, "admin"));
    }

    // === Activate / Deactivate ===
    @Test
    void activateCategory_WithValidId_ShouldReturnActiveStatus() {
        AdminCategoryResponse response = service.activateCategory(1L, "admin");
        assertNotNull(response);
        assertEquals(ClassificationStatus.ACTIVE, response.status());
    }

    @Test
    void deactivateCategory_WithValidId_ShouldReturnInactiveStatus() {
        AdminCategoryResponse response = service.deactivateCategory(1L, "admin");
        assertNotNull(response);
        assertEquals(ClassificationStatus.INACTIVE, response.status());
    }

    @Test
    void activateCategory_WithNullId_ShouldThrow() {
        assertThrows(AppException.class, () -> service.activateCategory(null, "admin"));
    }

    // ==================== 【新增】ID 不存在测试 ====================
    @Test
    void activateCategory_WithNonExistingId_ShouldThrowNotFound() {
        assertThrows(AppException.class, () -> service.activateCategory(9999L, "admin"));
    }

    @Test
    void deactivateCategory_WithNonExistingId_ShouldThrowNotFound() {
        assertThrows(AppException.class, () -> service.deactivateCategory(9999L, "admin"));
    }

    // ==================== 2. Tag 测试 ====================
    // === Create ===
    @Test
    void createTag_WithValidData_ShouldSucceed() {
        var req = new AdminTagRequest("Valid Tag");
        var res = service.createTag(req, "admin");
        assertNotNull(res);
    }

    @Test
    void createTag_WithMaxLengthName_ShouldSucceed() {
        String maxName = "A".repeat(100);
        var req = new AdminTagRequest(maxName);
        var res = service.createTag(req, "admin");
        assertNotNull(res);
    }

    @Test
    void createTag_WithOverMaxLength_ShouldThrow() {
        String longName = "A".repeat(101);
        var req = new AdminTagRequest(longName);
        assertThrows(AppException.class, () -> service.createTag(req, "admin"));
    }

    @Test
    void createTag_WithBlankName_ShouldThrow() {
        var req = new AdminTagRequest("");
        assertThrows(AppException.class, () -> service.createTag(req, "admin"));
    }

    // === Update ===
    @Test
    void updateTag_WithValidData_ShouldSucceed() {
        var req = new AdminTagRequest("Updated Tag");
        var res = service.updateTag(1L, req, "admin");
        assertNotNull(res);
    }

    @Test
    void updateTag_WithNullId_ShouldThrow() {
        var req = new AdminTagRequest("Test");
        assertThrows(AppException.class, () -> service.updateTag(null, req, "admin"));
    }

    // ==================== 【新增】ID 不存在测试 ====================
    @Test
    void updateTag_WithNonExistingId_ShouldThrowNotFound() {
        var req = new AdminTagRequest("Valid Name");
        assertThrows(AppException.class, () -> service.updateTag(9999L, req, "admin"));
    }

    // === Activate / Deactivate ===
    @Test
    void activateTag_WithValidId_ShouldSucceed() {
        var res = service.activateTag(1L, "admin");
        assertNotNull(res);
        assertEquals(ClassificationStatus.ACTIVE, res.status());
    }

    @Test
    void deactivateTag_WithValidId_ShouldSucceed() {
        var res = service.deactivateTag(1L, "admin");
        assertNotNull(res);
        assertEquals(ClassificationStatus.INACTIVE, res.status());
    }

    // ==================== 【新增】Tag ID = null 测试 ====================
    @Test
    void activateTag_WithNullId_ShouldThrow() {
        assertThrows(AppException.class, () -> service.activateTag(null, "admin"));
    }

    @Test
    void deactivateTag_WithNullId_ShouldThrow() {
        assertThrows(AppException.class, () -> service.deactivateTag(null, "admin"));
    }

    // ==================== 【新增】ID 不存在测试 ====================
    @Test
    void activateTag_WithNonExistingId_ShouldThrowNotFound() {
        assertThrows(AppException.class, () -> service.activateTag(9999L, "admin"));
    }

    @Test
    void deactivateTag_WithNonExistingId_ShouldThrowNotFound() {
        assertThrows(AppException.class, () -> service.deactivateTag(9999L, "admin"));
    }

    // ==================== 3. ResourceType 测试 ====================
    // === Create ===
    @Test
    void createResourceType_WithValidData_ShouldSucceed() {
        var req = new AdminResourceTypeRequest("Valid Type");
        var res = service.createResourceType(req, "admin");
        assertNotNull(res);
    }

    @Test
    void createResourceType_WithMaxLengthName_ShouldSucceed() {
        String maxName = "A".repeat(50);
        var req = new AdminResourceTypeRequest(maxName);
        var res = service.createResourceType(req, "admin");
        assertNotNull(res);
    }

    @Test
    void createResourceType_WithOverMaxLength_ShouldThrow() {
        String longName = "A".repeat(51);
        var req = new AdminResourceTypeRequest(longName);
        assertThrows(AppException.class, () -> service.createResourceType(req, "admin"));
    }

    // === Update ===
    @Test
    void updateResourceType_WithValidData_ShouldSucceed() {
        var req = new AdminResourceTypeRequest("Updated Type");
        var res = service.updateResourceType(1L, req, "admin");
        assertNotNull(res);
    }

    @Test
    void updateResourceType_WithNullId_ShouldThrow() {
        var req = new AdminResourceTypeRequest("Test");
        assertThrows(AppException.class, () -> service.updateResourceType(null, req, "admin"));
    }

    // ==================== 【新增】ID 不存在测试 ====================
    @Test
    void updateResourceType_WithNonExistingId_ShouldThrowNotFound() {
        var req = new AdminResourceTypeRequest("Valid Name");
        assertThrows(AppException.class, () -> service.updateResourceType(9999L, req, "admin"));
    }

    // === Activate / Deactivate ===
    @Test
    void activateResourceType_WithValidId_ShouldSucceed() {
        var res = service.activateResourceType(1L, "admin");
        assertNotNull(res);
        assertEquals(ClassificationStatus.ACTIVE, res.status());
    }

    @Test
    void deactivateResourceType_WithValidId_ShouldSucceed() {
        var res = service.deactivateResourceType(1L, "admin");
        assertNotNull(res);
        assertEquals(ClassificationStatus.INACTIVE, res.status());
    }

    // ==================== 【新增】ResourceType ID = null 测试 ====================
    @Test
    void activateResourceType_WithNullId_ShouldThrow() {
        assertThrows(AppException.class, () -> service.activateResourceType(null, "admin"));
    }

    @Test
    void deactivateResourceType_WithNullId_ShouldThrow() {
        assertThrows(AppException.class, () -> service.deactivateResourceType(null, "admin"));
    }

    // ==================== 【新增】ID 不存在测试 ====================
    @Test
    void activateResourceType_WithNonExistingId_ShouldThrowNotFound() {
        assertThrows(AppException.class, () -> service.activateResourceType(9999L, "admin"));
    }

    @Test
    void deactivateResourceType_WithNonExistingId_ShouldThrowNotFound() {
        assertThrows(AppException.class, () -> service.deactivateResourceType(9999L, "admin"));
    }
}