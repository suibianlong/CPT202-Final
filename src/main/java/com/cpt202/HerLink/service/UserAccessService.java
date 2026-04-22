package com.cpt202.HerLink.service;

import com.cpt202.HerLink.dto.auth.AccountUpdateRequest;
import com.cpt202.HerLink.dto.auth.ContributorReviewDecisionRequest;
import com.cpt202.HerLink.dto.auth.ContributorRequestSubmitRequest;
import com.cpt202.HerLink.dto.auth.LoginRequest;
import com.cpt202.HerLink.dto.auth.RegisterRequest;
import com.cpt202.HerLink.dto.auth.RegisterVerificationCodeRequest;
import com.cpt202.HerLink.vo.ContributorRequestVO;
import com.cpt202.HerLink.vo.CurrentUserVO;
import java.util.List;

public interface UserAccessService {

    void sendRegisterVerificationCode(RegisterVerificationCodeRequest request);

    CurrentUserVO register(RegisterRequest request);

    CurrentUserVO login(LoginRequest request);

    CurrentUserVO getCurrentUserById(Long userId);

    CurrentUserVO updateAccount(Long userId, AccountUpdateRequest request);

    ContributorRequestVO submitContributorRequest(Long userId, ContributorRequestSubmitRequest request);

    ContributorRequestVO getMyLatestContributorRequest(Long userId);

    List<ContributorRequestVO> listPendingContributorRequests();

    ContributorRequestVO reviewContributorRequest(Long adminUserId,
                                                  Long requestId,
                                                  ContributorReviewDecisionRequest request);
}
