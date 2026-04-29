package com.cpt202.HerLink.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.ResourceSubmission;

// Data access of resource submission table
@Mapper
public interface ResourceSubmissionMapper {

    int insert(ResourceSubmission resourceSubmission);

    Integer countByResourceId(@Param("resourceId") Long resourceId);

    ResourceSubmission selectLatestByResourceId(@Param("resourceId") Long resourceId);

    List<ResourceSubmission> selectByResourceId(@Param("resourceId") Long resourceId);
}
