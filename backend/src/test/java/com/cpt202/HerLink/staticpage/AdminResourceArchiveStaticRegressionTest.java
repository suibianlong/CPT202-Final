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
        assertTrue(script.contains("Archive this approved resource? It will be hidden from public discovery but kept in the system records."));
    }

    @Test
    void reviewerApprovalPage_shouldNotOfferArchiveAction() throws IOException {
        String reviewScript = Files.readString(Path.of("../frontend/module5/review-approval.js"));

        assertFalse(reviewScript.contains("data-archive-resource"));
        assertFalse(reviewScript.contains("/api/admin/resources"));
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
