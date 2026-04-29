package com.cpt202.HerLink.mapper;

import com.cpt202.HerLink.entity.ContributorRequest;
import com.cpt202.HerLink.vo.ContributorRequestVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContributorRequestMapper {

    int insert(ContributorRequest contributorRequest);

    ContributorRequest selectLatestByUserId(@Param("userId") Long userId);

    ContributorRequest selectById(@Param("requestId") Long requestId);

    ContributorRequest selectByIdForUpdate(@Param("requestId") Long requestId);

    int updateReviewDecision(ContributorRequest contributorRequest);

    ContributorRequestVO selectRequestViewById(@Param("requestId") Long requestId);

    List<ContributorRequestVO> selectPendingRequestViews();

    List<ContributorRequestVO> selectApprovedContributorViews();
}
