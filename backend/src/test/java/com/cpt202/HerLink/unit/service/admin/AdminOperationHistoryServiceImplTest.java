package com.cpt202.HerLink.unit.service.admin;

import com.cpt202.HerLink.dto.admin.AdminOperationHistoryResponse;
import com.cpt202.HerLink.mapper.AdminOperationHistoryMapper;
import com.cpt202.HerLink.service.admin.AdminOperationHistoryServiceImpl;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOperationHistoryServiceImplTest {

    @Mock
    private AdminOperationHistoryMapper adminOperationHistoryMapper;

    @InjectMocks
    private AdminOperationHistoryServiceImpl service;

    @Test
    @DisplayName("Record operation stores all supplied fields and generated timestamp")
    void recordOperation_validInput_capturesAllFields() {
        AtomicReference<Object[]> captured = new AtomicReference<>();
        when(adminOperationHistoryMapper.insert(any(), any(), any(), any(), any(), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    captured.set(new Object[] {
                            invocation.getArgument(0),
                            invocation.getArgument(1),
                            invocation.getArgument(2),
                            invocation.getArgument(3),
                            invocation.getArgument(4),
                            invocation.getArgument(5)
                    });
                    return 1;
                });

        assertDoesNotThrow(() ->
                service.recordOperation("Temple", "Resource", "resource", "ARCHIVE", "Olivia"));

        Object[] values = captured.get();
        assertAll(
                () -> assertNotNull(values),
                () -> assertEquals("Temple", values[0]),
                () -> assertEquals("Resource", values[1]),
                () -> assertEquals("resource", values[2]),
                () -> assertEquals("ARCHIVE", values[3]),
                () -> assertEquals("Olivia", values[4]),
                () -> assertTrue(values[5] instanceof LocalDateTime)
        );
    }

    @Test
    @DisplayName("Record operation swallows mapper runtime exception")
    void recordOperation_mapperThrows_doesNotPropagate() {
        when(adminOperationHistoryMapper.insert(any(), any(), any(), any(), any(), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("database unavailable"));

        assertDoesNotThrow(() ->
                service.recordOperation("Temple", "Resource", "resource", "ARCHIVE", "Olivia"));
    }

    @Test
    @DisplayName("Get operation history returns all rows when module is null")
    void getOperationHistory_nullModule_returnsAllRows() {
        List<AdminOperationHistoryResponse> expected = List.of(history("resource"));
        when(adminOperationHistoryMapper.selectAll()).thenReturn(expected);

        List<AdminOperationHistoryResponse> result = service.getOperationHistory(null);

        assertSame(expected, result);
        assertEquals(1, result.size());
        assertEquals("resource", result.get(0).getModule());
    }

    @Test
    @DisplayName("Get operation history returns all rows when module is blank")
    void getOperationHistory_blankModule_returnsAllRows() {
        List<AdminOperationHistoryResponse> expected = List.of(history("classification"));
        when(adminOperationHistoryMapper.selectAll()).thenReturn(expected);

        List<AdminOperationHistoryResponse> result = service.getOperationHistory("   ");

        assertSame(expected, result);
        assertEquals("classification", result.get(0).getModule());
    }

    @Test
    @DisplayName("Get operation history trims module before filtered query")
    void getOperationHistory_moduleWithSpaces_queriesTrimmedModule() {
        List<AdminOperationHistoryResponse> expected = List.of(history("tag"));
        when(adminOperationHistoryMapper.selectByModule(eq("tag"))).thenReturn(expected);

        List<AdminOperationHistoryResponse> result = service.getOperationHistory("  tag  ");

        assertSame(expected, result);
        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals("tag", result.get(0).getModule()),
                () -> assertEquals("Updated", result.get(0).getAction())
        );
    }

    private AdminOperationHistoryResponse history(String module) {
        AdminOperationHistoryResponse response = new AdminOperationHistoryResponse();
        response.setItemName("Temple");
        response.setKind("Resource");
        response.setModule(module);
        response.setAction("Updated");
        response.setAdministrator("Olivia");
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }
}
