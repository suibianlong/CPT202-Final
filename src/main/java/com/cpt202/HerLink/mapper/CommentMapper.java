package com.cpt202.HerLink.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.Comment;

@Mapper
public interface CommentMapper {

    int insert(Comment comment);

    Comment selectById(@Param("id") Long id);

    List<Comment> selectByResourceId(@Param("resourceId") Long resourceId);

    Comment selectLatestByResourceIdAndUserIdAndContent(@Param("resourceId") Long resourceId,
                                                        @Param("userId") Long userId,
                                                        @Param("content") String content);
}
