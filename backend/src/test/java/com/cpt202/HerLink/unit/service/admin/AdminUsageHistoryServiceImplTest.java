package com.cpt202.HerLink.unit.service.admin;

import com.cpt202.HerLink.dto.admin.ClassificationUsageHistoryResponse;
import com.cpt202.HerLink.dto.admin.TagUsageHistoryResponse;
import com.cpt202.HerLink.mapper.AdminUsageHistoryMapper;
import com.cpt202.HerLink.service.admin.AdminUsageHistoryServiceImpl;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUsageHistoryServiceImpl Unit Test")
class AdminUsageHistoryServiceImplTest {

    @Mock
    private AdminUsageHistoryMapper adminUsageHistoryMapper;

    @InjectMocks
    private AdminUsageHistoryServiceImpl adminUsageHistoryService;

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 5, 5, 8, 0, 0);

    @Nested
    @DisplayName("getClassificationUsageHistory")
    class GetClassificationUsageHistoryTests {

        @Test
        @DisplayName("Should return classification usage history list when mapper returns data")
        void shouldReturnClassificationUsageHistoryList() {
            ClassificationUsageHistoryResponse response = classificationHistory(11L, "Health", "Category", 99L, "Resource A");
            when(adminUsageHistoryMapper.selectClassificationUsageHistory()).thenReturn(List.of(response));

            List<ClassificationUsageHistoryResponse> result = adminUsageHistoryService.getClassificationUsageHistory();

            assertEquals(1, result.size());
            assertEquals(11L, result.get(0).getClassificationId());
            assertEquals("Health", result.get(0).getName());
            assertEquals("Category", result.get(0).getKind());
            assertEquals(99L, result.get(0).getResourceId());
            assertEquals("Resource A", result.get(0).getRelatedRecordName());
            assertEquals(FIXED_TIME, result.get(0).getDateOfUse());
        }

        @Test
        @DisplayName("Should return empty list when mapper returns empty classification history")
        void shouldReturnEmptyListWhenMapperReturnsEmptyList() {
            when(adminUsageHistoryMapper.selectClassificationUsageHistory()).thenReturn(Collections.emptyList());

            List<ClassificationUsageHistoryResponse> result = adminUsageHistoryService.getClassificationUsageHistory();

            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should return null when mapper returns null for classification history")
        void shouldReturnNullWhenMapperReturnsNull() {
            when(adminUsageHistoryMapper.selectClassificationUsageHistory()).thenReturn(null);

            List<ClassificationUsageHistoryResponse> result = adminUsageHistoryService.getClassificationUsageHistory();

            assertNull(result);
        }

        @Test
        @DisplayName("Should propagate runtime exception thrown by mapper for classification history")
        void shouldPropagateRuntimeExceptionFromMapper() {
            when(adminUsageHistoryMapper.selectClassificationUsageHistory())
                    .thenThrow(new RuntimeException("Database unavailable"));

            assertThrows(RuntimeException.class, () -> adminUsageHistoryService.getClassificationUsageHistory());
        }
    }

    @Nested
    @DisplayName("getTagUsageHistory")
    class GetTagUsageHistoryTests {

        @Test
        @DisplayName("Should return tag usage history list when mapper returns data")
        void shouldReturnTagUsageHistoryList() {
            TagUsageHistoryResponse response = tagHistory(21L, "Heritage", 88L, "Resource B");
            when(adminUsageHistoryMapper.selectTagUsageHistory()).thenReturn(List.of(response));

            List<TagUsageHistoryResponse> result = adminUsageHistoryService.getTagUsageHistory();

            assertEquals(1, result.size());
            assertEquals(21L, result.get(0).getTagId());
            assertEquals("Heritage", result.get(0).getTagName());
            assertEquals(88L, result.get(0).getResourceId());
            assertEquals("Resource B", result.get(0).getRelatedRecordName());
            assertEquals(FIXED_TIME, result.get(0).getDateOfUse());
        }

        @Test
        @DisplayName("Should return empty list when mapper returns empty tag history")
        void shouldReturnEmptyListWhenMapperReturnsEmptyTagList() {
            when(adminUsageHistoryMapper.selectTagUsageHistory()).thenReturn(Collections.emptyList());

            List<TagUsageHistoryResponse> result = adminUsageHistoryService.getTagUsageHistory();

            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should return null when mapper returns null for tag history")
        void shouldReturnNullWhenMapperReturnsNull() {
            when(adminUsageHistoryMapper.selectTagUsageHistory()).thenReturn(null);

            List<TagUsageHistoryResponse> result = adminUsageHistoryService.getTagUsageHistory();

            assertNull(result);
        }

        @Test
        @DisplayName("Should propagate runtime exception thrown by mapper for tag history")
        void shouldPropagateRuntimeExceptionFromMapper() {
            when(adminUsageHistoryMapper.selectTagUsageHistory())
                    .thenThrow(new RuntimeException("Database unavailable"));

            assertThrows(RuntimeException.class, () -> adminUsageHistoryService.getTagUsageHistory());
        }
    }

    @Test
    @DisplayName("Should return the same list instance from mapper without extra transformation")
    void shouldReturnSameListInstanceFromMapper() {
        List<TagUsageHistoryResponse> tagList = List.of(tagHistory(31L, "Craft", 77L, "Resource C"));
        when(adminUsageHistoryMapper.selectTagUsageHistory()).thenReturn(tagList);

        List<TagUsageHistoryResponse> result = adminUsageHistoryService.getTagUsageHistory();

        assertSame(tagList, result);
    }

    private ClassificationUsageHistoryResponse classificationHistory(Long classificationId,
                                                                      String name,
                                                                      String kind,
                                                                      Long resourceId,
                                                                      String relatedRecordName) {
        ClassificationUsageHistoryResponse response = new ClassificationUsageHistoryResponse();
        response.setClassificationId(classificationId);
        response.setName(name);
        response.setKind(kind);
        response.setResourceId(resourceId);
        response.setRelatedRecordName(relatedRecordName);
        response.setDateOfUse(FIXED_TIME);
        return response;
    }

    private TagUsageHistoryResponse tagHistory(Long tagId,
                                               String tagName,
                                               Long resourceId,
                                               String relatedRecordName) {
        TagUsageHistoryResponse response = new TagUsageHistoryResponse();
        response.setTagId(tagId);
        response.setTagName(tagName);
        response.setResourceId(resourceId);
        response.setRelatedRecordName(relatedRecordName);
        response.setDateOfUse(FIXED_TIME);
        return response;
    }
}
