package com.cpt202.HerLink.service;

import com.cpt202.HerLink.dto.admin.AdminResourceLifecycleResponse;
import com.cpt202.HerLink.dto.admin.ResourceLifecycleRow;
import com.cpt202.HerLink.dto.review.ResourceReviewStatus;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.mapper.AdminResourceLifecycleMapper;
import com.cpt202.HerLink.service.admin.AdminResourceLifecycleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminResourceLifecycleServiceImplTest {

    @Mock
    private AdminResourceLifecycleMapper adminResourceLifecycleMapper;

    @Test
    void archiveResource_shouldArchiveApprovedResource() {
        AdminResourceLifecycleServiceImpl service = new AdminResourceLifecycleServiceImpl(adminResourceLifecycleMapper);
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
        )).thenReturn(1);

        AdminResourceLifecycleResponse result = service.archiveResource(5L);

        assertTrue(result.changed());
        assertEquals(ResourceReviewStatus.APPROVED, result.previousStatus());
        assertEquals(ResourceReviewStatus.ARCHIVED, result.resourceStatus());
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
