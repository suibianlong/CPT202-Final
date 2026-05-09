package com.cpt202.HerLink.service.admin;

import com.cpt202.HerLink.dto.admin.ClassificationUsageHistoryResponse;
import com.cpt202.HerLink.dto.admin.TagUsageHistoryResponse;
import com.cpt202.HerLink.mapper.AdminUsageHistoryMapper;
import java.util.List;
import org.springframework.stereotype.Service;

// Provide admin usage history data for classifications and tags.
@Service
public class AdminUsageHistoryServiceImpl implements AdminUsageHistoryService {

    private final AdminUsageHistoryMapper adminUsageHistoryMapper;

    public AdminUsageHistoryServiceImpl(AdminUsageHistoryMapper adminUsageHistoryMapper) {
        this.adminUsageHistoryMapper = adminUsageHistoryMapper;
    }

    // Return usage history for categories and resource types.
    @Override
    public List<ClassificationUsageHistoryResponse> getClassificationUsageHistory() {
        return adminUsageHistoryMapper.selectClassificationUsageHistory();
    }

    // Return usage history for tags.
    @Override
    public List<TagUsageHistoryResponse> getTagUsageHistory() {
        return adminUsageHistoryMapper.selectTagUsageHistory();
    }
}
