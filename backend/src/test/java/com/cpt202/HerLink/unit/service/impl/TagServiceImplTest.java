package com.cpt202.HerLink.unit.service.impl;

import com.cpt202.HerLink.service.impl.*;
import com.cpt202.HerLink.service.impl.TagServiceImpl;
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
import com.cpt202.HerLink.entity.Tag;
import com.cpt202.HerLink.mapper.TagMapper;
import com.cpt202.HerLink.vo.CategoryTagOptionVO;

/**
 * Unit test for TagServiceImpl
 * Fully isolated: no database / network / file system dependencies
 */
@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    // Mock Mapper to isolate external dependencies
    @Mock
    private TagMapper tagMapper;

    // Inject mock objects into the target test class
    @InjectMocks
    private TagServiceImpl tagService;

    //Test Constants
    private static final Long TEST_TAG_ID = 1L;
    private static final String TEST_TAG_NAME = "Test Tag";
    private static final Long SECOND_TAG_ID = 2L;
    private static final String SECOND_TAG_NAME = "Tech Tag";

    //Normal Scenarios
    @Test
    @DisplayName("Normal case: Query single active tag, return corresponding option list")
    void listTagOptions_WithSingleTag_ReturnSingleOption() {
        // 1. Build test data
        Tag testTag = new Tag();
        testTag.setTagId(TEST_TAG_ID);
        testTag.setTagName(TEST_TAG_NAME);

        // 2. Mock mapper behavior (no real database call)
        when(tagMapper.selectActiveTags()).thenReturn(List.of(testTag));

        // 3. Execute target method
        List<CategoryTagOptionVO> result = tagService.listTagOptions();

        // 4. Standard JUnit assertions
        assertNotNull(result);
        assertEquals(1, result.size());
        
        CategoryTagOptionVO option = result.get(0);
        assertEquals(TEST_TAG_ID, option.getId());
        assertEquals(TEST_TAG_NAME, option.getName());
    }

    @Test
    @DisplayName("Normal case: Query multiple active tags, return full option list")
    void listTagOptions_WithMultipleTags_ReturnAllOptions() {
        // 1. Build test data
        Tag tag1 = new Tag();
        tag1.setTagId(TEST_TAG_ID);
        tag1.setTagName(TEST_TAG_NAME);

        Tag tag2 = new Tag();
        tag2.setTagId(SECOND_TAG_ID);
        tag2.setTagName(SECOND_TAG_NAME);

        // 2. Mock mapper behavior
        when(tagMapper.selectActiveTags()).thenReturn(List.of(tag1, tag2));

        // 3. Execute target method
        List<CategoryTagOptionVO> result = tagService.listTagOptions();

        // 4. Assertions
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify first tag
        assertEquals(TEST_TAG_ID, result.get(0).getId());
        assertEquals(TEST_TAG_NAME, result.get(0).getName());
        // Verify second tag
        assertEquals(SECOND_TAG_ID, result.get(1).getId());
        assertEquals(SECOND_TAG_NAME, result.get(1).getName());
    }

    //Boundary Scenarios
    @Test
    @DisplayName("Boundary case: No active tags exist, return empty list (not null)")
    void listTagOptions_WithEmptyList_ReturnEmptyList() {
        // 1. Mock mapper returns empty collection
        when(tagMapper.selectActiveTags()).thenReturn(List.of());

        // 2. Execute target method
        List<CategoryTagOptionVO> result = tagService.listTagOptions();

        // 3. Assertions
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    //Exception / Null Scenarios
    @Test
    @DisplayName("Null case: Mapper returns null, handle safely and return empty list")
    void listTagOptions_WhenMapperReturnNull_ReturnEmptyList() {
        // 1. Mock mapper returns null
        when(tagMapper.selectActiveTags()).thenReturn(null);

        // 2. Execute target method
        List<CategoryTagOptionVO> result = tagService.listTagOptions();

        // 3. Core assertion: null is safely converted to empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
