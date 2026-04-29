package com.cpt202.HerLink.service.admin;

import com.cpt202.HerLink.dto.admin.AdminResourceLifecycleResponse;
import com.cpt202.HerLink.dto.admin.ResourceLifecycleRow;
import com.cpt202.HerLink.dto.review.ResourceReviewStatus;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.AdminResourceLifecycleMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminResourceLifecycleServiceImpl implements AdminResourceLifecycleService {

    private final AdminResourceLifecycleMapper adminResourceLifecycleMapper;

    public AdminResourceLifecycleServiceImpl(AdminResourceLifecycleMapper adminResourceLifecycleMapper) {
        this.adminResourceLifecycleMapper = adminResourceLifecycleMapper;
    }

    @Override
    @Transactional
    public AdminResourceLifecycleResponse archiveResource(Long resourceId) {
        ResourceLifecycleRow current = loadResource(resourceId);
        ResourceReviewStatus currentStatus = ResourceReviewStatus.fromDatabaseValue(current.getStatus());

        if (currentStatus == ResourceReviewStatus.ARCHIVED) {
            return new AdminResourceLifecycleResponse(
                    current.getResourceId(),
                    current.getTitle(),
                    currentStatus,
                    currentStatus,
                    current.getArchivedAt(),
                    current.getUpdatedAt(),
                    false,
                    "Resource is already archived."
            );
        }

        if (currentStatus != ResourceReviewStatus.APPROVED) {
            throw AppException.conflict("Only Approved resources can be archived through admin published-resource management.");
        }

        LocalDateTime archivedAt = LocalDateTime.now();
        int updatedRows = adminResourceLifecycleMapper.archiveApprovedResource(
                resourceId,
                archivedAt,
                ResourceStatusEnum.ARCHIVED.getValue(),
                ResourceStatusEnum.APPROVED.getValue()
        );
        if (updatedRows == 0) {
            throw AppException.conflict("Resource could not be archived from its current status.");
        }

        ResourceLifecycleRow archived = loadResource(resourceId);
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
        ResourceLifecycleRow resource = adminResourceLifecycleMapper.selectResourceLifecycle(resourceId);
        if (resource == null) {
            throw AppException.notFound("Resource does not exist.");
        }
        return resource;
    }
}
