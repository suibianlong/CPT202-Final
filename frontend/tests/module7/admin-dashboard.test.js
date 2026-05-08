const path = require("path");

const ADMIN_DASHBOARD_SCRIPT_PATH = path.resolve(__dirname, "../../module7/admin-dashboard.js");

function loadDashboardDomReadyHandler(adminModule) {
    jest.resetModules();
    window.AdminModule = adminModule;

    const capturedHandlers = [];
    const originalAddEventListener = document.addEventListener.bind(document);
    const addEventListenerSpy = jest.spyOn(document, "addEventListener")
        .mockImplementation((type, listener, options) => {
            if (type === "DOMContentLoaded") {
                capturedHandlers.push(listener);
                return;
            }
            return originalAddEventListener(type, listener, options);
        });

    require(ADMIN_DASHBOARD_SCRIPT_PATH);
    addEventListenerSpy.mockRestore();
    return capturedHandlers[0];
}

describe("admin-dashboard.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    test("initializes dashboard and renders the administrator name in a normal case", async () => {
        document.body.innerHTML = `
            <h1 id="adminWelcomeTitle"></h1>
            <p id="adminWelcomeText"></p>
        `;
        const adminModule = {
            bindAdminBasics: jest.fn(),
            requireAdmin: jest.fn().mockResolvedValue({
                userId: 1,
                name: "Luna",
                role: "ADMINISTRATOR"
            })
        };

        const domReadyHandler = loadDashboardDomReadyHandler(adminModule);
        await domReadyHandler();

        expect(adminModule.bindAdminBasics).toHaveBeenCalledTimes(1);
        expect(adminModule.requireAdmin).toHaveBeenCalledTimes(1);
        expect(document.getElementById("adminWelcomeTitle").textContent).toBe("Welcome, Luna");
        expect(document.getElementById("adminWelcomeText").textContent)
            .toBe("Choose a management area for resources, classification, tags, or approval workflow.");
    });

    test("uses fallback administrator label when user name is missing", async () => {
        document.body.innerHTML = `
            <h1 id="adminWelcomeTitle"></h1>
            <p id="adminWelcomeText"></p>
        `;
        const adminModule = {
            bindAdminBasics: jest.fn(),
            requireAdmin: jest.fn().mockResolvedValue({
                userId: 2,
                name: "",
                role: "ADMINISTRATOR"
            })
        };

        const domReadyHandler = loadDashboardDomReadyHandler(adminModule);
        await domReadyHandler();

        expect(document.getElementById("adminWelcomeTitle").textContent).toBe("Welcome, Administrator");
    });

    test("returns early without mutating dashboard text when no current user is available", async () => {
        document.body.innerHTML = `
            <h1 id="adminWelcomeTitle">Original Title</h1>
            <p id="adminWelcomeText">Original Text</p>
        `;
        const adminModule = {
            bindAdminBasics: jest.fn(),
            requireAdmin: jest.fn().mockResolvedValue(null)
        };

        const domReadyHandler = loadDashboardDomReadyHandler(adminModule);
        await domReadyHandler();

        expect(document.getElementById("adminWelcomeTitle").textContent).toBe("Original Title");
        expect(document.getElementById("adminWelcomeText").textContent).toBe("Original Text");
    });

    test("handles missing dashboard text nodes gracefully", async () => {
        document.body.innerHTML = '<div id="placeholder"></div>';
        const adminModule = {
            bindAdminBasics: jest.fn(),
            requireAdmin: jest.fn().mockResolvedValue({
                userId: 3,
                name: "Admin",
                role: "ADMINISTRATOR"
            })
        };

        const domReadyHandler = loadDashboardDomReadyHandler(adminModule);

        await expect(domReadyHandler()).resolves.toBeUndefined();
        expect(adminModule.bindAdminBasics).toHaveBeenCalledTimes(1);
        expect(adminModule.requireAdmin).toHaveBeenCalledTimes(1);
    });
});
