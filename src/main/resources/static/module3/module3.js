const AUTH_API_BASE = "/api/auth";
const API_BASE = "/api/contributor/resources";
const ARTWORK_BASE_PATH = "./module3/assets/artwork";
const HERO_IMAGE_FILES = ["1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg", "6.jpg"];
const PANEL_IMAGE_FILES = ["7.jpg", "8.jpg", "9.jpg", "10.jpg", "11.jpg", "12.jpg", "13.jpg", "14.jpg"];
const DASHBOARD_PANEL_IMAGE_FILES = ["15.jpg", "16.jpg", "17.jpg", "18.jpg", "19.jpg", "20.jpg", "21.jpg", "22.jpg"];
const MAIN_BACKGROUND_IMAGE_FILES = ["23.jpg", "24.jpg", "25.jpg"];
const SCROLL_SCENE_IMAGE_FILES = ["23.jpg", "24.jpg", "25.jpg", "26.jpg"];
const {
    bindLogoutButtons,
    escapeHtml,
    requestJson,
    setValue,
    showMessageFromQuery,
    showToast
} = window.SharedApp;
const formatDateTime = value => window.SharedApp.formatDateTime(value, { emptyText: "—" });
const HERITAGE_MODULE_META = {
    "module-identity": {
        label: "Heritage Identity",
        hint: "Fill in the title and topic.",
        focusId: "title"
    },
    "module-location": {
        label: "Location",
        hint: "Add the place linked to this resource.",
        focusId: "place"
    },
    "module-media": {
    label: "Media Upload",
    hint: "Choose a resource type and upload the primary media file.",
    focusId: "resourceTypeMirror"
    },
    "module-description": {
        label: "Description",
        hint: "Write the historical background and cultural notes.",
        focusId: "description"
    },
    "module-preview": {
        label: "Preview Image",
        hint: "Upload a preview image for cards and cover display.",
        focusId: "previewImage"
    },
    "module-submit": {
        label: "Submit Review",
        hint: "Add a submission note and prepare the draft for review.",
        focusId: "submissionNote"
    },
    "module-tags": {
        label: "Tags & Keywords",
        hint: "Add your own tags and keywords for this resource.",
        focusId: "tagInput"
    }
};

let categoryOptionCache = [];
let savedResourceTypeValue = "";
let pendingResourceTypeSave = Promise.resolve();
let tagDraftValues = [];
let historyModalState = createEmptyHistoryState();

document.addEventListener("DOMContentLoaded", () => {
    const page = document.body.dataset.page;
    bindLogoutButtons({
        logoutUrl: `${AUTH_API_BASE}/logout`,
        redirectTo: `./index.html?message=${encodeURIComponent("You have logged out successfully.")}`
    });
    showMessageFromQuery({ duration: 2600 });

    if (page === "my-resources") {
        initMyResourcesPage();
    }

    if (page === "resource-edit") {
        initResourceEditPage();
    }
});

async function initMyResourcesPage() {
    applyMyResourcesArtwork();
    bindCreateDraftButton();
    bindListFilterButtons();
    bindHistoryModal();
    await loadCategoryFilterOptions();

    try {
        const currentUser = await ensureContributorWorkspaceAccess();
        populateWorkspaceSession(currentUser);
    } catch (error) {
        if (error.isNetworkError) {
            showToast("Unable to verify contributor access right now.");
        }
        return;
    }

    await loadResourceList();
}

async function initResourceEditPage() {
    applyRandomEditorArtwork();
    bindHeritageLanding();
    bindModuleModalUI();
    bindCreateDraftButton();
    bindFilePickerUI();
    bindResourceTypeMirror();
    bindTagEditor();
    bindMetadataForm();
    bindUploadForm();
    bindSubmitForm();
    await loadCategorySelectOptions();

    try {
        const currentUser = await ensureContributorWorkspaceAccess();
        populateWorkspaceSession(currentUser);
    } catch (error) {
        if (error.isNetworkError) {
            showToast("Unable to verify contributor access right now.");
        }
        return;
    }

    const resourceId = getResourceIdFromQuery();

    if (resourceId) {
        await loadResourceDetail(resourceId);
    } else {
        updateEditorMeta(null);
    }
}

function bindFilePickerUI() {
    bindSingleFilePicker("mediaFile", "mediaFileNameText");
    bindSingleFilePicker("previewImage", "previewImageNameText");
    updateMediaFileAccept(document.getElementById("resourceType")?.value || "");
}

function bindSingleFilePicker(inputId, textId) {
    const input = document.getElementById(inputId);
    const text = document.getElementById(textId);
    const button = document.querySelector(`[data-file-target="${inputId}"]`);
    if (!input || !text) return;

    if (button) {
        button.addEventListener("click", () => {
            if (typeof input.showPicker === "function") {
                try {
                    input.showPicker();
                    return;
                } catch (error) {
                    // Fallback when showPicker exists but is rejected in this context.
                }
            }

            input.click();
        });
    }

    input.addEventListener("change", () => {
        const file = input.files?.[0];
        text.textContent = file ? file.name : "No file selected";
    });
}

function applyRandomEditorArtwork() {
    const scrollScene = document.querySelector('[data-random-art="scroll-scene"]');
    if (scrollScene) {
        const sceneImage = pickRandomItem(SCROLL_SCENE_IMAGE_FILES);
        scrollScene.style.setProperty("--scroll-scene-image", `url("${ARTWORK_BASE_PATH}/${sceneImage}")`);
    }

    const hero = document.querySelector('[data-random-art="hero"]');
    if (hero) {
        const heroImage = pickRandomItem(HERO_IMAGE_FILES);
        hero.style.setProperty("--hero-bg-image", `url("${ARTWORK_BASE_PATH}/${heroImage}")`);
    }

    const panels = Array.from(document.querySelectorAll('[data-random-art="panel"]'));
    if (!panels.length) return;

    const panelImages = shuffleArray([...PANEL_IMAGE_FILES]);
    panels.forEach((panel, index) => {
        const panelImage = panelImages[index % panelImages.length];
        panel.style.setProperty(
            "--panel-bg-image",
            `linear-gradient(180deg, rgba(255, 255, 255, 0.2), rgba(255, 255, 255, 0.08)), url("${ARTWORK_BASE_PATH}/${panelImage}")`
        );
    });
}

function bindHeritageLanding() {
    const nodes = Array.from(document.querySelectorAll(".heritage-node, .module-pill"));
    if (nodes.length === 0) return;

    document.body.classList.add("module-flow-enabled");

    window.setTimeout(() => {
        document.body.classList.add("scroll-unfurled");
    }, 180);

    nodes.forEach(node => {
        node.addEventListener("click", (event) => {
            event.preventDefault();
            openHeritageModuleModal(getModuleIdFromTrigger(node));
        });
    });

    const initialModuleId = getInitialModuleId();
    if (window.location.hash) {
        window.setTimeout(() => {
            openHeritageModuleModal(initialModuleId, { shouldFocus: false, updateHistory: false });
        }, 1200);
    } else {
        resetActiveModuleMeta();
        clearHeritageModuleSelection();
    }
}

function bindModuleModalUI() {
    document.querySelectorAll("[data-close-module-modal]").forEach(element => {
        element.addEventListener("click", (event) => {
            event.preventDefault();
            closeHeritageModuleModal();
        });
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && document.body.classList.contains("module-modal-open")) {
            closeHeritageModuleModal();
        }
    });
}

function applyMyResourcesArtwork() {
    const main = document.querySelector('[data-random-art="main"]');
    if (main) {
        const mainImage = pickRandomItem(MAIN_BACKGROUND_IMAGE_FILES);
        main.style.setProperty("--page-bg-image", `url("${ARTWORK_BASE_PATH}/${mainImage}")`);
    }

    const hero = document.querySelector('[data-random-art="hero"]');
    if (hero) {
        const heroImage = pickRandomItem(HERO_IMAGE_FILES);
        hero.style.setProperty("--hero-bg-image", `url("${ARTWORK_BASE_PATH}/${heroImage}")`);
    }

    const panels = Array.from(document.querySelectorAll('[data-random-art="panel"]'));
    if (!panels.length) return;

    const panelImages = shuffleArray([...DASHBOARD_PANEL_IMAGE_FILES]);
    panels.forEach((panel, index) => {
        const panelImage = panelImages[index % panelImages.length];
        panel.style.setProperty(
            "--panel-bg-image",
            `linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.06)), url("${ARTWORK_BASE_PATH}/${panelImage}")`
        );
    });
}

function pickRandomItem(items) {
    return items[Math.floor(Math.random() * items.length)];
}

function shuffleArray(items) {
    for (let i = items.length - 1; i > 0; i -= 1) {
        const j = Math.floor(Math.random() * (i + 1));
        [items[i], items[j]] = [items[j], items[i]];
    }
    return items;
}

function bindCreateDraftButton() {
    const createDraftBtn = document.getElementById("createDraftBtn");
    if (!createDraftBtn) return;

    createDraftBtn.addEventListener("click", async () => {
        createDraftBtn.disabled = true;

        try {
            const resource = await requestJson(API_BASE, {
                method: "POST"
            });

            showToast("Draft created successfully.");
            window.location.href = `./resource-edit.html?id=${resource.id}`;
        } catch (error) {
            showToast(error.message || "Failed to create draft.");
        } finally {
            createDraftBtn.disabled = false;
        }
    });
}

function bindResourceTypeMirror() {
    const primarySelect = document.getElementById("resourceType");
    const mirrorSelect = document.getElementById("resourceTypeMirror");
    if (!primarySelect || !mirrorSelect) return;

    primarySelect.addEventListener("change", () => {
        mirrorSelect.value = primarySelect.value;
        updateMediaFileAccept(primarySelect.value);
    });

    mirrorSelect.addEventListener("change", () => {
        primarySelect.value = mirrorSelect.value;
        updateMediaFileAccept(mirrorSelect.value);
        pendingResourceTypeSave = persistResourceTypeSelection({ showError: true });
    });

    syncResourceTypeMirror();
}

function bindTagEditor() {
    const input = document.getElementById("tagInput");
    if (!input) return;

    input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === ",") {
            event.preventDefault();
            commitTagInputValue(input);
        }
    });

    input.addEventListener("blur", () => {
        commitTagInputValue(input);
    });

    setTagValues([]);
}

function bindListFilterButtons() {
    const searchBtn = document.getElementById("searchBtn");
    const resetBtn = document.getElementById("resetBtn");
    const keywordInput = document.getElementById("keyword");

    if (searchBtn) {
        searchBtn.addEventListener("click", loadResourceList);
    }

    if (resetBtn) {
        resetBtn.addEventListener("click", async () => {
            document.getElementById("keyword").value = "";
            document.getElementById("statusFilter").value = "";
            document.getElementById("categoryFilter").value = "";
            await loadResourceList();
        });
    }

    if (keywordInput) {
        keywordInput.addEventListener("keydown", async (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                await loadResourceList();
            }
        });
    }
}

function bindHistoryModal() {
    const tableBody = document.getElementById("resourceTableBody");
    const historyModal = document.getElementById("historyModal");
    if (!tableBody || !historyModal) return;

    tableBody.addEventListener("click", async (event) => {
        const historyButton = event.target.closest("[data-history-id]");
        if (!historyButton) return;

        await openHistoryModal(historyButton.dataset.historyId);
    });

    historyModal.addEventListener("click", async (event) => {
        const closeTrigger = event.target.closest("[data-close-history-modal]");
        if (closeTrigger) {
            closeHistoryModal();
            return;
        }

        const actionButton = event.target.closest("[data-version-action]");
        if (!actionButton) {
            return;
        }

        const resourceId = actionButton.dataset.resourceId;
        const versionNo = Number(actionButton.dataset.versionNo);
        const actionType = actionButton.dataset.versionAction;
        actionButton.disabled = true;

        try {
            if (actionType === "view") {
                await viewVersionSnapshot(resourceId, versionNo);
            } else if (actionType === "compare") {
                await compareVersionWithCurrent(resourceId, versionNo);
            } else if (actionType === "restore") {
                await rollbackToVersion(resourceId, versionNo);
            }
        } finally {
            actionButton.disabled = false;
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !historyModal.classList.contains("hidden")) {
            closeHistoryModal();
        }
    });
}

async function loadCategoryFilterOptions() {
    const select = document.getElementById("categoryFilter");
    if (!select) return;

    try {
        const options = await getCategoryOptions();
        renderSelectOptions(select, options, "All Topics");
        select.disabled = false;
    } catch (error) {
        categoryOptionCache = [];
        renderSelectOptions(select, [], "Category unavailable");
        select.value = "";
        select.disabled = true;
        showToast("Failed to load categories. Category filter is unavailable.");
    }
}

async function loadCategorySelectOptions() {
    const select = document.getElementById("categoryId");
    if (!select) return;

    try {
        const options = await getCategoryOptions();
        renderSelectOptions(select, options, "Select topic");
        select.disabled = false;
        setEditorCategoryAvailability(true);
    } catch (error) {
        categoryOptionCache = [];
        renderSelectOptions(select, [], "Categories unavailable");
        select.value = "";
        select.disabled = true;
        setEditorCategoryAvailability(false);
        showToast(error.message || "Failed to load categories.");
    }
}

async function loadResourceList() {
    const tbody = document.getElementById("resourceTableBody");
    const listMeta = document.getElementById("listMeta");

    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="7" class="empty-row">Loading resources...</td></tr>`;

    try {
        const keyword = document.getElementById("keyword")?.value?.trim() || "";
        const status = document.getElementById("statusFilter")?.value || "";
        const categoryId = document.getElementById("categoryFilter")?.value || "";

        const query = new URLSearchParams();
        if (keyword) query.set("keyword", keyword);
        if (status) query.set("status", status);
        if (categoryId) query.set("categoryId", categoryId);

        const url = `${API_BASE}/my${query.toString() ? `?${query.toString()}` : ""}`;
        const resources = await requestJson(url, { method: "GET" });

        renderResourceTable(resources);
        if (listMeta) {
            listMeta.textContent = `${resources.length} item(s)`;
        }
    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-row">${escapeHtml(error.message || "Failed to load resources.")}</td></tr>`;
        if (listMeta) {
            listMeta.textContent = "Load failed";
        }
        showToast(error.message || "Failed to load resources.");
    }
}

function renderResourceTable(resources) {
    const tbody = document.getElementById("resourceTableBody");
    if (!tbody) return;

    if (!resources || resources.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-row">No resources found.</td></tr>`;
        return;
    }

    tbody.innerHTML = resources.map(resource => {
        const categoryText = resource.categoryName || "-";
        const updatedAtText = formatDateTime(resource.updatedAt);
        const versionText = resource.currentVersionNo != null
            ? `V${resource.currentVersionNo}`
            : "No version record";
        const submittedText = resource.lastSubmittedAt
            ? `Last submitted: ${formatDateTime(resource.lastSubmittedAt)}`
            : "Not submitted yet";

        return `
            <tr>
                <td>${resource.id ?? "-"}</td>
                <td>
                    <div class="resource-title-stack">
                        <div class="resource-title-text">${escapeHtml(resource.title ?? "-")}</div>
                        <div class="resource-submeta">
                            <span class="resource-version-badge">${escapeHtml(versionText)}</span>
                            <span>${escapeHtml(submittedText)}</span>
                        </div>
                        ${resource.hasReviewFeedback ? `<div class="resource-feedback-note">Feedback available</div>` : ""}
                    </div>
                </td>
                <td>${escapeHtml(categoryText)}</td>
                <td>${escapeHtml(formatResourceType(resource.resourceType))}</td>
                <td><span class="status-pill status-${toStatusClassName(resource.status)}">${formatStatus(resource.status)}</span></td>
                <td>${updatedAtText}</td>
                <td>
                    <div class="action-group">
                        <a class="action-link" href="./resource-edit.html?id=${resource.id}">Edit</a>
                        <button type="button" class="action-link action-button" data-history-id="${resource.id}">History</button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

async function openHistoryModal(resourceId) {
    const historyModal = document.getElementById("historyModal");
    if (!historyModal || !resourceId) return;

    historyModalState = createEmptyHistoryState();
    historyModal.classList.remove("hidden");
    historyModal.setAttribute("aria-hidden", "false");
    document.body.classList.add("history-modal-open");
    renderHistoryLoading();

    try {
        await loadHistoryModalData(resourceId);
    } catch (error) {
        renderHistoryLoadFailure(error.message || "Failed to load resource history.");
        showToast(error.message || "Failed to load resource history.");
    }
}

function closeHistoryModal() {
    const historyModal = document.getElementById("historyModal");
    if (!historyModal) return;

    historyModal.classList.add("hidden");
    historyModal.setAttribute("aria-hidden", "true");
    document.body.classList.remove("history-modal-open");
    historyModalState = createEmptyHistoryState();
}

function renderHistoryLoading() {
    setHistorySummaryText("historyResourceTitle", "Loading...");
    setHistorySummaryText("historyResourceStatus", "Loading...");
    setHistorySummaryText("historyResourceVersion", "Loading...");
    setHistorySummaryText("historySubmittedAt", "Loading...");
    setHistorySummaryText("historyFeedbackText", "Loading...");
    renderSubmissionHistoryLoading();
    renderVersionHistoryLoading();
    renderHistoryInspectorEmpty("Loading version details...");
}

async function loadHistoryModalData(resourceId) {
    const [detailResult, submissionResult, versionResult] = await Promise.allSettled([
        requestJson(`${API_BASE}/${resourceId}`, { method: "GET" }),
        requestJson(`${API_BASE}/${resourceId}/submissions`, { method: "GET" }),
        requestJson(`${API_BASE}/${resourceId}/versions`, { method: "GET" })
    ]);

    if (detailResult.status !== "fulfilled") {
        throw detailResult.reason;
    }

    const detail = detailResult.value || {};
    historyModalState = {
        resourceId: String(resourceId),
        title: detail.title || "-",
        currentVersionNo: detail.currentVersionNo ?? null,
        currentStatus: detail.status || "",
        latestFeedbackComment: detail.latestFeedbackComment || ""
    };

    renderHistorySummary(detail);
    renderHistoryInspectorEmpty("Select View or Compare with Current to inspect a version snapshot.");

    if (submissionResult.status === "fulfilled") {
        renderSubmissionHistory(submissionResult.value || []);
    } else {
        renderSubmissionHistoryError(submissionResult.reason?.message || "Failed to load submission history.");
    }

    if (versionResult.status === "fulfilled") {
        renderVersionHistory(versionResult.value || []);
    } else {
        renderVersionHistoryError(versionResult.reason?.message || "Failed to load version history.");
    }
}

function renderHistorySummary(detail) {
    setHistorySummaryText("historyResourceTitle", detail.title || "Untitled Resource");
    setHistorySummaryText("historyResourceStatus", formatStatus(detail.status || "-"));
    setHistorySummaryText(
        "historyResourceVersion",
        detail.currentVersionNo != null ? `V${detail.currentVersionNo}` : "No version record"
    );
    setHistorySummaryText(
        "historySubmittedAt",
        detail.latestSubmittedAt ? formatDateTime(detail.latestSubmittedAt) : "Not submitted yet"
    );

    const feedbackParts = [];
    if (detail.latestReviewStatus) {
        feedbackParts.push(`Latest review: ${formatStatus(detail.latestReviewStatus)}`);
    }
    if (detail.latestFeedbackComment) {
        feedbackParts.push(detail.latestFeedbackComment);
    }

    setHistorySummaryText(
        "historyFeedbackText",
        feedbackParts.length ? feedbackParts.join(" | ") : "No review feedback available."
    );
}

function renderSubmissionHistoryLoading() {
    const tbody = document.getElementById("submissionHistoryBody");
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="4" class="empty-row">Loading submission history...</td></tr>`;
}

function renderSubmissionHistory(items) {
    const tbody = document.getElementById("submissionHistoryBody");
    if (!tbody) return;

    if (!items || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" class="empty-row">No submission history yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = items.map(item => `
        <tr>
            <td>${item.versionNo != null ? `V${escapeHtml(String(item.versionNo))}` : "-"}</td>
            <td>${formatDateTime(item.submittedAt)}</td>
            <td>${escapeHtml(item.submissionNote || "—")}</td>
            <td>${escapeHtml(formatStatus(item.statusSnapshot || "-"))}</td>
        </tr>
    `).join("");
}

function renderSubmissionHistoryError(message) {
    const tbody = document.getElementById("submissionHistoryBody");
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="4" class="empty-row">${escapeHtml(message)}</td></tr>`;
}

function renderVersionHistoryLoading() {
    const tbody = document.getElementById("versionHistoryBody");
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="5" class="empty-row">Loading version history...</td></tr>`;
}

function renderVersionHistory(items) {
    const tbody = document.getElementById("versionHistoryBody");
    if (!tbody) return;

    if (!items || items.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty-row">No version history yet.</td></tr>`;
        return;
    }

    const canRestore = isHistoryResourceEditable();
    const currentVersionNo = historyModalState.currentVersionNo;

    tbody.innerHTML = items.map(item => {
        const versionNo = item.versionNo != null ? Number(item.versionNo) : null;
        const restoreDisabled = !canRestore || versionNo === currentVersionNo;

        return `
            <tr>
                <td>${versionNo != null ? `V${escapeHtml(String(versionNo))}` : "-"}</td>
                <td>${escapeHtml(formatStatus(item.changeType || "-"))}</td>
                <td>${escapeHtml(item.changeSummary || "—")}</td>
                <td>${formatDateTime(item.createdAt)}</td>
                <td>
                    <div class="action-group">
                        <button
                            type="button"
                            class="action-link action-button"
                            data-version-action="view"
                            data-resource-id="${escapeHtml(historyModalState.resourceId)}"
                            data-version-no="${escapeHtml(String(versionNo ?? ""))}">
                            View
                        </button>
                        <button
                            type="button"
                            class="action-link action-button"
                            data-version-action="compare"
                            data-resource-id="${escapeHtml(historyModalState.resourceId)}"
                            data-version-no="${escapeHtml(String(versionNo ?? ""))}">
                            Compare
                        </button>
                        <button
                            type="button"
                            class="action-link action-button${restoreDisabled ? " action-button-disabled" : ""}"
                            data-version-action="restore"
                            data-resource-id="${escapeHtml(historyModalState.resourceId)}"
                            data-version-no="${escapeHtml(String(versionNo ?? ""))}"
                            ${restoreDisabled ? "disabled" : ""}>
                            Restore
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

function renderVersionHistoryError(message) {
    const tbody = document.getElementById("versionHistoryBody");
    if (!tbody) return;

    tbody.innerHTML = `<tr><td colspan="5" class="empty-row">${escapeHtml(message)}</td></tr>`;
}

async function viewVersionSnapshot(resourceId, versionNo) {
    renderHistoryInspectorEmpty("Loading version snapshot...");

    try {
        const version = await requestJson(`${API_BASE}/${resourceId}/versions/${versionNo}`, { method: "GET" });
        renderVersionSnapshot(version);
    } catch (error) {
        renderHistoryInspectorEmpty(error.message || "Failed to load version snapshot.");
        showToast(error.message || "Failed to load version snapshot.");
    }
}

async function compareVersionWithCurrent(resourceId, versionNo) {
    if (historyModalState.currentVersionNo == null) {
        renderHistoryInspectorEmpty("Current version is unavailable, so comparison cannot be displayed.");
        return;
    }

    renderHistoryInspectorEmpty("Comparing versions...");

    try {
        const compare = await requestJson(
            `${API_BASE}/${resourceId}/versions/compare?v1=${encodeURIComponent(versionNo)}&v2=${encodeURIComponent(historyModalState.currentVersionNo)}`,
            { method: "GET" }
        );
        renderVersionCompare(compare);
    } catch (error) {
        renderHistoryInspectorEmpty(error.message || "Failed to compare versions.");
        showToast(error.message || "Failed to compare versions.");
    }
}

async function rollbackToVersion(resourceId, versionNo) {
    if (!isHistoryResourceEditable()) {
        showToast("Only Draft or Rejected resources can be restored.");
        return;
    }

    const confirmed = window.confirm(
        `Restore metadata from version V${versionNo} into the current resource? Current editable content will be overwritten.`
    );
    if (!confirmed) {
        return;
    }

    renderHistoryInspectorEmpty(`Restoring version V${versionNo}...`);

    try {
        await requestJson(`${API_BASE}/${resourceId}/versions/${versionNo}/rollback`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ confirmed: true })
        });

        showToast(`Version V${versionNo} restored successfully.`);
        await loadResourceList();
        await loadHistoryModalData(resourceId);
    } catch (error) {
        renderHistoryInspectorEmpty(error.message || "Failed to restore version.");
        showToast(error.message || "Failed to restore version.");
    }
}

function renderVersionSnapshot(version) {
    const inspectorMeta = document.getElementById("historyInspectorMeta");
    const inspectorBody = document.getElementById("historyInspectorBody");
    if (!inspectorBody) return;

    if (inspectorMeta) {
        inspectorMeta.textContent = `Viewing snapshot for version V${version.versionNo ?? "-"}.`;
    }

    const snapshot = version.snapshotMap || {};
    const rows = [
        ["Title", snapshot.title],
        ["Description", snapshot.description],
        ["Copyright", snapshot.copyright],
        ["Category", snapshot.categoryName || formatSnapshotCategory(snapshot)],
        ["Place", snapshot.place],
        ["Resource Type", formatResourceType(snapshot.resourceType)],
        ["Preview Image", snapshot.previewImage],
        ["Media File", snapshot.mediaUrl],
        ["Tags", formatSnapshotTagNames(snapshot.tagNames)]
    ];

    inspectorBody.innerHTML = `
        <div class="history-inspector-grid">
            ${rows.map(([label, value]) => `
                <div class="history-inspector-row">
                    <span class="history-inspector-label">${escapeHtml(label)}</span>
                    <span class="history-inspector-value">${escapeHtml(value || "-")}</span>
                </div>
            `).join("")}
        </div>
    `;
}

function renderVersionCompare(compare) {
    const inspectorMeta = document.getElementById("historyInspectorMeta");
    const inspectorBody = document.getElementById("historyInspectorBody");
    if (!inspectorBody) return;

    if (inspectorMeta) {
        inspectorMeta.textContent = `Comparing version V${compare.leftVersionNo ?? "-"} with current version V${compare.rightVersionNo ?? "-"}.`;
    }

    const diffItems = Array.isArray(compare.diffItems) ? compare.diffItems : [];
    if (!diffItems.length) {
        inspectorBody.innerHTML = `<div class="empty-row history-inspector-empty">No compare data is available.</div>`;
        return;
    }

    inspectorBody.innerHTML = `
        <div class="table-wrap">
            <table class="data-table history-compare-table">
                <thead>
                <tr>
                    <th>Field</th>
                    <th>Version V${escapeHtml(String(compare.leftVersionNo ?? "-"))}</th>
                    <th>Current V${escapeHtml(String(compare.rightVersionNo ?? "-"))}</th>
                </tr>
                </thead>
                <tbody>
                ${diffItems.map(item => {
                    const leftValue = item.fieldName === "resourceType"
                        ? formatResourceType(item.leftValue)
                        : item.leftValue;
                    const rightValue = item.fieldName === "resourceType"
                        ? formatResourceType(item.rightValue)
                        : item.rightValue;

                    return `
                        <tr class="${item.changed ? "history-diff-row" : ""}">
                            <td>${escapeHtml(item.fieldLabel || item.fieldName || "-")}</td>
                            <td>${escapeHtml(leftValue || "-")}</td>
                            <td>${escapeHtml(rightValue || "-")}</td>
                        </tr>
                    `;
                }).join("")}
                </tbody>
            </table>
        </div>
    `;
}

function renderHistoryInspectorEmpty(message) {
    const inspectorMeta = document.getElementById("historyInspectorMeta");
    const inspectorBody = document.getElementById("historyInspectorBody");
    if (!inspectorBody) return;

    if (inspectorMeta) {
        inspectorMeta.textContent = "Select View or Compare with Current to inspect a version snapshot.";
    }

    inspectorBody.innerHTML = `<div class="empty-row history-inspector-empty">${escapeHtml(message)}</div>`;
}

function renderHistoryLoadFailure(message) {
    setHistorySummaryText("historyResourceTitle", "Load failed");
    setHistorySummaryText("historyResourceStatus", "Unavailable");
    setHistorySummaryText("historyResourceVersion", "Unavailable");
    setHistorySummaryText("historySubmittedAt", "Unavailable");
    setHistorySummaryText("historyFeedbackText", message);
    renderSubmissionHistoryError(message);
    renderVersionHistoryError(message);
    renderHistoryInspectorEmpty(message);
}

function setHistorySummaryText(elementId, value) {
    const element = document.getElementById(elementId);
    if (!element) return;
    element.textContent = value ?? "";
}

function isHistoryResourceEditable() {
    return historyModalState.currentStatus === "Draft" || historyModalState.currentStatus === "Rejected";
}

function formatSnapshotCategory(snapshot) {
    if (snapshot.categoryId == null || snapshot.categoryId === "") {
        return "-";
    }
    return `Category #${snapshot.categoryId}`;
}

function formatSnapshotTagNames(tagNames) {
    if (!Array.isArray(tagNames) || !tagNames.length) {
        return "-";
    }
    return tagNames.join(", ");
}

function createEmptyHistoryState() {
    return {
        resourceId: "",
        title: "",
        currentVersionNo: null,
        currentStatus: "",
        latestFeedbackComment: ""
    };
}

function bindMetadataForm() {
    const form = document.getElementById("metadataForm");
    if (!form) return;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const resourceId = getResourceIdFromQuery();
        if (!resourceId) {
            showToast("Please create a draft first.");
            return;
        }

        const payload = {
            title: document.getElementById("title").value.trim(),
            copyright: document.getElementById("copyright").value.trim(),
            categoryId: parseNullableLong(document.getElementById("categoryId").value),
            place: document.getElementById("place").value.trim(),
            description: document.getElementById("description").value.trim(),
            resourceType: document.getElementById("resourceType")?.value || null,
            tagNames: getSelectedTagNames()
        };

        try {
            const detail = await requestJson(`${API_BASE}/${resourceId}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            fillEditor(detail);
            showToast("Metadata saved successfully.");
        } catch (error) {
            showToast(error.message || "Failed to save metadata.");
        }
    });
}

function bindUploadForm() {
    const form = document.getElementById("uploadForm");
    if (!form) return;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const resourceId = getResourceIdFromQuery();
        if (!resourceId) {
            showToast("Please create a draft first.");
            return;
        }

        const previewImage = document.getElementById("previewImage").files[0];
        const mediaFile = document.getElementById("mediaFile").files[0];

        if (!previewImage && !mediaFile) {
            showToast("Select at least one file.");
            return;
        }

        const formData = new FormData();
        if (previewImage) formData.append("previewImage", previewImage);
        if (mediaFile) formData.append("mediaFile", mediaFile);

        try {
            try {
                await pendingResourceTypeSave;
            } catch (error) {
                pendingResourceTypeSave = Promise.resolve();
            }

            await persistResourceTypeSelection({ showError: false });

            const detail = await requestJson(`${API_BASE}/${resourceId}/files`, {
                method: "POST",
                body: formData
            });

            fillEditor(detail);
            showToast("Files uploaded successfully.");
        } catch (error) {
            showToast(error.message || "Failed to upload files.");
        }
    });
}

function bindSubmitForm() {
    const form = document.getElementById("submitForm");
    if (!form) return;

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const resourceId = getResourceIdFromQuery();
        if (!resourceId) {
            showToast("Please create a draft first.");
            return;
        }

        const payload = {
            submissionNote: document.getElementById("submissionNote").value.trim()
        };

        try {
            await requestJson(`${API_BASE}/${resourceId}/submit`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            showToast("Submitted for review.");
            await loadResourceDetail(resourceId);
        } catch (error) {
            showToast(error.message || "Failed to submit resource.");
        }
    });
}

async function loadResourceDetail(resourceId) {
    try {
        const detail = await requestJson(`${API_BASE}/${resourceId}`, { method: "GET" });
        fillEditor(detail);
    } catch (error) {
        showToast(error.message || "Failed to load resource detail.");
    }
}

function fillEditor(detail) {
    if (!detail) return;

    setValue("title", detail.title);
    setValue("copyright", detail.copyright);
    setValue("categoryId", detail.categoryId);
    setValue("place", detail.place);
    setValue("description", detail.description);
    setValue("resourceType", normalizeResourceTypeValue(detail.resourceType));
    setValue("submissionNote", "");
    setTagValues(detail.tagNames || []);

    const previewText = document.getElementById("previewImagePathText");
    const mediaText = document.getElementById("mediaFilePathText");

    if (previewText) {
        previewText.textContent = detail.previewImage || "—";
    }

    if (mediaText) {
        mediaText.textContent = detail.mediaUrl || "—";
    }

    syncResourceTypeMirror();
    updateMediaFileAccept(detail.resourceType);
    savedResourceTypeValue = normalizeResourceTypeValue(detail.resourceType);
    updateEditorMeta(detail);
}

function updateEditorMeta(detail) {
    const resourceIdText = document.getElementById("resourceIdText");
    const resourceStatusText = document.getElementById("resourceStatusText");
    const updatedAtText = document.getElementById("updatedAtText");
    const badge = document.getElementById("resourceStatusBadge");

    const idValue = detail?.id ?? getResourceIdFromQuery() ?? "Not created";
    const statusValue = detail?.status ?? "Draft";
    const updatedValue = detail?.updatedAt ? formatDateTime(detail.updatedAt) : "—";

    if (resourceIdText) resourceIdText.textContent = idValue;
    if (resourceStatusText) resourceStatusText.textContent = formatStatus(statusValue);
    if (updatedAtText) updatedAtText.textContent = updatedValue;

    if (badge) {
        badge.textContent = formatStatus(statusValue);
        badge.className = `status-pill status-${toStatusClassName(statusValue)}`;
    }
}

function getInitialModuleId() {
    const hashValue = window.location.hash.replace("#", "");
    if (hashValue && HERITAGE_MODULE_META[hashValue]) {
        return hashValue;
    }
    return "module-identity";
}

function getModuleIdFromTrigger(trigger) {
    const targetId = trigger?.dataset?.target;
    if (targetId) return targetId;

    const href = trigger?.getAttribute?.("href") || "";
    if (href.startsWith("#")) {
        return href.slice(1);
    }

    return "";
}

function activateHeritageModule(moduleId, options = {}) {
    if (!moduleId) return;

    const target = document.getElementById(moduleId);
    if (!target) return;

    const activeForm = target.closest("form");

    document.querySelectorAll(".module-stack").forEach(form => {
        form.classList.toggle("active-workspace", form === activeForm);
    });

    document.querySelectorAll(".module-card").forEach(card => {
        const isActive = card === target;
        card.classList.toggle("is-active", isActive);
        card.setAttribute("aria-hidden", isActive ? "false" : "true");
    });

    document.querySelectorAll(".heritage-node, .module-pill").forEach(trigger => {
        const isActive = getModuleIdFromTrigger(trigger) === moduleId;
        trigger.classList.toggle("is-active", isActive);
        trigger.setAttribute("aria-current", isActive ? "true" : "false");
    });

    updateActiveModuleMeta(moduleId);
    highlightModuleCard(target);

    if (options.shouldScroll) {
        target.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    if (options.shouldFocus) {
        focusModuleField(moduleId);
    }

    if (options.updateHistory !== false && window.history?.replaceState) {
        window.history.replaceState(null, "", `#${moduleId}`);
    }
}

function openHeritageModuleModal(moduleId, options = {}) {
    if (!moduleId) return;

    document.body.classList.add("module-modal-open");
    activateHeritageModule(moduleId, {
        shouldScroll: false,
        shouldFocus: options.shouldFocus !== false,
        updateHistory: options.updateHistory
    });
}

function closeHeritageModuleModal() {
    document.body.classList.remove("module-modal-open");
    clearHeritageModuleSelection();
    resetActiveModuleMeta();

    if (window.history?.replaceState) {
        const cleanUrl = `${window.location.pathname}${window.location.search}`;
        window.history.replaceState(null, "", cleanUrl);
    }
}

function highlightModuleCard(target) {
    if (!target?.classList) return;

    document.querySelectorAll(".module-card.module-focus").forEach(card => {
        card.classList.remove("module-focus");
    });

    target.classList.add("module-focus");

    window.clearTimeout(highlightModuleCard._timer);
    highlightModuleCard._timer = window.setTimeout(() => {
        target.classList.remove("module-focus");
    }, 1800);
}

function clearHeritageModuleSelection() {
    document.querySelectorAll(".module-stack").forEach(form => {
        form.classList.remove("active-workspace");
    });

    document.querySelectorAll(".module-card").forEach(card => {
        card.classList.remove("is-active", "module-focus");
        card.setAttribute("aria-hidden", "true");
    });

    document.querySelectorAll(".heritage-node, .module-pill").forEach(trigger => {
        trigger.classList.remove("is-active");
        trigger.setAttribute("aria-current", "false");
    });
}

function updateActiveModuleMeta(moduleId) {
    const meta = HERITAGE_MODULE_META[moduleId];
    if (!meta) return;

    const label = document.getElementById("activeModuleLabel");
    const hint = document.getElementById("activeModuleHint");

    if (label) {
        label.textContent = meta.label;
    }

    if (hint) {
        hint.textContent = meta.hint;
    }
}

function resetActiveModuleMeta() {
    const label = document.getElementById("activeModuleLabel");
    const hint = document.getElementById("activeModuleHint");

    if (label) {
        label.textContent = "Click an icon to begin";
    }

    if (hint) {
        hint.textContent = "Select any icon on the scroll to open the matching entry panel.";
    }
}

function focusModuleField(moduleId) {
    const meta = HERITAGE_MODULE_META[moduleId];
    if (!meta) return;

    window.setTimeout(() => {
        let element = document.getElementById(meta.focusId);

        if (moduleId === "module-tags") {
            element = document.getElementById("tagInput") || element;
        }

        if (!element) return;

        if (typeof element.focus === "function") {
            element.focus({ preventScroll: true });
        }
    }, 420);
}

function syncResourceTypeMirror() {
    const primarySelect = document.getElementById("resourceType");
    const mirrorSelect = document.getElementById("resourceTypeMirror");
    if (!primarySelect || !mirrorSelect) return;

    mirrorSelect.value = primarySelect.value;
}

function updateMediaFileAccept(resourceType) {
    const mediaInput = document.getElementById("mediaFile");
    if (!mediaInput) return;

    const normalizedResourceType = normalizeResourceTypeValue(resourceType);
    switch (normalizedResourceType.toLowerCase()) {
        case "photo":
            mediaInput.accept = "image/*";
            break;
        case "video":
            mediaInput.accept = "video/*";
            break;
        case "audio":
            mediaInput.accept = "audio/*";
            break;
        case "document":
            mediaInput.accept = ".pdf,.doc,.docx";
            break;
        default:
            mediaInput.accept = "";
            break;
    }
}

async function persistResourceTypeSelection(options = {}) {
    const primarySelect = document.getElementById("resourceType");
    const mirrorSelect = document.getElementById("resourceTypeMirror");
    const resourceId = getResourceIdFromQuery();

    if (!primarySelect || !mirrorSelect || !resourceId) {
        return null;
    }

    const currentValue = mirrorSelect.value ?? "";
    primarySelect.value = currentValue;
    updateMediaFileAccept(currentValue);

    if (!currentValue) {
        return null;
    }

    if (!options.force && currentValue === savedResourceTypeValue) {
        return null;
    }

    try {
        const detail = await requestJson(`${API_BASE}/${resourceId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                resourceType: currentValue
            })
        });

        if (detail) {
            fillEditor(detail);
        } else {
            savedResourceTypeValue = currentValue;
        }

        return detail;
    } catch (error) {
        if (options.showError !== false) {
            showToast(error.message || "Failed to save resource type.");
        }
        throw error;
    }
}

function renderSelectOptions(selectElement, options, placeholderText) {
    const currentValue = selectElement.value;
    const html = [`<option value="">${placeholderText}</option>`];

    normalizeCategoryOptions(options).forEach(option => {
        html.push(`<option value="${escapeHtml(String(option.id))}">${escapeHtml(option.name)}</option>`);
    });

    selectElement.innerHTML = html.join("");

    if (currentValue) {
        selectElement.value = currentValue;
    }
}

function commitTagInputValue(input) {
    if (!input) return;

    const nextValues = normalizeTagNames([input.value]);
    if (nextValues.length === 0) {
        input.value = "";
        return;
    }

    setTagValues([...tagDraftValues, ...nextValues]);
    input.value = "";
}

function setTagValues(values) {
    tagDraftValues = normalizeTagNames(values);
    renderTagChips(document.getElementById("tagOptions"), tagDraftValues);
}

function renderTagChips(container, tagNames) {
    if (!container) return;

    if (!tagNames || tagNames.length === 0) {
        container.innerHTML = `<div class="path-text">No tags added yet.</div>`;
        return;
    }

    container.innerHTML = tagNames.map(tagName => `
        <button
            type="button"
            class="chip active"
            data-tag-name="${escapeHtml(tagName)}"
            aria-label="Remove tag ${escapeHtml(tagName)}">
            ${escapeHtml(tagName)} ×
        </button>
    `).join("");

    container.querySelectorAll(".chip").forEach(chip => {
        chip.addEventListener("click", () => {
            const targetTagName = chip.dataset.tagName || "";
            setTagValues(tagDraftValues.filter(tagName => tagName.toLowerCase() !== targetTagName.toLowerCase()));
        });
    });
}

function getSelectedTagNames() {
    return [...tagDraftValues];
}

function normalizeTagNames(values) {
    const normalizedValues = [];
    const seen = new Set();

    (values || []).forEach(value => {
        String(value ?? "")
            .split(",")
            .map(item => item.trim())
            .filter(Boolean)
            .forEach(item => {
                const normalizedKey = item.toLowerCase();
                if (seen.has(normalizedKey)) {
                    return;
                }

                seen.add(normalizedKey);
                normalizedValues.push(item);
            });
    });

    return normalizedValues;
}

async function getCategoryOptions() {
    const options = normalizeCategoryOptions(await requestJson(`${API_BASE}/category-options`, { method: "GET" }));
    if (!options.length) {
        throw new Error("No active categories are available.");
    }

    categoryOptionCache = options;
    return categoryOptionCache;
}

function normalizeCategoryOptions(options) {
    if (!Array.isArray(options)) {
        return [];
    }

    return options
        .map((option, index) => normalizeCategoryOption(option, index))
        .filter(Boolean);
}

function normalizeCategoryOption(option, index) {
    if (option === null || option === undefined) {
        return null;
    }

    if (typeof option === "string") {
        const text = option.trim();
        if (!text) {
            return null;
        }
        return {
            id: index + 1,
            name: text
        };
    }

    const name = String(
        option.name
        ?? option.categoryTopic
        ?? option.topic
        ?? option.label
        ?? ""
    ).trim();

    const id = option.id
        ?? option.categoryId
        ?? option.value
        ?? null;

    if (!name || id === null || id === undefined || id === "") {
        return null;
    }

    return {
        id,
        name
    };
}

async function ensureContributorWorkspaceAccess() {
    try {
        const currentUser = await requestJson(`${AUTH_API_BASE}/me`, { method: "GET" });
        if (!currentUser?.contributor) {
            window.location.href = `./module6/heritage-viewer.html?message=${encodeURIComponent("Approved contributor access is required for the editor.")}`;
            throw new Error("Contributor access required.");
        }
        return currentUser;
    } catch (error) {
        if (error.status === 401) {
            const next = `${window.location.pathname}${window.location.search}`;
            window.location.href = `./login.html?next=${encodeURIComponent(next)}`;
            throw error;
        }

        if (error.status === 403) {
            window.location.href = `./module6/heritage-viewer.html?message=${encodeURIComponent(error.message || "Approved contributor access is required for the editor.")}`;
            throw error;
        }

        throw error;
    }
}

function populateWorkspaceSession(currentUser) {
    const nameNode = document.getElementById("resourceUserName");
    const accessNode = document.getElementById("resourceAccessText");

    if (nameNode) {
        nameNode.textContent = currentUser?.name || "Contributor";
    }

    if (accessNode) {
        accessNode.textContent = currentUser?.contributor
            ? "Approved contributor access is active for this workspace."
            : "Contributor permission is required for this workspace.";
    }
}

function getResourceIdFromQuery() {
    const params = new URLSearchParams(window.location.search);
    return params.get("id");
}

function parseNullableLong(value) {
    if (value === null || value === undefined || value === "") {
        return null;
    }
    return Number(value);
}

function formatStatus(status) {
    if (!status) return "-";
    return status
        .toString()
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, char => char.toUpperCase());
}

function formatResourceType(value) {
    const resourceType = normalizeResourceTypeValue(value);

    switch (resourceType.toLowerCase()) {
        case "photo":
            return "Photo/Image";
        case "video":
            return "Video";
        case "audio":
            return "Audio";
        case "document":
            return "File/Document";
        case "extra link":
            return "Extra Link";
        case "other":
            return "Other";
        default:
            return resourceType || "-";
    }
}

function normalizeResourceTypeValue(value) {
    const resourceType = String(value ?? "").trim().toLowerCase();

    switch (resourceType) {
        case "photo":
        case "picture":
        case "image":
        case "photo/image":
        case "photo_image":
            return "photo";
        case "video":
            return "video";
        case "audio":
            return "audio";
        case "document":
        case "file/document":
        case "file_document":
            return "document";
        case "extra link":
        case "extra_link":
            return "extra link";
        case "other":
            return "other";
        default:
            return String(value ?? "").trim();
    }
}

function toStatusClassName(status) {
    return String(status ?? "")
        .trim()
        .toLowerCase()
        .replaceAll(" ", "_");
}

function setEditorCategoryAvailability(isAvailable) {
    setActionDisabled("metadataSaveBtn", !isAvailable);
    setActionDisabled("submitReviewBtn", !isAvailable);

    if (isAvailable) {
        clearEditorAlert();
        return;
    }

    showEditorAlert("Categories failed to load. Metadata save and submit are temporarily disabled.");
}

function setActionDisabled(buttonId, disabled) {
    const button = document.getElementById(buttonId);
    if (!button) return;
    button.disabled = disabled;
}

function showEditorAlert(message) {
    const alert = document.getElementById("editorAlert");
    if (!alert) return;

    alert.textContent = message;
    alert.classList.remove("hidden");
}

function clearEditorAlert() {
    const alert = document.getElementById("editorAlert");
    if (!alert) return;

    alert.textContent = "";
    alert.classList.add("hidden");
}
