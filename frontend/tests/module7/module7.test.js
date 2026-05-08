const path = require("path");

const MODULE7_SCRIPT_PATH = path.resolve(__dirname, "../../module7/module7.js");

function createSharedAppMock() {
    return {
        bindLogoutButtons: jest.fn(),
        escapeHtml: jest.fn((value) => String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;")),
        formatDateTime: jest.fn((value) => value ? `FMT:${value}` : "-"),
        requestJson: jest.fn(),
        showToast: jest.fn()
    };
}

function loadModule7(url = "/admin-dashboard.html") {
    document.body.innerHTML = "";
    window.history.replaceState({}, "", url);
    jest.resetModules();

    const sharedApp = createSharedAppMock();
    window.SharedApp = sharedApp;
    delete window.AdminModule;

    require(MODULE7_SCRIPT_PATH);
    return { adminModule: window.AdminModule, sharedApp };
}

describe("module7.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    describe("bindAdminBasics", () => {
        test("binds logout behavior with the expected admin logout configuration", () => {
            const { adminModule, sharedApp } = loadModule7();

            adminModule.bindAdminBasics();

            expect(sharedApp.bindLogoutButtons).toHaveBeenCalledWith({
                logoutUrl: "/api/auth/logout",
                redirectTo: "./index.html?message=You%20have%20logged%20out%20successfully."
            });
        });
    });

    describe("requireAdmin", () => {
        test("returns current user when the authenticated user is an administrator", async () => {
            const { adminModule, sharedApp } = loadModule7();
            const currentUser = { userId: 7, name: "Admin", role: "ADMINISTRATOR" };
            sharedApp.requestJson.mockResolvedValue(currentUser);

            await expect(adminModule.requireAdmin()).resolves.toEqual(currentUser);
            expect(sharedApp.requestJson).toHaveBeenCalledWith("/api/auth/me", { method: "GET" });
        });

        test("shows access message and returns null when the authenticated user is not an administrator", async () => {
            const { adminModule, sharedApp } = loadModule7();
            document.body.innerHTML = '<main data-admin-main></main>';
            sharedApp.requestJson.mockResolvedValue({
                userId: 9,
                name: "Viewer",
                role: "REGISTERED_VIEWER"
            });

            await expect(adminModule.requireAdmin()).resolves.toBeNull();

            const main = document.querySelector("[data-admin-main]");
            expect(main.innerHTML).toContain("Administrator permission is required.");
            expect(main.innerHTML).toContain('href="./login.html"');
            expect(main.innerHTML).toContain('href="./index.html"');
        });

        test("redirects to login with encoded next path when access check returns 401", async () => {
            const { adminModule, sharedApp } = loadModule7("/admin-dashboard.html?tab=resources");
            const error = new Error("Unauthorized");
            error.status = 401;
            sharedApp.requestJson.mockRejectedValue(error);

            const originalLocation = window.location;
            Object.defineProperty(window, "location", {
                configurable: true,
                writable: true,
                value: {
                    pathname: "/admin-dashboard.html",
                    search: "?tab=resources",
                    href: ""
                }
            });

            try {
                await expect(adminModule.requireAdmin()).resolves.toBeNull();
                expect(window.location.href)
                    .toBe("./login.html?next=%2Fadmin-dashboard.html%3Ftab%3Dresources");
            } finally {
                Object.defineProperty(window, "location", {
                    configurable: true,
                    writable: true,
                    value: originalLocation
                });
            }
        });

        test("shows escaped error message and returns null when admin verification fails for non-401 reasons", async () => {
            const { adminModule, sharedApp } = loadModule7();
            document.body.innerHTML = '<main data-admin-main></main>';
            sharedApp.requestJson.mockRejectedValue(new Error("<Denied>"));

            await expect(adminModule.requireAdmin()).resolves.toBeNull();

            const main = document.querySelector("[data-admin-main]");
            expect(main.innerHTML).toContain("&lt;Denied&gt;");
        });
    });

    describe("rendering helpers", () => {
        test("formats status labels and status badges in normal and fallback cases", () => {
            const { adminModule } = loadModule7();

            expect(adminModule.formatLabel("PENDING_REVIEW")).toBe("Pending Review");
            expect(adminModule.formatLabel(null)).toBe("-");

            expect(adminModule.statusBadge("PENDING_REVIEW")).toContain("status-pending-review");
            expect(adminModule.statusBadge("ARCHIVED")).toContain("status-archived");
            expect(adminModule.statusBadge("UNKNOWN_STATE")).toContain("Unknown State");
        });

        test("renders empty rows and state panels with escaped text", () => {
            const { adminModule } = loadModule7();
            document.body.innerHTML = '<div id="panel"></div>';

            expect(adminModule.emptyRow(4, "No <records>"))
                .toContain("&lt;records&gt;");

            adminModule.setState("panel", "Loading <Admin> data...", "error");
            expect(document.getElementById("panel").innerHTML)
                .toContain('<div class="admin-error">Loading &lt;Admin&gt; data...</div>');
        });

        test("returns explicit or fallback error message", () => {
            const { adminModule } = loadModule7();

            expect(adminModule.getErrorMessage(new Error("Failed"), "Fallback"))
                .toBe("Failed");
            expect(adminModule.getErrorMessage({}, "Fallback"))
                .toBe("Fallback");
        });
    });

    describe("jsonRequest", () => {
        test("injects JSON content type when request body is present and preserves custom headers", async () => {
            const { adminModule, sharedApp } = loadModule7();
            sharedApp.requestJson.mockResolvedValue({ ok: true });

            await adminModule.jsonRequest("/api/admin/example", {
                method: "POST",
                body: JSON.stringify({ name: "Item" }),
                headers: {
                    "X-Trace": "abc123"
                }
            });

            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/admin/example",
                {
                    method: "POST",
                    body: JSON.stringify({ name: "Item" }),
                    headers: {
                        "Content-Type": "application/json",
                        "X-Trace": "abc123"
                    }
                }
            );
        });

        test("passes through headers unchanged when no request body is provided", async () => {
            const { adminModule, sharedApp } = loadModule7();
            sharedApp.requestJson.mockResolvedValue({ ok: true });

            await adminModule.jsonRequest("/api/admin/example", {
                method: "GET",
                headers: {
                    Accept: "application/json"
                }
            });

            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/admin/example",
                {
                    method: "GET",
                    headers: {
                        Accept: "application/json"
                    }
                }
            );
        });
    });
});
