package com.cpt202.HerLink.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.ReviewRecord;

// Data access of review record
@Mapper
public interface ReviewRecordMapper {

    int insert(ReviewRecord reviewRecord);

    ReviewRecord selectLatestByResourceId(Long resourceId);

    ReviewRecord selectLatestBySubmissionId(@Param("submissionId") Long submissionId);
}
