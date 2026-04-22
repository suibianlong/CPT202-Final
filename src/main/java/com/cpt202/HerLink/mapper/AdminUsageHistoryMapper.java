package com.cpt202.HerLink.mapper;

import com.cpt202.HerLink.dto.admin.ClassificationUsageHistoryResponse;
import com.cpt202.HerLink.dto.admin.TagUsageHistoryResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUsageHistoryMapper {

    List<ClassificationUsageHistoryResponse> selectClassificationUsageHistory();

    List<TagUsageHistoryResponse> selectTagUsageHistory();
}
