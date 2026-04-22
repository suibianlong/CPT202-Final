package com.cpt202.HerLink.mapper;

import com.cpt202.HerLink.dto.admin.AdminOperationHistoryResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminOperationHistoryMapper {

    int insert(@Param("itemName") String itemName,
               @Param("kind") String kind,
               @Param("module") String module,
               @Param("action") String action,
               @Param("administrator") String administrator,
               @Param("createdAt") LocalDateTime createdAt);

    List<AdminOperationHistoryResponse> selectAll();

    List<AdminOperationHistoryResponse> selectByModule(@Param("module") String module);
}
