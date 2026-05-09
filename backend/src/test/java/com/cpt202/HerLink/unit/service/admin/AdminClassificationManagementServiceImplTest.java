package com.cpt202.HerLink.unit.service.admin;

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
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.CategoryMapper;
import com.cpt202.HerLink.mapper.ResourceTypeMapper;
import com.cpt202.HerLink.mapper.TagMapper;
import com.cpt202.HerLink.service.admin.AdminClassificationManagementServiceImpl;
import com.cpt202.HerLink.service.admin.AdminOperationHistoryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminClassificationManagementServiceImplTest {

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

    @Test
    @DisplayName("Get all categories maps null mapper result to empty list")
    void getAllCategories_mapperReturnsNull_returnsEmptyList() {
        when(categoryMapper.selectAllCategories()).thenReturn(null);

        List<AdminCategoryResponse> result = service.getAllCategories();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Get active tags maps entity rows to response rows")
    void getActiveTags_rowsExist_returnsMappedResponses() {
        when(tagMapper.selectByStatus(ClassificationStatus.ACTIVE.name()))
                .thenReturn(List.of(tag(9L, "Festival", ClassificationStatus.ACTIVE.name(), 3)));

        List<AdminTagResponse> result = service.getActiveTags();

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(9L, result.get(0).tagId()),
                () -> assertEquals("Festival", result.get(0).tagName()),
                () -> assertEquals(ClassificationStatus.ACTIVE, result.get(0).status()),
                () -> assertEquals(3, result.get(0).usageCount())
        );
    }

    @Test
    @DisplayName("Get all resource types maps null mapper result to empty list")
    void getAllResourceTypes_mapperReturnsNull_returnsEmptyList() {
        when(resourceTypeMapper.selectAllResourceTypes()).thenReturn(null);

        List<AdminResourceTypeResponse> result = service.getAllResourceTypes();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Get active resource types maps rows to response rows")
    void getActiveResourceTypes_rowsExist_returnsMappedResponses() {
        when(resourceTypeMapper.selectByStatus(ClassificationStatus.ACTIVE.name()))
                .thenReturn(List.of(resourceType(7L, "photo", ClassificationStatus.ACTIVE.name(), 2)));

        List<AdminResourceTypeResponse> result = service.getActiveResourceTypes();

        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals(7L, result.get(0).resourceTypeId()),
                () -> assertEquals("photo", result.get(0).typeName()),
                () -> assertEquals(ClassificationStatus.ACTIVE, result.get(0).status()),
                () -> assertEquals(2, result.get(0).usageCount())
        );
    }

    @Test
    @DisplayName("Create category trims and collapses whitespace in valid name")
    void createCategory_validName_returnsCreatedCategory() {
        AtomicReference<Category> inserted = new AtomicReference<>();
        when(categoryMapper.countByTopicIgnoreCase(eq("Ancient Places"), isNull())).thenReturn(0);
        when(resourceTypeMapper.countByTypeNameIgnoreCase(eq("Ancient Places"), isNull())).thenReturn(0);
        when(categoryMapper.insert(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setCategoryId(7L);
            inserted.set(category);
            return 1;
        });
        when(categoryMapper.selectById(7L)).thenAnswer(invocation -> inserted.get());

        AdminCategoryResponse response = service.createCategory(new AdminCategoryRequest(" Ancient   Places "), "Olivia");

        assertAll(
                () -> assertEquals(7L, response.categoryId()),
                () -> assertEquals("Ancient Places", response.categoryTopic()),
                () -> assertEquals(ClassificationStatus.ACTIVE, response.status()),
                () -> assertEquals(0, response.usageCount()),
                () -> assertEquals("Ancient Places", inserted.get().getCategoryTopic()),
                () -> assertEquals(ClassificationStatus.ACTIVE.name(), inserted.get().getStatus())
        );
    }

    @Test
    @DisplayName("Create category rejects null and blank names")
    void createCategory_blankOrNullName_throwsBadRequest() {
        AppException nullException = assertThrows(AppException.class,
                () -> service.createCategory(null, "Olivia"));
        AppException blankException = assertThrows(AppException.class,
                () -> service.createCategory(new AdminCategoryRequest("   "), "Olivia"));

        assertAll(
                () -> assertEquals(400, nullException.getStatusCode()),
                () -> assertEquals("categoryTopic is invalid.", nullException.getMessage()),
                () -> assertTrue(nullException.getDetails().contains("categoryTopic cannot be blank.")),
                () -> assertEquals(400, blankException.getStatusCode()),
                () -> assertTrue(blankException.getDetails().contains("categoryTopic cannot be blank."))
        );
    }

    @Test
    @DisplayName("Create category rejects over max boundary length")
    void createCategory_nameOverMaxLength_throwsBadRequest() {
        String tooLong = "a".repeat(51);

        AppException exception = assertThrows(AppException.class,
                () -> service.createCategory(new AdminCategoryRequest(tooLong), "Olivia"));

        assertAll(
                () -> assertEquals(400, exception.getStatusCode()),
                () -> assertTrue(exception.getDetails().contains("categoryTopic cannot exceed 50 characters."))
        );
    }

    @Test
    @DisplayName("Create category rejects duplicate topic")
    void createCategory_duplicateTopic_throwsConflict() {
        when(categoryMapper.countByTopicIgnoreCase("Places", null)).thenReturn(1);

        AppException exception = assertThrows(AppException.class,
                () -> service.createCategory(new AdminCategoryRequest("Places"), "Olivia"));

        assertAll(
                () -> assertEquals(409, exception.getStatusCode()),
                () -> assertEquals("categoryTopic already exists.", exception.getMessage())
        );
    }

    @Test
    @DisplayName("Create category rejects name conflicting with resource type")
    void createCategory_conflictsWithResourceType_throwsConflict() {
        when(categoryMapper.countByTopicIgnoreCase("photo", null)).thenReturn(0);
        when(resourceTypeMapper.countByTypeNameIgnoreCase("photo", null)).thenReturn(1);

        AppException exception = assertThrows(AppException.class,
                () -> service.createCategory(new AdminCategoryRequest("photo"), "Olivia"));

        assertEquals("categoryTopic conflicts with an existing resource type.", exception.getMessage());
    }

    @Test
    @DisplayName("Update category returns updated response when row changes")
    void updateCategory_validRequest_returnsUpdatedCategory() {
        when(categoryMapper.selectById(5L)).thenReturn(
                category(5L, "Old", ClassificationStatus.ACTIVE.name(), 1),
                category(5L, "New Topic", ClassificationStatus.ACTIVE.name(), 1));
        when(categoryMapper.countByTopicIgnoreCase("New Topic", 5L)).thenReturn(0);
        when(resourceTypeMapper.countByTypeNameIgnoreCase("New Topic", null)).thenReturn(0);
        when(categoryMapper.updateTopic(eq(5L), eq("New Topic"), any(LocalDateTime.class))).thenReturn(1);

        AdminCategoryResponse response = service.updateCategory(5L, new AdminCategoryRequest("New Topic"), "Olivia");

        assertAll(
                () -> assertEquals(5L, response.categoryId()),
                () -> assertEquals("New Topic", response.categoryTopic()),
                () -> assertEquals(ClassificationStatus.ACTIVE, response.status())
        );
    }

    @Test
    @DisplayName("Update category rejects missing category id")
    void updateCategory_nullId_throwsBadRequest() {
        AppException exception = assertThrows(AppException.class,
                () -> service.updateCategory(null, new AdminCategoryRequest("Name"), "Olivia"));

        assertEquals("Category id is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Activate category returns existing response when already active")
    void activateCategory_alreadyActive_returnsUnchangedResponse() {
        when(categoryMapper.selectById(5L)).thenReturn(category(5L, "Places", ClassificationStatus.ACTIVE.name(), 1));

        AdminCategoryResponse response = service.activateCategory(5L, "Olivia");

        assertAll(
                () -> assertEquals(5L, response.categoryId()),
                () -> assertEquals(ClassificationStatus.ACTIVE, response.status()),
                () -> assertEquals("Places", response.categoryTopic())
        );
    }

    @Test
    @DisplayName("Deactivate category changes active category to inactive")
    void deactivateCategory_activeCategory_returnsInactiveResponse() {
        when(categoryMapper.selectById(5L)).thenReturn(
                category(5L, "Places", ClassificationStatus.ACTIVE.name(), 1),
                category(5L, "Places", ClassificationStatus.INACTIVE.name(), 1));
        when(categoryMapper.updateStatus(eq(5L), eq(ClassificationStatus.INACTIVE.name()), any(LocalDateTime.class))).thenReturn(1);

        AdminCategoryResponse response = service.deactivateCategory(5L, "Olivia");

        assertEquals(ClassificationStatus.INACTIVE, response.status());
    }

    @Test
    @DisplayName("Create tag accepts max boundary length")
    void createTag_maxLengthName_returnsCreatedTag() {
        String maxName = "t".repeat(100);
        AtomicReference<Tag> inserted = new AtomicReference<>();
        when(tagMapper.countByNameIgnoreCase(maxName, null)).thenReturn(0);
        when(tagMapper.insert(any(Tag.class))).thenAnswer(invocation -> {
            Tag tag = invocation.getArgument(0);
            tag.setTagId(11L);
            inserted.set(tag);
            return 1;
        });
        when(tagMapper.selectById(11L)).thenAnswer(invocation -> inserted.get());

        AdminTagResponse response = service.createTag(new AdminTagRequest(maxName), "Olivia");

        assertAll(
                () -> assertEquals(11L, response.tagId()),
                () -> assertEquals(maxName, response.tagName()),
                () -> assertEquals(100, response.tagName().length()),
                () -> assertEquals(ClassificationStatus.ACTIVE, response.status())
        );
    }

    @Test
    @DisplayName("Update tag rejects duplicate tag name")
    void updateTag_duplicateName_throwsConflict() {
        when(tagMapper.selectById(11L)).thenReturn(tag(11L, "Old", ClassificationStatus.ACTIVE.name(), 0));
        when(tagMapper.countByNameIgnoreCase("Festival", 11L)).thenReturn(1);

        AppException exception = assertThrows(AppException.class,
                () -> service.updateTag(11L, new AdminTagRequest("Festival"), "Olivia"));

        assertEquals("tagName already exists.", exception.getMessage());
    }

    @Test
    @DisplayName("Update tag treats mapper update 0 as not found")
    void updateTag_updateReturnsZero_throwsNotFound() {
        when(tagMapper.selectById(11L)).thenReturn(tag(11L, "Old", ClassificationStatus.ACTIVE.name(), 0));
        when(tagMapper.countByNameIgnoreCase("Festival", 11L)).thenReturn(0);
        when(tagMapper.updateTagName(eq(11L), eq("Festival"), any(LocalDateTime.class))).thenReturn(0);

        AppException exception = assertThrows(AppException.class,
                () -> service.updateTag(11L, new AdminTagRequest("Festival"), "Olivia"));

        assertAll(
                () -> assertEquals(404, exception.getStatusCode()),
                () -> assertEquals("Tag does not exist.", exception.getMessage())
        );
    }

    @Test
    @DisplayName("Deactivate tag returns existing response when already inactive")
    void deactivateTag_alreadyInactive_returnsExistingResponse() {
        when(tagMapper.selectById(11L)).thenReturn(tag(11L, "Festival", ClassificationStatus.INACTIVE.name(), 0));

        AdminTagResponse response = service.deactivateTag(11L, "Olivia");

        assertAll(
                () -> assertEquals(11L, response.tagId()),
                () -> assertEquals("Festival", response.tagName()),
                () -> assertEquals(ClassificationStatus.INACTIVE, response.status())
        );
    }

    @Test
    @DisplayName("Activate tag changes inactive tag to active")
    void activateTag_inactiveTag_returnsActiveResponse() {
        when(tagMapper.selectById(11L)).thenReturn(
                tag(11L, "Festival", ClassificationStatus.INACTIVE.name(), 0),
                tag(11L, "Festival", ClassificationStatus.ACTIVE.name(), 0));
        when(tagMapper.updateStatus(eq(11L), eq(ClassificationStatus.ACTIVE.name()), any(LocalDateTime.class))).thenReturn(1);

        AdminTagResponse response = service.activateTag(11L, "Olivia");

        assertEquals(ClassificationStatus.ACTIVE, response.status());
    }

    @Test
    @DisplayName("Create resource type rejects name conflicting with category topic")
    void createResourceType_conflictsWithCategory_throwsConflict() {
        when(resourceTypeMapper.countByTypeNameIgnoreCase("traditions", null)).thenReturn(0);
        when(categoryMapper.countByTopicIgnoreCase("traditions", null)).thenReturn(1);

        AppException exception = assertThrows(AppException.class,
                () -> service.createResourceType(new AdminResourceTypeRequest("traditions"), "Olivia"));

        assertEquals("typeName conflicts with an existing category topic.", exception.getMessage());
    }

    @Test
    @DisplayName("Create resource type returns active type for valid request")
    void createResourceType_validRequest_returnsCreatedType() {
        AtomicReference<ResourceType> inserted = new AtomicReference<>();
        when(resourceTypeMapper.countByTypeNameIgnoreCase("photo", null)).thenReturn(0);
        when(categoryMapper.countByTopicIgnoreCase("photo", null)).thenReturn(0);
        when(resourceTypeMapper.insert(any(ResourceType.class))).thenAnswer(invocation -> {
            ResourceType type = invocation.getArgument(0);
            type.setResourceTypeId(12L);
            inserted.set(type);
            return 1;
        });
        when(resourceTypeMapper.selectById(12L)).thenAnswer(invocation -> inserted.get());

        AdminResourceTypeResponse response = service.createResourceType(new AdminResourceTypeRequest(" photo "), "Olivia");

        assertAll(
                () -> assertEquals(12L, response.resourceTypeId()),
                () -> assertEquals("photo", response.typeName()),
                () -> assertEquals(ClassificationStatus.ACTIVE, response.status())
        );
    }

    @Test
    @DisplayName("Update used resource type rejects unsupported workflow name")
    void updateResourceType_usedTypeUnsupportedName_throwsConflict() {
        when(resourceTypeMapper.selectById(12L)).thenReturn(resourceType(12L, "photo", ClassificationStatus.ACTIVE.name(), 4));
        when(resourceTypeMapper.countByTypeNameIgnoreCase("custom type", 12L)).thenReturn(0);
        when(categoryMapper.countByTopicIgnoreCase("custom type", null)).thenReturn(0);

        AppException exception = assertThrows(AppException.class,
                () -> service.updateResourceType(12L, new AdminResourceTypeRequest("custom type"), "Olivia"));

        assertEquals(
                "Resource types already used by resources must keep a name supported by the current resource metadata flow.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Update used resource type accepts supported workflow name")
    void updateResourceType_usedTypeSupportedName_returnsUpdatedType() {
        when(resourceTypeMapper.selectById(12L)).thenReturn(
                resourceType(12L, "photo", ClassificationStatus.ACTIVE.name(), 4),
                resourceType(12L, "video", ClassificationStatus.ACTIVE.name(), 4)
        );
        when(resourceTypeMapper.countByTypeNameIgnoreCase("video", 12L)).thenReturn(0);
        when(categoryMapper.countByTopicIgnoreCase("video", null)).thenReturn(0);
        when(resourceTypeMapper.updateTypeName(eq(12L), eq("video"), any(LocalDateTime.class))).thenReturn(1);

        AdminResourceTypeResponse response = service.updateResourceType(
                12L,
                new AdminResourceTypeRequest("video"),
                "Olivia"
        );

        assertAll(
                () -> assertEquals(12L, response.resourceTypeId()),
                () -> assertEquals("video", response.typeName()),
                () -> assertEquals(ClassificationStatus.ACTIVE, response.status())
        );
    }

    @Test
    @DisplayName("Activate resource type returns existing response when already active")
    void activateResourceType_alreadyActive_returnsExistingResponse() {
        when(resourceTypeMapper.selectById(12L)).thenReturn(resourceType(12L, "photo", ClassificationStatus.ACTIVE.name(), 0));

        AdminResourceTypeResponse response = service.activateResourceType(12L, "Olivia");

        assertAll(
                () -> assertEquals(12L, response.resourceTypeId()),
                () -> assertEquals("photo", response.typeName()),
                () -> assertEquals(ClassificationStatus.ACTIVE, response.status())
        );
    }

    @Test
    @DisplayName("Deactivate resource type changes active type to inactive")
    void deactivateResourceType_activeType_returnsInactiveResponse() {
        when(resourceTypeMapper.selectById(12L)).thenReturn(
                resourceType(12L, "photo", ClassificationStatus.ACTIVE.name(), 0),
                resourceType(12L, "photo", ClassificationStatus.INACTIVE.name(), 0));
        when(resourceTypeMapper.updateStatus(eq(12L), eq(ClassificationStatus.INACTIVE.name()), any(LocalDateTime.class))).thenReturn(1);

        AdminResourceTypeResponse response = service.deactivateResourceType(12L, "Olivia");

        assertAll(
                () -> assertEquals(12L, response.resourceTypeId()),
                () -> assertEquals("photo", response.typeName()),
                () -> assertEquals(ClassificationStatus.INACTIVE, response.status())
        );
    }

    @Test
    @DisplayName("Mapper update returning zero is treated as not found")
    void updateResourceType_updateReturnsZero_throwsNotFound() {
        when(resourceTypeMapper.selectById(12L)).thenReturn(resourceType(12L, "photo", ClassificationStatus.ACTIVE.name(), 0));
        when(resourceTypeMapper.countByTypeNameIgnoreCase("video", 12L)).thenReturn(0);
        when(categoryMapper.countByTopicIgnoreCase("video", null)).thenReturn(0);
        when(resourceTypeMapper.updateTypeName(eq(12L), eq("video"), any(LocalDateTime.class))).thenReturn(0);

        AppException exception = assertThrows(AppException.class,
                () -> service.updateResourceType(12L, new AdminResourceTypeRequest("video"), "Olivia"));

        assertAll(
                () -> assertEquals(404, exception.getStatusCode()),
                () -> assertEquals("Resource type does not exist.", exception.getMessage())
        );
    }

    private Category category(Long id, String topic, String status, Integer usageCount) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryTopic(topic);
        category.setStatus(status);
        category.setUsageCount(usageCount);
        category.setCreatedAt(LocalDateTime.now().minusDays(2));
        category.setLastUpdatedAt(LocalDateTime.now().minusDays(1));
        return category;
    }

    private Tag tag(Long id, String name, String status, Integer usageCount) {
        Tag tag = new Tag();
        tag.setTagId(id);
        tag.setTagName(name);
        tag.setStatus(status);
        tag.setUsageCount(usageCount);
        tag.setCreatedAt(LocalDateTime.now().minusDays(2));
        tag.setLastUpdatedAt(LocalDateTime.now().minusDays(1));
        return tag;
    }

    private ResourceType resourceType(Long id, String name, String status, Integer usageCount) {
        ResourceType type = new ResourceType();
        type.setResourceTypeId(id);
        type.setTypeName(name);
        type.setStatus(status);
        type.setUsageCount(usageCount);
        type.setCreatedAt(LocalDateTime.now().minusDays(2));
        type.setLastUpdatedAt(LocalDateTime.now().minusDays(1));
        return type;
    }
}
