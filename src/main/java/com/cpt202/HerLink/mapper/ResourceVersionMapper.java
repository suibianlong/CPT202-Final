package com.cpt202.HerLink.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.ResourceVersion;

// Data access of resource version table
@Mapper
public interface ResourceVersionMapper {

    int insert(ResourceVersion resourceVersion);

    Integer selectMaxVersionNoByResourceId(@Param("resourceId") Long resourceId);

    List<ResourceVersion> selectByResourceId(@Param("resourceId") Long resourceId);

    ResourceVersion selectByResourceIdAndVersionNo(@Param("resourceId") Long resourceId,
                                                   @Param("versionNo") Integer versionNo);
}
