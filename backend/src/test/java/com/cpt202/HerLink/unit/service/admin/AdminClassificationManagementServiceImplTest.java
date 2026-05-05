package com.cpt202.HerLink.unit.service.admin;

import com.cpt202.HerLink.service.admin.*;
import com.cpt202.HerLink.service.admin.AdminClassificationManagementServiceImpl;
import com.cpt202.HerLink.service.admin.AdminOperationHistoryService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cpt202.HerLink.dto.admin.AdminCategoryRequest;
import com.cpt202.HerLink.dto.admin.AdminCategoryResponse;
import com.cpt202.HerLink.dto.admin.AdminResourceTypeRequest;
import com.cpt202.HerLink.dto.admin.AdminResourceTypeResponse;
import com.cpt202.HerLink.dto.admin.AdminTagRequest;
import com.cpt202.HerLink.dto.admin.AdminTagResponse;
import com.cpt202.HerLink.dto.admin.ClassificationStatus;
import com.cpt202.HerLink.entity.Category;
import com.cpt202.HerLink.entity.ResourceType;
import com.cpt202.HerLink.entity.Tag;
import com.cpt202.HerLink.enums.ResourceTypeEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.mapper.TagMapper;

@ExtendWith(MockitoExtension.class)
class AdminClassificationManagementServiceImplTest {

    private static final int MAX_CATEGORY_TOPIC_LENGTH = 50;
    private static final int MAX_RESOURCE_TYPE_LENGTH = 50;
    private static final int MAX_TAG_NAME_LENGTH = 100;
    private static final String ADMIN = "test_admin";
    private static final Long EXIST_ID = 1L;
    private static final Long NOT_EXIST_ID = 999L;

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private ResourceTypeMapper resourceTypeMapper;
    @Mock
    private AdminOperationHistoryService operationHistoryService;

    @InjectMocks
    private AdminClassificationManagementServiceImpl service;

    private Category testCategory;
    private Tag testTag;
    private ResourceType testResourceType;
    private AdminCategoryRequest testCategoryRequest;
    private AdminTagRequest testTagRequest;
    private AdminResourceTypeRequest testResourceTypeRequest;

    @BeforeEach
    void setUp() {
        // 初始化测试分类
        testCategory = new Category();
        testCategory.setCategoryId(EXIST_ID);
        testCategory.setCategoryTopic("test category");
        testCategory.setStatus(ClassificationStatus.ACTIVE.name());
        testCategory.setUsageCount(0);
        testCategory.setCreatedAt(LocalDateTime.now());
        testCategory.setLastUpdatedAt(LocalDateTime.now());

        // 初始化测试标签
        testTag = new Tag();
        testTag.setTagId(EXIST_ID);
        testTag.setTagName("test tag");
        testTag.setStatus(ClassificationStatus.ACTIVE.name());
        testTag.setUsageCount(0);
        testTag.setCreatedAt(LocalDateTime.now());
        testTag.setLastUpdatedAt(LocalDateTime.now());

        // 初始化测试资源类型
        testResourceType = new ResourceType();
        testResourceType.setResourceTypeId(EXIST_ID);
        testResourceType.setTypeName(ResourceTypeEnum.DOCUMENT.name());
        testResourceType.setStatus(ClassificationStatus.ACTIVE.name());
        testResourceType.setUsageCount(0);
        testResourceType.setCreatedAt(LocalDateTime.now());
        testResourceType.setLastUpdatedAt(LocalDateTime.now());

        // 初始化请求DTO
        testCategoryRequest = new AdminCategoryRequest("test category");
        testTagRequest = new AdminTagRequest("test tag");
        testResourceTypeRequest = new AdminResourceTypeRequest(ResourceTypeEnum.DOCUMENT.name());
    }

    // ======================== 分类（Category）测试 ========================
    @Test
    @DisplayName("get All Categories Should Return Correct List")
    void getAllCategories_ShouldReturnCorrectList() {
        when(categoryMapper.selectAllCategories()).thenReturn(List.of(testCategory));
        List<AdminCategoryResponse> result = service.getAllCategories();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(testCategory.getCategoryTopic(), result.get(0).categoryTopic());
    }

    @Test
    @DisplayName("get Active Categories Should Return Only Active()")
    void getActiveCategories_ShouldReturnOnlyActive() {
        when(categoryMapper.selectByStatus(ClassificationStatus.ACTIVE.name())).thenReturn(List.of(testCategory));
        List<AdminCategoryResponse> result = service.getActiveCategories();
        assertEquals(ClassificationStatus.ACTIVE, result.get(0).status());
    }

    @Test
    @DisplayName("create Category ValidData Should Create Success")
    void createCategory_ValidData_ShouldCreateSuccess() {
        when(categoryMapper.countByTopicIgnoreCase(anyString(), isNull())).thenReturn(0);
        when(resourceTypeMapper.countByTypeNameIgnoreCase(anyString(), isNull())).thenReturn(0);

        // 核心修复：insert时手动设置ID，否则ID为null会报错
        when(categoryMapper.insert(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setCategoryId(EXIST_ID); // 手动赋ID
            return 1;
        });

        when(categoryMapper.selectById(EXIST_ID)).thenReturn(testCategory);

        AdminCategoryResponse result = service.createCategory(testCategoryRequest, ADMIN);
        assertNotNull(result);
        assertEquals("test category", result.categoryTopic());
        assertEquals(ClassificationStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("create Category Blank Topic Should Throw Bad Request")
    void createCategory_BlankTopic_ShouldThrowBadRequest() {
        AdminCategoryRequest request = new AdminCategoryRequest("    ");
        AppException exception = assertThrows(AppException.class, () -> service.createCategory(request, ADMIN));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    @DisplayName("create Category just Over Max Length Should Throw Bad Request")
    void createCategory_OverMaxLength_ShouldThrowBadRequest() {
        String longName = "a".repeat(MAX_CATEGORY_TOPIC_LENGTH + 1);
        AdminCategoryRequest request = new AdminCategoryRequest(longName);
        AppException exception = assertThrows(AppException.class, () -> service.createCategory(request, ADMIN));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    @DisplayName("create Category Duplicate Topic Should Throw Conflict")
    void createCategory_DuplicateTopic_ShouldThrowConflict() {
        when(categoryMapper.countByTopicIgnoreCase(anyString(), isNull())).thenReturn(1);
        AppException exception = assertThrows(AppException.class, () -> service.createCategory(testCategoryRequest, ADMIN));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("update Category Valid Data Should Update Success")
    void updateCategory_ValidData_ShouldUpdateSuccess() {
        when(categoryMapper.selectById(EXIST_ID)).thenReturn(testCategory);
        when(categoryMapper.countByTopicIgnoreCase(anyString(), eq(EXIST_ID))).thenReturn(0);
        when(resourceTypeMapper.countByTypeNameIgnoreCase(anyString(), isNull())).thenReturn(0);
        when(categoryMapper.updateTopic(anyLong(), anyString(), any())).thenReturn(1);
        when(categoryMapper.selectById(EXIST_ID)).thenReturn(testCategory);

        AdminCategoryResponse result = service.updateCategory(EXIST_ID, testCategoryRequest, ADMIN);
        assertNotNull(result);
        assertEquals(EXIST_ID, result.categoryId());
    }

    @Test
    @DisplayName("update Category Not Exist Id Should Throw Not Found")
    void updateCategory_NotExistId_ShouldThrowNotFound() {
        when(categoryMapper.selectById(NOT_EXIST_ID)).thenReturn(null);
        AppException exception = assertThrows(AppException.class,
                () -> service.updateCategory(NOT_EXIST_ID, testCategoryRequest, ADMIN));
        assertTrue(exception.getMessage().contains("Category does not exist"));
    }

    @Test
    @DisplayName("update Category Null Id Should Throw Bad Request")
    void updateCategory_NullId_ShouldThrowBadRequest() {
    // 传入 null 作为 ID
        AppException exception = assertThrows(AppException.class,
                () -> service.updateCategory(null, testCategoryRequest, ADMIN));
    
    // 断言抛出正确的异常信息
        assertTrue(exception.getMessage().contains("Category id is required"));
    }

    @Test
    @DisplayName("deactivate Category Active To Inactive Should Change Status")
    void deactivateCategory_ActiveToInactive_ShouldChangeStatus() {
        when(categoryMapper.selectById(EXIST_ID)).thenReturn(testCategory);
        Category inactive = new Category();
        inactive.setCategoryId(EXIST_ID);
        inactive.setStatus(ClassificationStatus.INACTIVE.name());
        when(categoryMapper.selectById(EXIST_ID)).thenReturn(inactive);

        AdminCategoryResponse result = service.deactivateCategory(EXIST_ID, ADMIN);
        assertEquals(ClassificationStatus.INACTIVE, result.status());
    }

    @Test
    @DisplayName("deactivate Category Already Inactive No Change")
    void deactivateCategory_AlreadyInactive_NoChange() {
        testCategory.setStatus(ClassificationStatus.INACTIVE.name());
        when(categoryMapper.selectById(EXIST_ID)).thenReturn(testCategory);

        AdminCategoryResponse result = service.deactivateCategory(EXIST_ID, ADMIN);
        assertEquals(ClassificationStatus.INACTIVE, result.status());
    }

    @Test
    @DisplayName("deactivate Category Null Id Should Throw Bad Request")
    void deactivateCategory_NullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.deactivateCategory(null, ADMIN));
        assertTrue(exception.getMessage().contains("Category id is required"));
    }

    @Test
    @DisplayName("deactivate Category Not Exist Id Should Throw NotFound")
    void deactivateCategory_NotExistId_ShouldThrowNotFound() {
        when(categoryMapper.selectById(NOT_EXIST_ID)).thenReturn(null);
        AppException exception = assertThrows(AppException.class,
                () -> service.deactivateCategory(NOT_EXIST_ID, ADMIN));
        assertTrue(exception.getMessage().contains("Category does not exist"));
    }

    @Test
    @DisplayName("activate Category Inactive To Active Should Change Status")
    void activateCategory_InactiveToActive_ShouldChangeStatus() {
        // 1. 设置初始状态为 INACTIVE
        testCategory.setStatus(ClassificationStatus.INACTIVE.name());
        when(categoryMapper.selectById(EXIST_ID)).thenReturn(testCategory);

        // 2. mock updateStatus 操作

        // 3. 关键：mock 更新后的查询结果，返回一个状态为 ACTIVE 的对象
        Category updatedCategory = new Category();
        updatedCategory.setCategoryId(EXIST_ID);
        updatedCategory.setCategoryTopic(testCategory.getCategoryTopic());
        updatedCategory.setStatus(ClassificationStatus.ACTIVE.name());
        when(categoryMapper.selectById(EXIST_ID)).thenReturn(updatedCategory);

        AdminCategoryResponse result = service.activateCategory(EXIST_ID, ADMIN);
        assertEquals(ClassificationStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("activate Category Already Active Should No Change")
    void activateCategory_AlreadyActive_ShouldNoChange() {
        // 原本就是激活状态
        testCategory.setStatus(ClassificationStatus.ACTIVE.name());
        when(categoryMapper.selectById(EXIST_ID)).thenReturn(testCategory);

        AdminCategoryResponse result = service.activateCategory(EXIST_ID, ADMIN);
        assertEquals(ClassificationStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("activate Category Null Id Should Throw Bad Request")
    void activateCategory_NullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.activateCategory(null, ADMIN));
        assertTrue(exception.getMessage().contains("Category id is required"));
    }

    @Test
    @DisplayName("activate Category Not Exist Id Should Throw NotFound")
    void activateCategory_NotExistId_ShouldThrowNotFound() {
        when(categoryMapper.selectById(NOT_EXIST_ID)).thenReturn(null);
        AppException exception = assertThrows(AppException.class,
                () -> service.activateCategory(NOT_EXIST_ID, ADMIN));
        assertTrue(exception.getMessage().contains("Category does not exist"));
    }
    // ======================== 标签（Tag）测试 ========================
    @Test
    @DisplayName("get All Tags Should Return Correct List")
    void getAllTags_ShouldReturnCorrectList() {
        when(tagMapper.selectAllTags()).thenReturn(List.of(testTag));
        List<AdminTagResponse> result = service.getAllTags();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("get Active Tags Should Return Only Active")
    void getActiveTags_ShouldReturnOnlyActive() {
        when(tagMapper.selectByStatus(ClassificationStatus.ACTIVE.name())).thenReturn(List.of(testTag));
        List<AdminTagResponse> result = service.getActiveTags();
        assertEquals(ClassificationStatus.ACTIVE, result.get(0).status());
    }

    @Test
    @DisplayName("create Tag Valid Data Should Create Success")
    void createTag_ValidData_ShouldCreateSuccess() {
        // 1. 模拟唯一校验
        when(tagMapper.countByNameIgnoreCase(anyString(), isNull())).thenReturn(0);

        // 2. insert时，手动给实体设置ID（修复核心！）
        when(tagMapper.insert(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setTagId(EXIST_ID); // 手动设置ID，否则为null
            return 1;
        });

        // 3. 根据ID查询返回模拟对象
        when(tagMapper.selectById(EXIST_ID)).thenReturn(testTag);

        // 执行
        AdminTagResponse result = service.createTag(testTagRequest, ADMIN);

        // 断言
        assertNotNull(result);
        assertEquals("test tag", result.tagName());
    }

    @Test
    @DisplayName("create Tag Blank Name Should Throw Bad Request")
    void createTag_BlankName_ShouldThrowBadRequest() {
        AdminTagRequest request = new AdminTagRequest("");
        AppException exception = assertThrows(AppException.class, () -> service.createTag(request, ADMIN));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    @DisplayName("create Tag Over Maximum Length Should Throw Bad Request")
    void createTag_OverMaxLength_ShouldThrowBadRequest() {
        String longName = "a".repeat(MAX_TAG_NAME_LENGTH + 1);
        AdminTagRequest request = new AdminTagRequest(longName);
        AppException exception = assertThrows(AppException.class, () -> service.createTag(request, ADMIN));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    @DisplayName("create Tag Duplicate Name Should Throw Conflict")
    void createTag_DuplicateName_ShouldThrowConflict() {
        when(tagMapper.countByNameIgnoreCase(anyString(), isNull())).thenReturn(1);
        AppException exception = assertThrows(AppException.class,
                () -> service.createTag(testTagRequest, ADMIN));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("update Tag Not Exist Id Should Throw NotFound")
    void updateTag_NotExistId_ShouldThrowNotFound() {
        when(tagMapper.selectById(NOT_EXIST_ID)).thenReturn(null);
        AppException exception = assertThrows(AppException.class,
                () -> service.updateTag(NOT_EXIST_ID, testTagRequest, ADMIN));
        assertTrue(exception.getMessage().contains("Tag does not exist"));
    }

    @Test
    @DisplayName("update Tag Valid Data Should Update Success")
    void updateTag_ValidData_ShouldUpdateSuccess() {
        when(tagMapper.selectById(EXIST_ID)).thenReturn(testTag);
        when(tagMapper.countByNameIgnoreCase(anyString(), eq(EXIST_ID))).thenReturn(0);
        when(tagMapper.updateTagName(anyLong(), anyString(), any())).thenReturn(1);
        when(tagMapper.selectById(EXIST_ID)).thenReturn(testTag);

        AdminTagResponse result = service.updateTag(EXIST_ID, testTagRequest, ADMIN);
        assertNotNull(result);
        assertEquals(EXIST_ID, result.tagId());
    }

    // updateTag - ID 为 null
    @Test
    @DisplayName("update Tag Null Id Should Throw Bad Request")
    void updateTag_NullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.updateTag(null, testTagRequest, ADMIN));
        assertTrue(exception.getMessage().contains("Tag id is required"));
    }

    @Test
    @DisplayName("deactivate Tag Already Inactive No Change")
    void deactivateTag_AlreadyInactive_NoChange() {
        testTag.setStatus(ClassificationStatus.INACTIVE.name());
        when(tagMapper.selectById(EXIST_ID)).thenReturn(testTag);

        AdminTagResponse result = service.deactivateTag(EXIST_ID, ADMIN);
        assertEquals(ClassificationStatus.INACTIVE, result.status());
    }

    @Test
    @DisplayName("deactivate Tag Normal Should Change Status")
    void deactivateTag_Normal_ShouldChangeStatus() {
        when(tagMapper.selectById(EXIST_ID)).thenReturn(testTag);
        Tag inactive = new Tag();
        inactive.setTagId(EXIST_ID);
        inactive.setStatus(ClassificationStatus.INACTIVE.name());
        when(tagMapper.selectById(EXIST_ID)).thenReturn(inactive);

        AdminTagResponse result = service.deactivateTag(EXIST_ID, ADMIN);
        assertEquals(ClassificationStatus.INACTIVE, result.status());
    }

    // deactivateTag - Null ID
    @Test
    @DisplayName("deactivate Tag Null Id Should Throw Bad Request")
    void deactivateTag_NullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.deactivateTag(null, ADMIN));
        assertTrue(exception.getMessage().contains("Tag id is required"));
    }

    // deactivateTag - 不存在 ID
    @Test
    @DisplayName("deactivate Tag Not Exist Id Should Throw NotFound")
    void deactivateTag_NotExistId_ShouldThrowNotFound() {
        when(tagMapper.selectById(NOT_EXIST_ID)).thenReturn(null);
        AppException exception = assertThrows(AppException.class,
                () -> service.deactivateTag(NOT_EXIST_ID, ADMIN));
        assertTrue(exception.getMessage().contains("Tag does not exist"));
    }

    @Test
    @DisplayName("activate Tag Normal Should Change Status")
    void activateTag_Normal_ShouldChangeStatus() {
        // 1. 设置初始状态为 INACTIVE
        testTag.setStatus(ClassificationStatus.INACTIVE.name());
        when(tagMapper.selectById(EXIST_ID)).thenReturn(testTag);

        // 2. 关键：创建一个状态为 ACTIVE 的新对象，模拟更新后的数据库状态
        Tag activeTag = new Tag();
        activeTag.setTagId(EXIST_ID);
        activeTag.setStatus(ClassificationStatus.ACTIVE.name());
        when(tagMapper.selectById(EXIST_ID)).thenReturn(activeTag);

        // 执行
        AdminTagResponse result = service.activateTag(EXIST_ID, ADMIN);
        
        // 断言
        assertEquals(ClassificationStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("activate Tag Already Active Should No Change")
    void activateTag_AlreadyActive_ShouldNoChange() {
        when(tagMapper.selectById(EXIST_ID)).thenReturn(testTag);
        AdminTagResponse result = service.activateTag(EXIST_ID, ADMIN);
        assertEquals(ClassificationStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("activate Tag Null Id Should Throw Bad Request")
    void activateTag_NullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.activateTag(null, ADMIN));
        assertTrue(exception.getMessage().contains("Tag id is required"));
    }

    @Test
    @DisplayName("activate Tag Not Exist Id Should Throw NotFound")
    void activateTag_NotExistId_ShouldThrowNotFound() {
        when(tagMapper.selectById(NOT_EXIST_ID)).thenReturn(null);
        AppException exception = assertThrows(AppException.class,
                () -> service.activateTag(NOT_EXIST_ID, ADMIN));
        assertTrue(exception.getMessage().contains("Tag does not exist"));
    }

    // ======================== 资源类型（ResourceType）测试 ========================
    @Test
    @DisplayName("get All Resource Types Should Return Correct List")
    void getAllResourceTypes_ShouldReturnCorrectList() {
        when(resourceTypeMapper.selectAllResourceTypes()).thenReturn(List.of(testResourceType));
        List<AdminResourceTypeResponse> result = service.getAllResourceTypes();
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("get Active Resource Types Should Return Only Active")
    void getActiveResourceTypes_ShouldReturnOnlyActive() {
        when(resourceTypeMapper.selectByStatus(ClassificationStatus.ACTIVE.name())).thenReturn(List.of(testResourceType));
        List<AdminResourceTypeResponse> result = service.getActiveResourceTypes();
        assertEquals(ClassificationStatus.ACTIVE, result.get(0).status());
    }

    @Test
    @DisplayName("create Resource Type Valid Data Should Create Success")
    void createResourceType_ValidData_ShouldCreateSuccess() {
        when(resourceTypeMapper.countByTypeNameIgnoreCase(anyString(), isNull())).thenReturn(0);
        when(categoryMapper.countByTopicIgnoreCase(anyString(), isNull())).thenReturn(0);

        // 核心修复：插入时手动设置 ID，否则为 null 会报错
        when(resourceTypeMapper.insert(any(ResourceType.class))).thenAnswer(invocation -> {
            ResourceType rt = invocation.getArgument(0);
            rt.setResourceTypeId(EXIST_ID); // 手动给ID
            return 1;
        });

        when(resourceTypeMapper.selectById(EXIST_ID)).thenReturn(testResourceType);

        AdminResourceTypeResponse result = service.createResourceType(testResourceTypeRequest, ADMIN);
        assertNotNull(result);
    }

    @Test
    @DisplayName("create Resource Type Blank Name Should Throw Bad Request")
    void createResourceType_BlankName_ShouldThrowBadRequest() {
        AdminResourceTypeRequest request = new AdminResourceTypeRequest("   ");
        AppException exception = assertThrows(AppException.class,
                () -> service.createResourceType(request, ADMIN));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    // createResourceType - 过长名字
    @Test
    @DisplayName("create Resource Type Over Length Should Throw Bad Request")
    void createResourceType_OverLength_ShouldThrowBadRequest() {
        String longName = "a".repeat(MAX_RESOURCE_TYPE_LENGTH + 1);
        AdminResourceTypeRequest request = new AdminResourceTypeRequest(longName);
        AppException exception = assertThrows(AppException.class,
                () -> service.createResourceType(request, ADMIN));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    // createResourceType - 重复名称
    @Test
    @DisplayName("create Resource Type Duplicate Name Should Throw Conflict")
    void createResourceType_DuplicateName_ShouldThrowConflict() {
        when(resourceTypeMapper.countByTypeNameIgnoreCase(anyString(), isNull())).thenReturn(1);
        AppException exception = assertThrows(AppException.class,
                () -> service.createResourceType(testResourceTypeRequest, ADMIN));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("update Resource Type Used by Resource Invalid Enum Should Throw Conflict")
    void updateResourceType_UsedByResource_InvalidEnum_ShouldThrowConflict() {
        testResourceType.setUsageCount(10);
        when(resourceTypeMapper.selectById(EXIST_ID)).thenReturn(testResourceType);
        AdminResourceTypeRequest request = new AdminResourceTypeRequest("illegal_enum");

        AppException exception = assertThrows(AppException.class,
                () -> service.updateResourceType(EXIST_ID, request, ADMIN));
        assertTrue(exception.getMessage().contains("supported by the current resource metadata flow"));
    }

    @Test
    @DisplayName("update Resource Type Not Exist Id Should Throw NotFound")
    void updateResourceType_NotExistId_ShouldThrowNotFound() {
        when(resourceTypeMapper.selectById(NOT_EXIST_ID)).thenReturn(null);
        AppException exception = assertThrows(AppException.class,
                () -> service.updateResourceType(NOT_EXIST_ID, testResourceTypeRequest, ADMIN));
        assertTrue(exception.getMessage().contains("Resource type does not exist"));
    }

    @Test
    @DisplayName("update Resource Type Valid Data Should Update Success")
    void updateResourceType_ValidData_ShouldUpdateSuccess() {
        when(resourceTypeMapper.selectById(EXIST_ID)).thenReturn(testResourceType);
        when(resourceTypeMapper.countByTypeNameIgnoreCase(anyString(), eq(EXIST_ID))).thenReturn(0);
        when(categoryMapper.countByTopicIgnoreCase(anyString(), isNull())).thenReturn(0);
        when(resourceTypeMapper.updateTypeName(anyLong(), anyString(), any())).thenReturn(1);
        when(resourceTypeMapper.selectById(EXIST_ID)).thenReturn(testResourceType);

        AdminResourceTypeResponse result = service.updateResourceType(EXIST_ID, testResourceTypeRequest, ADMIN);
        assertNotNull(result);
    }

    // updateResourceType - Null ID
    @Test
    @DisplayName("update Resource Type Null Id Should Throw Bad Request")
    void updateResourceType_NullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.updateResourceType(null, testResourceTypeRequest, ADMIN));
        assertTrue(exception.getMessage().contains("Resource type id is required"));
    }

    @Test
    @DisplayName("deactivate Resource Type Valid Should Change Status")
    void deactivateResourceType_Valid_ShouldChangeStatus() {
        when(resourceTypeMapper.selectById(EXIST_ID)).thenReturn(testResourceType);
        ResourceType inactive = new ResourceType();
        inactive.setResourceTypeId(EXIST_ID);
        inactive.setStatus(ClassificationStatus.INACTIVE.name());
        when(resourceTypeMapper.selectById(EXIST_ID)).thenReturn(inactive);

        AdminResourceTypeResponse result = service.deactivateResourceType(EXIST_ID, ADMIN);
        assertEquals(ClassificationStatus.INACTIVE, result.status());
    }

    @Test
    @DisplayName("deactivate Resource Type Already Inactive Should No Change")
    void deactivateResourceType_AlreadyInactive_ShouldNoChange() {
        testResourceType.setStatus(ClassificationStatus.INACTIVE.name());
        when(resourceTypeMapper.selectById(EXIST_ID)).thenReturn(testResourceType);
        AdminResourceTypeResponse result = service.deactivateResourceType(EXIST_ID, ADMIN);
        assertEquals(ClassificationStatus.INACTIVE, result.status());
    }

    // deactivateResourceType - Null ID
    @Test
    @DisplayName("deactivate Resource Type Null Id Should Throw Bad Request")
    void deactivateResourceType_NullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.deactivateResourceType(null, ADMIN));
        assertTrue(exception.getMessage().contains("Resource type id is required"));
    }

    // deactivateResourceType - 不存在 ID
    @Test
    @DisplayName("deactivate Resource Type Not Exist Id Should Throw NotFound")
    void deactivateResourceType_NotExistId_ShouldThrowNotFound() {
        when(resourceTypeMapper.selectById(NOT_EXIST_ID)).thenReturn(null);
        AppException exception = assertThrows(AppException.class,
                () -> service.deactivateResourceType(NOT_EXIST_ID, ADMIN));
        assertTrue(exception.getMessage().contains("Resource type does not exist"));
    }

    // activateResourceType - 完全缺失
    @Test
    @DisplayName("activate Resource Type Normal Should Change Status")
    void activateResourceType_Normal_ShouldChangeStatus() {
        // 1. 设置初始状态为 INACTIVE
        testResourceType.setStatus(ClassificationStatus.INACTIVE.name());
        when(resourceTypeMapper.selectById(EXIST_ID)).thenReturn(testResourceType);

        // 2. 关键：创建一个状态为 ACTIVE 的新对象，模拟更新后的数据库状态
        ResourceType activeResourceType = new ResourceType();
        activeResourceType.setResourceTypeId(EXIST_ID);
        activeResourceType.setStatus(ClassificationStatus.ACTIVE.name());
        when(resourceTypeMapper.selectById(EXIST_ID)).thenReturn(activeResourceType);

        // 执行
        AdminResourceTypeResponse result = service.activateResourceType(EXIST_ID, ADMIN);
        
        // 断言
        assertEquals(ClassificationStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("activate Resource Type Already Active Should No Change")
    void activateResourceType_AlreadyActive_ShouldNoChange() {
        when(resourceTypeMapper.selectById(EXIST_ID)).thenReturn(testResourceType);
        AdminResourceTypeResponse result = service.activateResourceType(EXIST_ID, ADMIN);
        assertEquals(ClassificationStatus.ACTIVE, result.status());
    }

    @Test
    @DisplayName("activate Resource Type Null Id Should Throw Bad Request")
    void activateResourceType_NullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.activateResourceType(null, ADMIN));
        assertTrue(exception.getMessage().contains("Resource type id is required"));
    }

    @Test
    @DisplayName("activate Resource Type Not Exist Id Should Throw NotFound")
    void activateResourceType_NotExistId_ShouldThrowNotFound() {
        when(resourceTypeMapper.selectById(NOT_EXIST_ID)).thenReturn(null);
        AppException exception = assertThrows(AppException.class,
                () -> service.activateResourceType(NOT_EXIST_ID, ADMIN));
        assertTrue(exception.getMessage().contains("Resource type does not exist"));
    }

    // ======================== 空ID通用异常测试 ========================
    @Test
    @DisplayName("operate Category With Null Id Should Throw Bad Request")
    void operateCategory_WithNullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class, () -> service.deactivateCategory(null, ADMIN));
        assertTrue(exception.getMessage().contains("Category id is required"));
    }

    @Test
    @DisplayName("operate Tag With Null Id Should Throw Bad Request")
    void operateTag_WithNullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class, () -> service.deactivateTag(null, ADMIN));
        assertTrue(exception.getMessage().contains("Tag id is required"));
    }

    @Test
    @DisplayName("operate Resource Type With Null Id Should Throw Bad Request")
    void operateResourceType_WithNullId_ShouldThrowBadRequest() {
        AppException exception = assertThrows(AppException.class, () -> service.deactivateResourceType(null, ADMIN));
        assertTrue(exception.getMessage().contains("Resource type id is required"));
    }
}
