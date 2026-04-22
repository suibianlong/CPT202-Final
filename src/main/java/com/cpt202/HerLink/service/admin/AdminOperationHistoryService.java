package com.cpt202.HerLink.service.admin;

import com.cpt202.HerLink.dto.admin.AdminOperationHistoryResponse;
import java.util.List;

public interface AdminOperationHistoryService {

    void recordOperation(String itemName, String kind, String module, String action, String administrator);

    List<AdminOperationHistoryResponse> getOperationHistory(String module);
}
