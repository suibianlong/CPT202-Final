(() => {
    const ADMIN_RESOURCE_API = "/api/admin/resources";
    const ARCHIVE_CONFIRMATION = "Archive this approved resource? It will be hidden from public discovery but kept in the system records.";

    let resources = [];

    document.addEventListener("DOMContentLoaded", async () => {
        const admin = window.AdminModule;
        admin.bindAdminBasics();
        const currentUser = await admin.requireAdmin();
        if (!currentUser) return;

        bindResourceActions();
        await loadResources();
    });

    function bindResourceActions() {
        const refreshButton = document.getElementById("resourceRefreshBtn");
        if (refreshButton) {
            refreshButton.addEventListener("click", loadResources);
        }

        const filterForm = document.getElementById("resourceFilterForm");
        if (filterForm) {
            filterForm.addEventListener("submit", async event => {
                event.preventDefault();
                await loadResources();
            });
        }

        document.addEventListener("click", async event => {
            const archiveButton = event.target.closest("[data-admin-resource-archive]");
            if (!archiveButton) return;
            await archiveResource(Number(archiveButton.dataset.adminResourceArchive), archiveButton);
        });
    }

    async function loadResources() {
        const admin = window.AdminModule;
        admin.setState("resourceListPanel", "Loading resources...");

        try {
            const status = document.getElementById("resourceStatusFilter")?.value || "";
            const query = status ? `?status=${encodeURIComponent(status)}` : "";
            const rows = await admin.requestJson(`${ADMIN_RESOURCE_API}${query}`, { method: "GET" });
            resources = Array.isArray(rows) ? rows : [];
            renderResources();
        } catch (error) {
            admin.setState("resourceListPanel", admin.getErrorMessage(error, "Unable to load resources."), "error");
        }
    }

    function renderResources() {
        const panel = document.getElementById("resourceListPanel");
        if (!panel) return;
        const admin = window.AdminModule;

        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h2>Resources</h2>
                    <p>${resources.length} resource${resources.length === 1 ? "" : "s"} loaded.</p>
                </div>
            </div>
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Title</th>
                            <th>Status</th>
                            <th>Archived At</th>
                            <th>Updated At</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${resources.length ? resources.map(renderResourceRow).join("") : admin.emptyRow(6, "No resources found.")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderResourceRow(resource) {
        const admin = window.AdminModule;
        const status = normalizeStatus(resource.status);
        const archiveControl = renderArchiveControl(resource, status);
        return `
            <tr>
                <td>${resource.resourceId ?? "-"}</td>
                <td>${admin.escapeHtml(resource.title || "Untitled Resource")}</td>
                <td>${admin.statusBadge(resource.status)}</td>
                <td>${admin.escapeHtml(admin.formatDateTime(resource.archivedAt))}</td>
                <td>${admin.escapeHtml(admin.formatDateTime(resource.updatedAt))}</td>
                <td>
                    <div class="admin-row-actions">
                        ${archiveControl}
                    </div>
                </td>
            </tr>
        `;
    }

    function renderArchiveControl(resource, status) {
        const admin = window.AdminModule;
        if (status === "approved") {
            return `<button type="button" class="admin-btn danger" data-admin-resource-archive="${resource.resourceId}">Archive</button>`;
        }
        if (status === "archived") {
            return admin.statusBadge("Archived");
        }
        return `<span class="admin-muted">No lifecycle action</span>`;
    }

    async function archiveResource(resourceId, button) {
        if (!resourceId) {
            window.AdminModule.showToast("Resource id is required.");
            return;
        }

        if (!window.confirm(ARCHIVE_CONFIRMATION)) {
            return;
        }

        button.disabled = true;
        try {
            const response = await window.AdminModule.jsonRequest(`${ADMIN_RESOURCE_API}/${resourceId}/archive`, {
                method: "POST"
            });
            window.AdminModule.showToast(response?.message || "Resource archived.");
            await loadResources();
        } catch (error) {
            window.AdminModule.showToast(window.AdminModule.getErrorMessage(error, "Unable to archive resource."));
        } finally {
            button.disabled = false;
        }
    }

    function normalizeStatus(value) {
        return String(value || "")
            .replaceAll("_", " ")
            .replaceAll("-", " ")
            .trim()
            .toLowerCase();
    }
})();
