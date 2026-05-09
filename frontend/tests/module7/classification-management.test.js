const fs = require("fs");
const path = require("path");
const { evalWithCoverage } = require("../test-utils/eval-with-coverage");

const CLASSIFICATION_MANAGEMENT_SCRIPT_PATH = path.resolve(__dirname, "../../module7/classification-management.js");

function createAdminModuleMock() {
    const escapeHtml = jest.fn((value) => String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;"));
    const formatDateTime = jest.fn((value) => value ? `FMT:${value}` : "-");
    const statusBadge = jest.fn((value) => `<span class="status-badge">${escapeHtml(value ?? "-")}</span>`);
    const emptyRow = jest.fn((colspan, text) => `<tr><td colspan="${colspan}" class="admin-muted">${escapeHtml(text)}</td></tr>`);

    return {
        bindAdminBasics: jest.fn(),
        requireAdmin: jest.fn(),
        requestJson: jest.fn(),
        setState: jest.fn((elementId, message, type = "loading") => {
            const element = document.getElementById(elementId);
            if (element) {
                element.innerHTML = `<div class="admin-${type}">${escapeHtml(message)}</div>`;
            }
        }),
        getErrorMessage: jest.fn((error, fallback) => error && error.message ? error.message : fallback),
        escapeHtml,
        formatDateTime,
        statusBadge,
        emptyRow,
        jsonRequest: jest.fn(),
        showToast: jest.fn()
    };
}

function loadClassificationManagementHooks() {
    const source = fs.readFileSync(CLASSIFICATION_MANAGEMENT_SCRIPT_PATH, "utf8");
    const injectedSource = source.replace(
        /\}\)\(\);\s*$/m,
        `
        window.__classificationManagementTestHooks = {
            loadClassificationData,
            renderAll,
            renderResourceTypes,
            renderResourceTypeRow,
            renderCategories,
            renderCategoryRow,
            renderUsageOverview,
            renderUsageOverviewRow,
            renderUsageHistory,
            renderUsageHistoryRow,
            renderOperationHistory,
            renderOperationRow,
            createResourceType,
            createCategory,
            editClassificationItem,
            toggleClassificationItem,
            capitalize,
            __setState(nextState = {}) {
                if (Object.prototype.hasOwnProperty.call(nextState, "categories")) {
                    categories = nextState.categories;
                }
                if (Object.prototype.hasOwnProperty.call(nextState, "resourceTypes")) {
                    resourceTypes = nextState.resourceTypes;
                }
                if (Object.prototype.hasOwnProperty.call(nextState, "usageHistory")) {
                    usageHistory = nextState.usageHistory;
                }
                if (Object.prototype.hasOwnProperty.call(nextState, "operationHistory")) {
                    operationHistory = nextState.operationHistory;
                }
            },
            __getState() {
                return {
                    categories,
                    resourceTypes,
                    usageHistory,
                    operationHistory
                };
            },
            __resetState() {
                categories = [];
                resourceTypes = [];
                usageHistory = [];
                operationHistory = [];
            }
        };
    })();
        `
    );

    delete window.__classificationManagementTestHooks;
    evalWithCoverage(injectedSource, CLASSIFICATION_MANAGEMENT_SCRIPT_PATH);
    return window.__classificationManagementTestHooks;
}

function setupClassificationManagement() {
    document.body.innerHTML = "";
    jest.useRealTimers();
    window.prompt = jest.fn();
    window.confirm = jest.fn().mockReturnValue(true);

    const adminModule = createAdminModuleMock();
    window.AdminModule = adminModule;

    const hooks = loadClassificationManagementHooks();
    hooks.__resetState();
    return { hooks, adminModule };
}

async function flushPromises(times = 4) {
    for (let i = 0; i < times; i += 1) {
        await Promise.resolve();
    }
}

function buildClassificationPanelsDom() {
    document.body.innerHTML = `
        <div id="resourceTypePanel"></div>
        <div id="categoryPanel"></div>
        <div id="classificationUsageOverviewPanel"></div>
        <div id="classificationUsageHistoryPanel"></div>
        <div id="classificationOperationPanel"></div>
    `;
}

describe("classification-management.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    describe("DOMContentLoaded bootstrap", () => {
        test("initializes classification page, binds tabs/forms/actions, and refreshes data", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            document.body.innerHTML = `
                <button data-classification-tab="type" class="active"></button>
                <button data-classification-tab="category"></button>
                <section id="classificationTypeSection" class="admin-section active"></section>
                <section id="classificationCategorySection" class="admin-section"></section>
                <button id="classificationRefreshBtn" type="button"></button>
                <form id="resourceTypeCreateForm"><input id="resourceTypeName" value=""></form>
                <form id="categoryCreateForm"><input id="categoryTopic" value=""></form>
                <button data-classification-action="edit" data-kind="type" data-id="5" type="button"></button>
                <div id="resourceTypePanel"></div>
                <div id="categoryPanel"></div>
                <div id="classificationUsageOverviewPanel"></div>
                <div id="classificationUsageHistoryPanel"></div>
                <div id="classificationOperationPanel"></div>
            `;
            hooks.__setState({
                resourceTypes: [{ resourceTypeId: 5, typeName: "Photo", status: "ACTIVE" }]
            });
            adminModule.requireAdmin.mockResolvedValue({ role: "ADMINISTRATOR" });
            adminModule.requestJson
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);
            window.prompt.mockReturnValueOnce(null);

            document.dispatchEvent(new Event("DOMContentLoaded"));
            await flushPromises();
            await flushPromises();

            expect(adminModule.bindAdminBasics).toHaveBeenCalled();
            expect(adminModule.requireAdmin).toHaveBeenCalled();
            expect(adminModule.requestJson).toHaveBeenCalledWith(
                "/api/admin/resource-types",
                { method: "GET" }
            );

            document.querySelector("[data-classification-tab=\"category\"]")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            expect(document.getElementById("classificationCategorySection").classList.contains("active")).toBe(true);

            document.getElementById("resourceTypeCreateForm").dispatchEvent(new Event("submit", {
                bubbles: true,
                cancelable: true
            }));
            await flushPromises();
            expect(adminModule.showToast).toHaveBeenCalledWith("Resource type name is required.");

            document.getElementById("categoryCreateForm").dispatchEvent(new Event("submit", {
                bubbles: true,
                cancelable: true
            }));
            await flushPromises();
            expect(adminModule.showToast).toHaveBeenCalledWith("Category topic is required.");

            document.querySelector("[data-classification-action=\"edit\"]")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();

            document.getElementById("classificationRefreshBtn")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();
            expect(adminModule.requestJson).toHaveBeenLastCalledWith(
                "/api/admin/operation-history?module=classification",
                { method: "GET" }
            );
        });
    });

    describe("helper functions", () => {
        test("capitalizes a normal value and returns an empty string at the boundary", () => {
            const { hooks } = setupClassificationManagement();

            expect(hooks.capitalize("resource")).toBe("Resource");
            expect(hooks.capitalize("a")).toBe("A");
            expect(hooks.capitalize("")).toBe("");
            expect(hooks.capitalize(null)).toBe("");
        });
    });

    describe("data loading and rendering", () => {
        test("loads classification data successfully in a normal case", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            buildClassificationPanelsDom();

            adminModule.requestJson
                .mockResolvedValueOnce([
                    {
                        resourceTypeId: 5,
                        typeName: "Photo <Archive>",
                        status: "ACTIVE",
                        usageCount: 12,
                        lastUpdatedAt: "2026-05-05T10:00:00Z"
                    }
                ])
                .mockResolvedValueOnce([
                    {
                        categoryId: 7,
                        categoryTopic: "History <Topic>",
                        status: "INACTIVE",
                        usageCount: 4,
                        lastUpdatedAt: "2026-05-06T11:00:00Z"
                    }
                ])
                .mockResolvedValueOnce([
                    {
                        name: "Photo <Archive>",
                        kind: "Type",
                        relatedRecordName: "Resource <One>",
                        resourceId: 12,
                        dateOfUse: "2026-05-04T09:00:00Z"
                    }
                ])
                .mockResolvedValueOnce([
                    {
                        itemName: "History <Topic>",
                        kind: "Category",
                        action: "UPDATED",
                        administrator: "Admin <One>",
                        createdAt: "2026-05-03T08:00:00Z"
                    }
                ]);

            await hooks.loadClassificationData();

            expect(adminModule.requestJson).toHaveBeenNthCalledWith(1,
                "/api/admin/resource-types",
                { method: "GET" }
            );
            expect(adminModule.requestJson).toHaveBeenNthCalledWith(2,
                "/api/admin/categories",
                { method: "GET" }
            );
            expect(adminModule.requestJson).toHaveBeenNthCalledWith(3,
                "/api/admin/classifications/usage-history",
                { method: "GET" }
            );
            expect(adminModule.requestJson).toHaveBeenNthCalledWith(4,
                "/api/admin/operation-history?module=classification",
                { method: "GET" }
            );

            const state = hooks.__getState();
            expect(state.resourceTypes).toHaveLength(1);
            expect(state.categories).toHaveLength(1);
            expect(state.usageHistory).toHaveLength(1);
            expect(state.operationHistory).toHaveLength(1);

            expect(document.getElementById("resourceTypePanel").innerHTML).toContain("Photo &lt;Archive&gt;");
            expect(document.getElementById("resourceTypePanel").innerHTML).toContain("Deactivate");
            expect(document.getElementById("categoryPanel").innerHTML).toContain("History &lt;Topic&gt;");
            expect(document.getElementById("categoryPanel").innerHTML).toContain("Activate");
            expect(document.getElementById("classificationUsageOverviewPanel").innerHTML).toContain("Photo &lt;Archive&gt;");
            expect(document.getElementById("classificationUsageHistoryPanel").innerHTML).toContain("Resource &lt;One&gt;");
            expect(document.getElementById("classificationOperationPanel").innerHTML).toContain("Admin &lt;One&gt;");
        });

        test("shows error states when loading classification data fails", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            buildClassificationPanelsDom();
            adminModule.requestJson.mockRejectedValue(new Error("Unable to load classification data."));

            await hooks.loadClassificationData();

            expect(adminModule.setState).toHaveBeenCalledWith(
                "resourceTypePanel",
                "Unable to load classification data.",
                "error"
            );
            expect(adminModule.setState).toHaveBeenCalledWith(
                "categoryPanel",
                "Unable to load classification data.",
                "error"
            );
            expect(adminModule.setState).toHaveBeenCalledWith(
                "classificationUsageOverviewPanel",
                "Unable to load classification data.",
                "error"
            );
            expect(adminModule.setState).toHaveBeenCalledWith(
                "classificationUsageHistoryPanel",
                "Unable to load classification data.",
                "error"
            );
            expect(adminModule.setState).toHaveBeenCalledWith(
                "classificationOperationPanel",
                "Unable to load classification data.",
                "error"
            );
        });

        test("renders empty states for all sections at the boundary", () => {
            const { hooks } = setupClassificationManagement();
            buildClassificationPanelsDom();
            hooks.__setState({
                resourceTypes: [],
                categories: [],
                usageHistory: [],
                operationHistory: []
            });

            hooks.renderAll();

            expect(document.getElementById("resourceTypePanel").innerHTML).toContain("No resource types found.");
            expect(document.getElementById("categoryPanel").innerHTML).toContain("No categories found.");
            expect(document.getElementById("classificationUsageOverviewPanel").innerHTML)
                .toContain("No usage overview is available.");
            expect(document.getElementById("classificationUsageHistoryPanel").innerHTML)
                .toContain("No resource usage records found.");
            expect(document.getElementById("classificationOperationPanel").innerHTML)
                .toContain("No operation history found.");
        });
    });

    describe("create flows", () => {
        test("creates a resource type successfully in a normal case", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            buildClassificationPanelsDom();
            const input = document.createElement("input");
            input.value = "  Photo  ";

            adminModule.jsonRequest.mockResolvedValueOnce(null);
            adminModule.requestJson
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            await hooks.createResourceType(input);

            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/resource-types",
                {
                    method: "POST",
                    body: JSON.stringify({ typeName: "Photo" })
                }
            );
            expect(input.value).toBe("");
            expect(adminModule.showToast).toHaveBeenCalledWith("Resource type created.");
        });

        test("rejects blank resource type names at the boundary", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            const input = document.createElement("input");
            input.value = "   ";

            await hooks.createResourceType(input);

            expect(adminModule.showToast).toHaveBeenCalledWith("Resource type name is required.");
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();
        });

        test("shows an error when creating a category fails", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            const input = document.createElement("input");
            input.value = "Traditional Craft";
            adminModule.jsonRequest.mockRejectedValue(new Error("Category already exists."));

            await hooks.createCategory(input);

            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/categories",
                {
                    method: "POST",
                    body: JSON.stringify({ categoryTopic: "Traditional Craft" })
                }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Category already exists.");
            expect(input.value).toBe("Traditional Craft");
        });
    });

    describe("edit flows", () => {
        test("updates a resource type successfully in a normal case", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            buildClassificationPanelsDom();
            hooks.__setState({
                resourceTypes: [
                    {
                        resourceTypeId: 5,
                        typeName: "Old Type",
                        status: "ACTIVE"
                    }
                ]
            });
            window.prompt.mockReturnValueOnce("  New Type  ");
            adminModule.jsonRequest.mockResolvedValueOnce(null);
            adminModule.requestJson
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            await hooks.editClassificationItem("type", 5);

            expect(window.prompt).toHaveBeenCalledWith("Update resource type name", "Old Type");
            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/resource-types/5",
                {
                    method: "PUT",
                    body: JSON.stringify({ typeName: "New Type" })
                }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Classification item updated.");
        });

        test("blocks blank edited names and treats prompt cancel as a boundary case", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            hooks.__setState({
                categories: [
                    {
                        categoryId: 9,
                        categoryTopic: "Original Topic",
                        status: "ACTIVE"
                    }
                ]
            });

            window.prompt.mockReturnValueOnce("   ");
            await hooks.editClassificationItem("category", 9);
            expect(adminModule.showToast).toHaveBeenCalledWith("Name cannot be blank.");
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();

            adminModule.showToast.mockClear();
            window.prompt.mockReturnValueOnce(null);
            await hooks.editClassificationItem("category", 9);
            expect(adminModule.showToast).not.toHaveBeenCalled();
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();
        });
    });

    describe("toggle flows", () => {
        test("deactivates a category successfully in a normal case", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            buildClassificationPanelsDom();
            hooks.__setState({
                categories: [
                    {
                        categoryId: 7,
                        categoryTopic: "Living Heritage",
                        status: "ACTIVE"
                    }
                ]
            });
            window.confirm.mockReturnValueOnce(true);
            adminModule.jsonRequest.mockResolvedValueOnce(null);
            adminModule.requestJson
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            await hooks.toggleClassificationItem("category", 7);

            expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to deactivate "Living Heritage"?');
            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/categories/7/deactivate",
                { method: "PUT" }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Classification item deactivated.");
        });

        test("activates an inactive resource type in a normal case", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            buildClassificationPanelsDom();
            hooks.__setState({
                resourceTypes: [
                    {
                        resourceTypeId: 11,
                        typeName: "Audio Archive",
                        status: "INACTIVE"
                    }
                ]
            });
            window.confirm.mockReturnValueOnce(true);
            adminModule.jsonRequest.mockResolvedValueOnce(null);
            adminModule.requestJson
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            await hooks.toggleClassificationItem("type", 11);

            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/resource-types/11/activate",
                { method: "PUT" }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Classification item activated.");
        });

        test("does nothing when toggle is cancelled and shows an error when toggle fails", async () => {
            const { hooks, adminModule } = setupClassificationManagement();
            hooks.__setState({
                resourceTypes: [
                    {
                        resourceTypeId: 12,
                        typeName: "Video Archive",
                        status: "ACTIVE"
                    }
                ]
            });

            window.confirm.mockReturnValueOnce(false);
            await hooks.toggleClassificationItem("type", 12);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();

            window.confirm.mockReturnValueOnce(true);
            adminModule.jsonRequest.mockRejectedValueOnce(new Error("Unable to deactivate item."));
            await hooks.toggleClassificationItem("type", 12);
            expect(adminModule.showToast).toHaveBeenCalledWith("Unable to deactivate item.");
        });
    });
});
