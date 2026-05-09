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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminResourceLifecycleServiceImplTest {

    private static final Long RESOURCE_ID = 100L;
    private static final String APPROVED = ResourceStatusEnum.APPROVED.getValue();
    private static final String ARCHIVED = ResourceStatusEnum.ARCHIVED.getValue();
    private static final String PENDING = ResourceStatusEnum.PENDING_REVIEW.getValue();

    @Mock
    private AdminResourceLifecycleMapper mapper;

    private RecordingOperationHistory operationHistory;
    private AdminResourceLifecycleServiceImpl service;

    @BeforeEach
    void setUp() {
        operationHistory = new RecordingOperationHistory();
        service = new AdminResourceLifecycleServiceImpl(mapper, operationHistory);
    }

    @Test
    @DisplayName("List resources returns empty list when mapper returns null")
    void listResources_mapperReturnsNull_returnsEmptyList() {
        when(mapper.selectResourceLifecycles(null)).thenReturn(null);

        List<ResourceLifecycleRow> result = service.listResources(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("List resources normalizes supported status filter")
    void listResources_supportedStatus_returnsMapperRows() {
        List<ResourceLifecycleRow> rows = List.of(row(ARCHIVED, LocalDateTime.now(), LocalDateTime.now(), "Archived item"));
        when(mapper.selectResourceLifecycles(ARCHIVED)).thenReturn(rows);

        List<ResourceLifecycleRow> result = service.listResources(" archived ");

        assertEquals(rows, result);
        assertEquals(ARCHIVED, result.get(0).getStatus());
    }

    @Test
    @DisplayName("List resources rejects unsupported status filter")
    void listResources_unsupportedStatus_throwsBadRequest() {
        AppException exception = assertThrows(AppException.class, () -> service.listResources("published"));

        assertAll(
                () -> assertEquals(400, exception.getStatusCode()),
                () -> assertEquals("Unsupported resource status.", exception.getMessage())
        );
    }

    @Test
    @DisplayName("Archive resource rejects null resource id")
    void archiveResource_nullId_throwsBadRequest() {
        AppException exception = assertThrows(AppException.class, () -> service.archiveResource(null, "Olivia"));

        assertEquals("Resource id is required.", exception.getMessage());
        assertFalse(operationHistory.called);
    }

    @Test
    @DisplayName("Archive resource rejects non-positive resource id boundary")
    void archiveResource_zeroId_throwsBadRequest() {
        AppException exception = assertThrows(AppException.class, () -> service.archiveResource(0L, "Olivia"));

        assertAll(
                () -> assertEquals(400, exception.getStatusCode()),
                () -> assertEquals("Resource id is invalid.", exception.getMessage())
        );
    }

    @Test
    @DisplayName("Archive resource throws not found when mapper has no row")
    void archiveResource_missingResource_throwsNotFound() {
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(null);

        AppException exception = assertThrows(AppException.class, () -> service.archiveResource(RESOURCE_ID, "Olivia"));

        assertAll(
                () -> assertEquals(404, exception.getStatusCode()),
                () -> assertEquals("Resource does not exist.", exception.getMessage())
        );
    }

    @Test
    @DisplayName("Archive resource rejects resource that is not approved")
    void archiveResource_pendingStatus_throwsConflict() {
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(row(PENDING, null, null, "Pending item"));

        AppException exception = assertThrows(AppException.class, () -> service.archiveResource(RESOURCE_ID, "Olivia"));

        assertAll(
                () -> assertEquals(409, exception.getStatusCode()),
                () -> assertEquals("Only Approved resources can be archived.", exception.getMessage()),
                () -> assertFalse(operationHistory.called)
        );
    }

    @Test
    @DisplayName("Archive resource returns unchanged response when already archived")
    void archiveResource_alreadyArchived_returnsUnchangedResponse() {
        LocalDateTime archivedAt = LocalDateTime.now().minusDays(1);
        ResourceLifecycleRow archived = row(ARCHIVED, archivedAt, LocalDateTime.now(), "Archived item");
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(archived);

        AdminResourceLifecycleResponse response = service.archiveResource(RESOURCE_ID, "Olivia");

        assertAll(
                () -> assertEquals(RESOURCE_ID, response.resourceId()),
                () -> assertEquals(ResourceReviewStatus.ARCHIVED, response.previousStatus()),
                () -> assertEquals(ResourceReviewStatus.ARCHIVED, response.resourceStatus()),
                () -> assertEquals(archivedAt, response.archivedAt()),
                () -> assertFalse(response.changed()),
                () -> assertEquals("Resource is already archived.", response.message()),
                () -> assertFalse(operationHistory.called)
        );
    }

    @Test
    @DisplayName("Archive resource succeeds and records normalized operation")
    void archiveResource_approvedStatus_returnsChangedResponse() {
        ResourceLifecycleRow before = row(APPROVED, null, null, "  Long title  ");
        ResourceLifecycleRow after = row(ARCHIVED, LocalDateTime.now(), LocalDateTime.now(), "Long title");
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(before, after);
        when(mapper.archiveApprovedResource(eq(RESOURCE_ID), any(), eq(ARCHIVED), eq(APPROVED))).thenReturn(1);

        AdminResourceLifecycleResponse response = service.archiveResource(RESOURCE_ID, "  Olivia Admin  ");

        assertAll(
                () -> assertEquals(ResourceReviewStatus.APPROVED, response.previousStatus()),
                () -> assertEquals(ResourceReviewStatus.ARCHIVED, response.resourceStatus()),
                () -> assertTrue(response.changed()),
                () -> assertEquals("Resource archived and hidden from public discovery.", response.message()),
                () -> assertTrue(operationHistory.called),
                () -> assertEquals("Long title (#100)", operationHistory.itemName),
                () -> assertEquals("Resource", operationHistory.kind),
                () -> assertEquals("resource", operationHistory.module),
                () -> assertEquals("ARCHIVE_RESOURCE Approved -> Archived", operationHistory.action),
                () -> assertEquals("Olivia Admin", operationHistory.administrator)
        );
    }

    @Test
    @DisplayName("Archive resource handles concurrent archive as unchanged archived response")
    void archiveResource_concurrentArchive_returnsArchivedResponse() {
        ResourceLifecycleRow before = row(APPROVED, null, null, "Concurrent");
        ResourceLifecycleRow latest = row(ARCHIVED, LocalDateTime.now(), LocalDateTime.now(), "Concurrent");
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(before, latest);
        when(mapper.archiveApprovedResource(eq(RESOURCE_ID), any(), eq(ARCHIVED), eq(APPROVED))).thenReturn(0);

        AdminResourceLifecycleResponse response = service.archiveResource(RESOURCE_ID, "Olivia");

        assertAll(
                () -> assertFalse(response.changed()),
                () -> assertEquals(ResourceReviewStatus.ARCHIVED, response.resourceStatus()),
                () -> assertEquals("Resource is already archived.", response.message()),
                () -> assertFalse(operationHistory.called)
        );
    }

    @Test
    @DisplayName("Archive resource throws conflict when conditional update fails without archived latest state")
    void archiveResource_updateFailsWithoutConcurrentArchive_throwsConflict() {
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(
                row(APPROVED, null, null, "Approved"),
                row(PENDING, null, null, "Pending"));
        when(mapper.archiveApprovedResource(eq(RESOURCE_ID), any(), eq(ARCHIVED), eq(APPROVED))).thenReturn(0);

        AppException exception = assertThrows(AppException.class, () -> service.archiveResource(RESOURCE_ID, "Olivia"));

        assertEquals("Resource could not be archived from its current status.", exception.getMessage());
    }

    @Test
    @DisplayName("Unarchive resource returns unchanged response when already approved")
    void unarchiveResource_alreadyApproved_returnsUnchangedResponse() {
        ResourceLifecycleRow approved = row(APPROVED, null, LocalDateTime.now(), "Approved");
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(approved);

        AdminResourceLifecycleResponse response = service.unarchiveResource(RESOURCE_ID, "Olivia");

        assertAll(
                () -> assertFalse(response.changed()),
                () -> assertEquals(ResourceReviewStatus.APPROVED, response.previousStatus()),
                () -> assertEquals(ResourceReviewStatus.APPROVED, response.resourceStatus()),
                () -> assertNull(response.archivedAt()),
                () -> assertEquals("Resource is already approved and visible.", response.message()),
                () -> assertFalse(operationHistory.called)
        );
    }

    @Test
    @DisplayName("Unarchive resource rejects status that is not archived")
    void unarchiveResource_pendingStatus_throwsConflict() {
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(row(PENDING, null, null, "Pending"));

        AppException exception = assertThrows(AppException.class, () -> service.unarchiveResource(RESOURCE_ID, "Olivia"));

        assertEquals("Only archived resources can be unarchived.", exception.getMessage());
    }

    @Test
    @DisplayName("Unarchive resource succeeds and falls back to default administrator when blank")
    void unarchiveResource_archivedStatus_returnsChangedResponse() {
        ResourceLifecycleRow before = row(ARCHIVED, LocalDateTime.now().minusDays(1), null, "");
        ResourceLifecycleRow after = row(APPROVED, null, LocalDateTime.now(), "Restored");
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(before, after);
        when(mapper.unarchiveResource(eq(RESOURCE_ID), any(), eq(APPROVED), eq(ARCHIVED))).thenReturn(1);

        AdminResourceLifecycleResponse response = service.unarchiveResource(RESOURCE_ID, "   ");

        assertAll(
                () -> assertTrue(response.changed()),
                () -> assertEquals(ResourceReviewStatus.ARCHIVED, response.previousStatus()),
                () -> assertEquals(ResourceReviewStatus.APPROVED, response.resourceStatus()),
                () -> assertEquals("Resource restored to approved and visible to viewers.", response.message()),
                () -> assertTrue(operationHistory.called),
                () -> assertEquals("Resource (#100)", operationHistory.itemName),
                () -> assertEquals("UNARCHIVE_RESOURCE Archived -> Approved", operationHistory.action),
                () -> assertEquals("admin", operationHistory.administrator)
        );
    }

    @Test
    @DisplayName("Unarchive resource handles concurrent unarchive as unchanged approved response")
    void unarchiveResource_concurrentUnarchive_returnsApprovedResponse() {
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(
                row(ARCHIVED, LocalDateTime.now(), null, "Archived"),
                row(APPROVED, null, LocalDateTime.now(), "Approved"));
        when(mapper.unarchiveResource(eq(RESOURCE_ID), any(), eq(APPROVED), eq(ARCHIVED))).thenReturn(0);

        AdminResourceLifecycleResponse response = service.unarchiveResource(RESOURCE_ID, "Olivia");

        assertAll(
                () -> assertFalse(response.changed()),
                () -> assertEquals(ResourceReviewStatus.APPROVED, response.resourceStatus()),
                () -> assertEquals("Resource is already approved and visible.", response.message())
        );
    }

    @Test
    @DisplayName("Unarchive resource throws conflict when update fails without approved latest state")
    void unarchiveResource_updateFailsWithoutConcurrentUnarchive_throwsConflict() {
        when(mapper.selectResourceLifecycle(RESOURCE_ID)).thenReturn(
                row(ARCHIVED, LocalDateTime.now(), null, "Archived"),
                row(PENDING, null, null, "Pending"));
        when(mapper.unarchiveResource(eq(RESOURCE_ID), any(), eq(APPROVED), eq(ARCHIVED))).thenReturn(0);

        AppException exception = assertThrows(AppException.class, () -> service.unarchiveResource(RESOURCE_ID, "Olivia"));

        assertEquals("Resource could not be unarchived from its current status.", exception.getMessage());
    }

    private ResourceLifecycleRow row(String status, LocalDateTime archivedAt, LocalDateTime updatedAt, String title) {
        ResourceLifecycleRow row = new ResourceLifecycleRow();
        row.setResourceId(RESOURCE_ID);
        row.setTitle(title);
        row.setStatus(status);
        row.setArchivedAt(archivedAt);
        row.setUpdatedAt(updatedAt);
        return row;
    }

    private static class RecordingOperationHistory implements AdminOperationHistoryService {
        private boolean called;
        private String itemName;
        private String kind;
        private String module;
        private String action;
        private String administrator;

        @Override
        public void recordOperation(String itemName, String kind, String module, String action, String administrator) {
            this.called = true;
            this.itemName = itemName;
            this.kind = kind;
            this.module = module;
            this.action = action;
            this.administrator = administrator;
        }

        @Override
        public List<com.cpt202.HerLink.dto.admin.AdminOperationHistoryResponse> getOperationHistory(String module) {
            return Collections.emptyList();
        }
    }
}
