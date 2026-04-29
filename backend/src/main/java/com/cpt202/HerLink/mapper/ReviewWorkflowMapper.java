package com.cpt202.HerLink.mapper;

import com.cpt202.HerLink.dto.review.ReviewHistoryRow;
import com.cpt202.HerLink.dto.review.ReviewSubmissionRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewWorkflowMapper {

    long countPendingReviews();

    List<ReviewSubmissionRow> selectPendingReviews(@Param("offset") int offset,
                                                   @Param("pageSize") int pageSize);

    ReviewSubmissionRow selectSubmissionDetail(@Param("submissionId") Long submissionId);

    List<ReviewHistoryRow> selectReviewHistoryRows(@Param("resourceId") Long resourceId);

    int updateResourceAfterDecision(@Param("resourceId") Long resourceId,
                                    @Param("status") String status,
                                    @Param("reviewedAt") LocalDateTime reviewedAt,
                                    @Param("expectedStatus") String expectedStatus);
}
