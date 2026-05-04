package com.cpt202.HerLink.service.admin;

import com.cpt202.HerLink.dto.admin.AdminResourceLifecycleResponse;
import com.cpt202.HerLink.dto.admin.ResourceLifecycleRow;
import com.cpt202.HerLink.dto.review.ResourceReviewStatus;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.AdminResourceLifecycleMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminResourceLifecycleServiceImpl implements AdminResourceLifecycleService {

    private static final String RESOURCE_MODULE = "resource";
    private static final String RESOURCE_KIND = "Resource";
    private static final String ARCHIVE_ACTION = "ARCHIVE_RESOURCE Approved -> Archived";
    private static final int OPERATION_ITEM_NAME_MAX_LENGTH = 255;

    private final AdminResourceLifecycleMapper adminResourceLifecycleMapper;
    private final AdminOperationHistoryService operationHistoryService;

    public AdminResourceLifecycleServiceImpl(AdminResourceLifecycleMapper adminResourceLifecycleMapper,
                                             AdminOperationHistoryService operationHistoryService) {
        this.adminResourceLifecycleMapper = adminResourceLifecycleMapper;
        this.operationHistoryService = operationHistoryService;
    }

    @Override
    public List<ResourceLifecycleRow> listResources(String status) {
        String normalizedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                normalizedStatus = ResourceStatusEnum.fromValue(status).getValue();
            } catch (IllegalArgumentException exception) {
                throw AppException.badRequest("Unsupported resource status.");
            }
        }
        List<ResourceLifecycleRow> resources = adminResourceLifecycleMapper.selectResourceLifecycles(normalizedStatus);
        return resources == null ? Collections.emptyList() : resources;
    }

    @Override
    @Transactional
    public AdminResourceLifecycleResponse archiveResource(Long resourceId, String administrator) {
        ResourceLifecycleRow current = loadResource(resourceId);
        ResourceReviewStatus currentStatus = ResourceReviewStatus.fromDatabaseValue(current.getStatus());

        if (currentStatus == ResourceReviewStatus.ARCHIVED) {
            return archivedResponse(current);
        }

        if (currentStatus != ResourceReviewStatus.APPROVED) {
            throw AppException.conflict("Only Approved resources can be archived.");
        }

        LocalDateTime archivedAt = LocalDateTime.now();
        int updatedRows = adminResourceLifecycleMapper.archiveApprovedResource(
                resourceId,
                archivedAt,
                ResourceStatusEnum.ARCHIVED.getValue(),
                ResourceStatusEnum.APPROVED.getValue()
        );
        if (updatedRows == 0) {
            ResourceLifecycleRow latest = loadResource(resourceId);
            ResourceReviewStatus latestStatus = ResourceReviewStatus.fromDatabaseValue(latest.getStatus());
            if (latestStatus == ResourceReviewStatus.ARCHIVED) {
                return archivedResponse(latest);
            }
            throw AppException.conflict("Resource could not be archived from its current status.");
        }

        ResourceLifecycleRow archived = loadResource(resourceId);
        recordArchiveOperation(current, administrator);
        return new AdminResourceLifecycleResponse(
                archived.getResourceId(),
                archived.getTitle(),
                currentStatus,
                ResourceReviewStatus.fromDatabaseValue(archived.getStatus()),
                archived.getArchivedAt(),
                archived.getUpdatedAt(),
                true,
                "Resource archived and hidden from public discovery."
        );
    }

    private ResourceLifecycleRow loadResource(Long resourceId) {
        if (resourceId == null) {
            throw AppException.badRequest("Resource id is required.");
        }
        if (resourceId <= 0) {
            throw AppException.badRequest("Resource id is invalid.");
        }
        ResourceLifecycleRow resource = adminResourceLifecycleMapper.selectResourceLifecycle(resourceId);
        if (resource == null) {
            throw AppException.notFound("Resource does not exist.");
        }
        return resource;
    }

    private AdminResourceLifecycleResponse archivedResponse(ResourceLifecycleRow resource) {
        return new AdminResourceLifecycleResponse(
                resource.getResourceId(),
                resource.getTitle(),
                ResourceReviewStatus.ARCHIVED,
                ResourceReviewStatus.ARCHIVED,
                resource.getArchivedAt(),
                resource.getUpdatedAt(),
                false,
                "Resource is already archived."
        );
    }

    private void recordArchiveOperation(ResourceLifecycleRow resource, String administrator) {
        String title = resource.getTitle() == null || resource.getTitle().isBlank()
                ? "Resource"
                : resource.getTitle().trim();
        String itemName = truncate(title + " (#" + resource.getResourceId() + ")", OPERATION_ITEM_NAME_MAX_LENGTH);
        String operator = administrator == null || administrator.isBlank()
                ? "admin"
                : administrator.trim();

        operationHistoryService.recordOperation(
                itemName,
                RESOURCE_KIND,
                RESOURCE_MODULE,
                ARCHIVE_ACTION,
                operator
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
