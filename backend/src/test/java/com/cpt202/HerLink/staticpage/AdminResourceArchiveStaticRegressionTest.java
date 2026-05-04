package com.cpt202.HerLink.staticpage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminResourceArchiveStaticRegressionTest {

    @Test
    void adminDashboard_shouldExposeResourceManagementPage() throws IOException {
        String dashboard = Files.readString(Path.of("../frontend/admin-dashboard.html"));
        String page = Files.readString(Path.of("../frontend/admin-resources.html"));
        String script = Files.readString(Path.of("../frontend/module7/admin-resources.js"));

        assertTrue(dashboard.contains("admin-resources.html"));
        assertTrue(page.contains("resourceListPanel"));
        assertTrue(script.contains("/api/admin/resources"));
        assertTrue(script.contains("data-admin-resource-archive"));
        assertTrue(script.contains("data-admin-resource-unarchive"));
        assertTrue(script.contains("Archive this approved resource? It will be hidden from public discovery but kept in the system records."));
        assertTrue(script.contains("Unarchive this resource? It will become approved and visible to viewers again."));
        assertTrue(script.contains("`${ADMIN_RESOURCE_API}/${resourceId}/unarchive`"));
        assertTrue(script.contains("Resource restored to approved and visible to viewers."));
    }

    @Test
    void adminResources_shouldRenderLifecycleButtonsByStatus() throws IOException {
        String script = Files.readString(Path.of("../frontend/module7/admin-resources.js"));

        assertTrue(script.contains("status === \"approved\""));
        assertTrue(script.contains("data-admin-resource-archive"));
        assertTrue(script.contains(">Archive</button>"));
        assertTrue(script.contains("status === \"archived\""));
        assertTrue(script.contains("data-admin-resource-unarchive"));
        assertTrue(script.contains(">Unarchive</button>"));
        assertTrue(script.contains("No lifecycle action"));
    }

    @Test
    void reviewerApprovalPage_shouldNotOfferArchiveOrUnarchiveAction() throws IOException {
        String reviewScript = Files.readString(Path.of("../frontend/module5/review-approval.js"));

        assertFalse(reviewScript.contains("data-archive-resource"));
        assertFalse(reviewScript.contains("data-admin-resource-archive"));
        assertFalse(reviewScript.contains("data-admin-resource-unarchive"));
        assertFalse(reviewScript.contains("/api/admin/resources"));
        assertFalse(reviewScript.contains("/archive"));
        assertFalse(reviewScript.contains("/unarchive"));
    }

    @Test
    void viewerPage_shouldNotOfferArchiveOrUnarchiveAction() throws IOException {
        String viewerScript = Files.readString(Path.of("../frontend/module6/module6.js"));

        assertFalse(viewerScript.contains("data-admin-resource-archive"));
        assertFalse(viewerScript.contains("data-admin-resource-unarchive"));
        assertFalse(viewerScript.contains("/api/admin/resources"));
        assertFalse(viewerScript.contains("/archive"));
        assertFalse(viewerScript.contains("/unarchive"));
    }

    @Test
    void contributorMyResources_shouldShowArchivedAsNonEditable() throws IOException {
        String myResourcesHtml = Files.readString(Path.of("../frontend/my-resources.html"));
        String script = Files.readString(Path.of("../frontend/module3/module3.js"));

        assertTrue(myResourcesHtml.contains("<option value=\"Archived\">Archived</option>"));
        assertTrue(script.contains("renderMyResourceEditAction"));
        assertTrue(script.contains("Archived resources are kept in system records and cannot be edited or resubmitted."));
    }
}
