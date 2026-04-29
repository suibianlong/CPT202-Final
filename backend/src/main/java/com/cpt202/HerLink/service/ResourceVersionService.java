package com.cpt202.HerLink.service;

import java.util.List;

import com.cpt202.HerLink.vo.ResourceDetailVO;
import com.cpt202.HerLink.vo.ResourceVersionCompareVO;
import com.cpt202.HerLink.vo.ResourceVersionVO;

public interface ResourceVersionService {

    void saveVersionSnapshot(Long resourceId, Long userId, String changeType, String changeSummary);

    List<ResourceVersionVO> listVersions(Long currentUserId, Long resourceId);

    ResourceVersionVO getVersion(Long currentUserId, Long resourceId, Integer versionNo);

    ResourceVersionCompareVO compareVersions(Long currentUserId, Long resourceId, Integer leftVersionNo, Integer rightVersionNo);

    ResourceDetailVO rollbackToVersion(Long currentUserId, Long resourceId, Integer versionNo);
}
