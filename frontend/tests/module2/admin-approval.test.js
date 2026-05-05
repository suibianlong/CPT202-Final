const fs = require("fs");
const path = require("path");

const ADMIN_APPROVAL_SCRIPT_PATH = path.resolve(__dirname, "../../module2/admin-approval.js");

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
        statusBadge
    };
}

function loadAdminApprovalHooks() {
    const source = fs.readFileSync(ADMIN_APPROVAL_SCRIPT_PATH, "utf8");
    const injectedSource = source.replace(
        /window\.Module2Approval\s*=\s*\{\s*loadContributorApprovalData\s*\};\s*\}\)\(\);/m,
        `
        window.Module2Approval = {
            loadContributorApprovalData
        };

        window.__adminApprovalTestHooks = {
            bindApprovalTabs,
            bindContributorActions,
            loadContributorApprovalData,
            renderPendingRequests,
            renderPendingRequestCard,
            renderApprovedContributors,
            renderApprovedContributorRow,
            loadContributorDetail,
            renderContributorDetail,
            submitContributorDecision,
            revokeContributor,
            detailItem,
            capitalize,
            __setState(nextState = {}) {
                if (Object.prototype.hasOwnProperty.call(nextState, "pendingRequests")) {
                    pendingRequests = nextState.pendingRequests;
                }
                if (Object.prototype.hasOwnProperty.call(nextState, "approvedContributors")) {
                    approvedContributors = nextState.approvedContributors;
                }
            },
            __getState() {
                return {
                    pendingRequests,
                    approvedContributors
                };
            },
            __resetState() {
                pendingRequests = [];
                approvedContributors = [];
            }
        };
    })();
        `
    );

    delete window.__adminApprovalTestHooks;
    window.eval(injectedSource);
    return window.__adminApprovalTestHooks;
}

function setupAdminApproval() {
    document.body.innerHTML = "";
    jest.useRealTimers();
    window.confirm = jest.fn().mockReturnValue(true);

    const adminModule = createAdminModuleMock();
    window.AdminModule = adminModule;

    const hooks = loadAdminApprovalHooks();
    hooks.__resetState();
    return { hooks, adminModule };
}

describe("admin-approval.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    describe("helper functions", () => {
        test("capitalizes values in normal and boundary cases", () => {
            const { hooks } = setupAdminApproval();

            expect(hooks.capitalize("pending")).toBe("Pending");
            expect(hooks.capitalize("Approved")).toBe("Approved");
            expect(hooks.capitalize("")).toBe("");
            expect(hooks.capitalize(null)).toBe("");
        });

        test("renders contributor detail items with escaped content", () => {
            const { hooks } = setupAdminApproval();

            const html = hooks.detailItem('Label <X>', 'Value <Y>');

            expect(html).toContain("Label &lt;X&gt;");
            expect(html).toContain("Value &lt;Y&gt;");
        });
    });

    describe("approval data loading", () => {
        test("loads contributor approval data successfully and renders both panels", async () => {
            const { hooks, adminModule } = setupAdminApproval();
            document.body.innerHTML = `
                <div id="contributorPendingPanel"></div>
                <div id="approvedContributorPanel"></div>
            `;
            adminModule.requestJson
                .mockResolvedValueOnce([
                    {
                        requestId: 4,
                        userName: 'Alice <Applicant>',
                        userEmail: "alice@example.com",
                        requestedAt: "2026-05-05T10:00:00Z",
                        status: "PENDING"
                    }
                ])
                .mockResolvedValueOnce([
                    {
                        userId: 9,
                        userName: 'Bob <Contributor>',
                        userEmail: "bob@example.com",
                        requestId: 14
                    }
                ]);

            await hooks.loadContributorApprovalData();

            expect(adminModule.setState).toHaveBeenNthCalledWith(1, "contributorPendingPanel", "Loading contributor applications...");
            expect(adminModule.setState).toHaveBeenNthCalledWith(2, "approvedContributorPanel", "Loading contributors...");
            expect(adminModule.requestJson).toHaveBeenNthCalledWith(1,
                "/api/admin/contributor-requests/pending",
                { method: "GET" }
            );
            expect(adminModule.requestJson).toHaveBeenNthCalledWith(2,
                "/api/admin/contributor-requests/approved-contributors",
                { method: "GET" }
            );
            expect(document.getElementById("contributorPendingPanel").innerHTML)
                .toContain("Alice &lt;Applicant&gt;");
            expect(document.getElementById("approvedContributorPanel").innerHTML)
                .toContain("Bob &lt;Contributor&gt;");
            expect(hooks.__getState().pendingRequests).toHaveLength(1);
            expect(hooks.__getState().approvedContributors).toHaveLength(1);
        });

        test("shows error states in both panels when approval data loading fails", async () => {
            const { hooks, adminModule } = setupAdminApproval();
            document.body.innerHTML = `
                <div id="contributorPendingPanel"></div>
                <div id="approvedContributorPanel"></div>
            `;
            adminModule.requestJson.mockRejectedValue(new Error("Unable to load contributor approval data."));

            await hooks.loadContributorApprovalData();

            expect(adminModule.setState).toHaveBeenLastCalledWith(
                "approvedContributorPanel",
                "Unable to load contributor approval data.",
                "error"
            );
            expect(document.getElementById("contributorPendingPanel").innerHTML)
                .toContain("Unable to load contributor approval data.");
        });
    });

    describe("pending request rendering", () => {
        test("renders the empty pending state when there are no pending requests", () => {
            const { hooks } = setupAdminApproval();
            document.body.innerHTML = '<div id="contributorPendingPanel"></div>';
            hooks.__setState({ pendingRequests: [] });

            hooks.renderPendingRequests();

            expect(document.getElementById("contributorPendingPanel").textContent)
                .toContain("There are no pending contributor applications.");
        });

        test("renders pending request cards with escaped and formatted content", () => {
            const { hooks } = setupAdminApproval();
            document.body.innerHTML = '<div id="contributorPendingPanel"></div>';
            hooks.__setState({
                pendingRequests: [
                    {
                        requestId: 7,
                        userName: 'User <One>',
                        userEmail: "user@example.com",
                        requestedAt: "2026-05-05T10:00:00Z",
                        status: "PENDING_REVIEW"
                    }
                ]
            });

            hooks.renderPendingRequests();

            const html = document.getElementById("contributorPendingPanel").innerHTML;
            expect(html).toContain("User &lt;One&gt;");
            expect(html).toContain("FMT:2026-05-05T10:00:00Z");
            expect(html).toContain("contributorComment-7");
            expect(html).toContain("data-contributor-detail=\"7\"");
        });

        test("renders a single pending request card in isolation", () => {
            const { hooks } = setupAdminApproval();

            const html = hooks.renderPendingRequestCard({
                requestId: 11,
                userName: 'Candidate <X>',
                userEmail: "candidate@example.com",
                requestedAt: "2026-05-05T10:00:00Z",
                status: "PENDING"
            });

            expect(html).toContain("Candidate &lt;X&gt;");
            expect(html).toContain("candidate@example.com");
            expect(html).toContain("Approve");
            expect(html).toContain("Reject");
        });
    });

    describe("approved contributor rendering", () => {
        test("renders the empty approved contributor state when there are no active contributors", () => {
            const { hooks } = setupAdminApproval();
            document.body.innerHTML = '<div id="approvedContributorPanel"></div>';
            hooks.__setState({ approvedContributors: [] });

            hooks.renderApprovedContributors();

            expect(document.getElementById("approvedContributorPanel").textContent)
                .toContain("No approved contributors are currently active.");
        });

        test("renders approved contributor rows and the revoke button", () => {
            const { hooks } = setupAdminApproval();
            document.body.innerHTML = '<div id="approvedContributorPanel"></div>';
            hooks.__setState({
                approvedContributors: [
                    {
                        userId: 15,
                        userName: 'Approved <User>',
                        userEmail: "approved@example.com",
                        requestId: 21
                    }
                ]
            });

            hooks.renderApprovedContributors();

            const html = document.getElementById("approvedContributorPanel").innerHTML;
            expect(html).toContain("Approved &lt;User&gt;");
            expect(html).toContain("#21");
            expect(html).toContain("data-revoke-contributor=\"15\"");
        });

        test("renders a single approved contributor row with fallback labels", () => {
            const { hooks } = setupAdminApproval();

            const html = hooks.renderApprovedContributorRow({
                userId: 18,
                userName: "",
                userEmail: "",
                requestId: null
            });

            expect(html).toContain("User 18");
            expect(html).toContain("-");
            expect(html).toContain("Revoke Contributor");
        });
    });

    describe("contributor detail flow", () => {
        test("loads contributor detail successfully and renders the detail panel", async () => {
            const { hooks, adminModule } = setupAdminApproval();
            document.body.innerHTML = '<div id="contributorDetailPanel"></div>';
            adminModule.requestJson.mockResolvedValue({
                requestId: 6,
                status: "APPROVED",
                userName: 'Applicant <A>',
                userEmail: "applicant@example.com",
                userId: 3,
                reviewedBy: "admin_1",
                requestedAt: "2026-05-05T10:00:00Z",
                reviewedAt: "2026-05-06T10:00:00Z",
                applicationReason: "I want to help.",
                reviewComment: "Looks good."
            });

            await hooks.loadContributorDetail(6);

            expect(adminModule.setState).toHaveBeenCalledWith("contributorDetailPanel", "Loading contributor application detail...");
            expect(adminModule.requestJson).toHaveBeenCalledWith(
                "/api/admin/contributor-requests/6",
                { method: "GET" }
            );
            const html = document.getElementById("contributorDetailPanel").innerHTML;
            expect(html).toContain("Application #6");
            expect(html).toContain("Applicant &lt;A&gt;");
            expect(html).toContain("Looks good.");
        });

        test("shows an error state when contributor detail loading fails", async () => {
            const { hooks, adminModule } = setupAdminApproval();
            document.body.innerHTML = '<div id="contributorDetailPanel"></div>';
            adminModule.requestJson.mockRejectedValue(new Error("Unable to load application detail."));

            await hooks.loadContributorDetail(6);

            expect(adminModule.setState).toHaveBeenLastCalledWith(
                "contributorDetailPanel",
                "Unable to load application detail.",
                "error"
            );
        });

        test("returns early when contributor detail is requested with an invalid id", async () => {
            const { hooks, adminModule } = setupAdminApproval();

            await hooks.loadContributorDetail(0);

            expect(adminModule.requestJson).not.toHaveBeenCalled();
        });
    });

    describe("decision and revoke flows", () => {
        test("returns early when decision input is missing or the user cancels", async () => {
            const { hooks, adminModule } = setupAdminApproval();
            const button = document.createElement("button");

            await hooks.submitContributorDecision(0, "APPROVED", button);
            await hooks.submitContributorDecision(3, "", button);

            expect(adminModule.jsonRequest).not.toHaveBeenCalled();

            window.confirm.mockReturnValueOnce(false);
            await hooks.submitContributorDecision(3, "APPROVED", button);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();
        });

        test("submits a contributor decision successfully and reloads data and detail", async () => {
            const { hooks, adminModule } = setupAdminApproval();
            document.body.innerHTML = `
                <div id="contributorPendingPanel"></div>
                <div id="approvedContributorPanel"></div>
                <div id="contributorDetailPanel"></div>
                <textarea id="contributorComment-8"> Approved now. </textarea>
            `;
            const button = document.createElement("button");

            adminModule.jsonRequest.mockResolvedValueOnce(null);
            adminModule.requestJson
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce({
                    requestId: 8,
                    status: "APPROVED",
                    userName: "User",
                    userEmail: "user@example.com",
                    userId: 7,
                    reviewedBy: "admin_1",
                    requestedAt: "2026-05-05T10:00:00Z",
                    reviewedAt: "2026-05-06T10:00:00Z",
                    applicationReason: "Reason",
                    reviewComment: "Approved now."
                });

            await hooks.submitContributorDecision(8, "APPROVED", button);

            expect(window.confirm).toHaveBeenCalledWith("Are you sure you want to approved this contributor application?");
            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/contributor-requests/8/decision",
                {
                    method: "POST",
                    body: JSON.stringify({
                        decision: "APPROVED",
                        reviewComment: "Approved now."
                    })
                }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Contributor application approved successfully.");
            expect(button.disabled).toBe(false);
        });

        test("shows an error message when contributor decision submission fails", async () => {
            const { hooks, adminModule } = setupAdminApproval();
            document.body.innerHTML = '<textarea id="contributorComment-8">Rejected</textarea>';
            const button = document.createElement("button");
            adminModule.jsonRequest.mockRejectedValueOnce(new Error("Decision failed."));

            await hooks.submitContributorDecision(8, "REJECTED", button);

            expect(adminModule.showToast).toHaveBeenCalledWith("Decision failed.");
            expect(button.disabled).toBe(false);
        });

        test("returns early when revoke input is missing or the user cancels", async () => {
            const { hooks, adminModule } = setupAdminApproval();
            const button = document.createElement("button");

            await hooks.revokeContributor(0, button);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();

            window.confirm.mockReturnValueOnce(false);
            await hooks.revokeContributor(7, button);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();
        });

        test("revokes a contributor successfully and refreshes approval data", async () => {
            const { hooks, adminModule } = setupAdminApproval();
            document.body.innerHTML = `
                <div id="contributorPendingPanel"></div>
                <div id="approvedContributorPanel"></div>
            `;
            const button = document.createElement("button");
            adminModule.jsonRequest.mockResolvedValueOnce(null);
            adminModule.requestJson
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            await hooks.revokeContributor(19, button);

            expect(window.confirm).toHaveBeenCalledWith("Revoke this user's contributor role? Their account and history will be kept.");
            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/admin/contributor-requests/contributors/19/revoke",
                { method: "POST" }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Contributor role revoked.");
            expect(button.disabled).toBe(false);
        });

        test("shows an error message when revoke contributor fails", async () => {
            const { hooks, adminModule } = setupAdminApproval();
            const button = document.createElement("button");
            adminModule.jsonRequest.mockRejectedValueOnce(new Error("Revoke failed."));

            await hooks.revokeContributor(19, button);

            expect(adminModule.showToast).toHaveBeenCalledWith("Revoke failed.");
            expect(button.disabled).toBe(false);
        });
    });
});
