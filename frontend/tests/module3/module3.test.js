const fs = require("fs");
const path = require("path");
const { evalWithCoverage } = require("../test-utils/eval-with-coverage");

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
                initMyResourcesPage,
                initResourceEditPage,
                normalizeTagNames,
                normalizeMediaUrls,
                getMediaFileDisplayName,
                normalizeCategoryOptions,
                normalizeCategoryOption,
                normalizeResourceTypeOptions,
                normalizeResourceTypeOption,
                normalizeResourceTypeValue,
                formatResourceType,
                formatStatus,
                normalizeStatusForCheck,
                isEditableResourceStatus,
                parseNullableLong,
                toPublicMediaUrl,
                renderSelectOptions,
                renderResourceTypeSelectOptions,
                ensureResourceTypeOption,
                persistResourceTypeSelection,
                renderTagChips,
                commitTagInputValue,
                renderResourceTable,
                renderMyResourceEditAction,
                renderVersionSnapshot,
                renderVersionCompare,
                renderHistoryInspectorEmpty,
                updateMediaFileAccept,
                bindCreateDraftButton,
                bindListFilterButtons,
                bindHistoryModal,
                bindHeritageLanding,
                bindModuleModalUI,
                bindSingleFilePicker,
                clearSelectedFile,
                formatSelectedFileText,
                bindMediaFileList,
                bindMetadataForm,
                bindFilePickerUI,
                bindSubmitForm,
                scheduleMetadataAutoSave,
                saveMetadata,
                uploadSelectedFiles,
                loadCategoryFilterOptions,
                loadCategorySelectOptions,
                loadResourceTypeSelectOptions,
                loadResourceList,
                loadResourceDetail,
                openHistoryModal,
                closeHistoryModal,
                loadHistoryModalData,
                viewVersionSnapshot,
                compareVersionWithCurrent,
                rollbackToVersion,
                createDraftForEditing,
                updateEditorUrlWithResourceId,
                activateHeritageModule,
                openHeritageModuleModal,
                closeHeritageModuleModal,
                focusModuleField,
                ensureContributorWorkspaceAccess,
                populateWorkspaceSession,
                getResourceIdFromQuery
            };
        })();
    `;

    delete window.__module3TestHooks;
    evalWithCoverage(wrappedSource, MODULE3_SCRIPT_PATH);
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

async function flushPromises(times = 6) {
    for (let i = 0; i < times; i += 1) {
        await Promise.resolve();
    }
}

describe("module3.js", () => {
    afterEach(() => {
        jest.clearAllMocks();
        jest.clearAllTimers();
        jest.restoreAllMocks();
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

    describe("resource editor save flow", () => {
        test("does not auto create a draft when autosave runs before the first explicit save", async () => {
            jest.useFakeTimers();
            const { hooks, sharedApp } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <form id="metadataForm">
                    <input id="title" value="Temple archive">
                    <input id="copyright" value="Museum rights">
                    <select id="categoryId"><option value="7" selected>Topic</option></select>
                    <input id="place" value="Suzhou">
                    <textarea id="description">Historical description</textarea>
                    <select id="resourceType"><option value="photo" selected>Photo</option></select>
                    <select id="resourceTypeMirror"><option value="photo" selected>Photo</option></select>
                    <input id="tagInput">
                    <div id="tagOptions"></div>
                </form>
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;

            hooks.bindMetadataForm();
            document.getElementById("title")
                .dispatchEvent(new Event("input", { bubbles: true }));

            jest.advanceTimersByTime(900);
            await flushPromises();
            await flushPromises();

            expect(sharedApp.requestJson).not.toHaveBeenCalled();
            expect(sharedApp.showToast).not.toHaveBeenCalled();
            expect(window.location.search).toBe("");
        });

        test("creates a draft on the first explicit metadata save and updates the url", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <form id="metadataForm">
                    <input id="title" value="Temple archive">
                    <input id="copyright" value="Museum rights">
                    <select id="categoryId"><option value="7" selected>Topic</option></select>
                    <input id="place" value="Suzhou">
                    <textarea id="description">Historical description</textarea>
                    <select id="resourceType"><option value="photo" selected>Photo</option></select>
                    <select id="resourceTypeMirror"><option value="photo" selected>Photo</option></select>
                    <input id="tagInput">
                    <div id="tagOptions"></div>
                </form>
                <input id="mediaFile" type="file">
                <input id="previewImage" type="file">
                <span id="mediaFileNameText"></span>
                <span id="previewImageNameText"></span>
                <span id="mediaFilePathText"></span>
                <span id="previewImagePathText"></span>
                <div id="previewImageFrame"></div>
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;
            sharedApp.requestJson
                .mockResolvedValueOnce({
                    id: 108,
                    status: "Draft"
                })
                .mockResolvedValueOnce({
                    id: 108,
                    title: "Temple archive",
                    copyright: "Museum rights",
                    categoryId: 7,
                    place: "Suzhou",
                    description: "Historical description",
                    resourceType: "photo",
                    updatedAt: "2026-05-06T10:00:00",
                    status: "Draft"
                });

            hooks.bindMetadataForm();
            document.getElementById("metadataForm")
                .dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
            await flushPromises();
            await flushPromises();

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                1,
                "/api/contributor/resources",
                expect.objectContaining({ method: "POST" })
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                2,
                "/api/contributor/resources/108",
                expect.objectContaining({ method: "PUT" })
            );
            expect(window.location.search).toBe("?id=108");
            expect(document.getElementById("resourceIdText").textContent).toBe("108");
            expect(document.getElementById("updatedAtText").textContent).toBe("FMT:2026-05-06T10:00:00");
            expect(sharedApp.showToast).toHaveBeenLastCalledWith("Metadata saved successfully.");
        });

        test("auto saves metadata changes after a short debounce", async () => {
            jest.useFakeTimers();
            const { hooks, sharedApp } = setupModule3("/resource-edit.html?id=42");
            document.body.innerHTML = `
                <form id="metadataForm">
                    <input id="title" value="Temple archive">
                    <input id="copyright" value="Museum rights">
                    <select id="categoryId"><option value="7" selected>Topic</option></select>
                    <input id="place" value="Suzhou">
                    <textarea id="description">Historical description</textarea>
                    <select id="resourceType"><option value="photo" selected>Photo</option></select>
                    <select id="resourceTypeMirror"><option value="photo" selected>Photo</option></select>
                    <input id="tagInput">
                    <div id="tagOptions"></div>
                </form>
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;
            sharedApp.requestJson.mockResolvedValue({
                id: 42,
                title: "Temple archive",
                copyright: "Museum rights",
                categoryId: 7,
                place: "Suzhou",
                description: "Historical description",
                resourceType: "photo",
                status: "Draft"
            });

            hooks.bindMetadataForm();
            document.getElementById("title")
                .dispatchEvent(new Event("input", { bubbles: true }));
            jest.advanceTimersByTime(899);
            await flushPromises();
            expect(sharedApp.requestJson).not.toHaveBeenCalled();

            jest.advanceTimersByTime(1);
            await flushPromises();
            await flushPromises();

            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/contributor/resources/42",
                expect.objectContaining({ method: "PUT" })
            );
            expect(sharedApp.showToast).not.toHaveBeenCalledWith("Metadata saved successfully.");
        });

        test("saves selected files through the metadata save action", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html?id=42");
            document.body.innerHTML = `
                <form id="metadataForm">
                    <input id="title" value="Temple archive">
                    <input id="copyright" value="Museum rights">
                    <select id="categoryId"><option value="7" selected>Topic</option></select>
                    <input id="place" value="Suzhou">
                    <textarea id="description">Historical description</textarea>
                    <select id="resourceType"><option value="photo" selected>Photo</option></select>
                    <select id="resourceTypeMirror"><option value="photo" selected>Photo</option></select>
                    <input id="tagInput">
                    <div id="tagOptions"></div>
                </form>
                <input id="mediaFile" type="file">
                <input id="previewImage" type="file">
                <span id="mediaFileNameText"></span>
                <span id="previewImageNameText"></span>
                <span id="mediaFilePathText"></span>
                <span id="previewImagePathText"></span>
                <div id="previewImageFrame"></div>
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;
            const mediaFile = new File(["image"], "temple.jpg", { type: "image/jpeg" });
            Object.defineProperty(document.getElementById("mediaFile"), "files", {
                value: [mediaFile],
                configurable: true
            });
            sharedApp.requestJson
                .mockResolvedValueOnce({
                    id: 42,
                    title: "Temple archive",
                    copyright: "Museum rights",
                    categoryId: 7,
                    place: "Suzhou",
                    description: "Historical description",
                    resourceType: "photo",
                    status: "Draft"
                })
                .mockResolvedValueOnce({
                    id: 42,
                    title: "Temple archive",
                    copyright: "Museum rights",
                    categoryId: 7,
                    place: "Suzhou",
                    description: "Historical description",
                    resourceType: "photo",
                    mediaUrl: "resource-42/temple.jpg",
                    status: "Draft"
                });

            hooks.bindMetadataForm();
            document.getElementById("metadataForm")
                .dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
            await flushPromises();
            await flushPromises();

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                1,
                "/api/contributor/resources/42",
                expect.objectContaining({ method: "PUT" })
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                2,
                "/api/contributor/resources/42/files",
                expect.objectContaining({ method: "POST" })
            );
            expect(sharedApp.showToast).toHaveBeenLastCalledWith("Metadata saved successfully.");
        });

        test("auto uploads selected media through the metadata save flow", async () => {
            jest.useFakeTimers();
            const { hooks, sharedApp } = setupModule3("/resource-edit.html?id=42");
            document.body.innerHTML = `
                <form id="metadataForm">
                    <input id="title" value="Temple archive">
                    <input id="copyright" value="Museum rights">
                    <select id="categoryId"><option value="7" selected>Topic</option></select>
                    <input id="place" value="Suzhou">
                    <textarea id="description">Historical description</textarea>
                    <select id="resourceType"><option value="photo" selected>Photo</option></select>
                    <select id="resourceTypeMirror"><option value="photo" selected>Photo</option></select>
                    <input id="tagInput">
                    <div id="tagOptions"></div>
                </form>
                <button data-file-target="mediaFile"></button>
                <button data-file-target="previewImage"></button>
                <input id="mediaFile" type="file">
                <input id="previewImage" type="file">
                <span id="mediaFileNameText"></span>
                <span id="previewImageNameText"></span>
                <span id="mediaFilePathText"></span>
                <span id="previewImagePathText"></span>
                <div id="previewImageFrame"></div>
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;
            const mediaFile = new File(["image"], "temple.jpg", { type: "image/jpeg" });
            Object.defineProperty(document.getElementById("mediaFile"), "files", {
                value: [mediaFile],
                configurable: true
            });
            sharedApp.requestJson
                .mockResolvedValueOnce({
                    id: 42,
                    title: "Temple archive",
                    copyright: "Museum rights",
                    categoryId: 7,
                    place: "Suzhou",
                    description: "Historical description",
                    resourceType: "photo",
                    status: "Draft"
                })
                .mockResolvedValueOnce({
                    id: 42,
                    title: "Temple archive",
                    copyright: "Museum rights",
                    categoryId: 7,
                    place: "Suzhou",
                    description: "Historical description",
                    resourceType: "photo",
                    mediaUrl: "resource-42/temple.jpg",
                    status: "Draft"
                });

            hooks.bindFilePickerUI();
            document.getElementById("mediaFile")
                .dispatchEvent(new Event("change", { bubbles: true }));
            jest.advanceTimersByTime(0);
            await flushPromises();
            await flushPromises();

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                1,
                "/api/contributor/resources/42",
                expect.objectContaining({ method: "PUT" })
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                2,
                "/api/contributor/resources/42/files",
                expect.objectContaining({ method: "POST" })
            );
            expect(document.getElementById("mediaFileNameText").textContent).toBe("Drag & drop file here or");
        });

        test("flushes autosave before submitting for review", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html?id=42");
            document.body.innerHTML = `
                <form id="submitForm">
                    <textarea id="submissionNote">Ready</textarea>
                </form>
                <input id="title" value="Temple archive">
                <input id="copyright" value="Museum rights">
                <select id="categoryId"><option value="7" selected>Topic</option></select>
                <input id="place" value="Suzhou">
                <textarea id="description">Historical description</textarea>
                <select id="resourceType"><option value="photo" selected>Photo</option></select>
                <select id="resourceTypeMirror"><option value="photo" selected>Photo</option></select>
                <input id="tagInput">
                <div id="tagOptions"></div>
                <input id="mediaFile" type="file">
                <input id="previewImage" type="file">
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;
            sharedApp.requestJson
                .mockResolvedValueOnce({
                    id: 42,
                    title: "Temple archive",
                    copyright: "Museum rights",
                    categoryId: 7,
                    place: "Suzhou",
                    description: "Historical description",
                    resourceType: "photo",
                    status: "Draft"
                })
                .mockResolvedValueOnce(null)
                .mockResolvedValueOnce({
                    id: 42,
                    title: "Temple archive",
                    status: "Pending Review"
                });

            hooks.bindSubmitForm();
            document.getElementById("submitForm")
                .dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
            await flushPromises();
            await flushPromises();

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                1,
                "/api/contributor/resources/42",
                expect.objectContaining({ method: "PUT" })
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                2,
                "/api/contributor/resources/42/submit",
                expect.objectContaining({ method: "POST" })
            );
        });

        test("creates then submits when review is requested before the first save", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html");
            jest.spyOn(window, "confirm").mockReturnValue(true);
            document.body.innerHTML = `
                <form id="submitForm">
                    <textarea id="submissionNote">Ready</textarea>
                </form>
                <input id="title" value="Temple archive">
                <input id="copyright" value="Museum rights">
                <select id="categoryId"><option value="7" selected>Topic</option></select>
                <input id="place" value="Suzhou">
                <textarea id="description">Historical description</textarea>
                <select id="resourceType"><option value="photo" selected>Photo</option></select>
                <select id="resourceTypeMirror"><option value="photo" selected>Photo</option></select>
                <input id="tagInput">
                <div id="tagOptions"></div>
                <input id="mediaFile" type="file">
                <input id="previewImage" type="file">
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;
            sharedApp.requestJson
                .mockResolvedValueOnce({
                    id: 88,
                    status: "Draft"
                })
                .mockResolvedValueOnce({
                    id: 88,
                    title: "Temple archive",
                    copyright: "Museum rights",
                    categoryId: 7,
                    place: "Suzhou",
                    description: "Historical description",
                    resourceType: "photo",
                    status: "Draft"
                })
                .mockResolvedValueOnce(null)
                .mockResolvedValueOnce({
                    id: 88,
                    title: "Temple archive",
                    status: "Pending Review"
                });

            hooks.bindSubmitForm();
            document.getElementById("submitForm")
                .dispatchEvent(new Event("submit", { bubbles: true, cancelable: true }));
            await flushPromises();
            await flushPromises();

            expect(window.confirm).toHaveBeenCalled();
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                1,
                "/api/contributor/resources",
                expect.objectContaining({ method: "POST" })
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                2,
                "/api/contributor/resources/88",
                expect.objectContaining({ method: "PUT" })
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                3,
                "/api/contributor/resources/88/submit",
                expect.objectContaining({ method: "POST" })
            );
            expect(window.location.search).toBe("?id=88");
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

        test("normalizes media URLs and resolves public media URLs", () => {
            const { hooks } = setupModule3("/resource-edit.html");

            expect(hooks.normalizeMediaUrls(["a.jpg", "a.jpg", "", null, "b.mp4"]))
                .toEqual(["a.jpg", "b.mp4"]);
            expect(hooks.normalizeMediaUrls("not-array")).toEqual([]);
            expect(hooks.getMediaFileDisplayName("folder/asset.png")).toBe("asset.png");
            expect(hooks.getMediaFileDisplayName("")).toBe("Media file");
            expect(hooks.toPublicMediaUrl("abc.jpg")).toBe("/uploads/abc.jpg");
            expect(hooks.toPublicMediaUrl("/uploads/abc.jpg")).toBe("/uploads/abc.jpg");
            expect(hooks.toPublicMediaUrl("https://cdn.example/a.jpg")).toBe("https://cdn.example/a.jpg");
            expect(hooks.toPublicMediaUrl("   ")).toBe("");
        });

        test("normalizes status for checks and editable decision", () => {
            const { hooks } = setupModule3("/resource-edit.html");

            expect(hooks.normalizeStatusForCheck("PENDING_REVIEW")).toBe("pending review");
            expect(hooks.normalizeStatusForCheck(" archived-status ")).toBe("archived status");
            expect(hooks.isEditableResourceStatus("Draft")).toBe(true);
            expect(hooks.isEditableResourceStatus("Rejected")).toBe(true);
            expect(hooks.isEditableResourceStatus("Approved")).toBe(false);
        });
    });

    describe("my resources list and history flows", () => {
        test("initializes my-resources page and loads workspace session in a normal case", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <button id="createDraftBtn" type="button"></button>
                <button id="searchBtn" type="button"></button>
                <button id="resetBtn" type="button"></button>
                <input id="keyword" value="">
                <select id="statusFilter"><option value=""></option></select>
                <select id="categoryFilter"></select>
                <table><tbody id="resourceTableBody"></tbody></table>
                <div id="historyModal" class="hidden"></div>
                <div id="listMeta"></div>
                <span id="resourceUserName"></span>
                <span id="resourceAccessText"></span>
            `;
            sharedApp.requestJson
                .mockResolvedValueOnce([{ id: 2, name: "Oral History" }])
                .mockResolvedValueOnce({ name: "Ava", contributor: true })
                .mockResolvedValueOnce([]);

            await hooks.initMyResourcesPage();

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                1,
                "/api/contributor/resources/category-options",
                { method: "GET" }
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                2,
                "/api/auth/me",
                { method: "GET" }
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                3,
                "/api/contributor/resources/my",
                { method: "GET" }
            );
            expect(document.getElementById("resourceUserName").textContent).toBe("Ava");
            expect(document.getElementById("resourceTableBody").textContent).toContain("No resources found.");
        });

        test("shows a network toast when my-resources access check fails", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <button id="createDraftBtn" type="button"></button>
                <input id="keyword" value="">
                <select id="statusFilter"><option value=""></option></select>
                <select id="categoryFilter"></select>
                <table><tbody id="resourceTableBody"></tbody></table>
                <div id="historyModal" class="hidden"></div>
                <span id="resourceUserName"></span>
                <span id="resourceAccessText"></span>
            `;
            const networkError = new Error("network");
            networkError.isNetworkError = true;
            sharedApp.requestJson
                .mockResolvedValueOnce([{ id: 2, name: "Oral History" }])
                .mockRejectedValueOnce(networkError);

            await hooks.initMyResourcesPage();

            expect(sharedApp.showToast).toHaveBeenCalledWith("Unable to verify contributor access right now.");
            expect(sharedApp.requestJson).toHaveBeenCalledTimes(2);
        });

        test("loads category filter options and list data in normal case", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <select id="categoryFilter"></select>
                <input id="keyword" value="  silk  ">
                <select id="statusFilter"><option value="Draft" selected>Draft</option></select>
                <table><tbody id="resourceTableBody"></tbody></table>
                <div id="listMeta"></div>
            `;

            sharedApp.requestJson
                .mockResolvedValueOnce([{ id: 3, name: "Architecture" }])
                .mockResolvedValueOnce([
                    {
                        id: 11,
                        title: "Old Bridge",
                        categoryName: "Architecture",
                        updatedAt: "2026-05-07T00:00:00",
                        currentVersionNo: 4,
                        lastSubmittedAt: "2026-05-08T00:00:00",
                        hasReviewFeedback: true,
                        resourceType: "photo",
                        status: "Draft"
                    }
                ]);

            await hooks.loadCategoryFilterOptions();
            await hooks.loadResourceList();

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                1,
                "/api/contributor/resources/category-options",
                { method: "GET" }
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                2,
                "/api/contributor/resources/my?keyword=silk&status=Draft",
                { method: "GET" }
            );
            expect(document.getElementById("categoryFilter").disabled).toBe(false);
            expect(document.getElementById("resourceTableBody").innerHTML).toContain("Old Bridge");
            expect(document.getElementById("resourceTableBody").innerHTML).toContain("Feedback available");
            expect(document.getElementById("listMeta").textContent).toBe("1 item(s)");
        });

        test("handles list loading failure and empty resource table", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <input id="keyword" value="">
                <select id="statusFilter"><option value=""></option></select>
                <select id="categoryFilter"><option value=""></option></select>
                <table><tbody id="resourceTableBody"></tbody></table>
                <div id="listMeta"></div>
            `;

            sharedApp.requestJson.mockRejectedValue(new Error("Server offline"));
            await hooks.loadResourceList();

            expect(document.getElementById("resourceTableBody").textContent).toContain("Server offline");
            expect(document.getElementById("listMeta").textContent).toBe("Load failed");
            expect(sharedApp.showToast).toHaveBeenCalledWith("Server offline");

            hooks.renderResourceTable([]);
            expect(document.getElementById("resourceTableBody").textContent).toContain("No resources found.");
        });

        test("renders edit action for editable and locked statuses", () => {
            const { hooks } = setupModule3("/my-resources.html");

            expect(hooks.renderMyResourceEditAction({ id: 1, status: "Draft" }))
                .toContain("./resource-edit.html?id=1");
            expect(hooks.renderMyResourceEditAction({ id: 1, status: "Archived" }))
                .toContain("Archived");
            expect(hooks.renderMyResourceEditAction({ id: 1, status: "Pending Review" }))
                .toContain("Locked");
        });

        test("opens history modal and renders loaded histories", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <div id="historyModal" class="hidden" aria-hidden="true">
                    <div class="heritage-history-modal-dialog"></div>
                    <button data-close-history-modal type="button"></button>
                    <div id="historyResourceTitle"></div>
                    <div id="historyResourceStatus"></div>
                    <div id="historyResourceVersion"></div>
                    <div id="historySubmittedAt"></div>
                    <div id="historyFeedbackText"></div>
                    <div id="historyInspectorMeta"></div>
                    <div id="historyInspectorBody"></div>
                    <table><tbody id="submissionHistoryBody"></tbody></table>
                    <table><tbody id="versionHistoryBody"></tbody></table>
                </div>
                <table><tbody id="resourceTableBody"></tbody></table>
            `;

            sharedApp.requestJson
                .mockResolvedValueOnce({
                    title: "Ancient Bell",
                    status: "Draft",
                    currentVersionNo: 3,
                    latestSubmittedAt: "2026-05-08T00:00:00",
                    latestReviewStatus: "Rejected",
                    latestFeedbackComment: "Need clearer license"
                })
                .mockResolvedValueOnce([
                    { versionNo: 2, submittedAt: "2026-05-01T00:00:00", submissionNote: "v2", statusSnapshot: "Draft" }
                ])
                .mockResolvedValueOnce([
                    { versionNo: 2, changeType: "Update", changeSummary: "Metadata updated", createdAt: "2026-05-01T00:00:00" }
                ]);

            await hooks.openHistoryModal("66");

            expect(document.getElementById("historyModal").classList.contains("hidden")).toBe(false);
            expect(document.body.classList.contains("history-modal-open")).toBe(true);
            expect(document.getElementById("historyResourceTitle").textContent).toBe("Ancient Bell");
            expect(document.getElementById("submissionHistoryBody").textContent).toContain("v2");
            expect(document.getElementById("versionHistoryBody").textContent).toContain("Update");
            expect(document.getElementById("versionHistoryBody").innerHTML).toContain("data-version-action=\"restore\"");
        });

        test("renders history load failure when detail request fails", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <div id="historyModal" class="hidden" aria-hidden="true">
                    <div id="historyResourceTitle"></div>
                    <div id="historyResourceStatus"></div>
                    <div id="historyResourceVersion"></div>
                    <div id="historySubmittedAt"></div>
                    <div id="historyFeedbackText"></div>
                    <div id="historyInspectorMeta"></div>
                    <div id="historyInspectorBody"></div>
                    <table><tbody id="submissionHistoryBody"></tbody></table>
                    <table><tbody id="versionHistoryBody"></tbody></table>
                </div>
            `;

            sharedApp.requestJson
                .mockRejectedValueOnce(new Error("history down"))
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            await hooks.openHistoryModal("88");

            expect(document.getElementById("historyResourceTitle").textContent).toBe("Load failed");
            expect(document.getElementById("historyInspectorBody").textContent).toContain("history down");
            expect(sharedApp.showToast).toHaveBeenCalledWith("history down");
        });

        test("closes history modal and resets aria hidden", async () => {
            jest.useFakeTimers();
            const { hooks } = setupModule3("/my-resources.html");
            document.body.innerHTML = `<div id="historyModal" aria-hidden="false"></div>`;

            hooks.closeHistoryModal();
            jest.advanceTimersByTime(100);

            expect(document.getElementById("historyModal").classList.contains("hidden")).toBe(true);
            expect(document.getElementById("historyModal").getAttribute("aria-hidden")).toBe("true");
            expect(document.body.classList.contains("history-modal-open")).toBe(false);
        });

        test("binds history modal actions for close and escape", async () => {
            jest.useFakeTimers();
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <div id="historyModal" aria-hidden="false">
                    <button id="closeBtn" data-close-history-modal type="button">Close</button>
                </div>
                <table><tbody id="resourceTableBody"><tr><td><button id="historyBtn" data-history-id="17">History</button></td></tr></tbody></table>
                <div id="historyResourceTitle"></div>
                <div id="historyResourceStatus"></div>
                <div id="historyResourceVersion"></div>
                <div id="historySubmittedAt"></div>
                <div id="historyFeedbackText"></div>
                <div id="historyInspectorMeta"></div>
                <div id="historyInspectorBody"></div>
                <table><tbody id="submissionHistoryBody"></tbody></table>
                <table><tbody id="versionHistoryBody"></tbody></table>
            `;
            sharedApp.requestJson
                .mockResolvedValueOnce({ title: "T", status: "Draft" })
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);

            hooks.bindHistoryModal();

            document.getElementById("historyBtn")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();
            await flushPromises();

            document.getElementById("closeBtn")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            jest.advanceTimersByTime(100);
            expect(document.getElementById("historyModal").classList.contains("hidden")).toBe(true);

            document.getElementById("historyModal").classList.remove("hidden");
            document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape", bubbles: true }));
            jest.advanceTimersByTime(100);
            expect(document.getElementById("historyModal").classList.contains("hidden")).toBe(true);
        });
    });

    describe("resource edit initialization flows", () => {
        function buildResourceEditDom() {
            document.body.innerHTML = `
                <form id="metadataForm">
                    <input id="title" value="">
                    <input id="copyright" value="">
                    <select id="categoryId"></select>
                    <input id="place" value="">
                    <textarea id="description"></textarea>
                    <select id="resourceType"></select>
                    <select id="resourceTypeMirror"></select>
                    <input id="tagInput">
                    <div id="tagOptions"></div>
                </form>
                <form id="uploadForm"></form>
                <form id="submitForm"><textarea id="submissionNote"></textarea></form>
                <button id="createDraftBtn" type="button"></button>
                <button id="metadataSaveBtn" type="button"></button>
                <button id="submitReviewBtn" type="button"></button>
                <div id="editorAlert" class="hidden"></div>
                <button data-file-target="mediaFile" type="button"></button>
                <button data-file-target="previewImage" type="button"></button>
                <input id="mediaFile" type="file">
                <input id="previewImage" type="file">
                <span id="mediaFileNameText">Drag & drop file here or</span>
                <span id="previewImageNameText">No file selected</span>
                <span id="mediaFilePathText"></span>
                <span id="previewImagePathText"></span>
                <div id="mediaFileList"></div>
                <div id="previewImageFrame"></div>
                <span id="resourceUserName"></span>
                <span id="resourceAccessText"></span>
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;
        }

        test("initializes resource-edit page and loads an existing resource", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html?id=42");
            buildResourceEditDom();
            sharedApp.requestJson
                .mockResolvedValueOnce([{ id: 5, name: "Ceremony" }])
                .mockResolvedValueOnce([{ name: "Photo" }, { name: "Video" }])
                .mockResolvedValueOnce({ name: "Editor", contributor: true })
                .mockResolvedValueOnce({
                    id: 42,
                    title: "Temple Archive",
                    copyright: "Museum rights",
                    categoryId: 5,
                    place: "Suzhou",
                    description: "Historical description",
                    resourceType: "photo",
                    status: "Draft",
                    mediaUrl: "resource-42/temple.jpg",
                    previewImage: "resource-42/preview.jpg"
                });

            await hooks.initResourceEditPage();

            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                1,
                "/api/contributor/resources/category-options",
                { method: "GET" }
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                2,
                "/api/contributor/resources/resource-type-options",
                { method: "GET" }
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                3,
                "/api/auth/me",
                { method: "GET" }
            );
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                4,
                "/api/contributor/resources/42",
                { method: "GET" }
            );
            expect(document.getElementById("title").value).toBe("Temple Archive");
            expect(document.getElementById("resourceIdText").textContent).toBe("42");
            expect(document.getElementById("mediaFileList").innerHTML).toContain("temple.jpg");
        });

        test("initializes resource-edit page without resource id and applies default meta", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html");
            buildResourceEditDom();
            sharedApp.requestJson
                .mockResolvedValueOnce([{ id: 5, name: "Ceremony" }])
                .mockResolvedValueOnce([{ name: "Photo" }])
                .mockResolvedValueOnce({ name: "Editor", contributor: true });

            await hooks.initResourceEditPage();

            expect(sharedApp.requestJson).toHaveBeenCalledTimes(3);
            expect(document.getElementById("resourceIdText").textContent).toBe("Not saved yet");
            expect(document.getElementById("resourceStatusText").textContent).toBe("Draft");
        });
    });

    describe("additional branch coverage helpers", () => {
        test("persists resource type selection across force, skip, and error branches", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html?id=77");
            document.body.innerHTML = `
                <select id="resourceType"><option value="video">Video</option><option value="photo">Photo</option></select>
                <select id="resourceTypeMirror"><option value="video" selected>Video</option><option value="photo">Photo</option></select>
                <input id="mediaFile" type="file">
                <div id="tagOptions"></div>
                <div id="mediaFileList"></div>
                <div id="previewImageFrame"></div>
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;

            sharedApp.requestJson.mockResolvedValueOnce(null);
            const first = await hooks.persistResourceTypeSelection({ force: true });
            expect(first).toBeNull();
            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/contributor/resources/77",
                expect.objectContaining({ method: "PUT" })
            );

            sharedApp.requestJson.mockClear();
            await hooks.persistResourceTypeSelection();
            expect(sharedApp.requestJson).not.toHaveBeenCalled();

            document.getElementById("resourceTypeMirror").value = "photo";
            sharedApp.requestJson.mockRejectedValueOnce(new Error("save type failed"));
            await expect(hooks.persistResourceTypeSelection({ force: true })).rejects.toThrow("save type failed");
            expect(sharedApp.showToast).toHaveBeenCalledWith("save type failed");
        });

        test("commits and removes tag chips through input and click interactions", () => {
            const { hooks } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <input id="tagInput" value="">
                <div id="tagOptions"></div>
            `;
            const input = document.getElementById("tagInput");

            hooks.commitTagInputValue(null);
            input.value = "   ";
            hooks.commitTagInputValue(input);
            expect(input.value).toBe("");

            input.value = "Dynasty, Archive";
            hooks.commitTagInputValue(input);
            expect(document.getElementById("tagOptions").innerHTML).toContain("Dynasty");
            expect(document.getElementById("tagOptions").innerHTML).toContain("Archive");

            document.querySelector("[data-tag-name=\"Dynasty\"]")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            expect(document.getElementById("tagOptions").innerHTML).not.toContain("Dynasty");
        });

        test("binds single file picker button fallback and auto upload branch", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html?id=42");
            document.body.innerHTML = `
                <button data-file-target="previewImage" type="button"></button>
                <input id="previewImage" type="file">
                <span id="previewImageNameText">No file selected</span>
                <button data-file-target="mediaFile" type="button"></button>
                <input id="mediaFile" type="file">
                <span id="mediaFileNameText">Drag & drop file here or</span>
                <span id="mediaFilePathText"></span>
                <span id="previewImagePathText"></span>
                <div id="previewImageFrame"></div>
                <div id="mediaFileList"></div>
                <select id="resourceType"><option value="" selected></option></select>
                <select id="resourceTypeMirror"><option value="" selected></option></select>
                <div id="tagOptions"></div>
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;
            const previewInput = document.getElementById("previewImage");
            const clickSpy = jest.spyOn(previewInput, "click");
            const originalCreateObjectURL = URL.createObjectURL;
            const originalRevokeObjectURL = URL.revokeObjectURL;
            URL.createObjectURL = jest.fn(() => "blob:preview");
            URL.revokeObjectURL = jest.fn();
            previewInput.showPicker = jest.fn(() => {
                throw new Error("blocked");
            });

            hooks.bindSingleFilePicker("previewImage", "previewImageNameText", { autoUpload: true });
            document.querySelector('[data-file-target="previewImage"]')
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            expect(previewInput.showPicker).toHaveBeenCalled();
            expect(clickSpy).toHaveBeenCalled();

            const previewFile = new File(["img"], "preview.png", { type: "image/png" });
            Object.defineProperty(previewInput, "files", {
                configurable: true,
                value: [previewFile]
            });
            sharedApp.requestJson.mockResolvedValueOnce({
                id: 42,
                status: "Draft",
                resourceType: "photo",
                previewImage: "resource-42/preview.png"
            });

            previewInput.dispatchEvent(new Event("change", { bubbles: true }));
            await flushPromises();
            await flushPromises();

            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/contributor/resources/42/files",
                expect.objectContaining({ method: "POST" })
            );
            expect(document.getElementById("previewImageNameText").textContent).toContain("preview.png");

            URL.createObjectURL = originalCreateObjectURL;
            URL.revokeObjectURL = originalRevokeObjectURL;
        });
    });

    describe("history inspector actions", () => {
        test("loads and renders version snapshot", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <div id="historyInspectorMeta"></div>
                <div id="historyInspectorBody"></div>
            `;

            sharedApp.requestJson.mockResolvedValue({
                versionNo: 6,
                snapshotMap: {
                    title: "Snapshot",
                    resourceType: "document",
                    tagNames: ["paper", "scan"],
                    categoryId: 9
                }
            });

            await hooks.viewVersionSnapshot("10", 6);

            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/contributor/resources/10/versions/6",
                { method: "GET" }
            );
            expect(document.getElementById("historyInspectorMeta").textContent).toContain("V6");
            expect(document.getElementById("historyInspectorBody").textContent).toContain("Snapshot");
            expect(document.getElementById("historyInspectorBody").textContent).toContain("File/Document");
        });

        test("handles version snapshot failure", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <div id="historyInspectorMeta"></div>
                <div id="historyInspectorBody"></div>
            `;

            sharedApp.requestJson.mockRejectedValue(new Error("snapshot error"));
            await hooks.viewVersionSnapshot("10", 6);

            expect(document.getElementById("historyInspectorBody").textContent).toContain("snapshot error");
            expect(sharedApp.showToast).toHaveBeenCalledWith("snapshot error");
        });

        test("compares version with current and renders diff rows", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <div id="historyInspectorMeta"></div>
                <div id="historyInspectorBody"></div>
            `;

            await hooks.loadHistoryModalData("31").catch(() => {});
            sharedApp.requestJson.mockReset();
            sharedApp.requestJson.mockResolvedValue({
                leftVersionNo: 2,
                rightVersionNo: 5,
                diffItems: [
                    { fieldName: "resourceType", fieldLabel: "Resource Type", leftValue: "photo", rightValue: "video", changed: true },
                    { fieldName: "title", fieldLabel: "Title", leftValue: "A", rightValue: "A", changed: false }
                ]
            });

            // set history state by opening modal data load once
            sharedApp.requestJson
                .mockResolvedValueOnce({ title: "x", status: "Draft", currentVersionNo: 5 })
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);
            document.body.innerHTML += `
                <div id="historyResourceTitle"></div>
                <div id="historyResourceStatus"></div>
                <div id="historyResourceVersion"></div>
                <div id="historySubmittedAt"></div>
                <div id="historyFeedbackText"></div>
                <table><tbody id="submissionHistoryBody"></tbody></table>
                <table><tbody id="versionHistoryBody"></tbody></table>
            `;
            await hooks.loadHistoryModalData("31");

            sharedApp.requestJson.mockReset();
            sharedApp.requestJson.mockResolvedValue({
                leftVersionNo: 2,
                rightVersionNo: 5,
                diffItems: [
                    { fieldName: "resourceType", fieldLabel: "Resource Type", leftValue: "photo", rightValue: "video", changed: true }
                ]
            });

            await hooks.compareVersionWithCurrent("31", 2);

            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/contributor/resources/31/versions/compare?v1=2&v2=5",
                { method: "GET" }
            );
            expect(document.getElementById("historyInspectorBody").innerHTML).toContain("history-diff-row");
            expect(document.getElementById("historyInspectorBody").textContent).toContain("Photo/Image");
            expect(document.getElementById("historyInspectorBody").textContent).toContain("Video");
        });

        test("shows fallback when current version missing for compare", async () => {
            const { hooks } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <div id="historyInspectorMeta"></div>
                <div id="historyInspectorBody"></div>
            `;

            await hooks.compareVersionWithCurrent("9", 1);
            expect(document.getElementById("historyInspectorBody").textContent)
                .toContain("comparison cannot be displayed");
        });

        test("rolls back a version when editable and confirmed", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            jest.spyOn(window, "confirm").mockReturnValue(true);
            document.body.innerHTML = `
                <input id="keyword" value="">
                <select id="statusFilter"><option value=""></option></select>
                <select id="categoryFilter"><option value=""></option></select>
                <table><tbody id="resourceTableBody"></tbody></table>
                <div id="listMeta"></div>
                <div id="historyInspectorMeta"></div>
                <div id="historyInspectorBody"></div>
                <div id="historyResourceTitle"></div>
                <div id="historyResourceStatus"></div>
                <div id="historyResourceVersion"></div>
                <div id="historySubmittedAt"></div>
                <div id="historyFeedbackText"></div>
                <table><tbody id="submissionHistoryBody"></tbody></table>
                <table><tbody id="versionHistoryBody"></tbody></table>
            `;

            sharedApp.requestJson
                .mockResolvedValueOnce({ title: "R", status: "Draft", currentVersionNo: 3 })
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce(null)
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce({ title: "R", status: "Draft", currentVersionNo: 3 })
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);
            await hooks.loadHistoryModalData("15");
            await hooks.rollbackToVersion("15", 2);

            expect(sharedApp.requestJson).toHaveBeenCalledWith(
                "/api/contributor/resources/15/versions/2/rollback",
                expect.objectContaining({ method: "POST" })
            );
            expect(sharedApp.showToast).toHaveBeenCalledWith("Version V2 restored successfully.");
        });

        test("blocks rollback when not editable or user cancels", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <div id="historyInspectorMeta"></div>
                <div id="historyInspectorBody"></div>
                <div id="historyResourceTitle"></div>
                <div id="historyResourceStatus"></div>
                <div id="historyResourceVersion"></div>
                <div id="historySubmittedAt"></div>
                <div id="historyFeedbackText"></div>
                <table><tbody id="submissionHistoryBody"></tbody></table>
                <table><tbody id="versionHistoryBody"></tbody></table>
            `;

            sharedApp.requestJson
                .mockResolvedValueOnce({ title: "R", status: "Approved", currentVersionNo: 8 })
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);
            await hooks.loadHistoryModalData("18");
            await hooks.rollbackToVersion("18", 7);
            expect(sharedApp.showToast).toHaveBeenCalledWith("Only Draft or Rejected resources can be restored.");

            sharedApp.showToast.mockClear();
            jest.spyOn(window, "confirm").mockReturnValue(false);
            sharedApp.requestJson
                .mockResolvedValueOnce({ title: "R", status: "Draft", currentVersionNo: 8 })
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);
            await hooks.loadHistoryModalData("18");
            await hooks.rollbackToVersion("18", 7);
            expect(window.confirm).toHaveBeenCalled();
            expect(sharedApp.requestJson).not.toHaveBeenCalledWith(
                "/api/contributor/resources/18/versions/7/rollback",
                expect.anything()
            );
        });

        test("renders compare fallback when no diff items", () => {
            const { hooks } = setupModule3("/my-resources.html");
            document.body.innerHTML = `
                <div id="historyInspectorMeta"></div>
                <div id="historyInspectorBody"></div>
            `;

            hooks.renderVersionCompare({ leftVersionNo: 1, rightVersionNo: 2, diffItems: [] });
            expect(document.getElementById("historyInspectorBody").textContent)
                .toContain("No compare data is available.");
        });
    });

    describe("heritage module modal and routing helpers", () => {
        test("activates and closes heritage module modal", async () => {
            jest.useFakeTimers();
            const { hooks } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <form class="module-stack" id="formA">
                    <section class="module-card" id="module-identity"></section>
                </form>
                <form class="module-stack" id="formB">
                    <section class="module-card" id="module-tags"></section>
                </form>
                <a id="nodeIdentity" class="heritage-node" data-target="module-identity"></a>
                <a id="nodeTags" class="module-pill" href="#module-tags"></a>
                <div id="activeModuleLabel"></div>
                <div id="activeModuleHint"></div>
                <input id="title">
                <input id="tagInput">
            `;
            document.getElementById("module-identity").scrollIntoView = jest.fn();
            document.getElementById("title").focus = jest.fn();

            hooks.activateHeritageModule("module-identity", { shouldScroll: true, shouldFocus: true });
            jest.advanceTimersByTime(420);
            expect(document.getElementById("module-identity").classList.contains("is-active")).toBe(true);
            expect(document.getElementById("formA").classList.contains("active-workspace")).toBe(true);
            expect(document.getElementById("nodeIdentity").getAttribute("aria-current")).toBe("true");

            hooks.openHeritageModuleModal("module-tags", { shouldFocus: false });
            expect(document.body.classList.contains("module-modal-open")).toBe(true);

            hooks.closeHeritageModuleModal();
            expect(document.body.classList.contains("module-modal-open")).toBe(false);
            expect(document.getElementById("module-tags").getAttribute("aria-hidden")).toBe("true");
            expect(document.getElementById("activeModuleLabel").textContent).toContain("Click an icon");
        });

        test("binds heritage landing and module modal shortcuts", () => {
            jest.useFakeTimers();
            const { hooks } = setupModule3("/resource-edit.html#module-tags");
            document.body.innerHTML = `
                <form class="module-stack"><section class="module-card" id="module-tags"></section></form>
                <a id="nodeTags" class="heritage-node" data-target="module-tags"></a>
                <button id="closeModuleBtn" data-close-module-modal type="button">x</button>
                <div id="activeModuleLabel"></div>
                <div id="activeModuleHint"></div>
                <input id="tagInput">
            `;
            document.getElementById("tagInput").focus = jest.fn();

            hooks.bindHeritageLanding();
            hooks.bindModuleModalUI();
            jest.advanceTimersByTime(1200);
            expect(document.body.classList.contains("module-flow-enabled")).toBe(true);

            document.getElementById("nodeTags")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            expect(document.body.classList.contains("module-modal-open")).toBe(true);

            document.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape", bubbles: true }));
            expect(document.body.classList.contains("module-modal-open")).toBe(false);
        });

        test("focus helper prefers tag input for module-tags", () => {
            jest.useFakeTimers();
            const { hooks } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <input id="submissionNote">
                <input id="tagInput">
            `;
            document.getElementById("tagInput").focus = jest.fn();

            hooks.focusModuleField("module-tags");
            jest.advanceTimersByTime(420);
            expect(document.getElementById("tagInput").focus).toHaveBeenCalled();
        });
    });

    describe("access and draft helpers", () => {
        test("ensures contributor access and redirects on errors", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html?id=7");
            jest.spyOn(console, "error").mockImplementation(() => {});

            sharedApp.requestJson.mockResolvedValueOnce({ contributor: true, name: "A" });
            await expect(hooks.ensureContributorWorkspaceAccess()).resolves.toEqual({ contributor: true, name: "A" });

            sharedApp.requestJson.mockResolvedValueOnce({ contributor: false });
            await expect(hooks.ensureContributorWorkspaceAccess()).rejects.toThrow("Contributor access required.");

            sharedApp.requestJson.mockRejectedValueOnce({ status: 401 });
            await expect(hooks.ensureContributorWorkspaceAccess()).rejects.toEqual({ status: 401 });

            sharedApp.requestJson.mockRejectedValueOnce({ status: 403, message: "Denied" });
            await expect(hooks.ensureContributorWorkspaceAccess()).rejects.toEqual({ status: 403, message: "Denied" });
        });

        test("creates draft and updates url, or returns null on failure", async () => {
            const { hooks, sharedApp } = setupModule3("/resource-edit.html");
            document.body.innerHTML = `
                <span id="resourceIdText"></span>
                <span id="resourceStatusText"></span>
                <span id="updatedAtText"></span>
                <span id="resourceStatusBadge"></span>
            `;

            sharedApp.requestJson.mockResolvedValueOnce({ id: 123, status: "Draft" });
            const created = await hooks.createDraftForEditing();
            expect(created.id).toBe(123);
            expect(window.location.search).toBe("?id=123");

            sharedApp.requestJson.mockRejectedValueOnce(new Error("create failed"));
            await expect(hooks.createDraftForEditing({ throwOnError: true })).rejects.toThrow("create failed");

            sharedApp.requestJson.mockRejectedValueOnce(new Error("silent fail"));
            const fallback = await hooks.createDraftForEditing();
            expect(fallback).toBeNull();
            expect(sharedApp.showToast).toHaveBeenCalledWith("silent fail");
        });

        test("binds create draft button and list filter actions", async () => {
            const { hooks, sharedApp } = setupModule3("/my-resources.html");
            jest.spyOn(console, "error").mockImplementation(() => {});
            document.body.innerHTML = `
                <button id="createDraftBtn">create</button>
                <button id="searchBtn">search</button>
                <button id="resetBtn">reset</button>
                <input id="keyword" value="needle">
                <select id="statusFilter"><option value="Draft" selected>Draft</option></select>
                <select id="categoryFilter"><option value="8" selected>8</option></select>
                <table><tbody id="resourceTableBody"></tbody></table>
                <div id="listMeta"></div>
            `;

            sharedApp.requestJson
                .mockResolvedValueOnce({ id: 59 })
                .mockResolvedValueOnce([])
                .mockResolvedValueOnce([]);
            hooks.bindCreateDraftButton();
            hooks.bindListFilterButtons();

            document.getElementById("createDraftBtn")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();
            expect(sharedApp.showToast).toHaveBeenCalledWith("Draft created successfully.");
            expect(sharedApp.requestJson).toHaveBeenNthCalledWith(
                1,
                "/api/contributor/resources",
                { method: "POST" }
            );

            document.getElementById("searchBtn")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();

            document.getElementById("resetBtn")
                .dispatchEvent(new MouseEvent("click", { bubbles: true }));
            await flushPromises();

            expect(document.getElementById("keyword").value).toBe("");
            expect(document.getElementById("statusFilter").value).toBe("");
            expect(document.getElementById("categoryFilter").value).toBe("");
        });
    });
});
