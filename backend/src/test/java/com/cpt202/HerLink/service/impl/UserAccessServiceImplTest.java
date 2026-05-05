package com.cpt202.HerLink.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cpt202.HerLink.dto.auth.AccountUpdateRequest;
import com.cpt202.HerLink.dto.auth.ContributorRequestSubmitRequest;
import com.cpt202.HerLink.dto.auth.LoginRequest;
import com.cpt202.HerLink.dto.auth.RegisterVerificationCodeRequest;
import com.cpt202.HerLink.exception.AppException;

public class UserAccessServiceImplTest {

    private UserAccessServiceImpl userAccessService;

    @BeforeEach
    void setUp() {
        // 所有 Mapper / 依赖传入 null，只测试【纯参数校验】
        userAccessService = new UserAccessServiceImpl(
                null, null, null, null, null, null,
                "", "", 10, 60
        );
    }

    // ========================== 能测：参数 null / 格式 / 非法值 异常 ==========================

    // 1. 发送验证码 - 邮箱为空 / 格式错误
    @Test
    void sendRegisterVerificationCode_InvalidEmail_ShouldThrow() {
        RegisterVerificationCodeRequest nullEmail = new RegisterVerificationCodeRequest();
        nullEmail.setEmail(null);
        assertThrows(AppException.class, () -> userAccessService.sendRegisterVerificationCode(nullEmail));

        RegisterVerificationCodeRequest badEmail = new RegisterVerificationCodeRequest();
        badEmail.setEmail("not-an-email");
        assertThrows(AppException.class, () -> userAccessService.sendRegisterVerificationCode(badEmail));
    }

    // 2. 注册 - 请求体为 null
    @Test
    void register_RequestNull_ShouldThrow() {
        assertThrows(AppException.class, () -> userAccessService.register(null));
    }

    // 3. 登录 - 请求体为 null / 邮箱密码为空
    @Test
    void login_InvalidInput_ShouldThrow() {
        assertThrows(AppException.class, () -> userAccessService.login(null));

        LoginRequest empty = new LoginRequest();
        empty.setEmail(null);
        empty.setPassword(null);
        assertThrows(AppException.class, () -> userAccessService.login(empty));
    }

    // 4. 获取当前用户 - userId 为 null
    @Test
    void getCurrentUserById_UserIdNull_ShouldThrow() {
        assertThrows(AppException.class, () -> userAccessService.getCurrentUserById(null));
    }

    // 5. 更新账户 - 输入非法（name/email/bio）
    @Test
    void updateAccount_InvalidInput_ShouldThrow() {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setName(null);
        request.setEmail(null);
        assertThrows(AppException.class, () -> userAccessService.updateAccount(1L, request));
    }

    // 6. 提交创作者申请 - 申请理由为空 / 超长
    @Test
    void submitContributorRequest_InvalidReason_ShouldThrow() {
        ContributorRequestSubmitRequest nullReason = new ContributorRequestSubmitRequest();
        nullReason.setApplicationReason(null);
        assertThrows(AppException.class, () -> userAccessService.submitContributorRequest(1L, nullReason));

        ContributorRequestSubmitRequest longReason = new ContributorRequestSubmitRequest();
        longReason.setApplicationReason("a".repeat(2001));
        assertThrows(AppException.class, () -> userAccessService.submitContributorRequest(1L, longReason));
    }

    // 7. 获取申请详情 - ID 为 null
    @Test
    void getContributorRequestDetail_IdNull_ShouldThrow() {
        assertThrows(AppException.class, () -> userAccessService.getContributorRequestDetail(null));
    }

    // ========================== 以下 PUBLIC 方法 100% 依赖数据库 → 单元测试无法运行 ==========================

    @Test
    void getMyLatestContributorRequest_UnTestable_InUnitTest() {}

    @Test
    void listPendingContributorRequests_UnTestable_InUnitTest() {}

    @Test
    void listApprovedContributors_UnTestable_InUnitTest() {}

    @Test
    void reviewContributorRequest_UnTestable_InUnitTest() {}

    @Test
    void revokeContributor_UnTestable_InUnitTest() {}
}