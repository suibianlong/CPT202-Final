package com.cpt202.HerLink.service.admin;

import com.cpt202.HerLink.dto.admin.AdminResourceLifecycleResponse;
import com.cpt202.HerLink.dto.admin.ResourceLifecycleRow;
import java.util.List;

public interface AdminResourceLifecycleService {

    List<ResourceLifecycleRow> listResources(String status);

    default AdminResourceLifecycleResponse archiveResource(Long resourceId) {
        return archiveResource(resourceId, null);
    }

    AdminResourceLifecycleResponse archiveResource(Long resourceId, String administrator);

    default AdminResourceLifecycleResponse unarchiveResource(Long resourceId) {
        return unarchiveResource(resourceId, null);
    }

    AdminResourceLifecycleResponse unarchiveResource(Long resourceId, String administrator);
}
