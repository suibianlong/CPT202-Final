(() => {
    const CATEGORY_API = "/api/admin/categories";
    const RESOURCE_TYPE_API = "/api/admin/resource-types";
    const CLASSIFICATION_API = "/api/admin/classifications";
    const OPERATION_API = "/api/admin/operation-history";

    let categories = [];
    let resourceTypes = [];
    let usageHistory = [];
    let operationHistory = [];

    document.addEventListener("DOMContentLoaded", async () => {
        const admin = window.AdminModule;
        admin.bindAdminBasics();
        const currentUser = await admin.requireAdmin();
        if (!currentUser) return;

        bindTabs();
        bindActions();
        await loadClassificationData();
    });

    function bindTabs() {
        document.addEventListener("click", event => {
            const button = event.target.closest("[data-classification-tab]");
            if (!button) return;
            const tab = button.dataset.classificationTab;
            document.querySelectorAll("[data-classification-tab]").forEach(item => {
                item.classList.toggle("active", item === button);
            });
            document.querySelectorAll(".admin-section").forEach(section => {
                section.classList.remove("active");
            });
            const section = document.getElementById(`classification${capitalize(tab)}Section`);
            if (section) section.classList.add("active");
        });
    }

    function bindActions() {
        const refreshButton = document.getElementById("classificationRefreshBtn");
        if (refreshButton) {
            refreshButton.addEventListener("click", loadClassificationData);
        }

        const resourceTypeForm = document.getElementById("resourceTypeCreateForm");
        if (resourceTypeForm) {
            resourceTypeForm.addEventListener("submit", async event => {
                event.preventDefault();
                const input = document.getElementById("resourceTypeName");
                await createResourceType(input);
            });
        }

        const categoryForm = document.getElementById("categoryCreateForm");
        if (categoryForm) {
            categoryForm.addEventListener("submit", async event => {
                event.preventDefault();
                const input = document.getElementById("categoryTopic");
                await createCategory(input);
            });
        }

        document.addEventListener("click", async event => {
            const button = event.target.closest("[data-classification-action]");
            if (!button) return;
            const action = button.dataset.classificationAction;
            const kind = button.dataset.kind;
            const id = Number(button.dataset.id);
            if (!id || !kind) return;

            if (action === "edit") {
                await editClassificationItem(kind, id);
                return;
            }
            if (action === "toggle") {
                await toggleClassificationItem(kind, id);
            }
        });
    }

    async function loadClassificationData() {
        const admin = window.AdminModule;
        admin.setState("resourceTypePanel", "Loading resource types...");
        admin.setState("categoryPanel", "Loading categories...");
        admin.setState("classificationUsageOverviewPanel", "Loading usage overview...");
        admin.setState("classificationUsageHistoryPanel", "Loading usage history...");
        admin.setState("classificationOperationPanel", "Loading operation history...");

        try {
            const [types, topics, usage, operations] = await Promise.all([
                admin.requestJson(RESOURCE_TYPE_API, { method: "GET" }),
                admin.requestJson(CATEGORY_API, { method: "GET" }),
                admin.requestJson(`${CLASSIFICATION_API}/usage-history`, { method: "GET" }),
                admin.requestJson(`${OPERATION_API}?module=classification`, { method: "GET" })
            ]);
            resourceTypes = Array.isArray(types) ? types : [];
            categories = Array.isArray(topics) ? topics : [];
            usageHistory = Array.isArray(usage) ? usage : [];
            operationHistory = Array.isArray(operations) ? operations : [];
            renderAll();
        } catch (error) {
            const message = admin.getErrorMessage(error, "Unable to load classification data.");
            admin.setState("resourceTypePanel", message, "error");
            admin.setState("categoryPanel", message, "error");
            admin.setState("classificationUsageOverviewPanel", message, "error");
            admin.setState("classificationUsageHistoryPanel", message, "error");
            admin.setState("classificationOperationPanel", message, "error");
        }
    }

    function renderAll() {
        renderResourceTypes();
        renderCategories();
        renderUsageOverview();
        renderUsageHistory();
        renderOperationHistory();
    }

    function renderResourceTypes() {
        const panel = document.getElementById("resourceTypePanel");
        if (!panel) return;
        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h3>Resource Types</h3>
                    <p>${resourceTypes.length} type${resourceTypes.length === 1 ? "" : "s"} loaded from the backend.</p>
                </div>
            </div>
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Type Name</th>
                            <th>Status</th>
                            <th>Usage Count</th>
                            <th>Last Updated</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${resourceTypes.length ? resourceTypes.map(renderResourceTypeRow).join("") : window.AdminModule.emptyRow(6, "No resource types found.")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderResourceTypeRow(item) {
        const admin = window.AdminModule;
        const inactive = String(item.status || "").toUpperCase() === "INACTIVE";
        return `
            <tr>
                <td>${item.resourceTypeId ?? "-"}</td>
                <td>${admin.escapeHtml(item.typeName || "-")}</td>
                <td>${admin.statusBadge(item.status)}</td>
                <td>${item.usageCount ?? 0}</td>
                <td>${admin.escapeHtml(admin.formatDateTime(item.lastUpdatedAt))}</td>
                <td>
                    <div class="admin-row-actions">
                        <button type="button" class="admin-btn" data-classification-action="edit" data-kind="type" data-id="${item.resourceTypeId}">Edit</button>
                        <button type="button" class="admin-btn ${inactive ? "primary" : "danger"}" data-classification-action="toggle" data-kind="type" data-id="${item.resourceTypeId}">
                            ${inactive ? "Activate" : "Deactivate"}
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    function renderCategories() {
        const panel = document.getElementById("categoryPanel");
        if (!panel) return;
        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h3>Category Topics</h3>
                    <p>${categories.length} categor${categories.length === 1 ? "y" : "ies"} loaded from the backend.</p>
                </div>
            </div>
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Category Topic</th>
                            <th>Status</th>
                            <th>Usage Count</th>
                            <th>Last Updated</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${categories.length ? categories.map(renderCategoryRow).join("") : window.AdminModule.emptyRow(6, "No categories found.")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderCategoryRow(item) {
        const admin = window.AdminModule;
        const inactive = String(item.status || "").toUpperCase() === "INACTIVE";
        return `
            <tr>
                <td>${item.categoryId ?? "-"}</td>
                <td>${admin.escapeHtml(item.categoryTopic || "-")}</td>
                <td>${admin.statusBadge(item.status)}</td>
                <td>${item.usageCount ?? 0}</td>
                <td>${admin.escapeHtml(admin.formatDateTime(item.lastUpdatedAt))}</td>
                <td>
                    <div class="admin-row-actions">
                        <button type="button" class="admin-btn" data-classification-action="edit" data-kind="category" data-id="${item.categoryId}">Edit</button>
                        <button type="button" class="admin-btn ${inactive ? "primary" : "danger"}" data-classification-action="toggle" data-kind="category" data-id="${item.categoryId}">
                            ${inactive ? "Activate" : "Deactivate"}
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }

    function renderUsageOverview() {
        const panel = document.getElementById("classificationUsageOverviewPanel");
        if (!panel) return;
        const rows = [
            ...resourceTypes.map(item => ({ name: item.typeName, kind: "Type", usageCount: item.usageCount })),
            ...categories.map(item => ({ name: item.categoryTopic, kind: "Topic", usageCount: item.usageCount }))
        ];
        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h3>Usage Overview</h3>
                    <p>Current usage count for every type and topic.</p>
                </div>
            </div>
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Kind</th>
                            <th>Usage Count</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${rows.length ? rows.map(renderUsageOverviewRow).join("") : window.AdminModule.emptyRow(3, "No usage overview is available.")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderUsageOverviewRow(item) {
        const admin = window.AdminModule;
        return `
            <tr>
                <td>${admin.escapeHtml(item.name || "-")}</td>
                <td>${admin.escapeHtml(item.kind || "-")}</td>
                <td>${item.usageCount ?? 0}</td>
            </tr>
        `;
    }

    function renderUsageHistory() {
        const panel = document.getElementById("classificationUsageHistoryPanel");
        if (!panel) return;
        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h3>Usage History</h3>
                    <p>Resources currently linked to classifications.</p>
                </div>
            </div>
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Kind</th>
                            <th>Resource</th>
                            <th>Date of Use</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${usageHistory.length ? usageHistory.map(renderUsageHistoryRow).join("") : window.AdminModule.emptyRow(4, "No resource usage records found.")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderUsageHistoryRow(item) {
        const admin = window.AdminModule;
        return `
            <tr>
                <td>${admin.escapeHtml(item.name || "-")}</td>
                <td>${admin.escapeHtml(item.kind || "-")}</td>
                <td>${admin.escapeHtml(item.relatedRecordName || `Resource ${item.resourceId ?? "-"}`)}</td>
                <td>${admin.escapeHtml(admin.formatDateTime(item.dateOfUse))}</td>
            </tr>
        `;
    }

    function renderOperationHistory() {
        const panel = document.getElementById("classificationOperationPanel");
        if (!panel) return;
        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h3>Operation History</h3>
                    <p>Admin classification actions recorded by the backend.</p>
                </div>
            </div>
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>Item</th>
                            <th>Kind</th>
                            <th>Action</th>
                            <th>Administrator</th>
                            <th>Created At</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${operationHistory.length ? operationHistory.map(renderOperationRow).join("") : window.AdminModule.emptyRow(5, "No operation history found.")}
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
                <td>${admin.escapeHtml(item.kind || "-")}</td>
                <td>${admin.escapeHtml(item.action || "-")}</td>
                <td>${admin.escapeHtml(item.administrator || "-")}</td>
                <td>${admin.escapeHtml(admin.formatDateTime(item.createdAt))}</td>
            </tr>
        `;
    }

    async function createResourceType(input) {
        const name = input ? input.value.trim() : "";
        if (!name) {
            window.AdminModule.showToast("Resource type name is required.");
            return;
        }
        try {
            await window.AdminModule.jsonRequest(RESOURCE_TYPE_API, {
                method: "POST",
                body: JSON.stringify({ typeName: name })
            });
            if (input) input.value = "";
            window.AdminModule.showToast("Resource type created.");
            await loadClassificationData();
        } catch (error) {
            window.AdminModule.showToast(window.AdminModule.getErrorMessage(error, "Unable to create resource type."));
        }
    }

    async function createCategory(input) {
        const topic = input ? input.value.trim() : "";
        if (!topic) {
            window.AdminModule.showToast("Category topic is required.");
            return;
        }
        try {
            await window.AdminModule.jsonRequest(CATEGORY_API, {
                method: "POST",
                body: JSON.stringify({ categoryTopic: topic })
            });
            if (input) input.value = "";
            window.AdminModule.showToast("Category created.");
            await loadClassificationData();
        } catch (error) {
            window.AdminModule.showToast(window.AdminModule.getErrorMessage(error, "Unable to create category."));
        }
    }

    async function editClassificationItem(kind, id) {
        const isType = kind === "type";
        const item = isType
            ? resourceTypes.find(type => Number(type.resourceTypeId) === id)
            : categories.find(category => Number(category.categoryId) === id);
        if (!item) return;

        const currentName = isType ? item.typeName : item.categoryTopic;
        const nextName = window.prompt(`Update ${isType ? "resource type" : "category"} name`, currentName || "");
        if (nextName == null) return;
        const normalized = nextName.trim();
        if (!normalized) {
            window.AdminModule.showToast("Name cannot be blank.");
            return;
        }

        try {
            await window.AdminModule.jsonRequest(`${isType ? RESOURCE_TYPE_API : CATEGORY_API}/${id}`, {
                method: "PUT",
                body: JSON.stringify(isType ? { typeName: normalized } : { categoryTopic: normalized })
            });
            window.AdminModule.showToast("Classification item updated.");
            await loadClassificationData();
        } catch (error) {
            window.AdminModule.showToast(window.AdminModule.getErrorMessage(error, "Unable to update item."));
        }
    }

    async function toggleClassificationItem(kind, id) {
        const isType = kind === "type";
        const item = isType
            ? resourceTypes.find(type => Number(type.resourceTypeId) === id)
            : categories.find(category => Number(category.categoryId) === id);
        if (!item) return;

        const currentStatus = String(item.status || "").toUpperCase();
        const action = currentStatus === "INACTIVE" ? "activate" : "deactivate";
        const name = isType ? item.typeName : item.categoryTopic;
        if (!window.confirm(`Are you sure you want to ${action} "${name}"?`)) return;

        try {
            await window.AdminModule.jsonRequest(`${isType ? RESOURCE_TYPE_API : CATEGORY_API}/${id}/${action}`, {
                method: "PUT"
            });
            window.AdminModule.showToast(`Classification item ${action}d.`);
            await loadClassificationData();
        } catch (error) {
            window.AdminModule.showToast(window.AdminModule.getErrorMessage(error, `Unable to ${action} item.`));
        }
    }

    function capitalize(value) {
        if (!value) return "";
        return value.charAt(0).toUpperCase() + value.slice(1);
    }
})();
