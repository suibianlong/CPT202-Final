package com.cpt202.HerLink.mapper;

import com.cpt202.HerLink.dto.admin.ResourceLifecycleRow;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminResourceLifecycleMapper {

    ResourceLifecycleRow selectResourceLifecycle(@Param("resourceId") Long resourceId);

    int archiveApprovedResource(@Param("resourceId") Long resourceId,
                                @Param("archivedAt") LocalDateTime archivedAt,
                                @Param("archivedStatus") String archivedStatus,
                                @Param("approvedStatus") String approvedStatus);
}
