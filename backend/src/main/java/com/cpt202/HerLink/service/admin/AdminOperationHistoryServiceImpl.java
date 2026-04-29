package com.cpt202.HerLink.service.admin;

import com.cpt202.HerLink.dto.admin.AdminOperationHistoryResponse;
import com.cpt202.HerLink.mapper.AdminOperationHistoryMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminOperationHistoryServiceImpl implements AdminOperationHistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminOperationHistoryServiceImpl.class);

    private final AdminOperationHistoryMapper adminOperationHistoryMapper;

    public AdminOperationHistoryServiceImpl(AdminOperationHistoryMapper adminOperationHistoryMapper) {
        this.adminOperationHistoryMapper = adminOperationHistoryMapper;
    }

    @Override
    public void recordOperation(String itemName, String kind, String module, String action, String administrator) {
        try {
            adminOperationHistoryMapper.insert(
                    itemName,
                    kind,
                    module,
                    action,
                    administrator,
                    LocalDateTime.now()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Admin operation history could not be recorded.", exception);
        }
    }

    @Override
    public List<AdminOperationHistoryResponse> getOperationHistory(String module) {
        if (module == null || module.isBlank()) {
            return adminOperationHistoryMapper.selectAll();
        }
        return adminOperationHistoryMapper.selectByModule(module.trim());
    }
}
