package com.cpt202.HerLink.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.ResourceType;

// Data access of resourceType table
@Mapper
public interface ResourceTypeMapper {

    List<ResourceType> selectAllResourceTypes();

    List<ResourceType> selectActiveResourceTypes();

    List<ResourceType> selectByStatus(@Param("status") String status);

    ResourceType selectById(@Param("resourceTypeId") Long resourceTypeId);

    ResourceType selectActiveByTypeName(@Param("typeName") String typeName);

    ResourceType selectByTypeNameIgnoreCase(@Param("typeName") String typeName);

    Integer countByTypeNameIgnoreCase(@Param("typeName") String typeName,
                                      @Param("excludedResourceTypeId") Long excludedResourceTypeId);

    int refreshUsageCount(@Param("resourceTypeId") Long resourceTypeId);

    int insert(ResourceType resourceType);

    int updateTypeName(@Param("resourceTypeId") Long resourceTypeId,
                       @Param("typeName") String typeName,
                       @Param("lastUpdatedAt") LocalDateTime lastUpdatedAt);

    int updateStatus(@Param("resourceTypeId") Long resourceTypeId,
                     @Param("status") String status,
                     @Param("lastUpdatedAt") LocalDateTime lastUpdatedAt);
}
