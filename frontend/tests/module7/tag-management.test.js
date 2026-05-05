const fs = require("fs");
const path = require("path");

const TAG_MANAGEMENT_SCRIPT_PATH = path.resolve(__dirname, "../../module7/tag-management.js");

function createAdminModuleMock() {
    const escapeHtml = jest.fn((value) => String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;"));
    const formatDateTime = jest.fn((value) => value ? `FMT:${value}` : "-");
    const formatLabel = jest.fn((value) => {
        if (value == null || value === "") {
            return "-";
        }
        return String(value)
            .replaceAll("_", " ")
            .replaceAll("-", " ")
            .toLowerCase()
            .replace(/\b\w/g, char => char.toUpperCase());
    });
    const statusBadge = jest.fn((value) => `<span class="status-badge">${escapeHtml(formatLabel(value))}</span>`);
    const emptyRow = jest.fn((colspan, text) => `<tr><td colspan="${colspan}" class="admin-muted">${escapeHtml(text)}</td></tr>`);

    return {
        bindAdminBasics: jest.fn(),
        requireAdmin: jest.fn(),
        requestJson: jest.fn(),
        jsonRequest: jest.fn(),
        showToast: jest.fn(),
        getErrorMessage: jest.fn((error, fallback) => error && error.message ? error.message : fallback),
        setState: jest.fn((elementId, message, type = "loading") => {
            const element = document.getElementById(elementId);
            if (element) {
                element.innerHTML = `<div class="admin-${type}">${escapeHtml(message)}</div>`;
            }
        }),
        escapeHtml,
        formatDateTime,
        statusBadge,
        emptyRow
    };
}

function loadTagManagementHooks() {
    const source = fs.readFileSync(TAG_MANAGEMENT_SCRIPT_PATH, "utf8");
    const injectedSource = source.replace(
        /\}\)\(\);\s*$/m,
        `
        window.__tagManagementTestHooks = {
            bindTabs,
            bindActions,
            loadTagData,
            renderAll,
            renderTags,
            renderTagRow,
            renderUsageOverview,
            renderUsageOverviewRow,
            renderUsageHistory,
            renderUsageHistoryRow,
            renderOperationHistory,
            renderOperationRow,
            editTag,
            toggleTag,
            capitalize,
            __setState(nextState = {}) {
                if (Object.prototype.hasOwnProperty.call(nextState, "tags")) {
                    tags = nextState.tags;
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
                    tags,
                    usageHistory,
                    operationHistory
                };
            },
            __resetState() {
                tags = [];
                usageHistory = [];
                operationHistory = [];
            }
        };
    })();
        `
    );

    delete window.__tagManagementTestHooks;
    window.eval(injectedSource);
    return window.__tagManagementTestHooks;
}

function setupTagManagement() {
    document.body.innerHTML = "";
    jest.useRealTimers();
    window.confirm = jest.fn().mockReturnValue(true);
    window.prompt = jest.fn();

    const adminModule = createAdminModuleMock();
    window.AdminModule = adminModule;

    const hooks = loadTagManagementHooks();
    hooks.__resetState();
    return { hooks, adminModule };
}

describe("tag-management.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    describe("helper functions", () => {
        test("capitalizes values in normal and boundary cases", () => {
            const { hooks } = setupTagManagement();

            expect(hooks.capitalize("usage")).toBe("Usage");
            expect(hooks.capitalize("Tag")).toBe("Tag");
            expect(hooks.capitalize("")).toBe("");
            expect(hooks.capitalize(null)).toBe("");
        });
    });

    describe("data loading", () => {
        test("loads tag data successfully and renders all sections", async () => {
            const { hooks, adminModule } = setupTagManagement();
            document.body.innerHTML = `
                <div id="tagListPanel"></div>
                <div id="tagUsageOverviewPanel"></div>
                <div id="tagUsageHistoryPanel"></div>
                <div id="tagOperationPanel"></div>
            `;
            adminModule.requestJson
                .mockResolvedValueOnce([
                    {
                        tagId: 1,
                        tagName: 'Story <Tag>',
                        status: "ACTIVE",
                        usageCount: 4,
                        lastUpdatedAt: "2026-05-05T10:00:00Z"
                    }
                ])
                .mockResolvedValueOnce([
                    {
                        tagName: 'Story <Tag>',
                        relatedRecordName: "Resource A",
                        dateOfUse: "2026-05-05T12:00:00Z"
                    }
                ])
                .mockResolvedValueOnce([
                    {
                        itemName: "Story <Tag>",
                        action: "UPDATED",
                        administrator: "admin_1",
                        createdAt: "2026-05-06T08:00:00Z"
                    }
                ]);

            await hooks.loadTagData();

            expect(adminModule.setState).toHaveBeenNthCalledWith(1, "tagListPanel", "Loading tags...");
            expect(adminModule.setState).toHaveBeenNthCalledWith(2, "tagUsageOverviewPanel", "Loading usage overview...");
            expect(adminModule.setState).toHaveBeenNthCalledWith(3, "tagUsageHistoryPanel", "Loading usage history...");
            expect(adminModule.setState).toHaveBeenNthCalledWith(4, "tagOperationPanel", "Loading operation history...");

            expect(adminModule.requestJson).toHaveBeenNthCalledWith(1, "/api/admin/tags", { method: "GET" });
            expect(adminModule.requestJson).toHaveBeenNthCalledWith(2, "/api/admin/tags/usage-history", { method: "GET" });
            expect(adminModule.requestJson).toHaveBeenNthCalledWith(3, "/api/admin/operation-history?module=tag", { method: "GET" });

            expect(document.getElementById("tagListPanel").innerHTML).toContain("Story &lt;Tag&gt;");
            expect(document.getElementById("tagUsageOverviewPanel").innerHTML).toContain("Usage Overview");
            expect(document.getElementById("tagUsageHistoryPanel").innerHTML).toContain("Resource A");
            expect(document.getElementById("tagOperationPanel").innerHTML).toContain("UPDATED");

            expect(hooks.__getState().tags).toHaveLength(1);
            expect(hooks.__getState().usageHistory).toHaveLength(1);
            expect(hooks.__getState().operationHistory).toHaveLength(1);
        });

        test("shows error states in all sections when loading tag data fails", async () => {
            const { hooks, adminModule } = setupTagManagement();
            document.body.innerHTML = `
                <div id="tagListPanel"></div>
                <div id="tagUsageOverviewPanel"></div>
                <div id="tagUsageHistoryPanel"></div>
                <div id="tagOperationPanel"></div>
            `;
            adminModule.requestJson.mockRejectedValue(new Error("Unable to load tag data."));

            await hooks.loadTagData();

            expect(adminModule.setState).toHaveBeenLastCalledWith(
                "tagOperationPanel",
                "Unable to load tag data.",
                "error"
            );
            expect(document.getElementById("tagListPanel").innerHTML).toContain("Unable to load tag data.");
        });
    });

    describe("table rendering", () => {
        test("renders the empty states for tags, usage overview, usage history, and operation history", () => {
            const { hooks } = setupTagManagement();
            document.body.innerHTML = `
                <div id="tagListPanel"></div>
                <div id="tagUsageOverviewPanel"></div>
                <div id="tagUsageHistoryPanel"></div>
                <div id="tagOperationPanel"></div>
            `;
            hooks.__setState({
                tags: [],
                usageHistory: [],
                operationHistory: []
            });

            hooks.renderAll();

            expect(document.getElementById("tagListPanel").innerHTML).toContain("No tags found.");
            expect(document.getElementById("tagUsageOverviewPanel").innerHTML).toContain("No tag overview is available.");
            expect(document.getElementById("tagUsageHistoryPanel").innerHTML).toContain("No tag usage records found.");
            expect(document.getElementById("tagOperationPanel").innerHTML).toContain("No operation history found.");
        });

        test("renders tag rows with status badges and the correct toggle action", () => {
            const { hooks } = setupTagManagement();

            const activeHtml = hooks.renderTagRow({
                tagId: 7,
                tagName: 'Heritage <Tag>',
                status: "ACTIVE",
                usageCount: 9,
                lastUpdatedAt: "2026-05-05T10:00:00Z"
            });
            expect(activeHtml).toContain("Heritage &lt;Tag&gt;");
            expect(activeHtml).toContain("Deactivate");
            expect(activeHtml).toContain("FMT:2026-05-05T10:00:00Z");

            const inactiveHtml = hooks.renderTagRow({
                tagId: 8,
                tagName: "Inactive Tag",
                status: "INACTIVE",
                usageCount: 0,
                lastUpdatedAt: null
            });
            expect(inactiveHtml).toContain("Activate");
            expect(inactiveHtml).toContain("0");
        });

        test("renders usage overview, usage history, and operation rows with escaped values and fallbacks", () => {
            const { hooks } = setupTagManagement();

            const overviewHtml = hooks.renderUsageOverviewRow({
                tagName: 'Story <Tag>',
                usageCount: 5
            });
            expect(overviewHtml).toContain("Story &lt;Tag&gt;");
            expect(overviewHtml).toContain("5");

            const usageHistoryHtml = hooks.renderUsageHistoryRow({
                tagName: 'Story <Tag>',
                relatedRecordName: 'Resource <A>',
                dateOfUse: "2026-05-05T10:00:00Z"
            });
            expect(usageHistoryHtml).toContain("Resource &lt;A&gt;");
            expect(usageHistoryHtml).toContain("FMT:2026-05-05T10:00:00Z");

            const fallbackHistoryHtml = hooks.renderUsageHistoryRow({
                tagName: null,
                relatedRecordName: "",
                resourceId: 22,
                dateOfUse: null
            });
            expect(fallbackHistoryHtml).toContain("Resource 22");
            expect(fallbackHistoryHtml).toContain("-");

            const operationHtml = hooks.renderOperationRow({
                itemName: 'Story <Tag>',
                action: "ACTIVATED",
                administrator: 'Admin <One>',
                createdAt: "2026-05-06T10:00:00Z"
            });
            expect(operationHtml).toContain("Story &lt;Tag&gt;");
            expect(operationHtml).toContain("Admin &lt;One&gt;");
            expect(operationHtml).toContain("FMT:2026-05-06T10:00:00Z");
        });
    });

    describe("edit tag flow", () => {
        test("returns early when the tag does not exist, the prompt is cancelled, or the new name is blank", async () => {
            const { hooks, adminModule } = setupTagManagement();
            hooks.__setState({
                tags: [
                    { tagId: 1, tagName: "Known Tag" }
                ]
            });

            await hooks.editTag(999);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();

            window.prompt.mockReturnValueOnce(null);
            await hooks.editTag(1);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();

            window.prompt.mockReturnValueOnce("   ");
            await hooks.editTag(1);
            expect(adminModule.showToast).toHaveBeenCalledWith("Tag name cannot be blank.");
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();
        });

        test("updates a tag successfully in a normal case", async () => {
            const { hooks, adminModule } = setupTagManagement();
            hooks.__setState({
                tags: [
                    { tagId: 4, tagName: "Old Tag" }
                ]
            });
            document.body.innerHTML = `
                <div id="tagListPanel"></div>
                <div id="tagUsageOverviewPanel"></div>
                <div id="tagUsageHistoryPanel"></div>
                <div id="tagOperationPanel"></div>
            `;
            window.prompt.mockReturnValueOnce("  New Tag Name  ");
            adminModule.jsonRequest.mockResolvedValueOnce(null);
            adminModule.requestJson
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            await hooks.editTag(4);

            expect(window.prompt).toHaveBeenCalledWith("Update tag name", "Old Tag");
            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/tags/4",
                {
                    method: "PUT",
                    body: JSON.stringify({ tagName: "New Tag Name" })
                }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Tag updated.");
        });

        test("shows an error message when updating a tag fails", async () => {
            const { hooks, adminModule } = setupTagManagement();
            hooks.__setState({
                tags: [
                    { tagId: 5, tagName: "Original Tag" }
                ]
            });
            window.prompt.mockReturnValueOnce("Edited Tag");
            adminModule.jsonRequest.mockRejectedValueOnce(new Error("Update failed."));

            await hooks.editTag(5);

            expect(adminModule.showToast).toHaveBeenCalledWith("Update failed.");
        });
    });

    describe("toggle tag flow", () => {
        test("returns early when the tag does not exist or the user cancels", async () => {
            const { hooks, adminModule } = setupTagManagement();
            hooks.__setState({
                tags: [
                    { tagId: 1, tagName: "Known Tag", status: "ACTIVE" }
                ]
            });

            await hooks.toggleTag(999);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();

            window.confirm.mockReturnValueOnce(false);
            await hooks.toggleTag(1);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();
        });

        test("deactivates and activates tags successfully in normal cases", async () => {
            const { hooks, adminModule } = setupTagManagement();
            document.body.innerHTML = `
                <div id="tagListPanel"></div>
                <div id="tagUsageOverviewPanel"></div>
                <div id="tagUsageHistoryPanel"></div>
                <div id="tagOperationPanel"></div>
            `;
            hooks.__setState({
                tags: [
                    { tagId: 8, tagName: "Active Tag", status: "ACTIVE" }
                ]
            });
            adminModule.jsonRequest.mockResolvedValueOnce(null);
            adminModule.requestJson
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            await hooks.toggleTag(8);

            expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to deactivate "Active Tag"?');
            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/tags/8/deactivate",
                { method: "PUT" }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Tag deactivated.");

            hooks.__setState({
                tags: [
                    { tagId: 9, tagName: "Inactive Tag", status: "INACTIVE" }
                ]
            });
            adminModule.jsonRequest.mockResolvedValueOnce(null);
            adminModule.requestJson
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            await hooks.toggleTag(9);

            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/tags/9/activate",
                { method: "PUT" }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Tag activated.");
        });

        test("shows an error message when toggling a tag fails", async () => {
            const { hooks, adminModule } = setupTagManagement();
            hooks.__setState({
                tags: [
                    { tagId: 10, tagName: "Broken Tag", status: "ACTIVE" }
                ]
            });
            adminModule.jsonRequest.mockRejectedValueOnce(new Error("Toggle failed."));

            await hooks.toggleTag(10);

            expect(adminModule.showToast).toHaveBeenCalledWith("Toggle failed.");
        });
    });
});
