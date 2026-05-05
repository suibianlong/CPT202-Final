const fs = require("fs");
const path = require("path");

const REVIEW_APPROVAL_SCRIPT_PATH = path.resolve(__dirname, "../../module5/review-approval.js");

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
        setState: jest.fn((elementId, message, type = "loading") => {
            const element = document.getElementById(elementId);
            if (element) {
                element.innerHTML = `<div class="admin-${type}">${escapeHtml(message)}</div>`;
            }
        }),
        getErrorMessage: jest.fn((error, fallback) => error && error.message ? error.message : fallback),
        escapeHtml,
        formatDateTime,
        formatLabel,
        statusBadge,
        emptyRow,
        jsonRequest: jest.fn(),
        showToast: jest.fn()
    };
}

function loadReviewApprovalHooks() {
    const source = fs.readFileSync(REVIEW_APPROVAL_SCRIPT_PATH, "utf8");
    const injectedSource = source.replace(
        /window\.Module5ReviewApproval\s*=\s*\{\s*loadResourceReviewData\s*\};\s*\}\)\(\);/m,
        `
        window.Module5ReviewApproval = {
            loadResourceReviewData
        };

        window.__reviewApprovalTestHooks = {
            loadResourceReviewData,
            renderPendingResources,
            renderPendingResourceCard,
            loadResourceDetail,
            renderResourceDetail,
            renderMediaSection,
            renderReviewHistory,
            submitResourceDecision,
            toPublicMediaUrl,
            detailItem,
            __setState(nextState = {}) {
                if (Object.prototype.hasOwnProperty.call(nextState, "pendingSubmissions")) {
                    pendingSubmissions = nextState.pendingSubmissions;
                }
                if (Object.prototype.hasOwnProperty.call(nextState, "selectedSubmission")) {
                    selectedSubmission = nextState.selectedSubmission;
                }
                if (Object.prototype.hasOwnProperty.call(nextState, "currentPage")) {
                    currentPage = nextState.currentPage;
                }
            },
            __getState() {
                return {
                    pendingSubmissions,
                    selectedSubmission,
                    currentPage
                };
            },
            __resetState() {
                pendingSubmissions = [];
                selectedSubmission = null;
                currentPage = 1;
            }
        };
    })();
        `
    );

    delete window.__reviewApprovalTestHooks;
    window.eval(injectedSource);
    return window.__reviewApprovalTestHooks;
}

function setupReviewApproval() {
    document.body.innerHTML = "";
    jest.useRealTimers();
    window.confirm = jest.fn().mockReturnValue(true);

    const adminModule = createAdminModuleMock();
    window.AdminModule = adminModule;

    const hooks = loadReviewApprovalHooks();
    hooks.__resetState();
    return { hooks, adminModule };
}

describe("review-approval.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    describe("helper functions", () => {
        test("normalizes public media urls in normal, empty, and boundary cases", () => {
            const { hooks } = setupReviewApproval();

            expect(hooks.toPublicMediaUrl("https://cdn.example.com/file.png"))
                .toBe("https://cdn.example.com/file.png");
            expect(hooks.toPublicMediaUrl("/uploads/file.png")).toBe("/uploads/file.png");
            expect(hooks.toPublicMediaUrl("/nested/path/file.png")).toBe("/uploads/nested/path/file.png");
            expect(hooks.toPublicMediaUrl("   ")).toBe("");
            expect(hooks.toPublicMediaUrl(null)).toBe("");
        });

        test("renders a detail item and escapes label and value content", () => {
            const { hooks } = setupReviewApproval();

            const html = hooks.detailItem("Label <X>", "Value <Y>");

            expect(html).toContain("Label &lt;X&gt;");
            expect(html).toContain("Value &lt;Y&gt;");
        });
    });

    describe("pending review list", () => {
        test("loads pending submissions successfully and renders cards with pagination", async () => {
            const { hooks, adminModule } = setupReviewApproval();
            document.body.innerHTML = '<div id="resourcePendingPanel"></div>';
            adminModule.requestJson.mockResolvedValue({
                items: [
                    {
                        submissionId: 12,
                        resourceId: 8,
                        versionNo: 3,
                        title: "Village Song <Archive>",
                        contributorId: 6,
                        contributorName: "Alice <Reviewer>",
                        categoryTopic: "folk music",
                        submittedAt: "2026-05-05T10:00:00Z",
                        resourceStatus: "PENDING_REVIEW"
                    }
                ],
                total: 15
            });

            await hooks.loadResourceReviewData();

            expect(adminModule.setState).toHaveBeenCalledWith("resourcePendingPanel", "Loading resource submissions...");
            expect(adminModule.requestJson).toHaveBeenCalledWith(
                "/api/reviewer/reviews/pending?page=1&pageSize=10",
                { method: "GET" }
            );
            const html = document.getElementById("resourcePendingPanel").innerHTML;
            expect(html).toContain("Village Song &lt;Archive&gt;");
            expect(html).toContain("Submission #12");
            expect(html).toContain("Page 1 of 2");
            expect(hooks.__getState().pendingSubmissions).toHaveLength(1);
        });

        test("renders the empty state when there are no pending submissions", () => {
            const { hooks } = setupReviewApproval();
            document.body.innerHTML = '<div id="resourcePendingPanel"></div>';
            hooks.__setState({ pendingSubmissions: [] });

            hooks.renderPendingResources({
                emptyMessage: "Nothing pending right now.",
                total: 0
            });

            expect(document.getElementById("resourcePendingPanel").innerHTML)
                .toContain("Nothing pending right now.");
        });

        test("shows an error state when loading pending submissions fails", async () => {
            const { hooks, adminModule } = setupReviewApproval();
            document.body.innerHTML = '<div id="resourcePendingPanel"></div>';
            adminModule.requestJson.mockRejectedValue(new Error("Unable to load pending submissions."));

            await hooks.loadResourceReviewData();

            expect(adminModule.setState).toHaveBeenLastCalledWith(
                "resourcePendingPanel",
                "Unable to load pending submissions.",
                "error"
            );
        });

        test("renders a single pending card with escaped and formatted content", () => {
            const { hooks } = setupReviewApproval();

            const html = hooks.renderPendingResourceCard({
                submissionId: 22,
                resourceId: 5,
                versionNo: 1,
                title: "Archive <Item>",
                contributorId: 7,
                contributorName: "User <Seven>",
                categoryTopic: "stories",
                submittedAt: "2026-05-05T10:00:00Z",
                resourceStatus: "PENDING_REVIEW"
            });

            expect(html).toContain("Archive &lt;Item&gt;");
            expect(html).toContain("User &lt;Seven&gt;");
            expect(html).toContain("FMT:2026-05-05T10:00:00Z");
            expect(html).toContain("data-resource-detail=\"22\"");
        });
    });

    describe("resource detail rendering", () => {
        test("loads and renders resource detail successfully in a normal case", async () => {
            const { hooks, adminModule } = setupReviewApproval();
            document.body.innerHTML = '<div id="resourceDetailPanel"></div>';
            adminModule.requestJson.mockResolvedValue({
                submissionId: 9,
                resourceId: 4,
                versionNo: 2,
                resourceStatus: "APPROVED",
                contributor: {
                    userId: 13,
                    username: "Contributor <A>"
                },
                category: {
                    categoryTopic: "traditional dance"
                },
                submission: {
                    submissionNote: "Initial note",
                    submittedAt: "2026-05-05T10:00:00Z",
                    currentContextLabel: "Pending Review",
                    statusSnapshot: "PENDING_REVIEW"
                },
                resource: {
                    title: "Dance Archive <Set>",
                    resourceType: "video",
                    place: "Liverpool",
                    description: "Detailed description",
                    copyrightDeclaration: "Open use",
                    previewImage: "/preview.png",
                    mediaUrl: "/media.mp4",
                    files: []
                },
                tags: ["dance", "community"],
                reviewHistory: []
            });

            await hooks.loadResourceDetail(9);

            expect(adminModule.setState).toHaveBeenCalledWith("resourceDetailPanel", "Loading resource detail...");
            expect(adminModule.requestJson).toHaveBeenCalledWith(
                "/api/reviewer/reviews/submissions/9",
                { method: "GET" }
            );
            const html = document.getElementById("resourceDetailPanel").innerHTML;
            expect(html).toContain("Dance Archive &lt;Set&gt;");
            expect(html).toContain("Contributor &lt;A&gt;");
            expect(html).not.toContain("Archive Resource");
            expect(hooks.__getState().selectedSubmission.submissionId).toBe(9);
        });

        test("renders media section and review history in normal and empty cases", () => {
            const { hooks } = setupReviewApproval();

            const mediaHtml = hooks.renderMediaSection({
                previewImage: "/preview.png",
                mediaUrl: "https://example.com/file.pdf",
                files: [
                    {
                        originalFilename: "evidence.pdf",
                        fileType: "application/pdf",
                        fileSize: 128
                    }
                ]
            });
            expect(mediaHtml).toContain("/uploads/preview.png");
            expect(mediaHtml).toContain("https://example.com/file.pdf");
            expect(mediaHtml).toContain("evidence.pdf");

            const historyHtml = hooks.renderReviewHistory([
                {
                    action: "APPROVED",
                    status: "APPROVED",
                    reviewerName: "Admin <One>",
                    reviewedAt: "2026-05-05T10:00:00Z",
                    feedbackComment: "Looks good."
                }
            ]);
            expect(historyHtml).toContain("Admin &lt;One&gt;");
            expect(historyHtml).toContain("Looks good.");

            const emptyHistory = hooks.renderReviewHistory([]);
            expect(emptyHistory).toContain("No review history has been recorded yet.");
        });

        test("shows an error state and clears selection when loading detail fails", async () => {
            const { hooks, adminModule } = setupReviewApproval();
            document.body.innerHTML = '<div id="resourceDetailPanel"></div>';
            hooks.__setState({ selectedSubmission: { submissionId: 99 } });
            adminModule.requestJson.mockRejectedValue(new Error("Unable to load detail."));

            await hooks.loadResourceDetail(7);

            expect(hooks.__getState().selectedSubmission).toBeNull();
            expect(adminModule.setState).toHaveBeenLastCalledWith(
                "resourceDetailPanel",
                "Unable to load detail.",
                "error"
            );
        });
    });

    describe("resource decision flow", () => {
        test("blocks decision submission when no resource has been selected", async () => {
            const { hooks, adminModule } = setupReviewApproval();
            const button = document.createElement("button");

            await hooks.submitResourceDecision("approve", button);

            expect(adminModule.showToast).toHaveBeenCalledWith("Select a resource submission first.");
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();
        });

        test("blocks reject submission when the feedback comment is empty", async () => {
            const { hooks, adminModule } = setupReviewApproval();
            document.body.innerHTML = '<textarea id="resourceFeedbackComment"></textarea>';
            hooks.__setState({
                selectedSubmission: {
                    submissionId: 7,
                    resourceId: 3,
                    versionNo: 1
                }
            });
            const button = document.createElement("button");

            await hooks.submitResourceDecision("reject", button);

            expect(adminModule.showToast).toHaveBeenCalledWith("Feedback comment is required when rejecting a resource.");
            expect(document.activeElement).toBe(document.getElementById("resourceFeedbackComment"));
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();
        });

        test("submits an approve decision successfully and reloads list and detail", async () => {
            const { hooks, adminModule } = setupReviewApproval();
            document.body.innerHTML = `
                <div id="resourcePendingPanel"></div>
                <div id="resourceDetailPanel"></div>
                <textarea id="resourceFeedbackComment">Approved comment</textarea>
            `;
            hooks.__setState({
                selectedSubmission: {
                    submissionId: 7,
                    resourceId: 3,
                    versionNo: 1
                }
            });
            const button = document.createElement("button");

            adminModule.jsonRequest.mockResolvedValueOnce(null);
            adminModule.requestJson
                .mockResolvedValueOnce({ items: [], total: 0, emptyMessage: "No items." })
                .mockResolvedValueOnce({
                    submissionId: 7,
                    resourceId: 3,
                    versionNo: 1,
                    resourceStatus: "APPROVED",
                    resource: {},
                    contributor: {},
                    category: {},
                    submission: {},
                    reviewHistory: []
                });

            await hooks.submitResourceDecision("approve", button);

            expect(window.confirm).toHaveBeenCalledWith("Are you sure you want to approve this resource submission?");
            expect(adminModule.jsonRequest).toHaveBeenCalledWith(
                "/api/reviewer/reviews/7/approve",
                {
                    method: "POST",
                    body: JSON.stringify({
                        resourceId: 3,
                        versionNo: 1,
                        feedbackComment: "Approved comment"
                    })
                }
            );
            expect(adminModule.showToast).toHaveBeenCalledWith("Resource approved successfully.");
            expect(button.disabled).toBe(false);
        });

        test("shows an error message when decision submission fails or is cancelled", async () => {
            const { hooks, adminModule } = setupReviewApproval();
            document.body.innerHTML = '<textarea id="resourceFeedbackComment">Comment</textarea>';
            hooks.__setState({
                selectedSubmission: {
                    submissionId: 5,
                    resourceId: 2,
                    versionNo: 8
                }
            });
            const button = document.createElement("button");

            window.confirm.mockReturnValueOnce(false);
            await hooks.submitResourceDecision("approve", button);
            expect(adminModule.jsonRequest).not.toHaveBeenCalled();

            window.confirm.mockReturnValueOnce(true);
            adminModule.jsonRequest.mockRejectedValueOnce(new Error("Decision failed."));
            await hooks.submitResourceDecision("approve", button);

            expect(adminModule.showToast).toHaveBeenCalledWith("Decision failed.");
            expect(button.disabled).toBe(false);
        });
    });
});
