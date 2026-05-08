const fs = require("fs");
const path = require("path");
const { evalWithCoverage } = require("../test-utils/eval-with-coverage");

const MODULE1_SCRIPT_PATH = path.resolve(__dirname, "../../module1/module1.js");

function createSharedAppMock() {
    const escapeHtml = jest.fn((value) => String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;"));
    const formatDateTime = jest.fn((value, { emptyText = "-" } = {}) => value ? `FMT:${value}` : emptyText);
    const requestJson = jest.fn();
    const setText = jest.fn((id, value) => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value ?? "";
        }
    });
    const setValue = jest.fn((id, value) => {
        const element = document.getElementById(id);
        if (element) {
            element.value = value ?? "";
        }
    });

    return {
        bindLogoutButtons: jest.fn(),
        escapeHtml,
        formatDateTime,
        requestJson,
        setText,
        setValue,
        showMessageFromQuery: jest.fn(),
        showToast: jest.fn()
    };
}

function loadModule1TestHooks() {
    const source = fs.readFileSync(MODULE1_SCRIPT_PATH, "utf8");
    const wrappedSource = `
        (() => {
            ${source}
            window.__module1TestHooks = {
                initLoginPage,
                initRegisterPage,
                startRegisterCodeCooldown,
                loadPendingRequestList,
                renderHomeSession,
                populateLatestRequestCard,
                populateContributorPanel,
                resolvePostLoginRedirect,
                resolveAccountBackFallback,
                buildAuthPageUrl,
                sanitizeNextPath,
                isAllowedNextPath,
                formatEnumLabel,
                isApprovedContributor,
                getContributorRecordLabel,
                getAccountRequestStatusTone,
                buildAccountStatusCopy
            };
        })();
    `;

    delete window.__module1TestHooks;
    evalWithCoverage(wrappedSource, MODULE1_SCRIPT_PATH);
    return window.__module1TestHooks;
}

function setupModule1(url = "/") {
    document.body.innerHTML = "";
    document.body.removeAttribute("data-page");
    window.history.replaceState({}, "", url);

    const sharedApp = createSharedAppMock();
    window.SharedApp = sharedApp;

    const hooks = loadModule1TestHooks();
    return { hooks, sharedApp };
}

async function flushPromises() {
    await Promise.resolve();
    await Promise.resolve();
}

function createUnauthorizedError(message = "Please log in first.") {
    const error = new Error(message);
    error.status = 401;
    return error;
}

describe("module1.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    describe("navigation helpers", () => {
        test("returns a sanitized allowed next path in a normal case", () => {
            const { hooks } = setupModule1("/login.html");

            expect(hooks.sanitizeNextPath("./module6/viewer-detail.html?id=7#comments"))
                .toBe("/module6/viewer-detail.html?id=7#comments");
        });

        test("returns null for blank, external, or unsupported next paths", () => {
            const { hooks } = setupModule1("/login.html");

            expect(hooks.sanitizeNextPath("   ")).toBeNull();
            expect(hooks.sanitizeNextPath("https://evil.example/steal")).toBeNull();
            expect(hooks.sanitizeNextPath("javascript:alert(1)")).toBeNull();
            expect(hooks.sanitizeNextPath("/forbidden.html")).toBeNull();
        });

        test("keeps the safe next parameter and ignores empty extra parameters when building an auth page URL", () => {
            const { hooks } = setupModule1("/login.html?next=/account.html");

            const result = hooks.buildAuthPageUrl("./register.html", {
                message: "Account created successfully.",
                empty: "",
                ignored: null
            });
            const url = new URL(`http://localhost${result}`);

            expect(url.pathname).toBe("/register.html");
            expect(url.searchParams.get("next")).toBe("/account.html");
            expect(url.searchParams.get("message")).toBe("Account created successfully.");
            expect(url.searchParams.has("empty")).toBe(false);
            expect(url.searchParams.has("ignored")).toBe(false);
        });

        test("prefers a safe next path before role-based redirect rules", () => {
            const { hooks } = setupModule1("/login.html?next=/account.html");

            expect(hooks.resolvePostLoginRedirect({
                role: "ADMINISTRATOR",
                contributor: true
            })).toBe("/account.html");
        });

        test("falls back to dashboard, contributor workspace, or viewer page when no next path is present", () => {
            const { hooks } = setupModule1("/login.html");

            expect(hooks.resolvePostLoginRedirect({
                role: "ADMINISTRATOR",
                contributor: false
            })).toBe("./admin-dashboard.html");
            expect(hooks.resolvePostLoginRedirect({
                role: "REGISTERED_VIEWER",
                contributor: true
            })).toBe("./resource-edit.html");
            expect(hooks.resolvePostLoginRedirect({
                role: "REGISTERED_VIEWER",
                contributor: false
            })).toBe("./module6/heritage-viewer.html");
        });

        test("returns the correct account back fallback path for each user type", () => {
            const { hooks } = setupModule1("/account.html");

            expect(hooks.resolveAccountBackFallback({
                role: "ADMINISTRATOR",
                contributor: false
            })).toBe("./admin-dashboard.html");
            expect(hooks.resolveAccountBackFallback({
                role: "REGISTERED_VIEWER",
                contributor: true
            })).toBe("./resource-edit.html");
            expect(hooks.resolveAccountBackFallback({
                role: "REGISTERED_VIEWER",
                contributor: false,
                contributorStatus: "NONE"
            })).toBe("./module6/heritage-viewer.html");
        });
    });

    describe("enum and status helpers", () => {
        test("formats enum labels in normal and boundary cases", () => {
            const { hooks } = setupModule1("/account.html");

            expect(hooks.formatEnumLabel("PENDING_REVIEW")).toBe("Pending Review");
            expect(hooks.formatEnumLabel("approved")).toBe("Approved");
            expect(hooks.formatEnumLabel(null)).toBe("None");
        });

        test("identifies approved contributors from either contributor flag or contributor status", () => {
            const { hooks } = setupModule1("/account.html");

            expect(hooks.isApprovedContributor({ contributor: true, contributorStatus: "NONE" })).toBe(true);
            expect(hooks.isApprovedContributor({ contributor: false, contributorStatus: "APPROVED" })).toBe(true);
            expect(hooks.isApprovedContributor({ contributor: false, contributorStatus: "PENDING" })).toBe(false);
            expect(hooks.isApprovedContributor(null)).toBe(false);
        });

        test("returns contributor labels and request tones for normal and fallback cases", () => {
            const { hooks } = setupModule1("/account.html");

            expect(hooks.getContributorRecordLabel({ role: "ADMINISTRATOR" })).toBe("Not Required");
            expect(hooks.getContributorRecordLabel({ role: "REGISTERED_VIEWER", contributor: false, contributorStatus: "NONE" }))
                .toBe("Not Submitted");
            expect(hooks.getContributorRecordLabel({ role: "REGISTERED_VIEWER", contributor: false, contributorStatus: "REJECTED" }))
                .toBe("Rejected");

            expect(hooks.getAccountRequestStatusTone("APPROVED")).toBe("approved");
            expect(hooks.getAccountRequestStatusTone("REJECTED")).toBe("rejected");
            expect(hooks.getAccountRequestStatusTone("PENDING")).toBe("pending");
            expect(hooks.getAccountRequestStatusTone("UNKNOWN")).toBe("default");
        });

        test("builds account status copy for administrator, approved, rejected, and default users", () => {
            const { hooks } = setupModule1("/account.html");

            expect(hooks.buildAccountStatusCopy({ role: "ADMINISTRATOR" })).toContain("review contributor applications");
            expect(hooks.buildAccountStatusCopy({ role: "REGISTERED_VIEWER", contributor: true })).toContain("workspace is unlocked");
            expect(hooks.buildAccountStatusCopy({ role: "REGISTERED_VIEWER", contributor: false, contributorStatus: "REJECTED" }))
                .toContain("was rejected");
            expect(hooks.buildAccountStatusCopy({ role: "REGISTERED_VIEWER", contributor: false, contributorStatus: "NONE" }))
                .toContain("Registered User");
        });
    });

    describe("startRegisterCodeCooldown", () => {
        test("does nothing when the button is missing", () => {
            const { hooks } = setupModule1("/register.html");

            expect(() => hooks.startRegisterCodeCooldown(null, 3)).not.toThrow();
        });

        test("disables the button, updates the countdown text, and restores the original label at the boundary", () => {
            jest.useFakeTimers();
            const { hooks } = setupModule1("/register.html");
            document.body.innerHTML = '<button id="sendCodeBtn"></button>';
            const button = document.getElementById("sendCodeBtn");

            hooks.startRegisterCodeCooldown(button, 2);

            expect(button.disabled).toBe(true);
            expect(button.textContent).toBe("Send Code (2s)");

            jest.advanceTimersByTime(1000);
            expect(button.textContent).toBe("Send Code (1s)");

            jest.advanceTimersByTime(1000);
            expect(button.disabled).toBe(false);
            expect(button.textContent).toBe("Send Code");
        });
    });

    describe("loadPendingRequestList", () => {
        test("renders pending requests in a normal case", async () => {
            const { hooks, sharedApp } = setupModule1("/admin-approval.html");
            document.body.innerHTML = '<div id="pendingRequestList"></div>';
            sharedApp.requestJson.mockResolvedValue([
                {
                    requestId: 9,
                    userName: 'Alice <Admin>',
                    userEmail: "alice@example.com",
                    requestedAt: "2026-05-05T10:00:00Z",
                    applicationReason: "I love <heritage> archives.",
                    status: "PENDING"
                }
            ]);

            await hooks.loadPendingRequestList();

            const container = document.getElementById("pendingRequestList");
            expect(sharedApp.requestJson).toHaveBeenCalledWith("/api/admin/contributor-requests/pending", { method: "GET" });
            expect(container.innerHTML).toContain("reviewComment-9");
            expect(container.innerHTML).toContain("Alice &lt;Admin&gt;");
            expect(container.innerHTML).toContain("I love &lt;heritage&gt; archives.");
            expect(container.innerHTML).toContain("Pending");
        });

        test("renders an empty state when there are no pending requests", async () => {
            const { hooks, sharedApp } = setupModule1("/admin-approval.html");
            document.body.innerHTML = '<div id="pendingRequestList"></div>';
            sharedApp.requestJson.mockResolvedValue([]);

            await hooks.loadPendingRequestList();

            expect(document.getElementById("pendingRequestList").textContent)
                .toContain("There are no pending contributor requests right now.");
        });

        test("renders an escaped error message when loading pending requests fails", async () => {
            const { hooks, sharedApp } = setupModule1("/admin-approval.html");
            document.body.innerHTML = '<div id="pendingRequestList"></div>';
            sharedApp.requestJson.mockRejectedValue(new Error("<script>alert(1)</script>"));

            await hooks.loadPendingRequestList();

            expect(document.getElementById("pendingRequestList").innerHTML)
                .toContain("&lt;script&gt;alert(1)&lt;/script&gt;");
        });

        test("returns early without calling the API when the container is missing", async () => {
            const { hooks, sharedApp } = setupModule1("/admin-approval.html");

            await hooks.loadPendingRequestList();

            expect(sharedApp.requestJson).not.toHaveBeenCalled();
        });
    });

    describe("account rendering helpers", () => {
        test("renders an empty contributor request card when no request exists", () => {
            const { hooks } = setupModule1("/account.html");
            document.body.innerHTML = '<div id="latestContributorRequest"></div>';

            hooks.populateLatestRequestCard(null);

            const container = document.getElementById("latestContributorRequest");
            expect(container.dataset.state).toBe("empty");
            expect(container.textContent).toContain("No contributor request has been submitted yet.");
        });

        test("renders a filled contributor request card with escaped values", () => {
            const { hooks } = setupModule1("/account.html");
            document.body.innerHTML = '<div id="latestContributorRequest"></div>';

            hooks.populateLatestRequestCard({
                userName: "Bob <Viewer>",
                userEmail: "bob@example.com",
                applicationReason: "Because <culture> matters",
                status: "REJECTED",
                requestedAt: "2026-05-05T10:00:00Z",
                reviewedAt: null,
                reviewComment: "<Denied>"
            });

            const container = document.getElementById("latestContributorRequest");
            expect(container.dataset.state).toBe("filled");
            expect(container.innerHTML).toContain("Bob &lt;Viewer&gt;");
            expect(container.innerHTML).toContain("Because &lt;culture&gt; matters");
            expect(container.innerHTML).toContain("account-request-stamp-rejected");
            expect(container.innerHTML).toContain("&lt;Denied&gt;");
        });

        test("renders the home session card for unauthenticated and authenticated users", () => {
            const { hooks } = setupModule1("/index.html?next=/account.html");
            document.body.innerHTML = '<div id="homeSessionCard"></div>';
            const card = document.getElementById("homeSessionCard");

            hooks.renderHomeSession(null);
            expect(card.className).toBe("status-panel muted-panel");
            expect(card.textContent).toContain("Not logged in yet");

            hooks.renderHomeSession({
                name: "Luna <User>",
                role: "REGISTERED_VIEWER",
                contributorStatus: "NONE",
                contributor: false
            });

            expect(card.className).toBe("status-panel");
            expect(card.innerHTML).toContain("Luna &lt;User&gt;");
            expect(card.querySelector("a").getAttribute("href")).toBe("/account.html");
            expect(card.querySelector("[data-logout-btn]")).not.toBeNull();
        });

        test("updates the contributor panel correctly for administrator, approved, and rejected users", () => {
            const { hooks } = setupModule1("/account.html");
            document.body.innerHTML = `
                <div id="contributorActionText"></div>
                <button id="submitContributorRequestBtn" class="hidden"></button>
                <a id="accountStatusWorkspaceLink" class="hidden"></a>
                <div id="contributorReasonField" class="hidden"></div>
            `;

            hooks.populateContributorPanel(
                { role: "ADMINISTRATOR", contributor: false },
                null
            );
            expect(document.getElementById("contributorActionText").textContent).toContain("Administrators review contributor requests");
            expect(document.getElementById("submitContributorRequestBtn").classList.contains("hidden")).toBe(true);
            expect(document.getElementById("accountStatusWorkspaceLink").getAttribute("href")).toBe("./admin-dashboard.html");

            hooks.populateContributorPanel(
                { role: "REGISTERED_VIEWER", contributor: true },
                null
            );
            expect(document.getElementById("accountStatusWorkspaceLink").getAttribute("href")).toBe("./resource-edit.html");
            expect(document.getElementById("submitContributorRequestBtn").classList.contains("hidden")).toBe(true);

            hooks.populateContributorPanel(
                { role: "REGISTERED_VIEWER", contributor: false, contributorStatus: "REJECTED" },
                { status: "REJECTED" }
            );
            expect(document.getElementById("contributorActionText").textContent).toContain("was rejected");
            expect(document.getElementById("submitContributorRequestBtn").classList.contains("hidden")).toBe(false);
        });
    });

    describe("page initialization flows", () => {
        test("shows a validation error when register verification code is requested with an empty email", async () => {
            const { hooks, sharedApp } = setupModule1("/register.html");
            document.body.innerHTML = `
                <form id="registerForm"></form>
                <a id="registerLoginLink"></a>
                <button id="sendRegisterCodeBtn" type="button">Send Code</button>
                <input id="registerEmail" value="">
                <input id="registerVerificationCode" value="">
                <input id="registerPassword" value="">
                <input id="registerConfirmPassword" value="">
                <input id="registerName" value="">
            `;
            sharedApp.requestJson.mockRejectedValueOnce(createUnauthorizedError());

            await hooks.initRegisterPage();
            document.getElementById("sendRegisterCodeBtn").click();
            await flushPromises();

            expect(sharedApp.showToast).toHaveBeenCalledWith("Please enter your email address first.");
            expect(document.activeElement).toBe(document.getElementById("registerEmail"));
            expect(sharedApp.requestJson).toHaveBeenCalledTimes(1);
        });

        test("shows a validation error when register verification code is requested with an invalid email", async () => {
            const { hooks, sharedApp } = setupModule1("/register.html");
            document.body.innerHTML = `
                <form id="registerForm"></form>
                <a id="registerLoginLink"></a>
                <button id="sendRegisterCodeBtn" type="button">Send Code</button>
                <input id="registerEmail" value="invalid-email">
                <input id="registerVerificationCode" value="">
                <input id="registerPassword" value="">
                <input id="registerConfirmPassword" value="">
                <input id="registerName" value="">
            `;
            sharedApp.requestJson.mockRejectedValueOnce(createUnauthorizedError());

            await hooks.initRegisterPage();
            document.getElementById("sendRegisterCodeBtn").click();
            await flushPromises();

            expect(sharedApp.showToast)
                .toHaveBeenCalledWith("Please enter a valid email address before requesting a verification code.");
            expect(document.activeElement).toBe(document.getElementById("registerEmail"));
            expect(sharedApp.requestJson).toHaveBeenCalledTimes(1);
        });

        test("submits the register verification code request in a normal case and starts cooldown", async () => {
            jest.useFakeTimers();
            const { hooks, sharedApp } = setupModule1("/register.html");
            document.body.innerHTML = `
                <form id="registerForm"></form>
                <a id="registerLoginLink"></a>
                <button id="sendRegisterCodeBtn" type="button">Send Code</button>
                <input id="registerEmail" value=" user@example.com ">
                <input id="registerVerificationCode" value="">
                <input id="registerPassword" value="">
                <input id="registerConfirmPassword" value="">
                <input id="registerName" value="">
            `;
            sharedApp.requestJson
                .mockRejectedValueOnce(createUnauthorizedError())
                .mockResolvedValueOnce(null);

            await hooks.initRegisterPage();
            document.getElementById("sendRegisterCodeBtn").click();
            await flushPromises();

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(2,
                "/api/auth/register-verification-code",
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email: "user@example.com" })
                });
            expect(sharedApp.showToast).toHaveBeenCalledWith("Verification code sent. Please check your email.");
            expect(document.getElementById("sendRegisterCodeBtn").disabled).toBe(true);
            expect(document.getElementById("sendRegisterCodeBtn").textContent).toBe("Send Code (60s)");
            expect(document.activeElement).toBe(document.getElementById("registerVerificationCode"));

            jest.advanceTimersByTime(60000);
            expect(document.getElementById("sendRegisterCodeBtn").disabled).toBe(false);
            expect(document.getElementById("sendRegisterCodeBtn").textContent).toBe("Send Code");
        });

        test("rejects registration submit when password confirmation does not match", async () => {
            const { hooks, sharedApp } = setupModule1("/register.html");
            document.body.innerHTML = `
                <form id="registerForm"></form>
                <a id="registerLoginLink"></a>
                <button id="sendRegisterCodeBtn" type="button">Send Code</button>
                <input id="registerEmail" value="user@example.com">
                <input id="registerVerificationCode" value="123456">
                <input id="registerPassword" value="Secret123!">
                <input id="registerConfirmPassword" value="Mismatch123!">
                <input id="registerName" value="User Name">
            `;
            sharedApp.requestJson.mockRejectedValueOnce(createUnauthorizedError());

            await hooks.initRegisterPage();
            document.getElementById("registerForm").dispatchEvent(new Event("submit", {
                bubbles: true,
                cancelable: true
            }));
            await flushPromises();

            expect(sharedApp.showToast).toHaveBeenCalledWith("The password confirmation does not match.");
            expect(document.activeElement).toBe(document.getElementById("registerConfirmPassword"));
            expect(sharedApp.requestJson).toHaveBeenCalledTimes(1);
        });

        test("submits login and shows the server error message when login fails", async () => {
            const { hooks, sharedApp } = setupModule1("/login.html?next=/account.html");
            document.body.innerHTML = `
                <form id="loginForm"></form>
                <a id="loginCreateAccountLink"></a>
                <input id="loginEmail" value=" user@example.com ">
                <input id="loginPassword" value="WrongPassword!">
            `;
            sharedApp.requestJson
                .mockRejectedValueOnce(createUnauthorizedError())
                .mockRejectedValueOnce(new Error("Invalid email or password."));

            await hooks.initLoginPage();
            document.getElementById("loginForm").dispatchEvent(new Event("submit", {
                bubbles: true,
                cancelable: true
            }));
            await flushPromises();

            expect(document.getElementById("loginCreateAccountLink").getAttribute("href")).toBe("/register.html?next=%2Faccount.html");
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(2,
                "/api/auth/login",
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        email: "user@example.com",
                        password: "WrongPassword!"
                    })
                });
            expect(sharedApp.showToast).toHaveBeenCalledWith("Invalid email or password.");
        });
    });
});
