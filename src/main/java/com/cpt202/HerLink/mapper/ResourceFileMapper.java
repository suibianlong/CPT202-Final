package com.cpt202.HerLink.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.ResourceFile;

@Mapper
public interface ResourceFileMapper {

    int insert(ResourceFile resourceFile);

    List<ResourceFile> selectByResourceId(@Param("resourceId") Long resourceId);

    ResourceFile selectByResourceIdAndFilePath(@Param("resourceId") Long resourceId,
                                               @Param("filePath") String filePath);

    int deleteByResourceIdAndFilePath(@Param("resourceId") Long resourceId,
                                      @Param("filePath") String filePath);
}
