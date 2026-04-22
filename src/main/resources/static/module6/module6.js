const VIEWER_AUTH_API_BASE = "/api/auth";
const VIEWER_API_BASE = "/api/viewer/resources";
const VIEWER_FEEDBACK_API_BASE = "/api/viewer/feedback";
const {
    requestJson: viewerRequestJson,
    showMessageFromQuery: showViewerMessageFromQuery,
    showToast: showViewerToast,
    escapeHtml: escapeViewerHtml,
    formatDateTime: formatViewerDateTime
} = window.SharedApp;

let viewerCurrentUser = null;
let viewerCategoryOptions = [];
const VIEWER_CATEGORY_BADGE_ASSETS = {
    places: "./assets/category-places.png",
    traditions: "./assets/category-traditions.png",
    stories: "./assets/category-stories.png",
    objects: "./assets/category-objects.png",
    "educational materials": "./assets/category-education.png",
    education: "./assets/category-education.png"
};

document.addEventListener("DOMContentLoaded", async () => {
    const page = document.body.dataset.page;
    showViewerMessageFromQuery({ duration: 2600 });

    if (page === "heritage-viewer") {
        await initHeritageViewerPage();
    }

    if (page === "viewer-detail") {
        await initViewerDetailPage();
    }
});

async function initHeritageViewerPage() {
    bindViewerBackButton("backBtn");
    bindViewerAccountButton("accountBtn", "../account.html");
    bindViewerSearchControls();

    try {
        await ensureViewerAuthenticated();
        await loadViewerCategoryOptions();
        await loadApprovedResources();
    } catch (error) {
        handleViewerError(error, "Unable to load approved resources right now.");
    }
}

async function initViewerDetailPage() {
    bindViewerDetailForms();

    try {
        await ensureViewerAuthenticated();

        try {
            await loadViewerCategoryOptions();
        } catch (error) {
            console.warn("Viewer category options failed to load.", error);
        }

        await loadApprovedResourceDetail();
        await loadViewerSupplementaryPanels();
    } catch (error) {
        handleViewerError(error, "Unable to load the approved resource detail.");
    }
}

function bindViewerAccountButton(buttonId, href) {
    const accountButton = document.getElementById(buttonId);
    if (!accountButton) return;
    accountButton.addEventListener("click", () => {
        window.location.href = href;
    });
}

function bindViewerBackButton(buttonId) {
    const backButton = document.getElementById(buttonId);
    if (!backButton) return;

    backButton.addEventListener("click", () => {
        const referrer = document.referrer || "";

        if (referrer) {
            try {
                const referrerUrl = new URL(referrer);
                const isSameOrigin = referrerUrl.origin === window.location.origin;
                const isCurrentPage = referrerUrl.pathname === window.location.pathname
                    && referrerUrl.search === window.location.search;

                if (isSameOrigin && !isCurrentPage) {
                    window.location.href = referrerUrl.href;
                    return;
                }
            } catch (error) {
                console.warn("Unable to parse viewer referrer.", error);
            }
        }

        window.history.back();
    });
}

function bindViewerSearchControls() {
    const searchButton = document.getElementById("searchBtn");
    const loadAllButton = document.getElementById("loadAllBtn");
    const keywordInput = document.getElementById("keyword");
    const resourceList = document.getElementById("resourceList");

    if (searchButton) {
        searchButton.addEventListener("click", () => {
            void loadApprovedResources();
        });
    }

    if (loadAllButton) {
        loadAllButton.addEventListener("click", () => {
            resetViewerFilters();
            void loadApprovedResources();
        });
    }

    if (keywordInput) {
        keywordInput.addEventListener("keydown", event => {
            if (event.key === "Enter") {
                event.preventDefault();
                void loadApprovedResources();
            }
        });
    }

    if (resourceList) {
        resourceList.addEventListener("click", event => {
            const button = event.target.closest("[data-resource-id]");
            if (!button) return;
            window.location.href = `./viewer-detail.html?id=${encodeURIComponent(button.dataset.resourceId)}`;
        });
    }
}

function bindViewerDetailForms() {
    const commentForm = document.getElementById("commentForm");
    if (commentForm) {
        commentForm.addEventListener("submit", async event => {
            event.preventDefault();
            await submitViewerComment();
        });
    }

    const feedbackForm = document.getElementById("feedbackForm");
    if (feedbackForm) {
        feedbackForm.addEventListener("submit", async event => {
            event.preventDefault();
            await submitViewerFeedback();
        });
    }

    const feedbackFiles = document.getElementById("feedbackFiles");
    if (feedbackFiles) {
        feedbackFiles.addEventListener("change", updateViewerFeedbackFileText);
    }
}

async function ensureViewerAuthenticated() {
    if (viewerCurrentUser) {
        return viewerCurrentUser;
    }

    try {
        viewerCurrentUser = await viewerRequestJson(`${VIEWER_AUTH_API_BASE}/me`, { method: "GET" });
        return viewerCurrentUser;
    } catch (error) {
        if (error.status === 401) {
            redirectViewerToLogin();
        }
        throw error;
    }
}

async function loadViewerCategoryOptions() {
    viewerCategoryOptions = await viewerRequestJson(`${VIEWER_API_BASE}/category-options`, { method: "GET" });

    const select = document.getElementById("categoryId");
    if (!select) return;

    const currentValue = select.value;
    select.innerHTML = '<option value="">Select category</option>';

    viewerCategoryOptions.forEach(option => {
        const optionElement = document.createElement("option");
        optionElement.value = String(option.id);
        optionElement.textContent = capitalizeViewerLabel(option.name);
        select.appendChild(optionElement);
    });

    if (currentValue) {
        select.value = currentValue;
    }
}

async function loadApprovedResources() {
    const resourceList = document.getElementById("resourceList");
    if (!resourceList) return;

    resourceList.innerHTML = '<div class="viewer-empty-message">Loading approved resources...</div>';

    const params = new URLSearchParams();
    const keyword = document.getElementById("keyword")?.value.trim();
    const type = document.getElementById("type")?.value;
    const categoryId = document.getElementById("categoryId")?.value;
    const sortBy = document.getElementById("sortBy")?.value;

    if (keyword) params.set("keyword", keyword);
    if (type) params.set("type", type);
    if (categoryId) params.set("categoryId", categoryId);
    if (sortBy) params.set("sortBy", sortBy);

    const url = params.toString() ? `${VIEWER_API_BASE}?${params}` : VIEWER_API_BASE;
    const resources = await viewerRequestJson(url, { method: "GET" });
    renderApprovedResources(resources || []);
}

function renderApprovedResources(resources) {
    const resourceList = document.getElementById("resourceList");
    if (!resourceList) return;

    if (!resources.length) {
        resourceList.innerHTML = '<div class="viewer-empty-message">No matched approved resources.</div>';
        return;
    }

    resourceList.innerHTML = resources.map(resource => {
        const preview = resource.previewImage
                ? `<img class="viewer-resource-cover" src="${escapeViewerHtml(toPublicMediaUrl(resource.previewImage))}" alt="${escapeViewerHtml(resource.title || "Resource preview")}" />`
                : "";
        const description = buildViewerExcerpt(resource.description, 120);
        const resolvedCategoryName = resource.categoryName || resolveCategoryName(resource.categoryId);
        const categoryName = escapeViewerHtml(capitalizeViewerLabel(resolvedCategoryName));
        const categoryBadge = buildViewerCategoryBadge(resolvedCategoryName);
        const resourceType = escapeViewerHtml(formatViewerResourceType(resource.resourceType));
        const updatedAt = escapeViewerHtml(formatViewerDateTime(resource.updatedAt, { emptyText: "-" }));

        return `
            <article class="viewer-resource-card">
                ${categoryBadge}
                ${preview}
                <div class="viewer-resource-copy">
                    <h3 class="viewer-resource-title">${escapeViewerHtml(resource.title || "Untitled resource")}</h3>
                    <p class="viewer-resource-summary">${escapeViewerHtml(description)}</p>
                </div>
                <div class="viewer-resource-meta">
                    <span class="viewer-meta-pill">${categoryName}</span>
                    <span class="viewer-meta-pill">${resourceType}</span>
                </div>
                <div class="viewer-resource-footer">
                    <span class="viewer-updated-at">Updated ${updatedAt}</span>
                    <button type="button" class="btn btn-secondary" data-resource-id="${escapeViewerHtml(resource.id)}">View Detail</button>
                </div>
            </article>
        `;
    }).join("");
}

function buildViewerCategoryBadge(categoryName) {
    const categoryKey = normalizeViewerCategoryKey(categoryName);
    const badgeSrc = VIEWER_CATEGORY_BADGE_ASSETS[categoryKey];

    if (!badgeSrc) {
        return "";
    }

    return `
        <div class="viewer-resource-badge" aria-hidden="true">
            <img class="viewer-category-icon" src="${escapeViewerHtml(badgeSrc)}" alt="" />
        </div>
    `;
}

async function loadApprovedResourceDetail() {
    const resourceId = getViewerResourceIdFromQuery();
    if (!resourceId) {
        throw new Error("Resource id is required.");
    }

    const detail = await viewerRequestJson(`${VIEWER_API_BASE}/${resourceId}`, { method: "GET" });
    renderApprovedResourceDetail(detail);
    return detail;
}

function renderApprovedResourceDetail(detail) {
    const detailContent = document.getElementById("detailContent");
    const detailError = document.getElementById("detailError");
    if (!detailContent) return;

    if (detailError) {
        detailError.classList.add("hidden");
        detailError.textContent = "";
    }

    document.getElementById("detailTitle").textContent = detail.title || "Approved resource detail";
    document.getElementById("detailSubtitle").textContent = `Browse the published information for resource #${detail.id ?? "-"}.`;
    document.getElementById("detailId").textContent = detail.id ?? "-";
    document.getElementById("detailType").textContent = formatViewerResourceType(detail.resourceType);
    document.getElementById("detailCategory").textContent =
        capitalizeViewerLabel(detail.categoryName || resolveCategoryName(detail.categoryId));
    document.getElementById("detailPlace").textContent = detail.place || "-";
    document.getElementById("detailReviewedAt").textContent = formatViewerDateTime(detail.reviewedAt, { emptyText: "-" });
    document.getElementById("detailCopyright").textContent = detail.copyright || "-";
    document.getElementById("detailDescription").textContent = detail.description || "-";

    renderViewerTags(detail.tagNames || []);
    renderPreviewMedia(detail.previewImage);
    renderPrimaryMedia(detail);

    detailContent.classList.remove("hidden");
}

async function loadViewerSupplementaryPanels() {
    const results = await Promise.allSettled([
        loadViewerComments(),
        loadViewerFeedbackHistory()
    ]);

    if (results.some(result => result.status === "rejected" && result.reason?.status === 401)) {
        redirectViewerToLogin();
        return;
    }

    if (results[0].status === "rejected") {
        renderViewerCommentError(results[0].reason?.message || "Unable to load comments.");
    }

    if (results[1].status === "rejected") {
        renderViewerFeedbackError(results[1].reason?.message || "Unable to load feedback history.");
    }
}

async function loadViewerComments() {
    const resourceId = getViewerResourceIdFromQuery();
    if (!resourceId) {
        throw new Error("Resource id is required.");
    }

    const comments = await viewerRequestJson(`${VIEWER_API_BASE}/${resourceId}/comments`, { method: "GET" });
    renderViewerComments(comments || []);
}

async function submitViewerComment() {
    const resourceId = getViewerResourceIdFromQuery();
    const commentContent = document.getElementById("commentContent");
    const submitButton = document.getElementById("commentSubmitBtn");
    if (!resourceId || !commentContent) return;

    const payload = {
        content: commentContent.value.trim()
    };

    if (submitButton) {
        submitButton.disabled = true;
    }

    try {
        await viewerRequestJson(`${VIEWER_API_BASE}/${resourceId}/comments`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        commentContent.value = "";
        showViewerToast("Comment posted successfully.");
        await loadViewerComments();
    } catch (error) {
        if (error?.status === 401) {
            redirectViewerToLogin();
            return;
        }
        showViewerToast(error.message || "Unable to post the comment.");
    } finally {
        if (submitButton) {
            submitButton.disabled = false;
        }
    }
}

function renderViewerComments(comments) {
    const commentList = document.getElementById("commentList");
    if (!commentList) return;

    if (!comments.length) {
        commentList.innerHTML = '<div class="viewer-empty-message">No comments yet. Be the first to share your thoughts.</div>';
        return;
    }

    commentList.innerHTML = comments.map(comment => `
        <article class="viewer-comment-item">
            <div class="viewer-comment-head">
                <strong>${escapeViewerHtml(comment.userName || "Registered User")}</strong>
                <span>${escapeViewerHtml(formatViewerDateTime(comment.createdAt, { emptyText: "-" }))}</span>
            </div>
            <p>${escapeViewerHtml(comment.content || "")}</p>
        </article>
    `).join("");
}

function renderViewerCommentError(message) {
    const commentList = document.getElementById("commentList");
    if (!commentList) return;

    commentList.innerHTML = `<div class="viewer-error">${escapeViewerHtml(message)}</div>`;
}

async function loadViewerFeedbackHistory() {
    const feedbackList = await viewerRequestJson(`${VIEWER_FEEDBACK_API_BASE}/mine`, { method: "GET" });
    renderViewerFeedbackHistory(feedbackList || []);
}

async function submitViewerFeedback() {
    const feedbackType = document.getElementById("feedbackType");
    const feedbackDescription = document.getElementById("feedbackDescription");
    const feedbackFiles = document.getElementById("feedbackFiles");
    const feedbackSubmitButton = document.getElementById("feedbackSubmitBtn");
    if (!feedbackType || !feedbackDescription || !feedbackFiles) return;

    const formData = new FormData();
    formData.append("feedbackType", feedbackType.value);
    formData.append("description", feedbackDescription.value.trim());
    Array.from(feedbackFiles.files || []).forEach(file => {
        formData.append("files", file);
    });

    if (feedbackSubmitButton) {
        feedbackSubmitButton.disabled = true;
    }

    try {
        await viewerRequestJson(`${VIEWER_FEEDBACK_API_BASE}`, {
            method: "POST",
            body: formData
        });

        document.getElementById("feedbackForm")?.reset();
        updateViewerFeedbackFileText();
        showViewerToast("Feedback submitted successfully.");
        await loadViewerFeedbackHistory();
    } catch (error) {
        if (error?.status === 401) {
            redirectViewerToLogin();
            return;
        }
        showViewerToast(error.message || "Unable to submit feedback.");
    } finally {
        if (feedbackSubmitButton) {
            feedbackSubmitButton.disabled = false;
        }
    }
}

function renderViewerFeedbackHistory(feedbackList) {
    const feedbackContainer = document.getElementById("feedbackList");
    if (!feedbackContainer) return;

    if (!feedbackList.length) {
        feedbackContainer.innerHTML = '<div class="viewer-empty-message">You have not submitted any feedback yet.</div>';
        return;
    }

    feedbackContainer.innerHTML = feedbackList.map(feedback => {
        const attachments = Array.isArray(feedback.attachments) ? feedback.attachments : [];
        const attachmentHtml = attachments.length
            ? `
                <div class="viewer-feedback-attachments">
                    ${attachments.map(attachment => `
                        <a href="${escapeViewerHtml(toPublicMediaUrl(attachment.filePath))}" target="_blank" rel="noopener noreferrer">
                            ${escapeViewerHtml(attachment.originalFilename || attachment.filePath || "Attachment")}
                        </a>
                    `).join("")}
                </div>
            `
            : '<div class="viewer-input-note">No attachments uploaded.</div>';

        return `
            <article class="viewer-feedback-item">
                <div class="viewer-feedback-head">
                    <strong>${escapeViewerHtml(feedback.feedbackType || "-")}</strong>
                    <span>${escapeViewerHtml(formatViewerDateTime(feedback.uploadedAt, { emptyText: "-" }))}</span>
                </div>
                <p>${escapeViewerHtml(feedback.description || "")}</p>
                ${attachmentHtml}
            </article>
        `;
    }).join("");
}

function renderViewerFeedbackError(message) {
    const feedbackContainer = document.getElementById("feedbackList");
    if (!feedbackContainer) return;

    feedbackContainer.innerHTML = `<div class="viewer-error">${escapeViewerHtml(message)}</div>`;
}

function updateViewerFeedbackFileText() {
    const feedbackFiles = document.getElementById("feedbackFiles");
    const feedbackFilesText = document.getElementById("feedbackFilesText");
    if (!feedbackFiles || !feedbackFilesText) return;

    const files = Array.from(feedbackFiles.files || []);
    if (!files.length) {
        feedbackFilesText.textContent = "No files selected. JPG, PNG, PDF, or TXT. 10MB per file.";
        return;
    }

    feedbackFilesText.textContent = files.map(file => file.name).join(", ");
}

function renderViewerTags(tagNames) {
    const tagContainer = document.getElementById("detailTags");
    if (!tagContainer) return;

    if (!tagNames.length) {
        tagContainer.innerHTML = '<span class="viewer-empty-message">No tags attached.</span>';
        return;
    }

    tagContainer.innerHTML = tagNames
            .map(tagName => `<span class="viewer-tag">${escapeViewerHtml(tagName)}</span>`)
            .join("");
}

function renderPreviewMedia(previewImage) {
    const container = document.getElementById("previewContainer");
    if (!container) return;

    if (!previewImage) {
        container.innerHTML = '<div class="viewer-empty-message">No preview image provided.</div>';
        return;
    }

    container.innerHTML = `
        <img src="${escapeViewerHtml(toPublicMediaUrl(previewImage))}" alt="Preview image" />
    `;
}

function renderPrimaryMedia(detail) {
    const container = document.getElementById("mediaContainer");
    if (!container) return;

    if (!detail.mediaUrl) {
        container.innerHTML = '<div class="viewer-empty-message">No primary media uploaded.</div>';
        return;
    }

    const mediaUrl = escapeViewerHtml(toPublicMediaUrl(detail.mediaUrl));
    const type = normalizeViewerResourceType(detail.resourceType);

    if (type === "photo") {
        container.innerHTML = `<img src="${mediaUrl}" alt="${escapeViewerHtml(detail.title || "Resource image")}" />`;
        return;
    }

    if (type === "video") {
        container.innerHTML = `
            <video controls preload="metadata">
                <source src="${mediaUrl}" />
                Your browser does not support the video tag.
            </video>
        `;
        return;
    }

    if (type === "audio") {
        container.innerHTML = `
            <audio controls preload="metadata">
                <source src="${mediaUrl}" />
                Your browser does not support the audio element.
            </audio>
        `;
        return;
    }

    const linkLabel = type === "extra link"
        ? "Open external link"
        : type === "document"
            ? "Open attached document"
            : "Open attached media";

    container.innerHTML = `
        <a class="viewer-document-link" href="${mediaUrl}" target="_blank" rel="noopener noreferrer">${linkLabel}</a>
    `;
}

function handleViewerError(error, fallbackMessage) {
    if (error?.status === 401) {
        redirectViewerToLogin();
        return;
    }

    const message = error?.message || fallbackMessage;
    const detailError = document.getElementById("detailError");
    const resourceList = document.getElementById("resourceList");

    if (detailError) {
        detailError.textContent = message;
        detailError.classList.remove("hidden");
    }

    if (resourceList) {
        resourceList.innerHTML = `<div class="viewer-error">${escapeViewerHtml(message)}</div>`;
    }

    showViewerToast(message);
}

function resolveCategoryName(categoryId) {
    if (categoryId == null || categoryId === "") {
        return "-";
    }

    const matchedOption = viewerCategoryOptions.find(option => String(option.id) === String(categoryId));
    if (!matchedOption) {
        return "Unknown category";
    }
    return capitalizeViewerLabel(matchedOption.name);
}

function capitalizeViewerLabel(value) {
    if (!value) return "";
    return value
            .toString()
            .split(" ")
            .filter(Boolean)
            .map(segment => segment.charAt(0).toUpperCase() + segment.slice(1))
            .join(" ");
}

function normalizeViewerCategoryKey(value) {
    if (!value) {
        return "";
    }

    const normalizedValue = value.toString().trim().replaceAll(/\s+/g, " ").toLowerCase();
    if (normalizedValue === "education") {
        return "educational materials";
    }
    return normalizedValue;
}

function formatViewerResourceType(value) {
    const resourceType = normalizeViewerResourceType(value);

    switch (resourceType) {
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
            return value || "-";
    }
}

function normalizeViewerResourceType(value) {
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
            return resourceType;
    }
}

function buildViewerExcerpt(value, maxLength) {
    const normalized = (value || "").trim();
    if (!normalized) {
        return "No description provided yet.";
    }
    if (normalized.length <= maxLength) {
        return normalized;
    }
    return `${normalized.slice(0, maxLength - 1)}…`;
}

function resetViewerFilters() {
    const keyword = document.getElementById("keyword");
    const type = document.getElementById("type");
    const categoryId = document.getElementById("categoryId");
    const sortBy = document.getElementById("sortBy");

    if (keyword) keyword.value = "";
    if (type) type.value = "";
    if (categoryId) categoryId.value = "";
    if (sortBy) sortBy.value = "";
}

function getViewerResourceIdFromQuery() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id");
    if (!id) return null;
    const numericId = Number(id);
    return Number.isFinite(numericId) ? numericId : null;
}

function toPublicMediaUrl(value) {
    if (!value) return "";

    const normalized = value.trim();
    if (/^https?:\/\//i.test(normalized)) {
        return normalized;
    }

    return `/uploads/${normalized.replace(/^\/+/, "")}`;
}

function redirectViewerToLogin() {
    const next = `${window.location.pathname}${window.location.search}`;
    window.location.href = `../login.html?next=${encodeURIComponent(next)}`;
}
