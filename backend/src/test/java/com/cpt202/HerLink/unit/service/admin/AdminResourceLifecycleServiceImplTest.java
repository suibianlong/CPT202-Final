package com.cpt202.HerLink.unit.service.admin;

import com.cpt202.HerLink.dto.admin.AdminResourceLifecycleResponse;
import com.cpt202.HerLink.dto.admin.ResourceLifecycleRow;
import com.cpt202.HerLink.dto.review.ResourceReviewStatus;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.AdminResourceLifecycleMapper;
import com.cpt202.HerLink.service.admin.AdminOperationHistoryService;
import com.cpt202.HerLink.service.admin.AdminResourceLifecycleServiceImpl;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminResourceLifecycleServiceImplTest {

    private static final Long TEST_RESOURCE_ID = 100L;
    private static final String TEST_TITLE = "Test Resource";
    private static final String APPROVED = ResourceStatusEnum.APPROVED.getValue();
    private static final String ARCHIVED = ResourceStatusEnum.ARCHIVED.getValue();
    private static final String PENDING = ResourceStatusEnum.PENDING_REVIEW.getValue();

    @Mock
    private AdminResourceLifecycleMapper adminResourceLifecycleMapper;

    @Mock
    private AdminOperationHistoryService operationHistoryService;

    @InjectMocks
    private AdminResourceLifecycleServiceImpl service;

    @Test
    @DisplayName("Archive resource - resource ID is null")
    void archiveResource_resourceIdIsNull() {
        AppException ex = assertThrows(AppException.class, () -> service.archiveResource(null, "Olivia Admin"));

        assertEquals("Resource id is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Archive resource - resource ID is invalid")
    void archiveResource_resourceIdIsInvalid() {
        AppException ex = assertThrows(AppException.class, () -> service.archiveResource(0L, "Olivia Admin"));

        assertEquals("Resource id is invalid.", ex.getMessage());
    }

    @Test
    @DisplayName("Archive resource - resource not found")
    void archiveResource_resourceNotFound() {
        when(adminResourceLifecycleMapper.selectResourceLifecycle(TEST_RESOURCE_ID)).thenReturn(null);

        AppException ex = assertThrows(AppException.class, () -> service.archiveResource(TEST_RESOURCE_ID, "Olivia Admin"));

        assertEquals("Resource does not exist.", ex.getMessage());
    }

    @Test
    @DisplayName("Archive resource - status is not approved")
    void archiveResource_statusNotApproved() {
        ResourceLifecycleRow row = resourceRow(PENDING, null, null);
        when(adminResourceLifecycleMapper.selectResourceLifecycle(TEST_RESOURCE_ID)).thenReturn(row);

        AppException ex = assertThrows(AppException.class, () -> service.archiveResource(TEST_RESOURCE_ID, "Olivia Admin"));

        assertEquals("Only Approved resources can be archived.", ex.getMessage());
        verify(operationHistoryService, never()).recordOperation(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Archive resource - update failed and latest status is still invalid")
    void archiveResource_updateFailed() {
        ResourceLifecycleRow row = resourceRow(APPROVED, null, null);
        ResourceLifecycleRow latest = resourceRow(PENDING, null, LocalDateTime.now());

        when(adminResourceLifecycleMapper.selectResourceLifecycle(TEST_RESOURCE_ID)).thenReturn(row, latest);
        when(adminResourceLifecycleMapper.archiveApprovedResource(eq(TEST_RESOURCE_ID), any(), eq(ARCHIVED), eq(APPROVED))).thenReturn(0);

        AppException ex = assertThrows(AppException.class, () -> service.archiveResource(TEST_RESOURCE_ID, "Olivia Admin"));

        assertEquals("Resource could not be archived from its current status.", ex.getMessage());
    }

    @Test
    @DisplayName("Archive resource - already archived")
    void archiveResource_alreadyArchived() {
        ResourceLifecycleRow row = resourceRow(ARCHIVED, LocalDateTime.now(), LocalDateTime.now());

        when(adminResourceLifecycleMapper.selectResourceLifecycle(TEST_RESOURCE_ID)).thenReturn(row);

        AdminResourceLifecycleResponse response = service.archiveResource(TEST_RESOURCE_ID, "Olivia Admin");

        assertEquals(TEST_RESOURCE_ID, response.resourceId());
        assertEquals(TEST_TITLE, response.title());
        assertEquals(ResourceReviewStatus.ARCHIVED, response.previousStatus());
        assertEquals(ResourceReviewStatus.ARCHIVED, response.resourceStatus());
        assertFalse(response.changed());
        assertEquals("Resource is already archived.", response.message());
        verify(operationHistoryService, never()).recordOperation(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Archive resource - success")
    void archiveResource_success() {
        ResourceLifecycleRow before = resourceRow(APPROVED, null, null);
        ResourceLifecycleRow after = resourceRow(ARCHIVED, LocalDateTime.now(), LocalDateTime.now());

        when(adminResourceLifecycleMapper.selectResourceLifecycle(TEST_RESOURCE_ID)).thenReturn(before, after);
        when(adminResourceLifecycleMapper.archiveApprovedResource(eq(TEST_RESOURCE_ID), any(), eq(ARCHIVED), eq(APPROVED))).thenReturn(1);

        AdminResourceLifecycleResponse response = service.archiveResource(TEST_RESOURCE_ID, "Olivia Admin");

        assertEquals(TEST_RESOURCE_ID, response.resourceId());
        assertEquals(TEST_TITLE, response.title());
        assertEquals(ResourceReviewStatus.APPROVED, response.previousStatus());
        assertEquals(ResourceReviewStatus.ARCHIVED, response.resourceStatus());
        assertTrue(response.changed());
        assertEquals("Resource archived and hidden from public discovery.", response.message());
        verify(operationHistoryService).recordOperation(
                "Test Resource (#100)",
                "Resource",
                "resource",
                "ARCHIVE_RESOURCE Approved -> Archived",
                "Olivia Admin"
        );
    }

    @Test
    @DisplayName("Archive resource - blank administrator falls back to admin")
    void archiveResource_blankAdministratorFallsBackToAdmin() {
        ResourceLifecycleRow before = resourceRow(APPROVED, null, null);
        ResourceLifecycleRow after = resourceRow(ARCHIVED, LocalDateTime.now(), LocalDateTime.now());

        when(adminResourceLifecycleMapper.selectResourceLifecycle(TEST_RESOURCE_ID)).thenReturn(before, after);
        when(adminResourceLifecycleMapper.archiveApprovedResource(eq(TEST_RESOURCE_ID), any(), eq(ARCHIVED), eq(APPROVED))).thenReturn(1);

        service.archiveResource(TEST_RESOURCE_ID, "   ");

        verify(operationHistoryService).recordOperation(
                "Test Resource (#100)",
                "Resource",
                "resource",
                "ARCHIVE_RESOURCE Approved -> Archived",
                "admin"
        );
    }

    @Test
    @DisplayName("Unarchive resource - success")
    void unarchiveResource_success() {
        ResourceLifecycleRow before = resourceRow(ARCHIVED, LocalDateTime.now(), LocalDateTime.now());
        ResourceLifecycleRow after = resourceRow(APPROVED, null, LocalDateTime.now());

        when(adminResourceLifecycleMapper.selectResourceLifecycle(TEST_RESOURCE_ID)).thenReturn(before, after);
        when(adminResourceLifecycleMapper.unarchiveResource(eq(TEST_RESOURCE_ID), any(), eq(APPROVED), eq(ARCHIVED))).thenReturn(1);

        AdminResourceLifecycleResponse response = service.unarchiveResource(TEST_RESOURCE_ID, "Olivia Admin");

        assertEquals(ResourceReviewStatus.ARCHIVED, response.previousStatus());
        assertEquals(ResourceReviewStatus.APPROVED, response.resourceStatus());
        assertTrue(response.changed());
        assertEquals("Resource restored to approved and visible to viewers.", response.message());
        verify(operationHistoryService).recordOperation(
                "Test Resource (#100)",
                "Resource",
                "resource",
                "UNARCHIVE_RESOURCE Archived -> Approved",
                "Olivia Admin"
        );
    }

    @Test
    @DisplayName("List resources - unsupported status throws bad request")
    void listResources_unsupportedStatusThrowsBadRequest() {
        AppException ex = assertThrows(AppException.class, () -> service.listResources("unsupported"));

        assertEquals("Unsupported resource status.", ex.getMessage());
    }

    private ResourceLifecycleRow resourceRow(String status, LocalDateTime archivedAt, LocalDateTime updatedAt) {
        ResourceLifecycleRow row = new ResourceLifecycleRow();
        row.setResourceId(TEST_RESOURCE_ID);
        row.setTitle(TEST_TITLE);
        row.setStatus(status);
        row.setArchivedAt(archivedAt);
        row.setUpdatedAt(updatedAt);
        return row;
    }
}
