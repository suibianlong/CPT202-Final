package com.cpt202.HerLink.service.admin;

import com.cpt202.HerLink.dto.admin.ClassificationUsageHistoryResponse;
import com.cpt202.HerLink.dto.admin.TagUsageHistoryResponse;
import java.util.List;

public interface AdminUsageHistoryService {

    List<ClassificationUsageHistoryResponse> getClassificationUsageHistory();

    List<TagUsageHistoryResponse> getTagUsageHistory();
}
