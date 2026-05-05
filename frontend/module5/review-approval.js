(() => {
    const REVIEW_API = "/api/reviewer/reviews";
    const PAGE_SIZE = 10;

    let pendingSubmissions = [];
    let selectedSubmission = null;
    let currentPage = 1;

    document.addEventListener("DOMContentLoaded", async () => {
        const admin = window.AdminModule;
        admin.bindAdminBasics();
        const currentUser = await admin.requireAdmin();
        if (!currentUser) return;

        bindResourceReviewActions();
        await loadResourceReviewData();
    });

    function bindResourceReviewActions() {
        const refreshButton = document.getElementById("approvalRefreshBtn");
        if (refreshButton) {
            refreshButton.addEventListener("click", loadResourceReviewData);
        }

        document.addEventListener("click", async event => {
            const detailButton = event.target.closest("[data-resource-detail]");
            if (detailButton) {
                await loadResourceDetail(Number(detailButton.dataset.resourceDetail));
                return;
            }

            const pageButton = event.target.closest("[data-review-page]");
            if (pageButton) {
                const nextPage = Number(pageButton.dataset.reviewPage);
                if (nextPage > 0) {
                    currentPage = nextPage;
                    await loadResourceReviewData();
                }
                return;
            }

            const decisionButton = event.target.closest("[data-resource-decision]");
            if (decisionButton) {
                await submitResourceDecision(decisionButton.dataset.resourceDecision, decisionButton);
                return;
            }

        });
    }

    async function loadResourceReviewData() {
        const admin = window.AdminModule;
        admin.setState("resourcePendingPanel", "Loading resource submissions...");
        try {
            const page = await admin.requestJson(`${REVIEW_API}/pending?page=${currentPage}&pageSize=${PAGE_SIZE}`, {
                method: "GET"
            });
            pendingSubmissions = Array.isArray(page.items) ? page.items : [];
            renderPendingResources(page);
        } catch (error) {
            admin.setState("resourcePendingPanel", admin.getErrorMessage(error, "Unable to load resource submissions."), "error");
        }
    }

    function renderPendingResources(page) {
        const panel = document.getElementById("resourcePendingPanel");
        if (!panel) return;
        if (!pendingSubmissions.length) {
            panel.innerHTML = `<div class="admin-empty">${window.AdminModule.escapeHtml(page.emptyMessage || "No resource submissions are pending review.")}</div>`;
            return;
        }
        const totalPages = Math.max(1, Math.ceil((page.total || pendingSubmissions.length) / PAGE_SIZE));
        panel.innerHTML = `
            <div class="resource-review-list">
                ${pendingSubmissions.map(renderPendingResourceCard).join("")}
            </div>
            <div class="admin-row-actions review-pagination">
                <button type="button" class="admin-btn" data-review-page="${currentPage - 1}" ${currentPage <= 1 ? "disabled" : ""}>Previous</button>
                <span class="admin-muted">Page ${currentPage} of ${totalPages}</span>
                <button type="button" class="admin-btn" data-review-page="${currentPage + 1}" ${currentPage >= totalPages ? "disabled" : ""}>Next</button>
            </div>
        `;
    }

    function renderPendingResourceCard(item) {
        const admin = window.AdminModule;
        return `
            <article class="resource-review-card">
                <h3>${admin.escapeHtml(item.title || "Untitled Resource")}</h3>
                <p>Submission #${item.submissionId ?? "-"} · Resource #${item.resourceId ?? "-"} · Version ${item.versionNo ?? "-"}</p>
                <p>Contributor: ${admin.escapeHtml(item.contributorName || `User ${item.contributorId ?? "-"}`)}</p>
                <p>Category: ${admin.escapeHtml(item.categoryTopic || "-")} · Submitted: ${admin.escapeHtml(admin.formatDateTime(item.submittedAt))}</p>
                <p>Status: ${admin.statusBadge(item.resourceStatus)}</p>
                <div class="admin-row-actions">
                    <button type="button" class="admin-btn primary" data-resource-detail="${item.submissionId}">View Detail</button>
                </div>
            </article>
        `;
    }

    async function loadResourceDetail(submissionId) {
        if (!submissionId) return;
        const admin = window.AdminModule;
        admin.setState("resourceDetailPanel", "Loading resource detail...");
        try {
            selectedSubmission = await admin.requestJson(`${REVIEW_API}/submissions/${submissionId}`, { method: "GET" });
            renderResourceDetail(selectedSubmission);
        } catch (error) {
            selectedSubmission = null;
            admin.setState("resourceDetailPanel", admin.getErrorMessage(error, "Unable to load resource detail."), "error");
        }
    }

    function renderResourceDetail(detail) {
        const panel = document.getElementById("resourceDetailPanel");
        if (!panel) return;
        const admin = window.AdminModule;
        const resource = detail.resource || {};
        const contributor = detail.contributor || {};
        const category = detail.category || {};
        const submission = detail.submission || {};
        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h2>${admin.escapeHtml(resource.title || "Resource Detail")}</h2>
                    <p>Submission #${detail.submissionId ?? "-"} · Resource #${detail.resourceId ?? "-"} · Version ${detail.versionNo ?? "-"}</p>
                </div>
                ${admin.statusBadge(detail.resourceStatus)}
            </div>
            <div class="admin-detail-list">
                ${detailItem("Contributor", contributor.username || `User ${contributor.userId ?? "-"}`)}
                ${detailItem("Contributor ID", contributor.userId)}
                ${detailItem("Resource Type", resource.resourceType)}
                ${detailItem("Category", category.categoryTopic)}
                ${detailItem("Place", resource.place)}
                ${detailItem("Tags", Array.isArray(detail.tags) && detail.tags.length ? detail.tags.join(", ") : "-")}
                ${detailItem("Description", resource.description)}
                ${detailItem("Copyright", resource.copyrightDeclaration)}
                ${detailItem("Submission Note", submission.submissionNote)}
                ${detailItem("Submitted At", admin.formatDateTime(submission.submittedAt))}
                ${detailItem("Current Context", submission.currentContextLabel)}
                ${detailItem("Submitted Status", admin.formatLabel(submission.statusSnapshot))}
            </div>
            ${renderMediaSection(resource)}
            <div class="review-feedback-box">
                <label class="admin-muted" for="resourceFeedbackComment">Feedback Comment</label>
                <textarea class="admin-textarea" id="resourceFeedbackComment" placeholder="Required when rejecting a resource"></textarea>
            </div>
            <div class="admin-row-actions review-feedback-box">
                <button type="button" class="admin-btn primary" data-resource-decision="approve">Approve Resource</button>
                <button type="button" class="admin-btn danger" data-resource-decision="reject">Reject Resource</button>
            </div>
            ${renderReviewHistory(detail.reviewHistory)}
        `;
    }

    function renderMediaSection(resource) {
        const admin = window.AdminModule;
        const previewUrl = admin.escapeHtml(toPublicMediaUrl(resource.previewImage));
        const mediaUrl = admin.escapeHtml(toPublicMediaUrl(resource.mediaUrl));
        const preview = resource.previewImage
            ? `<img class="admin-media-preview" src="${previewUrl}" alt="Resource preview image" />`
            : "-";
        const media = resource.mediaUrl
            ? `<a class="admin-btn" href="${mediaUrl}" target="_blank" rel="noopener">Open Media File</a>`
            : "-";
        const files = Array.isArray(resource.files) && resource.files.length
            ? resource.files.map(file => {
                const fileUrl = admin.escapeHtml(toPublicMediaUrl(file.filePath));
                return `
                    <tr>
                        <td>${admin.escapeHtml(file.originalFilename || "-")}</td>
                        <td>${admin.escapeHtml(file.fileType || "-")}</td>
                        <td>${file.fileSize ?? "-"}</td>
                        <td><a class="admin-btn" href="${fileUrl || "#"}" target="_blank" rel="noopener">Open</a></td>
                    </tr>
                `;
            }).join("")
            : window.AdminModule.emptyRow(4, "No attached files found.");

        return `
            <div class="admin-grid review-feedback-box">
                <div class="admin-detail-item">
                    <span class="admin-detail-label">Preview Image</span>
                    <div class="admin-detail-value">${preview}</div>
                </div>
                <div class="admin-detail-item">
                    <span class="admin-detail-label">Media File</span>
                    <div class="admin-detail-value">${media}</div>
                </div>
            </div>
            <div class="admin-table-wrap review-feedback-box">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>File</th>
                            <th>Type</th>
                            <th>Size</th>
                            <th>Open</th>
                        </tr>
                    </thead>
                    <tbody>${files}</tbody>
                </table>
            </div>
        `;
    }

    function renderReviewHistory(history) {
        const admin = window.AdminModule;
        const rows = Array.isArray(history) ? history : [];
        if (!rows.length) {
            return `<div class="admin-empty review-feedback-box">No review history has been recorded yet.</div>`;
        }
        return `
            <div class="review-history-list review-feedback-box">
                ${rows.map(item => `
                    <article class="review-history-item">
                        <strong>${admin.escapeHtml(admin.formatLabel(item.action || item.status))}</strong>
                        ${admin.statusBadge(item.status)}
                        <p class="admin-muted">Reviewer: ${admin.escapeHtml(item.reviewerName || `User ${item.reviewerId ?? "-"}`)} · ${admin.escapeHtml(admin.formatDateTime(item.reviewedAt))}</p>
                        <p>${admin.escapeHtml(item.feedbackComment || "No feedback comment.")}</p>
                    </article>
                `).join("")}
            </div>
        `;
    }

    async function submitResourceDecision(decision, button) {
        if (!selectedSubmission) {
            window.AdminModule.showToast("Select a resource submission first.");
            return;
        }
        const normalizedDecision = String(decision || "").toLowerCase();
        const feedbackInput = document.getElementById("resourceFeedbackComment");
        const feedbackComment = feedbackInput ? feedbackInput.value.trim() : "";
        if (normalizedDecision === "reject" && !feedbackComment) {
            window.AdminModule.showToast("Feedback comment is required when rejecting a resource.");
            if (feedbackInput) feedbackInput.focus();
            return;
        }

        if (!window.confirm(`Are you sure you want to ${normalizedDecision} this resource submission?`)) {
            return;
        }

        button.disabled = true;
        const endpoint = `${REVIEW_API}/${selectedSubmission.submissionId}/${normalizedDecision === "approve" ? "approve" : "reject"}`;
        try {
            await window.AdminModule.jsonRequest(endpoint, {
                method: "POST",
                body: JSON.stringify({
                    resourceId: selectedSubmission.resourceId,
                    versionNo: selectedSubmission.versionNo,
                    feedbackComment
                })
            });
            window.AdminModule.showToast(`Resource ${normalizedDecision === "approve" ? "approved" : "rejected"} successfully.`);
            await loadResourceReviewData();
            await loadResourceDetail(selectedSubmission.submissionId);
        } catch (error) {
            window.AdminModule.showToast(window.AdminModule.getErrorMessage(error, "Unable to submit resource review decision."));
        } finally {
            button.disabled = false;
        }
    }

    function toPublicMediaUrl(value) {
        if (!value) {
            return "";
        }
        const normalized = String(value).trim();
        if (!normalized) {
            return "";
        }
        if (/^https?:\/\//i.test(normalized)) {
            return normalized;
        }
        if (normalized.startsWith("/uploads/")) {
            return normalized;
        }
        return `/uploads/${normalized.replace(/^\/+/, "")}`;
    }

    function detailItem(label, value) {
        const admin = window.AdminModule;
        return `
            <div class="admin-detail-item">
                <span class="admin-detail-label">${admin.escapeHtml(label)}</span>
                <div class="admin-detail-value">${admin.escapeHtml(value == null || value === "" ? "-" : value)}</div>
            </div>
        `;
    }

    window.Module5ReviewApproval = {
        loadResourceReviewData
    };
})();
