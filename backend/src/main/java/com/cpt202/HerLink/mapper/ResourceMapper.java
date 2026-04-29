package com.cpt202.HerLink.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.Resource;

// Data access of resource table
@Mapper
public interface ResourceMapper {

    int insert(Resource resource);

    Resource selectById(@Param("id") Long id);

    Resource selectByIdForUpdate(@Param("id") Long id);

    int updateById(Resource resource);

    List<Resource> selectMyResources(@Param("contributorId") Long contributorId,
                                     @Param("keyword") String keyword,
                                     @Param("status") String status,
                                     @Param("categoryId") Long categoryId);

    List<Resource> selectApprovedResources(@Param("keyword") String keyword,
                                           @Param("resourceTypeId") Long resourceTypeId,
                                           @Param("categoryId") Long categoryId,
                                           @Param("sortBy") String sortBy);

    Resource selectApprovedById(@Param("id") Long id);
}
