package com.cpt202.HerLink.service;

import com.cpt202.HerLink.dto.admin.AdminResourceLifecycleResponse;
import com.cpt202.HerLink.dto.admin.ResourceLifecycleRow;
import com.cpt202.HerLink.dto.review.ResourceReviewStatus;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.AdminResourceLifecycleMapper;
import com.cpt202.HerLink.service.admin.AdminOperationHistoryService;
import com.cpt202.HerLink.service.admin.AdminResourceLifecycleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminResourceLifecycleServiceImplTest {

    @Mock
    private AdminResourceLifecycleMapper adminResourceLifecycleMapper;

    @Mock
    private AdminOperationHistoryService operationHistoryService;

    private AdminResourceLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminResourceLifecycleServiceImpl(adminResourceLifecycleMapper, operationHistoryService);
    }

    @Test
    void archiveResource_shouldArchiveApprovedResource() {
        ResourceLifecycleRow approved = createRow(ResourceStatusEnum.APPROVED.getValue(), null);
        LocalDateTime archivedAt = LocalDateTime.now();
        ResourceLifecycleRow archived = createRow(ResourceStatusEnum.ARCHIVED.getValue(), archivedAt);

        when(adminResourceLifecycleMapper.selectResourceLifecycle(5L))
                .thenReturn(approved)
                .thenReturn(archived);
        when(adminResourceLifecycleMapper.archiveApprovedResource(
                eq(5L),
                any(LocalDateTime.class),
                eq(ResourceStatusEnum.ARCHIVED.getValue()),
                eq(ResourceStatusEnum.APPROVED.getValue())
        )).thenReturn(1);

        AdminResourceLifecycleResponse result = service.archiveResource(5L, "Admin User");

        assertTrue(result.changed());
        assertEquals(ResourceReviewStatus.APPROVED, result.previousStatus());
        assertEquals(ResourceReviewStatus.ARCHIVED, result.resourceStatus());
        assertNotNull(result.archivedAt());
        assertEquals("Resource archived and hidden from public discovery.", result.message());
        verify(operationHistoryService).recordOperation(
                "Approved resource (#5)",
                "Resource",
                "resource",
                "ARCHIVE_RESOURCE Approved -> Archived",
                "Admin User"
        );
    }

    @Test
    void archiveResource_shouldReturnUnchangedWhenAlreadyArchived() {
        LocalDateTime archivedAt = LocalDateTime.now().minusDays(1);
        ResourceLifecycleRow archived = createRow(ResourceStatusEnum.ARCHIVED.getValue(), archivedAt);
        when(adminResourceLifecycleMapper.selectResourceLifecycle(5L)).thenReturn(archived);

        AdminResourceLifecycleResponse result = service.archiveResource(5L, "Admin User");

        assertFalse(result.changed());
        assertEquals(ResourceReviewStatus.ARCHIVED, result.previousStatus());
        assertEquals(ResourceReviewStatus.ARCHIVED, result.resourceStatus());
        assertEquals(archivedAt, result.archivedAt());
        assertEquals("Resource is already archived.", result.message());
        verify(adminResourceLifecycleMapper, never()).archiveApprovedResource(any(), any(), any(), any());
        verify(operationHistoryService, never()).recordOperation(any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Draft", "Pending Review", "Rejected"})
    void archiveResource_shouldRejectDisallowedStatuses(String status) {
        ResourceLifecycleRow row = createRow(status, null);
        when(adminResourceLifecycleMapper.selectResourceLifecycle(5L)).thenReturn(row);

        AppException exception = assertThrows(AppException.class, () -> service.archiveResource(5L, "Admin User"));

        assertEquals(409, exception.getStatusCode());
        assertEquals("Only Approved resources can be archived.", exception.getMessage());
        verify(adminResourceLifecycleMapper, never()).archiveApprovedResource(any(), any(), any(), any());
        verify(operationHistoryService, never()).recordOperation(any(), any(), any(), any(), any());
    }

    @Test
    void archiveResource_shouldRejectInvalidResourceId() {
        AppException exception = assertThrows(AppException.class, () -> service.archiveResource(0L, "Admin User"));

        assertEquals(400, exception.getStatusCode());
        assertEquals("Resource id is invalid.", exception.getMessage());
        verify(adminResourceLifecycleMapper, never()).selectResourceLifecycle(any());
    }

    @Test
    void archiveResource_shouldThrowNotFoundWhenResourceDoesNotExist() {
        when(adminResourceLifecycleMapper.selectResourceLifecycle(5L)).thenReturn(null);

        AppException exception = assertThrows(AppException.class, () -> service.archiveResource(5L, "Admin User"));

        assertEquals(404, exception.getStatusCode());
        assertEquals("Resource does not exist.", exception.getMessage());
        verify(adminResourceLifecycleMapper, never()).archiveApprovedResource(any(), any(), any(), any());
    }

    @Test
    void archiveResource_shouldThrowConflictWhenConditionalUpdateFails() {
        ResourceLifecycleRow approved = createRow(ResourceStatusEnum.APPROVED.getValue(), null);
        when(adminResourceLifecycleMapper.selectResourceLifecycle(5L)).thenReturn(approved);
        when(adminResourceLifecycleMapper.archiveApprovedResource(
                eq(5L),
                any(LocalDateTime.class),
                eq(ResourceStatusEnum.ARCHIVED.getValue()),
                eq(ResourceStatusEnum.APPROVED.getValue())
        )).thenReturn(0);

        AppException exception = assertThrows(AppException.class, () -> service.archiveResource(5L, "Admin User"));

        assertEquals(409, exception.getStatusCode());
        assertEquals("Resource could not be archived from its current status.", exception.getMessage());
        verify(operationHistoryService, never()).recordOperation(any(), any(), any(), any(), any());
    }

    @Test
    void archiveResource_shouldReturnAlreadyArchivedWhenConcurrentArchiveWins() {
        ResourceLifecycleRow approved = createRow(ResourceStatusEnum.APPROVED.getValue(), null);
        ResourceLifecycleRow archived = createRow(ResourceStatusEnum.ARCHIVED.getValue(), LocalDateTime.now());
        when(adminResourceLifecycleMapper.selectResourceLifecycle(5L))
                .thenReturn(approved)
                .thenReturn(archived);
        when(adminResourceLifecycleMapper.archiveApprovedResource(
                eq(5L),
                any(LocalDateTime.class),
                eq(ResourceStatusEnum.ARCHIVED.getValue()),
                eq(ResourceStatusEnum.APPROVED.getValue())
        )).thenReturn(0);

        AdminResourceLifecycleResponse result = service.archiveResource(5L, "Admin User");

        assertFalse(result.changed());
        assertEquals(ResourceReviewStatus.ARCHIVED, result.previousStatus());
        assertEquals(ResourceReviewStatus.ARCHIVED, result.resourceStatus());
        assertEquals("Resource is already archived.", result.message());
        verify(operationHistoryService, never()).recordOperation(any(), any(), any(), any(), any());
    }

    @Test
    void listResources_shouldNormalizeOptionalStatusFilter() {
        service.listResources("archived");

        verify(adminResourceLifecycleMapper).selectResourceLifecycles(ResourceStatusEnum.ARCHIVED.getValue());
    }

    @Test
    void listResources_shouldRejectUnsupportedStatusFilter() {
        AppException exception = assertThrows(AppException.class, () -> service.listResources("Published"));

        assertEquals(400, exception.getStatusCode());
        assertEquals("Unsupported resource status.", exception.getMessage());
        verify(adminResourceLifecycleMapper, never()).selectResourceLifecycles(any());
    }

    private ResourceLifecycleRow createRow(String status, LocalDateTime archivedAt) {
        ResourceLifecycleRow row = new ResourceLifecycleRow();
        row.setResourceId(5L);
        row.setTitle("Approved resource");
        row.setStatus(status);
        row.setArchivedAt(archivedAt);
        row.setUpdatedAt(LocalDateTime.now());
        return row;
    }
}
