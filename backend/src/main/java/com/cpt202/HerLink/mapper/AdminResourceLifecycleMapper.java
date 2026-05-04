package com.cpt202.HerLink.mapper;

import com.cpt202.HerLink.dto.admin.ResourceLifecycleRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminResourceLifecycleMapper {

    List<ResourceLifecycleRow> selectResourceLifecycles(@Param("status") String status);

    ResourceLifecycleRow selectResourceLifecycle(@Param("resourceId") Long resourceId);

    int archiveApprovedResource(@Param("resourceId") Long resourceId,
                                @Param("archivedAt") LocalDateTime archivedAt,
                                @Param("archivedStatus") String archivedStatus,
                                @Param("approvedStatus") String approvedStatus);

    int unarchiveResource(@Param("resourceId") Long resourceId,
                          @Param("updatedAt") LocalDateTime updatedAt,
                          @Param("approvedStatus") String approvedStatus,
                          @Param("archivedStatus") String archivedStatus);
}
