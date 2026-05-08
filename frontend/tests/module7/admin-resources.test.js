const fs = require("fs");
const path = require("path");
const { evalWithCoverage } = require("../test-utils/eval-with-coverage");

const ADMIN_RESOURCES_SCRIPT_PATH = path.resolve(__dirname, "../../module7/admin-resources.js");

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

function loadAdminResourcesHooks() {
    const source = fs.readFileSync(ADMIN_RESOURCES_SCRIPT_PATH, "utf8");
    const injectedSource = source.replace(
        /\}\)\(\);\s*$/m,
        `
        window.__adminResourcesTestHooks = {
            bindResourceActions,
            loadResources,
            renderResources,
            filterResources,
            renderResourceRow,
            renderLifecycleControl,
            archiveResource,
            unarchiveResource,
            normalizeStatus,
            __setResources(nextResources) {
                resources = nextResources;
            },
            __getResources() {
                return resources;
            },
            __resetResources() {
                resources = [];
            }
        };
    })();
        `
    );

    delete window.__adminResourcesTestHooks;
    evalWithCoverage(injectedSource, ADMIN_RESOURCES_SCRIPT_PATH);
    return window.__adminResourcesTestHooks;
}

function setupAdminResources() {
    document.body.innerHTML = "";
    jest.useRealTimers();
    window.confirm = jest.fn().mockReturnValue(true);

    const adminModule = createAdminModuleMock();
    window.AdminModule = adminModule;

    const hooks = loadAdminResourcesHooks();
    hooks.__resetResources();
    return { hooks, adminModule };
}

async function flushPromises(times = 4) {
    for (let i = 0; i < times; i += 1) {
        await Promise.resolve();
    }
}

describe("admin-resources.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    describe("DOMContentLoaded bootstrap and action binding", () => {
        test("handles unauthenticated and authenticated bootstrap flows", async () => {
            const { adminModule } = setupAdminResources();
            document.body.innerHTML = `
                <button id="resourceRefreshBtn" type="button"></button>
                <form id="resourceFilterForm"></form>
                <select id="resourceStatusFilter">
                    <option value="Approved" selected>Approved</option>
                </select>
                <input id="resourceSearchInput" value="">
                <div id="resourceListPanel"></div>
            `;
            adminModule.requireAdmin
                .mockResolvedValueOnce(null)
                .mockResolvedValueOnce({ role: "ADMINISTRATOR" });
            adminModule.requestJson.mockResolvedValue([]);

            document.dispatchEvent(new Event("DOMContentLoaded"));
            await flushPromises();
            expect(adminModule.bindAdminBasics).toHaveBeenCalledTimes(1);
            expect(adminModule.requestJson).not.toHaveBeenCalled();

            document.dispatchEvent(new Event("DOMContentLoaded"));
            await flushPromises();
            expect(adminModule.bindAdminBasics).toHaveBeenCalledTimes(2);
            expect(adminModule.requestJson).toHaveBeenCalledWith(
                "/api/admin/resources?status=Approved",
                { method: "GET" }
            );
        });

        test("binds refresh and filter events and routes archive or unarchive clicks", async () => {
            const { hooks, adminModule } = setupAdminResources();
            document.body.innerHTML = `
                <button id="resourceRefreshBtn" type="button"></button>
                <form id="resourceFilterForm"></form>
                <select id="resourceStatusFilter">
                    <option value="" selected>All</option>
                </select>
                <input id="resourceSearchInput" value="">
                <div id="resourceListPanel"></div>
            `;
            adminModule.requestJson.mockResolvedValue([]);
            adminModule.jsonRequest
                .mockResolvedValueOnce({ message: "Archived." })
                .mockResolvedValueOnce({ message: "Unarchived." });

            hooks.bindResourceActions();

            document.getElementById("resourceRefreshBtn")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();

            document.getElementById("resourceFilterForm")
                .dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
            await flushPromises();

            const archiveButton = document.createElement("button");
            archiveButton.dataset.adminResourceArchive = "11";
            document.body.appendChild(archiveButton);
            archiveButton.dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();

            const unarchiveButton = document.createElement("button");
            unarchiveButton.dataset.adminResourceUnarchive = "12";
            document.body.appendChild(unarchiveButton);
            unarchiveButton.dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();

            expect(adminModule.requestJson).toHaveBeenCalledWith("/api/admin/resources", { method: "GET" });
            const jsonRequestUrls = adminModule.jsonRequest.mock.calls.map(call => call[0]);
            expect(jsonRequestUrls).toContain("/api/admin/resources/11/archive");
            expect(jsonRequestUrls).toContain("/api/admin/resources/12/unarchive");
        });

        test("ignores unrelated click targets after binding actions", async () => {
            const { hooks, adminModule } = setupAdminResources();
            hooks.bindResourceActions();

            const neutralNode = document.createElement("div");
            document.body.appendChild(neutralNode);
            neutralNode.dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();

            expect(adminModule.requestJson).not.toHaveBeenCalled();
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();
        });
    });

    describe("helper functions", () => {
        test("normalizes statuses in normal and boundary cases", () => {
            const { hooks } = setupAdminResources();

            expect(hooks.normalizeStatus("PENDING_REVIEW")).toBe("pending review");
            expect(hooks.normalizeStatus("Archived")).toBe("archived");
            expect(hooks.normalizeStatus(null)).toBe("");
        });

        test("filters resources by id and title while preserving all rows for blank search", () => {
            const { hooks } = setupAdminResources();
            document.body.innerHTML = '<input id="resourceSearchInput" value="archive">';
            const rows = [
                { resourceId: 1, title: "Archive item" },
                { resourceId: 2, title: "Viewer guide" },
                { resourceId: 31, title: "Community record" }
            ];

            expect(hooks.filterResources(rows)).toEqual([{ resourceId: 1, title: "Archive item" }]);

            document.getElementById("resourceSearchInput").value = "31";
            expect(hooks.filterResources(rows)).toEqual([{ resourceId: 31, title: "Community record" }]);

            document.getElementById("resourceSearchInput").value = "   ";
            expect(hooks.filterResources(rows)).toEqual(rows);
        });
    });

    describe("loading and rendering", () => {
        test("loads resources with a selected status filter in a normal case", async () => {
            const { hooks, adminModule } = setupAdminResources();
            document.body.innerHTML = `
                <select id="resourceStatusFilter">
                    <option value="Approved" selected>Approved</option>
                </select>
                <input id="resourceSearchInput" value="">
                <div id="resourceListPanel"></div>
            `;
            adminModule.requestJson.mockResolvedValue([
                {
                    resourceId: 7,
                    title: "Archive <Item>",
                    status: "Approved",
                    archivedAt: null,
                    updatedAt: "2026-05-05T10:00:00Z"
                }
            ]);

            await hooks.loadResources();

            expect(adminModule.setState).toHaveBeenCalledWith("resourceListPanel", "Loading resources...");
            expect(adminModule.requestJson).toHaveBeenCalledWith(
                "/api/admin/resources?status=Approved",
                { method: "GET" }
            );
            const html = document.getElementById("resourceListPanel").innerHTML;
            expect(html).toContain("Archive &lt;Item&gt;");
            expect(html).toContain("1 resource shown.");
            expect(html).toContain("Archive");
            expect(hooks.__getResources()).toHaveLength(1);
        });

        test("renders an empty state when no resources match the selected filters", () => {
            const { hooks } = setupAdminResources();
            document.body.innerHTML = `
                <input id="resourceSearchInput" value="none">
                <div id="resourceListPanel"></div>
            `;
            hooks.__setResources([
                { resourceId: 1, title: "Approved item", status: "Approved" }
            ]);

            hooks.renderResources();

            expect(document.getElementById("resourceListPanel").innerHTML)
                .toContain("No resources match the selected filters.");
        });

        test("shows an error state when loading resources fails", async () => {
            const { hooks, adminModule } = setupAdminResources();
            document.body.innerHTML = `
                <select id="resourceStatusFilter"></select>
                <div id="resourceListPanel"></div>
            `;
            adminModule.requestJson.mockRejectedValue(new Error("Unable to load resources."));

            await hooks.loadResources();

            expect(adminModule.setState).toHaveBeenLastCalledWith(
                "resourceListPanel",
                "Unable to load resources.",
                "error"
            );
        });

        test("renders lifecycle controls for approved, archived, and unsupported statuses", () => {
            const { hooks } = setupAdminResources();

            expect(hooks.renderLifecycleControl({ resourceId: 3 }, "approved"))
                .toContain("data-admin-resource-archive=\"3\"");
            expect(hooks.renderLifecycleControl({ resourceId: 4 }, "archived"))
                .toContain("data-admin-resource-unarchive=\"4\"");
            expect(hooks.renderLifecycleControl({ resourceId: 5 }, "pending review"))
                .toContain("No lifecycle action");
        });
    });

    describe("archive and unarchive flows", () => {
        test("blocks archive and unarchive when the resource id is missing", async () => {
            const { hooks, adminModule } = setupAdminResources();
            const button = document.createElement("button");

            await hooks.archiveResource(null, button);
            await hooks.unarchiveResource(0, button);

            expect(adminModule.showToast).toHaveBeenNthCalledWith(1, "Resource id is required.");
            expect(adminModule.showToast).toHaveBeenNthCalledWith(2, "Resource id is required.");
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();
        });

        test("archives a resource successfully and refreshes the list", async () => {
            const { hooks, adminModule } = setupAdminResources();
            document.body.innerHTML = `
                <select id="resourceStatusFilter"></select>
                <input id="resourceSearchInput" value="">
                <div id="resourceListPanel"></div>
            `;
            const button = document.createElement("button");
            adminModule.jsonRequest.mockResolvedValueOnce({
                message: "Resource archived and hidden from public discovery."
            });
            adminModule.requestJson.mockResolvedValueOnce([]);

            await hooks.archiveResource(9, button);

            expect(window.confirm).toHaveBeenCalledWith(
                "Archive this approved resource? It will be hidden from public discovery but kept in the system records."
            );
            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/resources/9/archive",
                { method: "POST" }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Resource archived and hidden from public discovery.");
            expect(button.disabled).toBe(false);
        });

        test("unarchives a resource successfully and refreshes the list", async () => {
            const { hooks, adminModule } = setupAdminResources();
            document.body.innerHTML = `
                <select id="resourceStatusFilter"></select>
                <input id="resourceSearchInput" value="">
                <div id="resourceListPanel"></div>
            `;
            const button = document.createElement("button");
            adminModule.jsonRequest.mockResolvedValueOnce({
                message: "Resource restored to approved and visible to viewers."
            });
            adminModule.requestJson.mockResolvedValueOnce([]);

            await hooks.unarchiveResource(5, button);

            expect(window.confirm).toHaveBeenCalledWith(
                "Unarchive this resource? It will become approved and visible to viewers again."
            );
            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/resources/5/unarchive",
                { method: "POST" }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Resource restored to approved and visible to viewers.");
            expect(button.disabled).toBe(false);
        });

        test("does nothing when archive is cancelled and shows an error when archive fails", async () => {
            const { hooks, adminModule } = setupAdminResources();
            const button = document.createElement("button");

            window.confirm.mockReturnValueOnce(false);
            await hooks.archiveResource(7, button);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();

            window.confirm.mockReturnValueOnce(true);
            adminModule.jsonRequest.mockRejectedValueOnce(new Error("Unable to archive resource."));
            await hooks.archiveResource(7, button);

            expect(adminModule.showToast).toHaveBeenCalledWith("Unable to archive resource.");
            expect(button.disabled).toBe(false);
        });

        test("does nothing when unarchive is cancelled and shows an error when unarchive fails", async () => {
            const { hooks, adminModule } = setupAdminResources();
            const button = document.createElement("button");

            window.confirm.mockReturnValueOnce(false);
            await hooks.unarchiveResource(7, button);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();

            window.confirm.mockReturnValueOnce(true);
            adminModule.jsonRequest.mockRejectedValueOnce(new Error("Unable to unarchive resource."));
            await hooks.unarchiveResource(7, button);

            expect(adminModule.showToast).toHaveBeenCalledWith("Unable to unarchive resource.");
            expect(button.disabled).toBe(false);
        });
    });
});
