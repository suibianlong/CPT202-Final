const fs = require("fs");
const path = require("path");
const { evalWithCoverage } = require("../test-utils/eval-with-coverage");

const MODULE6_SCRIPT_PATH = path.resolve(__dirname, "../../module6/module6.js");

function createSharedAppMock() {
    const escapeHtml = jest.fn((value) => String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;"));

    const formatDateTime = jest.fn((value, { emptyText = "-" } = {}) => value ? `FMT:${value}` : emptyText);

    return {
        requestJson: jest.fn(),
        showMessageFromQuery: jest.fn(),
        showToast: jest.fn(),
        escapeHtml,
        formatDateTime
    };
}

function loadModule6TestHooks() {
    const source = fs.readFileSync(MODULE6_SCRIPT_PATH, "utf8");
    const wrappedSource = `
        (() => {
            ${source}
            window.__module6TestHooks = {
                initHeritageViewerPage,
                initViewerDetailPage,
                initViewerFeedbackPage,
                bindViewerNavigationButton,
                bindViewerAccountButton,
                bindViewerBackButton,
                bindViewerSearchControls,
                bindViewerDetailForms,
                ensureViewerAuthenticated,
                loadViewerCategoryOptions,
                loadViewerResourceTypeOptions,
                loadApprovedResources,
                loadApprovedResourceDetail,
                renderApprovedResources,
                renderApprovedResourceDetail,
                loadViewerComments,
                submitViewerComment,
                renderViewerComments,
                renderViewerCommentError,
                deleteViewerComment,
                loadViewerFeedbackHistory,
                submitViewerFeedback,
                renderViewerFeedbackHistory,
                renderViewerFeedbackError,
                updateViewerFeedbackFileText,
                renderViewerTags,
                renderPreviewMedia,
                renderPrimaryMedia,
                handleViewerError,
                canViewerDeleteComment,
                isViewerAdmin,
                resolveCategoryName,
                getViewerCategoryImageUrl,
                capitalizeViewerLabel,
                normalizeViewerCategoryKey,
                normalizeViewerResourceTypeOptions,
                formatViewerResourceType,
                normalizeViewerResourceTypeValue,
                normalizeViewerResourceType,
                buildViewerExcerpt,
                resetViewerFilters,
                getViewerResourceIdFromQuery,
                toPublicMediaUrl,
                redirectViewerToLogin,
                __setViewerState(nextState = {}) {
                    if (Object.prototype.hasOwnProperty.call(nextState, "currentUser")) {
                        viewerCurrentUser = nextState.currentUser;
                    }
                    if (Object.prototype.hasOwnProperty.call(nextState, "categoryOptions")) {
                        viewerCategoryOptions = nextState.categoryOptions;
                    }
                    if (Object.prototype.hasOwnProperty.call(nextState, "resourceTypeOptions")) {
                        viewerResourceTypeOptions = nextState.resourceTypeOptions;
                    }
                },
                __resetViewerState() {
                    viewerCurrentUser = null;
                    viewerCategoryOptions = [];
                    viewerResourceTypeOptions = [];
                }
            };
        })();
    `;

    delete window.__module6TestHooks;
    evalWithCoverage(wrappedSource, MODULE6_SCRIPT_PATH);
    return window.__module6TestHooks;
}

function setupModule6(url = "/module6/heritage-viewer.html") {
    document.body.innerHTML = "";
    document.body.removeAttribute("data-page");
    window.history.replaceState({}, "", url);

    const sharedApp = createSharedAppMock();
    window.SharedApp = sharedApp;

    const hooks = loadModule6TestHooks();
    hooks.__resetViewerState();
    return { hooks, sharedApp };
}

async function flushPromises() {
    await Promise.resolve();
    await Promise.resolve();
}

function buildDetailDom() {
    document.body.innerHTML = `
        <section id="detailError" class="hidden"></section>
        <div id="detailContent" class="hidden"></div>
        <h1 id="detailTitle"></h1>
        <p id="detailSubtitle"></p>
        <strong id="detailId"></strong>
        <strong id="detailType"></strong>
        <strong id="detailCategory"></strong>
        <strong id="detailPlace"></strong>
        <strong id="detailReviewedAt"></strong>
        <strong id="detailCopyright"></strong>
        <p id="detailDescription"></p>
        <div id="detailTags"></div>
        <div id="previewContainer"></div>
        <div id="mediaContainer"></div>
        <div id="commentList"></div>
        <div id="feedbackList"></div>
    `;
}

describe("module6.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    describe("viewer helper functions", () => {
        test("normalizes category keys, resource types, and media URLs in normal and boundary cases", () => {
            const { hooks } = setupModule6("/module6/heritage-viewer.html");

            expect(hooks.normalizeViewerCategoryKey(" Education ")).toBe("educational materials");
            expect(hooks.normalizeViewerCategoryKey(null)).toBe("");

            expect(hooks.normalizeViewerResourceType(" Photo/Image ")).toBe("photo");
            expect(hooks.normalizeViewerResourceType("file_document")).toBe("document");
            expect(hooks.normalizeViewerResourceType("EXTRA_LINK")).toBe("extra link");
            expect(hooks.normalizeViewerResourceType("")).toBe("");

            expect(hooks.toPublicMediaUrl("https://cdn.example.com/file.png"))
                .toBe("https://cdn.example.com/file.png");
            expect(hooks.toPublicMediaUrl("/images/file.png")).toBe("/uploads/images/file.png");
            expect(hooks.toPublicMediaUrl(null)).toBe("");
        });

        test("normalizes resource type option payloads and formats display labels", () => {
            const { hooks } = setupModule6("/module6/heritage-viewer.html");

            expect(hooks.normalizeViewerResourceTypeOptions([
                " photo ",
                { typeName: "video" },
                { label: "extra link" },
                null,
                { value: "audio" },
                { name: "  " }
            ])).toEqual([
                { name: "photo" },
                { name: "video" },
                { name: "extra link" },
                { name: "audio" }
            ]);

            expect(hooks.formatViewerResourceType("photo")).toBe("Photo/Image");
            expect(hooks.formatViewerResourceType("file_document")).toBe("File/Document");
            expect(hooks.formatViewerResourceType("extra_link")).toBe("Extra Link");
            expect(hooks.formatViewerResourceType("custom-type")).toBe("custom-type");
            expect(hooks.formatViewerResourceType("")).toBe("-");
        });

        test("builds excerpts and parses resource ids in normal, empty, and invalid cases", () => {
            const { hooks } = setupModule6("/module6/viewer-detail.html?id=42");

            expect(hooks.buildViewerExcerpt("  concise description  ", 50)).toBe("concise description");
            expect(hooks.buildViewerExcerpt("   ", 50)).toBe("No description provided yet.");

            const truncated = hooks.buildViewerExcerpt("123456789012345", 10);
            expect(truncated.startsWith("123456789")).toBe(true);
            expect(truncated).not.toBe("123456789012345");

            expect(hooks.getViewerResourceIdFromQuery()).toBe(42);

            window.history.replaceState({}, "", "/module6/viewer-detail.html?id=not-a-number");
            expect(hooks.getViewerResourceIdFromQuery()).toBeNull();
        });

        test("resolves category names and delete permissions from current viewer state", () => {
            const { hooks } = setupModule6("/module6/heritage-viewer.html");
            hooks.__setViewerState({
                currentUser: { userId: 9, role: "REGISTERED_VIEWER" },
                categoryOptions: [
                    { id: 2, name: "oral history" }
                ]
            });

            expect(hooks.resolveCategoryName(2)).toBe("Oral History");
            expect(hooks.resolveCategoryName(999)).toBe("Unknown category");
            expect(hooks.resolveCategoryName("")).toBe("-");

            expect(hooks.canViewerDeleteComment({ userId: 9 })).toBe(true);
            expect(hooks.canViewerDeleteComment({ userId: 3 })).toBe(false);

            hooks.__setViewerState({
                currentUser: { userId: 1, role: "ADMINISTRATOR" }
            });
            expect(hooks.isViewerAdmin()).toBe(true);
            expect(hooks.canViewerDeleteComment({ userId: 3 })).toBe(true);
        });
    });

    describe("option loading and resource list flow", () => {
        test("loads category and resource type options, preserving existing selected values", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/heritage-viewer.html");
            document.body.innerHTML = `
                <select id="categoryId">
                    <option value="2" selected>Existing</option>
                </select>
                <select id="type">
                    <option value="video" selected>Existing Type</option>
                </select>
            `;
            sharedApp.requestJson
                .mockResolvedValueOnce([
                    { id: 2, name: "oral history" },
                    { id: 3, name: "folk songs" }
                ])
                .mockResolvedValueOnce([
                    "photo",
                    { typeName: "video" },
                    { label: "extra link" }
                ]);

            await hooks.loadViewerCategoryOptions();
            await hooks.loadViewerResourceTypeOptions();

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(1,
                "/api/viewer/resources/category-options",
                { method: "GET" }
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(2,
                "/api/viewer/resources/resource-type-options",
                { method: "GET" }
            );
            expect(document.getElementById("categoryId").value).toBe("2");
            expect(document.getElementById("type").value).toBe("video");
            expect(document.getElementById("type").textContent).toContain("Photo/Image");
            expect(document.getElementById("type").textContent).toContain("Extra Link");
        });

        test("loads approved resources with filters in a normal case and renders cards", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/heritage-viewer.html");
            hooks.__setViewerState({
                categoryOptions: [
                    { id: 2, name: "education" }
                ]
            });
            document.body.innerHTML = `
                <input id="keyword" value=" pottery ">
                <select id="type">
                    <option value="photo" selected>Photo</option>
                </select>
                <select id="categoryId">
                    <option value="2" selected>Education</option>
                </select>
                <select id="sortBy">
                    <option value="updatedAtDesc" selected>Latest</option>
                </select>
                <div id="resourceList"></div>
            `;
            sharedApp.requestJson.mockResolvedValue([
                {
                    id: 7,
                    title: "Clay Pot <Set>",
                    description: "A very long cultural description about pottery and community memory.",
                    categoryId: 2,
                    categoryName: "education",
                    resourceType: "photo/image",
                    previewImage: "/images/pot.png",
                    updatedAt: "2026-05-05T10:00:00Z"
                }
            ]);

            await hooks.loadApprovedResources();

            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/viewer/resources?keyword=pottery&type=photo&categoryId=2&sortBy=updatedAtDesc",
                { method: "GET" }
            );
            const resourceList = document.getElementById("resourceList").innerHTML;
            expect(resourceList).toContain("Clay Pot &lt;Set&gt;");
            expect(resourceList).toContain("/uploads/images/pot.png");
            expect(resourceList).toContain("category-education.png");
            expect(resourceList).toContain("Photo/Image");
            expect(resourceList).toContain("data-resource-id=\"7\"");
        });

        test("renders the empty state when there are no matched approved resources or the container is missing", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/heritage-viewer.html");
            document.body.innerHTML = `
                <input id="keyword" value="">
                <select id="type"></select>
                <select id="categoryId"></select>
                <select id="sortBy"></select>
                <div id="resourceList"></div>
            `;
            sharedApp.requestJson.mockResolvedValue([]);

            await hooks.loadApprovedResources();
            expect(document.getElementById("resourceList").textContent).toContain("No matched approved resources.");

            document.body.innerHTML = "";
            sharedApp.requestJson.mockClear();
            await hooks.loadApprovedResources();
            expect(sharedApp.requestJson).not.toHaveBeenCalled();
        });

        test("resets viewer filters to empty values at the boundary", () => {
            const { hooks } = setupModule6("/module6/heritage-viewer.html");
            document.body.innerHTML = `
                <input id="keyword" value="keywords">
                <select id="type"><option value="photo" selected>Photo</option></select>
                <select id="categoryId"><option value="2" selected>Education</option></select>
                <select id="sortBy"><option value="updatedAtDesc" selected>Latest</option></select>
            `;

            hooks.resetViewerFilters();

            expect(document.getElementById("keyword").value).toBe("");
            expect(document.getElementById("type").value).toBe("");
            expect(document.getElementById("categoryId").value).toBe("");
            expect(document.getElementById("sortBy").value).toBe("");
        });
    });

    describe("detail rendering", () => {
        test("renders approved resource detail in a normal case using category fallback and media branches", () => {
            const { hooks } = setupModule6("/module6/viewer-detail.html?id=7");
            hooks.__setViewerState({
                categoryOptions: [
                    { id: 3, name: "oral history" }
                ]
            });
            buildDetailDom();

            hooks.renderApprovedResourceDetail({
                id: 7,
                title: "Archive Image",
                resourceType: "photo/image",
                categoryId: 3,
                place: "Liverpool",
                reviewedAt: "2026-05-05T10:00:00Z",
                copyright: "Community archive",
                description: "Important historical photograph",
                tagNames: ["local", "memory"],
                previewImage: "/preview.png",
                mediaUrl: "/main-photo.png",
                mediaUrls: ["/main-photo.png", "/main-photo-2.png"]
            });

            expect(document.getElementById("detailTitle").textContent).toBe("Archive Image");
            expect(document.getElementById("detailSubtitle").textContent).toContain("#7");
            expect(document.getElementById("detailType").textContent).toBe("Photo/Image");
            expect(document.getElementById("detailCategory").textContent).toBe("Oral History");
            expect(document.getElementById("detailReviewedAt").textContent).toBe("FMT:2026-05-05T10:00:00Z");
            expect(document.getElementById("detailTags").innerHTML).toContain("viewer-tag");
            expect(document.getElementById("previewContainer").innerHTML).toContain("/uploads/preview.png");
            expect(document.getElementById("mediaContainer").innerHTML).toContain("<img");
            expect(document.querySelectorAll("#mediaContainer img")).toHaveLength(2);
            expect(document.getElementById("mediaContainer").innerHTML).toContain("/uploads/main-photo-2.png");
            expect(document.getElementById("detailContent").classList.contains("hidden")).toBe(false);
        });

        test("renders fallback content when preview, tags, or primary media are missing", () => {
            const { hooks } = setupModule6("/module6/viewer-detail.html?id=7");
            buildDetailDom();

            hooks.renderPreviewMedia(null);
            hooks.renderPrimaryMedia({ mediaUrl: "", resourceType: "document" });
            hooks.renderViewerTags([]);

            expect(document.getElementById("previewContainer").textContent).toContain("No preview image provided.");
            expect(document.getElementById("mediaContainer").textContent).toContain("No primary media uploaded.");
            expect(document.getElementById("detailTags").textContent).toContain("No tags attached.");
        });

        test("renders documents and extra links as anchor media", () => {
            const { hooks } = setupModule6("/module6/viewer-detail.html?id=7");
            document.body.innerHTML = '<div id="mediaContainer"></div>';

            hooks.renderPrimaryMedia({
                title: "Viewer Document",
                mediaUrl: "/files/doc.pdf",
                resourceType: "document"
            });
            expect(document.getElementById("mediaContainer").innerHTML).toContain("Open attached document");

            hooks.renderPrimaryMedia({
                title: "Viewer Link",
                mediaUrl: "https://example.com/item",
                resourceType: "extra_link"
            });
            expect(document.getElementById("mediaContainer").innerHTML).toContain("Open external link");
        });
    });

    describe("comment flow", () => {
        test("renders comments with delete permission for the owner and escapes comment content", () => {
            const { hooks } = setupModule6("/module6/viewer-detail.html?id=7");
            hooks.__setViewerState({
                currentUser: { userId: 5, role: "REGISTERED_VIEWER" }
            });
            document.body.innerHTML = '<div id="commentList"></div>';

            hooks.renderViewerComments([
                {
                    id: 11,
                    userId: 5,
                    userName: "Alice <Viewer>",
                    content: "Loved <this> archive!",
                    createdAt: "2026-05-05T10:00:00Z"
                }
            ]);

            const commentList = document.getElementById("commentList").innerHTML;
            expect(commentList).toContain("Alice &lt;Viewer&gt;");
            expect(commentList).toContain("Loved &lt;this&gt; archive!");
            expect(commentList).toContain("data-comment-id=\"11\"");
        });

        test("renders the empty state and comment error in boundary and failure cases", () => {
            const { hooks } = setupModule6("/module6/viewer-detail.html?id=7");
            document.body.innerHTML = '<div id="commentList"></div>';

            hooks.renderViewerComments([]);
            expect(document.getElementById("commentList").textContent)
                .toContain("No comments yet. Be the first to share your thoughts.");

            hooks.renderViewerCommentError("<comment failure>");
            expect(document.getElementById("commentList").innerHTML)
                .toContain("&lt;comment failure&gt;");
        });

        test("submits a viewer comment successfully and refreshes the comment list", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/viewer-detail.html?id=7");
            document.body.innerHTML = `
                <textarea id="commentContent">  Great archive item.  </textarea>
                <button id="commentSubmitBtn" type="button">Post</button>
                <div id="commentList"></div>
            `;
            sharedApp.requestJson
                .mockResolvedValueOnce(null)
                .mockResolvedValueOnce([
                    {
                        id: 18,
                        userId: 5,
                        userName: "Viewer",
                        content: "Great archive item.",
                        createdAt: "2026-05-05T10:00:00Z"
                    }
                ]);

            await hooks.submitViewerComment();

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(1,
                "/api/viewer/resources/7/comments",
                {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ content: "Great archive item." })
                });
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(2,
                "/api/viewer/resources/7/comments",
                { method: "GET" }
            );
            expect(document.getElementById("commentContent").value).toBe("");
            expect(document.getElementById("commentSubmitBtn").disabled).toBe(false);
            expect(sharedApp.showToast).toHaveBeenCalledWith("Comment posted successfully.");
            expect(document.getElementById("commentList").innerHTML).toContain("Great archive item.");
        });

        test("shows an error and keeps the input when submitting a viewer comment fails", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/viewer-detail.html?id=7");
            document.body.innerHTML = `
                <textarea id="commentContent">Still there</textarea>
                <button id="commentSubmitBtn" type="button">Post</button>
                <div id="commentList"></div>
            `;
            sharedApp.requestJson.mockRejectedValue(new Error("Unable to post comment."));

            await hooks.submitViewerComment();

            expect(sharedApp.showToast).toHaveBeenCalledWith("Unable to post comment.");
            expect(document.getElementById("commentContent").value).toBe("Still there");
            expect(document.getElementById("commentSubmitBtn").disabled).toBe(false);
        });

        test("deletes a viewer comment successfully and reloads the remaining comments", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/viewer-detail.html?id=7");
            document.body.innerHTML = '<div id="commentList"></div>';
            sharedApp.requestJson
                .mockResolvedValueOnce(null)
                .mockResolvedValueOnce([]);

            await hooks.deleteViewerComment(13);

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(1,
                "/api/viewer/resources/7/comments/13",
                { method: "DELETE" }
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(2,
                "/api/viewer/resources/7/comments",
                { method: "GET" }
            );
            expect(sharedApp.showToast).toHaveBeenCalledWith("Comment deleted successfully.");
            expect(document.getElementById("commentList").textContent)
                .toContain("No comments yet. Be the first to share your thoughts.");
        });
    });

    describe("feedback flow", () => {
        test("loads viewer feedback history from the correct endpoint for regular and admin users", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/viewer-feedback.html");
            document.body.innerHTML = '<div id="feedbackList"></div>';

            hooks.__setViewerState({
                currentUser: { userId: 2, role: "REGISTERED_VIEWER" }
            });
            sharedApp.requestJson.mockResolvedValueOnce([]);
            await hooks.loadViewerFeedbackHistory();
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(1,
                "/api/viewer/feedback/mine",
                { method: "GET" }
            );

            hooks.__setViewerState({
                currentUser: { userId: 1, role: "ADMINISTRATOR" }
            });
            sharedApp.requestJson.mockResolvedValueOnce([]);
            await hooks.loadViewerFeedbackHistory();
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(2,
                "/api/viewer/feedback/all",
                { method: "GET" }
            );
        });

        test("renders feedback history with attachments and the empty state", () => {
            const { hooks } = setupModule6("/module6/viewer-feedback.html");
            document.body.innerHTML = '<div id="feedbackList"></div>';

            hooks.renderViewerFeedbackHistory([]);
            expect(document.getElementById("feedbackList").textContent)
                .toContain("You have not submitted any feedback yet.");

            hooks.renderViewerFeedbackHistory([
                {
                    feedbackType: "BUG",
                    uploadedAt: "2026-05-05T10:00:00Z",
                    description: "Broken <link>",
                    attachments: [
                        {
                            originalFilename: "report.pdf",
                            filePath: "/attachments/report.pdf"
                        }
                    ]
                }
            ]);

            const html = document.getElementById("feedbackList").innerHTML;
            expect(html).toContain("BUG");
            expect(html).toContain("Broken &lt;link&gt;");
            expect(html).toContain("/uploads/attachments/report.pdf");
            expect(html).toContain("report.pdf");
        });

        test("updates feedback file text for selected files and default empty state", () => {
            const { hooks } = setupModule6("/module6/viewer-feedback.html");
            document.body.innerHTML = `
                <input id="feedbackFiles" type="file" multiple>
                <div id="feedbackFilesText"></div>
            `;
            const input = document.getElementById("feedbackFiles");

            hooks.updateViewerFeedbackFileText();
            expect(document.getElementById("feedbackFilesText").textContent)
                .toContain("No files selected. JPG, PNG, PDF, or TXT. 10MB per file.");

            Object.defineProperty(input, "files", {
                configurable: true,
                value: [
                    new File(["a"], "issue.png", { type: "image/png" }),
                    new File(["b"], "note.txt", { type: "text/plain" })
                ]
            });

            hooks.updateViewerFeedbackFileText();
            expect(document.getElementById("feedbackFilesText").textContent).toBe("issue.png, note.txt");
        });

        test("renders escaped feedback errors", () => {
            const { hooks } = setupModule6("/module6/viewer-feedback.html");
            document.body.innerHTML = '<div id="feedbackList"></div>';

            hooks.renderViewerFeedbackError("<feedback failure>");

            expect(document.getElementById("feedbackList").innerHTML)
                .toContain("&lt;feedback failure&gt;");
        });
    });

    describe("page initialization and interaction bindings", () => {
        test("initializes heritage viewer page and binds search interactions", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/heritage-viewer.html");
            jest.spyOn(console, "error").mockImplementation(() => {});
            document.body.innerHTML = `
                <button id="backBtn" type="button"></button>
                <button id="accountBtn" type="button"></button>
                <button id="feedbackBtn" type="button"></button>
                <button id="searchBtn" type="button"></button>
                <button id="loadAllBtn" type="button"></button>
                <input id="keyword" value=" pottery ">
                <select id="type"><option value="photo" selected>Photo</option></select>
                <select id="categoryId"><option value="2" selected>Category</option></select>
                <select id="sortBy"><option value="updatedAtDesc" selected>Latest</option></select>
                <div id="resourceList"></div>
            `;
            sharedApp.requestJson
                .mockResolvedValueOnce({ userId: 8, role: "REGISTERED_VIEWER" })
                .mockResolvedValueOnce([{ id: 2, name: "education" }])
                .mockResolvedValueOnce(["photo"])
                .mockResolvedValueOnce([
                    {
                        id: 9,
                        title: "Pottery",
                        description: "desc",
                        categoryName: "education",
                        resourceType: "photo",
                        previewImage: "/p.png",
                        updatedAt: "2026-05-08T00:00:00Z"
                    }
                ])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            await hooks.initHeritageViewerPage();
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(4,
                "/api/viewer/resources?keyword=pottery&type=photo&categoryId=2&sortBy=updatedAtDesc",
                { method: "GET" }
            );
            expect(document.getElementById("resourceList").textContent).toContain("Pottery");

            document.getElementById("searchBtn")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();

            document.getElementById("loadAllBtn")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();
            expect(document.getElementById("keyword").value).toBe("");

            document.getElementById("keyword")
                .dispatchEvent(new KeyboardEvent("keydown", { key: "Enter", bubbles: true, cancelable: true }));
            await flushPromises();

            document.getElementById("resourceList").innerHTML =
                '<button type="button" data-resource-id="55">Detail</button>';
            document.getElementById("resourceList").querySelector("button")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();
        });

        test("handles heritage viewer initialization failure via shared error handling", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/heritage-viewer.html");
            document.body.innerHTML = `
                <button id="backBtn" type="button"></button>
                <button id="accountBtn" type="button"></button>
                <button id="feedbackBtn" type="button"></button>
                <div id="resourceList"></div>
            `;
            sharedApp.requestJson.mockRejectedValueOnce(new Error("viewer down"));

            await hooks.initHeritageViewerPage();

            expect(document.getElementById("resourceList").innerHTML).toContain("viewer-error");
            expect(sharedApp.showToast).toHaveBeenCalledWith("viewer down");
        });

        test("initializes viewer detail page while tolerating category option loading failure", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/viewer-detail.html?id=7");
            const warnSpy = jest.spyOn(console, "warn").mockImplementation(() => {});
            buildDetailDom();
            document.body.innerHTML += `
                <form id="commentForm"></form>
                <textarea id="commentContent"></textarea>
                <button id="commentSubmitBtn" type="button"></button>
                <form id="feedbackForm"></form>
                <select id="feedbackType"><option value="BUG" selected>BUG</option></select>
                <textarea id="feedbackDescription"></textarea>
                <input id="feedbackFiles" type="file">
                <button id="feedbackSubmitBtn" type="button"></button>
                <div id="feedbackFilesText"></div>
            `;
            sharedApp.requestJson
                .mockResolvedValueOnce({ userId: 7, role: "REGISTERED_VIEWER" })
                .mockRejectedValueOnce(new Error("category unavailable"))
                .mockResolvedValueOnce({
                    id: 7,
                    title: "Detail Title",
                    resourceType: "photo",
                    categoryName: "education",
                    reviewedAt: null,
                    mediaUrl: "/media.png"
                })
                .mockResolvedValueOnce([]);

            await hooks.initViewerDetailPage();

            expect(warnSpy).toHaveBeenCalled();
            expect(document.getElementById("detailTitle").textContent).toBe("Detail Title");
            expect(document.getElementById("commentList").textContent)
                .toContain("No comments yet. Be the first to share your thoughts.");
        });

        test("initializes viewer feedback page and submits feedback successfully", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/viewer-feedback.html");
            document.body.innerHTML = `
                <form id="feedbackForm"></form>
                <select id="feedbackType"><option value="BUG" selected>BUG</option></select>
                <textarea id="feedbackDescription">Needs a fix</textarea>
                <input id="feedbackFiles" type="file">
                <button id="feedbackSubmitBtn" type="button">Submit</button>
                <div id="feedbackFilesText"></div>
                <div id="feedbackList"></div>
            `;
            const feedbackInput = document.getElementById("feedbackFiles");
            Object.defineProperty(feedbackInput, "files", {
                configurable: true,
                value: [new File(["abc"], "bug.txt", { type: "text/plain" })]
            });
            sharedApp.requestJson
                .mockResolvedValueOnce({ userId: 8, role: "REGISTERED_VIEWER" })
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce(null)
                .mockResolvedValueOnce([]);

            await hooks.initViewerFeedbackPage();
            document.getElementById("feedbackForm").dispatchEvent(new Event("submit", {
                bubbles: true,
                cancelable: true
            }));
            await flushPromises();
            await flushPromises();

            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/viewer/feedback",
                expect.objectContaining({ method: "POST" })
            );
            expect(document.getElementById("feedbackSubmitBtn").disabled).toBe(false);
            expect(sharedApp.showToast).toHaveBeenCalledWith("Feedback submitted successfully.");
        });
    });

    describe("authentication and error handling", () => {
        test("returns the authenticated viewer in a normal case and uses the cached viewer at the boundary", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/heritage-viewer.html");
            sharedApp.requestJson.mockResolvedValueOnce({
                userId: 7,
                role: "REGISTERED_VIEWER"
            });

            await expect(hooks.ensureViewerAuthenticated()).resolves.toEqual({
                userId: 7,
                role: "REGISTERED_VIEWER"
            });
            expect(sharedApp.requestJson).toHaveBeenCalledTimes(1);

            sharedApp.requestJson.mockClear();
            await expect(hooks.ensureViewerAuthenticated()).resolves.toEqual({
                userId: 7,
                role: "REGISTERED_VIEWER"
            });
            expect(sharedApp.requestJson).not.toHaveBeenCalled();
        });

        test("renders viewer error containers and toast for a non-authentication failure", () => {
            const { hooks, sharedApp } = setupModule6("/module6/viewer-detail.html?id=7");
            buildDetailDom();

            hooks.handleViewerError(new Error("<load failure>"), "Fallback");

            expect(document.getElementById("detailError").classList.contains("hidden")).toBe(false);
            expect(document.getElementById("detailError").textContent).toBe("<load failure>");
            expect(document.getElementById("resourceList")).toBeNull();
            expect(document.getElementById("commentList").innerHTML).toContain("&lt;load failure&gt;");
            expect(document.getElementById("feedbackList").innerHTML).toContain("&lt;load failure&gt;");
            expect(sharedApp.showToast).toHaveBeenCalledWith("<load failure>");
        });

        test("handles 401 branches for comment/feedback actions and redirects", async () => {
            const { hooks, sharedApp } = setupModule6("/module6/viewer-detail.html?id=7");
            jest.spyOn(console, "error").mockImplementation(() => {});
            document.body.innerHTML = `
                <textarea id="commentContent">Needs auth</textarea>
                <button id="commentSubmitBtn" type="button">Post</button>
                <div id="commentList"></div>
                <form id="feedbackForm"></form>
                <select id="feedbackType"><option value="BUG" selected>BUG</option></select>
                <textarea id="feedbackDescription">desc</textarea>
                <input id="feedbackFiles" type="file">
                <button id="feedbackSubmitBtn" type="button">Submit</button>
                <div id="feedbackFilesText"></div>
                <div id="feedbackList"></div>
            `;

            sharedApp.requestJson.mockRejectedValueOnce({ status: 401 });
            await hooks.submitViewerComment();

            sharedApp.requestJson.mockRejectedValueOnce({ status: 401 });
            await hooks.deleteViewerComment(3);

            sharedApp.requestJson.mockRejectedValueOnce({ status: 401 });
            await hooks.submitViewerFeedback();

            hooks.handleViewerError({ status: 401 }, "fallback");
            expect(sharedApp.showToast).not.toHaveBeenCalledWith("fallback");
        });

        test("throws when detail/comment loaders run without resource id", async () => {
            const { hooks } = setupModule6("/module6/viewer-detail.html");
            await expect(hooks.loadApprovedResourceDetail()).rejects.toThrow("Resource id is required.");
            await expect(hooks.loadViewerComments()).rejects.toThrow("Resource id is required.");
        });

        test("renders video and audio media branches", () => {
            const { hooks } = setupModule6("/module6/viewer-detail.html?id=7");
            document.body.innerHTML = '<div id="mediaContainer"></div>';

            hooks.renderPrimaryMedia({
                title: "Video resource",
                mediaUrl: "/video.mp4",
                resourceType: "video"
            });
            expect(document.getElementById("mediaContainer").innerHTML).toContain("<video");

            hooks.renderPrimaryMedia({
                title: "Audio resource",
                mediaUrl: "/audio.mp3",
                resourceType: "audio"
            });
            expect(document.getElementById("mediaContainer").innerHTML).toContain("<audio");
        });
    });
});
