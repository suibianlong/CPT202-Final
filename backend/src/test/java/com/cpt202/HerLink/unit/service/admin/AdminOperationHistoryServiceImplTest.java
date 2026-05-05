package com.cpt202.HerLink.unit.service.admin;

import com.cpt202.HerLink.service.admin.*;
import com.cpt202.HerLink.service.admin.AdminOperationHistoryServiceImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cpt202.HerLink.dto.admin.AdminOperationHistoryResponse;
import com.cpt202.HerLink.mapper.AdminOperationHistoryMapper;

@ExtendWith(MockitoExtension.class)
class AdminOperationHistoryServiceImplTest {

    @Mock
    private AdminOperationHistoryMapper adminOperationHistoryMapper;

    @InjectMocks
    private AdminOperationHistoryServiceImpl adminOperationHistoryService;

    private static final String TEST_ITEM = "TestItem";
    private static final String TEST_KIND = "TestKind";
    private static final String TEST_MODULE = "UserManagement";
    private static final String TEST_ACTION = "Update";
    private static final String TEST_ADMIN = "admin01";

    private AdminOperationHistoryResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new AdminOperationHistoryResponse();
        mockResponse.setItemName(TEST_ITEM);
        mockResponse.setModule(TEST_MODULE);
        mockResponse.setAction(TEST_ACTION);
        mockResponse.setAdministrator(TEST_ADMIN);
    }

    // recordOperation 测试

    @Test
    @DisplayName("Record operation with valid parameters should execute without exception")
    void recordOperation_WithValidParams_ShouldNotThrowException() {
        // JUnit 断言：无异常抛出
        assertDoesNotThrow(() -> {
            adminOperationHistoryService.recordOperation(TEST_ITEM, TEST_KIND, TEST_MODULE, TEST_ACTION, TEST_ADMIN);
        });
    }

    @Test
    @DisplayName("Record operation with null itemName should not throw exception")
    void recordOperation_WithNullItemName_ShouldNotThrowException() {
        assertDoesNotThrow(() -> {
            adminOperationHistoryService.recordOperation(null, TEST_KIND, TEST_MODULE, TEST_ACTION, TEST_ADMIN);
        });
    }

    @Test
    @DisplayName("Record operation with empty itemName should not throw exception")
    void recordOperation_WithEmptyItemName_ShouldNotThrowException() {
        assertDoesNotThrow(() -> {
            adminOperationHistoryService.recordOperation("", TEST_KIND, TEST_MODULE, TEST_ACTION, TEST_ADMIN);
        });
    }

    @Test
    @DisplayName("Record operation with blank kind should not throw exception")
    void recordOperation_WithBlankKind_ShouldNotThrowException() {
        assertDoesNotThrow(() -> {
            adminOperationHistoryService.recordOperation(TEST_ITEM, "   ", TEST_MODULE, TEST_ACTION, TEST_ADMIN);
        });
    }

    @Test
    @DisplayName("Record operation with null administrator should not throw exception")
    void recordOperation_WithNullAdministrator_ShouldNotThrowException() {
        assertDoesNotThrow(() -> {
            adminOperationHistoryService.recordOperation(TEST_ITEM, TEST_KIND, TEST_MODULE, TEST_ACTION, null);
        });
    }

    @Test
    @DisplayName("When mapper throws exception, record operation should catch and not propagate")
    void recordOperation_MapperThrowsRuntimeException_ShouldNotThrowToCaller() {
        doThrow(new RuntimeException("DB insertion failed"))
                .when(adminOperationHistoryMapper)
                .insert(anyString(), anyString(), anyString(), anyString(), anyString(), any());

        assertDoesNotThrow(() -> {
            adminOperationHistoryService.recordOperation(TEST_ITEM, TEST_KIND, TEST_MODULE, TEST_ACTION, TEST_ADMIN);
        });
    }

    // getOperationHistory

    @Test
    @DisplayName("Get history with valid module should return non-empty list")
    void getOperationHistory_WithValidModule_ShouldReturnMatchedList() {
        String module = "SystemConfig";
        List<AdminOperationHistoryResponse> expected = List.of(mockResponse);

        when(adminOperationHistoryMapper.selectByModule(module)).thenReturn(expected);

        List<AdminOperationHistoryResponse> actual = adminOperationHistoryService.getOperationHistory(module);

        assertEquals(expected.size(), actual.size());
        assertFalse(actual.isEmpty());
    }


    @Test
    @DisplayName("Get history with null module should return all records")
    void getOperationHistory_WithNullModule_ShouldReturnAllRecords() {
        List<AdminOperationHistoryResponse> expected = List.of(mockResponse);
        when(adminOperationHistoryMapper.selectAll()).thenReturn(expected); // 加这句

        List<AdminOperationHistoryResponse> actual = adminOperationHistoryService.getOperationHistory(null);

        assertEquals(expected.size(), actual.size());
        assertFalse(actual.isEmpty());
    }

    @Test
    @DisplayName("Get history with empty module should return all records")
    void getOperationHistory_WithEmptyModule_ShouldReturnAllRecords() {
        List<AdminOperationHistoryResponse> expected = List.of(mockResponse);
        when(adminOperationHistoryMapper.selectAll()).thenReturn(expected); // 加这句

        List<AdminOperationHistoryResponse> actual = adminOperationHistoryService.getOperationHistory("");

        assertEquals(expected.size(), actual.size());
    }

    @Test
    @DisplayName("Get history with blank module should return all records")
    void getOperationHistory_WithBlankModule_ShouldReturnAllRecords() {
        List<AdminOperationHistoryResponse> expected = List.of(mockResponse);
        when(adminOperationHistoryMapper.selectAll()).thenReturn(expected); // 加这句

        List<AdminOperationHistoryResponse> actual = adminOperationHistoryService.getOperationHistory("   ");

        assertEquals(expected.size(), actual.size());
    }

    @Test
    @DisplayName("Get history with whitespace wrapped module should trim and return results")
    void getOperationHistory_WithWhitespaceModule_ShouldTrimAndReturnResults() {
        String trimmedModule = "UserManagement";
        List<AdminOperationHistoryResponse> expected = List.of(mockResponse);
        when(adminOperationHistoryMapper.selectByModule(trimmedModule)).thenReturn(expected); // 加这句

        List<AdminOperationHistoryResponse> actual = adminOperationHistoryService.getOperationHistory("  UserManagement  ");

        assertEquals(expected.size(), actual.size());
    }

    @Test
    @DisplayName("Get history with no matching data should return empty list")
    void getOperationHistory_WithNoMatchedData_ShouldReturnEmptyList() {
        List<AdminOperationHistoryResponse> actual = adminOperationHistoryService.getOperationHistory(TEST_MODULE);

        assertTrue(actual.isEmpty());
        assertEquals(0, actual.size());
    }
}
