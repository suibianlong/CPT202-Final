const fs = require("fs");
const path = require("path");

const MODULE3_SCRIPT_PATH = path.resolve(__dirname, "../../module3/module3.js");

function createSharedAppMock() {
    const escapeHtml = jest.fn((value) => String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;"));

    return {
        bindLogoutButtons: jest.fn(),
        escapeHtml,
        formatDateTime: jest.fn((value, { emptyText = "-" } = {}) => value ? `FMT:${value}` : emptyText),
        requestJson: jest.fn(),
        setValue: jest.fn((id, value) => {
            const element = document.getElementById(id);
            if (element) {
                element.value = value ?? "";
            }
        }),
        showMessageFromQuery: jest.fn(),
        showToast: jest.fn()
    };
}

function loadModule3TestHooks() {
    const source = fs.readFileSync(MODULE3_SCRIPT_PATH, "utf8");
    const wrappedSource = `
        (() => {
            ${source}
            window.__module3TestHooks = {
                normalizeTagNames,
                normalizeCategoryOptions,
                normalizeCategoryOption,
                normalizeResourceTypeOptions,
                normalizeResourceTypeOption,
                normalizeResourceTypeValue,
                formatResourceType,
                formatStatus,
                parseNullableLong,
                renderSelectOptions,
                renderResourceTypeSelectOptions,
                ensureResourceTypeOption,
                updateMediaFileAccept,
                loadCategorySelectOptions,
                loadResourceTypeSelectOptions,
                populateWorkspaceSession,
                getResourceIdFromQuery
            };
        })();
    `;

    delete window.__module3TestHooks;
    window.eval(wrappedSource);
    return window.__module3TestHooks;
}

function setupModule3(url = "/") {
    document.body.innerHTML = "";
    document.body.removeAttribute("data-page");
    window.history.replaceState({}, "", url);

    const sharedApp = createSharedAppMock();
    window.SharedApp = sharedApp;

    const hooks = loadModule3TestHooks();
    return { hooks, sharedApp };
}

describe("module3.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.useRealTimers();
    });

    describe("normalization helpers", () => {
        test("normalizes tag names in a normal case and removes case-insensitive duplicates", () => {
            const { hooks } = setupModule3("/resource-edit.html");

            expect(hooks.normalizeTagNames([
                "  Silk Road  ",
                "heritage, Folk Art, heritage",
                null,
                "folk art"
            ])).toEqual(["Silk Road", "heritage", "Folk Art"]);
        });

        test("returns an empty array for blank or nullish tag inputs at the boundary", () => {
            const { hooks } = setupModule3("/resource-edit.html");

            expect(hooks.normalizeTagNames(null)).toEqual([]);
            expect(hooks.normalizeTagNames(["", "   ", undefined])).toEqual([]);
        });

        test("normalizes mixed category options in a normal case", () => {
            const { hooks } = setupModule3("/resource-edit.html");

            expect(hooks.normalizeCategoryOptions([
                "Festivals",
                { categoryId: 8, categoryTopic: "Architecture" },
                { id: 9, name: "Cuisine" }
            ])).toEqual([
                { id: 1, name: "Festivals" },
                { id: 8, name: "Architecture" },
                { id: 9, name: "Cuisine" }
            ]);
        });

        test("filters invalid category options in abnormal and boundary cases", () => {
            const { hooks } = setupModule3("/resource-edit.html");

            expect(hooks.normalizeCategoryOption(null, 0)).toBeNull();
            expect(hooks.normalizeCategoryOption("   ", 0)).toBeNull();
            expect(hooks.normalizeCategoryOption({ name: "", id: 3 }, 0)).toBeNull();
            expect(hooks.normalizeCategoryOption({ categoryTopic: "Topic", categoryId: "" }, 0)).toBeNull();
            expect(hooks.normalizeCategoryOptions("not-an-array")).toEqual([]);
        });

        test("normalizes and formats resource types in a normal case", () => {
            const { hooks } = setupModule3("/resource-edit.html");

            expect(hooks.normalizeResourceTypeValue(" photo/image ")).toBe("photo");
            expect(hooks.normalizeResourceTypeValue("Picture")).toBe("photo");
            expect(hooks.normalizeResourceTypeValue("file/document")).toBe("document");
            expect(hooks.formatResourceType("photo_image")).toBe("Photo/Image");
            expect(hooks.formatResourceType("document")).toBe("File/Document");
        });

        test("keeps unknown resource types and rejects invalid resource type inputs at the boundary", () => {
            const { hooks } = setupModule3("/resource-edit.html");

            expect(hooks.normalizeResourceTypeValue("3d model")).toBe("3d model");
            expect(hooks.formatResourceType("3d model")).toBe("3d model");
            expect(hooks.formatResourceType("")).toBe("-");
            expect(hooks.normalizeResourceTypeOption({})).toBeNull();
            expect(hooks.normalizeResourceTypeOptions(null)).toEqual([]);
        });
    });

    describe("select rendering helpers", () => {
        test("renders category options and preserves the selected value in a normal case", () => {
            const { hooks } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <select id="categoryId">
                    <option value="8" selected>Old Topic</option>
                </select>
            `;
            const select = document.getElementById("categoryId");

            hooks.renderSelectOptions(select, [
                { id: 5, name: "Ceremony" },
                { id: 8, name: "Intangible <Heritage>" }
            ], "Select topic");

            expect(select.options).toHaveLength(3);
            expect(select.options[0].textContent).toBe("Select topic");
            expect(select.options[2].value).toBe("8");
            expect(select.innerHTML).toContain("Intangible &lt;Heritage&gt;");
            expect(select.value).toBe("8");
        });

        test("keeps a custom current resource type by appending a fallback option at the boundary", () => {
            const { hooks } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <select id="resourceType">
                    <option value="archive" selected>Archive</option>
                </select>
            `;
            const select = document.getElementById("resourceType");

            hooks.renderResourceTypeSelectOptions(select, [
                { name: "Photo" },
                { typeName: "Video" }
            ], "Select type");

            expect(Array.from(select.options).map(option => option.value))
                .toEqual(["", "photo", "video", "archive"]);
            expect(select.value).toBe("archive");
            expect(select.options[3].textContent).toBe("archive");
        });
    });

    describe("editor option loading flows", () => {
        test("loads category select options in a normal case", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <select id="categoryId"></select>
                <button id="metadataSaveBtn"></button>
                <button id="submitReviewBtn"></button>
                <div id="editorAlert" class="hidden"></div>
            `;
            sharedApp.requestJson.mockResolvedValue([
                { id: 5, name: "Ceremony" }
            ]);

            await hooks.loadCategorySelectOptions();

            const select = document.getElementById("categoryId");
            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/contributor/resources/category-options",
                { method: "GET" }
            );
            expect(select.disabled).toBe(false);
            expect(select.options).toHaveLength(2);
            expect(select.options[0].textContent).toBe("Select topic");
            expect(select.options[1].value).toBe("5");
            expect(document.getElementById("metadataSaveBtn").disabled).toBe(false);
            expect(document.getElementById("submitReviewBtn").disabled).toBe(false);
            expect(document.getElementById("editorAlert").classList.contains("hidden")).toBe(true);
            expect(sharedApp.showToast).not.toHaveBeenCalled();
        });

        test("handles an empty category response as an abnormal case", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <select id="categoryId"></select>
                <button id="metadataSaveBtn"></button>
                <button id="submitReviewBtn"></button>
                <div id="editorAlert" class="hidden"></div>
            `;
            sharedApp.requestJson.mockResolvedValue([]);

            await hooks.loadCategorySelectOptions();

            const select = document.getElementById("categoryId");
            expect(select.disabled).toBe(true);
            expect(select.value).toBe("");
            expect(select.options).toHaveLength(1);
            expect(select.options[0].textContent).toBe("Categories unavailable");
            expect(document.getElementById("metadataSaveBtn").disabled).toBe(true);
            expect(document.getElementById("submitReviewBtn").disabled).toBe(true);
            expect(document.getElementById("editorAlert").textContent)
                .toBe("Categories failed to load. Metadata save and submit are temporarily disabled.");
            expect(document.getElementById("editorAlert").classList.contains("hidden")).toBe(false);
            expect(sharedApp.showToast).toHaveBeenCalledWith("No active categories are available.");
        });

        test("loads resource type options in a normal case", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <select id="resourceType"></select>
                <select id="resourceTypeMirror"></select>
                <button id="metadataSaveBtn"></button>
                <button id="submitReviewBtn"></button>
                <div id="editorAlert" class="hidden"></div>
            `;
            sharedApp.requestJson.mockResolvedValue([
                { name: "Photo" },
                { typeName: "Video" }
            ]);

            await hooks.loadResourceTypeSelectOptions();

            const mirror = document.getElementById("resourceTypeMirror");
            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/contributor/resources/resource-type-options",
                { method: "GET" }
            );
            expect(mirror.disabled).toBe(false);
            expect(Array.from(mirror.options).map(option => option.value))
                .toEqual(["", "photo", "video"]);
            expect(Array.from(mirror.options).map(option => option.textContent))
                .toEqual(["Select type", "Photo/Image", "Video"]);
            expect(sharedApp.showToast).not.toHaveBeenCalled();
        });

        test("disables editor actions when no resource types are available", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <select id="resourceType">
                    <option value="video" selected>Video</option>
                </select>
                <select id="resourceTypeMirror">
                    <option value="video" selected>Video</option>
                </select>
                <button id="metadataSaveBtn"></button>
                <button id="submitReviewBtn"></button>
                <div id="editorAlert" class="hidden"></div>
            `;
            sharedApp.requestJson.mockResolvedValue([]);

            await hooks.loadResourceTypeSelectOptions();

            const primarySelect = document.getElementById("resourceType");
            const mirrorSelect = document.getElementById("resourceTypeMirror");
            expect(primarySelect.value).toBe("");
            expect(mirrorSelect.value).toBe("");
            expect(mirrorSelect.disabled).toBe(true);
            expect(document.getElementById("metadataSaveBtn").disabled).toBe(true);
            expect(document.getElementById("submitReviewBtn").disabled).toBe(true);
            expect(document.getElementById("editorAlert").textContent)
                .toBe("Resource types failed to load. Metadata save and submit are temporarily disabled.");
            expect(sharedApp.showToast).toHaveBeenCalledWith("No active resource types are available.");
        });
    });

    describe("miscellaneous helpers", () => {
        test("updates the media input accept attribute for normal and boundary resource types", () => {
            const { hooks } = setupModule3("/resource-edit.html");
            document.body.innerHTML = '<input id="mediaFile">';
            const mediaInput = document.getElementById("mediaFile");

            hooks.updateMediaFileAccept("Picture");
            expect(mediaInput.accept).toBe("image/*");

            hooks.updateMediaFileAccept("video");
            expect(mediaInput.accept).toBe("video/*");

            hooks.updateMediaFileAccept("document");
            expect(mediaInput.accept).toBe(".pdf,.doc,.docx");

            hooks.updateMediaFileAccept("unknown-type");
            expect(mediaInput.accept).toBe("");
        });

        test("populates the workspace session and falls back correctly at the boundary", () => {
            const { hooks } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <span id="resourceUserName"></span>
                <span id="resourceAccessText"></span>
            `;

            hooks.populateWorkspaceSession({
                name: "Ava Contributor",
                contributor: true
            });
            expect(document.getElementById("resourceUserName").textContent).toBe("Ava Contributor");
            expect(document.getElementById("resourceAccessText").textContent)
                .toBe("Approved contributor access is active for this workspace.");

            hooks.populateWorkspaceSession({
                name: "",
                contributor: false
            });
            expect(document.getElementById("resourceUserName").textContent).toBe("Contributor");
            expect(document.getElementById("resourceAccessText").textContent)
                .toBe("Contributor permission is required for this workspace.");
        });

        test("reads the resource id from the query string and parses nullable numbers", () => {
            const { hooks } = setupModule3("/resource-edit.html?id=42");

            expect(hooks.getResourceIdFromQuery()).toBe("42");
            expect(hooks.parseNullableLong("42")).toBe(42);
            expect(hooks.parseNullableLong("0")).toBe(0);
            expect(hooks.parseNullableLong("")).toBeNull();
            expect(hooks.parseNullableLong(null)).toBeNull();
        });

        test("formats resource status text in normal and boundary cases", () => {
            const { hooks } = setupModule3("/resource-edit.html");

            expect(hooks.formatStatus("PENDING_REVIEW")).toBe("Pending Review");
            expect(hooks.formatStatus("approved")).toBe("Approved");
            expect(hooks.formatStatus(null)).toBe("-");
        });
    });
});
