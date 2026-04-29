package com.cpt202.HerLink.service.admin;

import com.cpt202.HerLink.dto.admin.ClassificationUsageHistoryResponse;
import com.cpt202.HerLink.dto.admin.TagUsageHistoryResponse;
import com.cpt202.HerLink.mapper.AdminUsageHistoryMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminUsageHistoryServiceImpl implements AdminUsageHistoryService {

    private final AdminUsageHistoryMapper adminUsageHistoryMapper;

    public AdminUsageHistoryServiceImpl(AdminUsageHistoryMapper adminUsageHistoryMapper) {
        this.adminUsageHistoryMapper = adminUsageHistoryMapper;
    }

    @Override
    public List<ClassificationUsageHistoryResponse> getClassificationUsageHistory() {
        return adminUsageHistoryMapper.selectClassificationUsageHistory();
    }

    @Override
    public List<TagUsageHistoryResponse> getTagUsageHistory() {
        return adminUsageHistoryMapper.selectTagUsageHistory();
    }
}
