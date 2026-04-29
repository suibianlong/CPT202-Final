package com.cpt202.HerLink.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.AttachedFile;

@Mapper
public interface AttachedFileMapper {

    int insert(AttachedFile attachedFile);

    List<AttachedFile> selectByFeedbackId(@Param("feedbackId") Long feedbackId);
}
