package com.cpt202.HerLink.mapper;

import java.util.List;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.Tag;

// Data access of tag table
@Mapper
public interface TagMapper {

    List<Tag> selectAllTags();

    List<Tag> selectActiveTags();

    List<Tag> selectByStatus(@Param("status") String status);

    Tag selectById(@Param("tagId") Long tagId);

    List<Tag> selectByIds(@Param("tagIds") List<Long> tagIds);

    Tag selectByName(@Param("tagName") String tagName);

    Tag selectByNameIgnoreCase(@Param("tagName") String tagName);

    Integer countByNameIgnoreCase(@Param("tagName") String tagName,
                                  @Param("excludedTagId") Long excludedTagId);

    int refreshUsageCount(@Param("tagId") Long tagId);

    int insert(Tag tag);

    int updateTagName(@Param("tagId") Long tagId,
                      @Param("tagName") String tagName,
                      @Param("lastUpdatedAt") LocalDateTime lastUpdatedAt);

    int updateStatus(@Param("tagId") Long tagId,
                     @Param("status") String status,
                     @Param("lastUpdatedAt") LocalDateTime lastUpdatedAt);
}
