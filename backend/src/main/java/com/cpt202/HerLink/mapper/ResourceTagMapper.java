package com.cpt202.HerLink.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.cpt202.HerLink.entity.ResourceTag;

@Mapper
public interface ResourceTagMapper {

    int insert(ResourceTag resourceTag);

    int deleteByResourceId(@Param("resourceId") Long resourceId);

    List<Long> selectTagIdsByResourceId(@Param("resourceId") Long resourceId);

    List<String> selectTagNamesByResourceId(@Param("resourceId") Long resourceId);
}
