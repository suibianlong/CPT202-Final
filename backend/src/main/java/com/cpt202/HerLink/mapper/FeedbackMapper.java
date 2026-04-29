package com.cpt202.HerLink.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.Feedback;

@Mapper
public interface FeedbackMapper {

    int insert(Feedback feedback);

    Feedback selectById(@Param("feedbackId") Long feedbackId);

    List<Feedback> selectByUserId(@Param("userId") Long userId);

    List<Feedback> selectAll();
}
