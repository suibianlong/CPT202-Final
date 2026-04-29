(() => {
    const TAG_API = "/api/admin/tags";
    const OPERATION_API = "/api/admin/operation-history";

    let tags = [];
    let usageHistory = [];
    let operationHistory = [];

    document.addEventListener("DOMContentLoaded", async () => {
        const admin = window.AdminModule;
        admin.bindAdminBasics();
        const currentUser = await admin.requireAdmin();
        if (!currentUser) return;

        bindTabs();
        bindActions();
        await loadTagData();
    });

    function bindTabs() {
        document.addEventListener("click", event => {
            const button = event.target.closest("[data-tag-tab]");
            if (!button) return;
            const tab = button.dataset.tagTab;
            document.querySelectorAll("[data-tag-tab]").forEach(item => {
                item.classList.toggle("active", item === button);
            });
            document.querySelectorAll(".admin-section").forEach(section => {
                section.classList.remove("active");
            });
            const section = document.getElementById(`tag${capitalize(tab)}Section`);
            if (section) section.classList.add("active");
        });
    }

    function bindActions() {
        const refreshButton = document.getElementById("tagRefreshBtn");
        if (refreshButton) {
            refreshButton.addEventListener("click", loadTagData);
        }

        document.addEventListener("click", async event => {
            const button = event.target.closest("[data-tag-action]");
            if (!button) return;
            const action = button.dataset.tagAction;
            const id = Number(button.dataset.id);
            if (!id) return;
            if (action === "edit") {
                await editTag(id);
                return;
            }
            if (action === "toggle") {
                await toggleTag(id);
            }
        });
    }

    async function loadTagData() {
        const admin = window.AdminModule;
        admin.setState("tagListPanel", "Loading tags...");
        admin.setState("tagUsageOverviewPanel", "Loading usage overview...");
        admin.setState("tagUsageHistoryPanel", "Loading usage history...");
        admin.setState("tagOperationPanel", "Loading operation history...");

        try {
            const [tagRows, usageRows, operationRows] = await Promise.all([
                admin.requestJson(TAG_API, { method: "GET" }),
                admin.requestJson(`${TAG_API}/usage-history`, { method: "GET" }),
                admin.requestJson(`${OPERATION_API}?module=tag`, { method: "GET" })
            ]);
            tags = Array.isArray(tagRows) ? tagRows : [];
            usageHistory = Array.isArray(usageRows) ? usageRows : [];
            operationHistory = Array.isArray(operationRows) ? operationRows : [];
            renderAll();
        } catch (error) {
            const message = admin.getErrorMessage(error, "Unable to load tag data.");
            admin.setState("tagListPanel", message, "error");
            admin.setState("tagUsageOverviewPanel", message, "error");
            admin.setState("tagUsageHistoryPanel", message, "error");
            admin.setState("tagOperationPanel", message, "error");
        }
    }

    function renderAll() {
        renderTags();
        renderUsageOverview();
        renderUsageHistory();
        renderOperationHistory();
    }

    function renderTags() {
        const panel = document.getElementById("tagListPanel");
        if (!panel) return;
        panel.innerHTML = `
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Tag Name</th>
                            <th>Status</th>
                            <th>Usage Count</th>
                            <th>Last Updated</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${tags.length ? tags.map(renderTagRow).join("") : window.AdminModule.emptyRow(6, "No tags found.")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderTagRow(item) {
        const admin = window.AdminModule;
        const inactive = String(item.status || "").toUpperCase() === "INACTIVE";
        return `
            <tr>
                <td>${item.tagId ?? "-"}</td>
                <td>${admin.escapeHtml(item.tagName || "-")}</td>
                <td>${admin.statusBadge(item.status)}</td>
                <td>${item.usageCount ?? 0}</td>
                <td>${admin.escapeHtml(admin.formatDateTime(item.lastUpdatedAt))}</td>
                <td>
                    <div class="admin-row-actions">
                        <button type="button" class="admin-btn" data-tag-action="edit" data-id="${item.tagId}">Edit</button>
                        <button type="button" class="admin-btn ${inactive ? "primary" : "danger"}" data-tag-action="toggle" data-id="${item.tagId}">
                            ${inactive ? "Activate" : "Deactivate"}
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    function renderUsageOverview() {
        const panel = document.getElementById("tagUsageOverviewPanel");
        if (!panel) return;
        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h3>Usage Overview</h3>
                    <p>Current usage count for every tag.</p>
                </div>
            </div>
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>Tag</th>
                            <th>Usage Count</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${tags.length ? tags.map(renderUsageOverviewRow).join("") : window.AdminModule.emptyRow(2, "No tag overview is available.")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderUsageOverviewRow(item) {
        return `
            <tr>
                <td>${window.AdminModule.escapeHtml(item.tagName || "-")}</td>
                <td>${item.usageCount ?? 0}</td>
            </tr>
        `;
    }

    function renderUsageHistory() {
        const panel = document.getElementById("tagUsageHistoryPanel");
        if (!panel) return;
        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h3>Usage History</h3>
                    <p>Resources currently linked to tags.</p>
                </div>
            </div>
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>Tag</th>
                            <th>Resource</th>
                            <th>Date of Use</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${usageHistory.length ? usageHistory.map(renderUsageHistoryRow).join("") : window.AdminModule.emptyRow(3, "No tag usage records found.")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderUsageHistoryRow(item) {
        const admin = window.AdminModule;
        return `
            <tr>
                <td>${admin.escapeHtml(item.tagName || "-")}</td>
                <td>${admin.escapeHtml(item.relatedRecordName || `Resource ${item.resourceId ?? "-"}`)}</td>
                <td>${admin.escapeHtml(admin.formatDateTime(item.dateOfUse))}</td>
            </tr>
        `;
    }

    function renderOperationHistory() {
        const panel = document.getElementById("tagOperationPanel");
        if (!panel) return;
        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h3>Operation History</h3>
                    <p>Admin tag actions recorded by the backend.</p>
                </div>
            </div>
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>Item</th>
                            <th>Action</th>
                            <th>Administrator</th>
                            <th>Created At</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${operationHistory.length ? operationHistory.map(renderOperationRow).join("") : window.AdminModule.emptyRow(4, "No operation history found.")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderOperationRow(item) {
        const admin = window.AdminModule;
        return `
            <tr>
                <td>${admin.escapeHtml(item.itemName || "-")}</td>
                <td>${admin.escapeHtml(item.action || "-")}</td>
                <td>${admin.escapeHtml(item.administrator || "-")}</td>
                <td>${admin.escapeHtml(admin.formatDateTime(item.createdAt))}</td>
            </tr>
        `;
    }

    async function editTag(id) {
        const tag = tags.find(item => Number(item.tagId) === id);
        if (!tag) return;
        const nextName = window.prompt("Update tag name", tag.tagName || "");
        if (nextName == null) return;
        const normalized = nextName.trim();
        if (!normalized) {
            window.AdminModule.showToast("Tag name cannot be blank.");
            return;
        }

        try {
            await window.AdminModule.jsonRequest(`${TAG_API}/${id}`, {
                method: "PUT",
                body: JSON.stringify({ tagName: normalized })
            });
            window.AdminModule.showToast("Tag updated.");
            await loadTagData();
        } catch (error) {
            window.AdminModule.showToast(window.AdminModule.getErrorMessage(error, "Unable to update tag."));
        }
    }

    async function toggleTag(id) {
        const tag = tags.find(item => Number(item.tagId) === id);
        if (!tag) return;
        const inactive = String(tag.status || "").toUpperCase() === "INACTIVE";
        const action = inactive ? "activate" : "deactivate";
        if (!window.confirm(`Are you sure you want to ${action} "${tag.tagName}"?`)) return;

        try {
            await window.AdminModule.jsonRequest(`${TAG_API}/${id}/${action}`, { method: "PUT" });
            window.AdminModule.showToast(`Tag ${action}d.`);
            await loadTagData();
        } catch (error) {
            window.AdminModule.showToast(window.AdminModule.getErrorMessage(error, `Unable to ${action} tag.`));
        }
    }

    function capitalize(value) {
        if (!value) return "";
        return value.charAt(0).toUpperCase() + value.slice(1);
    }
})();
