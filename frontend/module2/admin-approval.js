(() => {
    const ADMIN_CONTRIBUTOR_API = "/api/admin/contributor-requests";

    let pendingRequests = [];
    let approvedContributors = [];

    document.addEventListener("DOMContentLoaded", async () => {
        const admin = window.AdminModule;
        admin.bindAdminBasics();
        const currentUser = await admin.requireAdmin();
        if (!currentUser) return;

        bindApprovalTabs();
        bindContributorActions();
        await loadContributorApprovalData();
    });

    function bindApprovalTabs() {
        document.addEventListener("click", event => {
            const button = event.target.closest("[data-approval-tab]");
            if (!button) return;
            const tab = button.dataset.approvalTab;
            document.querySelectorAll("[data-approval-tab]").forEach(item => {
                item.classList.toggle("active", item === button);
            });
            document.querySelectorAll(".admin-section").forEach(section => {
                section.classList.remove("active");
            });
            const section = document.getElementById(`approval${capitalize(tab)}Section`);
            if (section) section.classList.add("active");
        });

        const refreshButton = document.getElementById("approvalRefreshBtn");
        if (refreshButton) {
            refreshButton.addEventListener("click", loadContributorApprovalData);
        }
    }

    function bindContributorActions() {
        document.addEventListener("click", async event => {
            const detailButton = event.target.closest("[data-contributor-detail]");
            if (detailButton) {
                await loadContributorDetail(Number(detailButton.dataset.contributorDetail));
                return;
            }

            const decisionButton = event.target.closest("[data-contributor-decision]");
            if (decisionButton) {
                await submitContributorDecision(
                    Number(decisionButton.dataset.requestId),
                    decisionButton.dataset.contributorDecision,
                    decisionButton
                );
                return;
            }

            const revokeButton = event.target.closest("[data-revoke-contributor]");
            if (revokeButton) {
                await revokeContributor(Number(revokeButton.dataset.revokeContributor), revokeButton);
            }
        });
    }

    async function loadContributorApprovalData() {
        const admin = window.AdminModule;
        admin.setState("contributorPendingPanel", "Loading contributor applications...");
        admin.setState("approvedContributorPanel", "Loading contributors...");

        try {
            const [pending, approved] = await Promise.all([
                admin.requestJson(`${ADMIN_CONTRIBUTOR_API}/pending`, { method: "GET" }),
                admin.requestJson(`${ADMIN_CONTRIBUTOR_API}/approved-contributors`, { method: "GET" })
            ]);
            pendingRequests = Array.isArray(pending) ? pending : [];
            approvedContributors = Array.isArray(approved) ? approved : [];
            renderPendingRequests();
            renderApprovedContributors();
        } catch (error) {
            const message = admin.getErrorMessage(error, "Unable to load contributor approval data.");
            admin.setState("contributorPendingPanel", message, "error");
            admin.setState("approvedContributorPanel", message, "error");
        }
    }

    function renderPendingRequests() {
        const panel = document.getElementById("contributorPendingPanel");
        if (!panel) return;
        if (!pendingRequests.length) {
            panel.innerHTML = `<div class="admin-empty">There are no pending contributor applications.</div>`;
            return;
        }
        panel.innerHTML = pendingRequests.map(renderPendingRequestCard).join("");
    }

    function renderPendingRequestCard(request) {
        const admin = window.AdminModule;
        const requestId = request.requestId;
        return `
            <article class="contributor-card">
                <h3>${admin.escapeHtml(request.userName || "Unknown Applicant")}</h3>
                <p>${admin.escapeHtml(request.userEmail || "-")}</p>
                <p>Requested: ${admin.escapeHtml(admin.formatDateTime(request.requestedAt))}</p>
                <p>Status: ${admin.statusBadge(request.status)}</p>
                <div class="contributor-comment">
                    <label class="admin-muted" for="contributorComment-${requestId}">Review Comment</label>
                    <textarea class="admin-textarea" id="contributorComment-${requestId}" placeholder="Optional comment for the applicant"></textarea>
                </div>
                <div class="admin-row-actions">
                    <button type="button" class="admin-btn" data-contributor-detail="${requestId}">View Detail</button>
                    <button type="button" class="admin-btn primary" data-request-id="${requestId}" data-contributor-decision="APPROVED">Approve</button>
                    <button type="button" class="admin-btn danger" data-request-id="${requestId}" data-contributor-decision="REJECTED">Reject</button>
                </div>
            </article>
        `;
    }

    function renderApprovedContributors() {
        const panel = document.getElementById("approvedContributorPanel");
        if (!panel) return;
        if (!approvedContributors.length) {
            panel.innerHTML = `<div class="admin-empty">No approved contributors are currently active.</div>`;
            return;
        }
        panel.innerHTML = `
            <div class="admin-table-wrap">
                <table class="admin-table">
                    <thead>
                        <tr>
                            <th>User</th>
                            <th>Email</th>
                            <th>Latest Application</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${approvedContributors.map(renderApprovedContributorRow).join("")}
                    </tbody>
                </table>
            </div>
        `;
    }

    function renderApprovedContributorRow(contributor) {
        const admin = window.AdminModule;
        return `
            <tr>
                <td>${admin.escapeHtml(contributor.userName || `User ${contributor.userId}`)}</td>
                <td>${admin.escapeHtml(contributor.userEmail || "-")}</td>
                <td>${contributor.requestId ? `#${contributor.requestId}` : "-"}</td>
                <td>
                    <button type="button" class="admin-btn danger" data-revoke-contributor="${contributor.userId}">
                        Revoke Contributor
                    </button>
                </td>
            </tr>
        `;
    }

    async function loadContributorDetail(requestId) {
        if (!requestId) return;
        const admin = window.AdminModule;
        admin.setState("contributorDetailPanel", "Loading contributor application detail...");
        try {
            const detail = await admin.requestJson(`${ADMIN_CONTRIBUTOR_API}/${requestId}`, { method: "GET" });
            renderContributorDetail(detail);
        } catch (error) {
            admin.setState("contributorDetailPanel", admin.getErrorMessage(error, "Unable to load application detail."), "error");
        }
    }

    function renderContributorDetail(detail) {
        const panel = document.getElementById("contributorDetailPanel");
        if (!panel) return;
        const admin = window.AdminModule;
        panel.innerHTML = `
            <div class="admin-panel-header">
                <div>
                    <h2>Contributor Application Detail</h2>
                    <p>Application #${detail.requestId ?? "-"}</p>
                </div>
                ${admin.statusBadge(detail.status)}
            </div>
            <div class="admin-detail-list">
                ${detailItem("Applicant", detail.userName)}
                ${detailItem("Email", detail.userEmail)}
                ${detailItem("User ID", detail.userId)}
                ${detailItem("Reviewed By", detail.reviewedBy)}
                ${detailItem("Requested At", admin.formatDateTime(detail.requestedAt))}
                ${detailItem("Reviewed At", admin.formatDateTime(detail.reviewedAt))}
                ${detailItem("Application Reason", detail.applicationReason)}
                ${detailItem("Review Comment", detail.reviewComment)}
            </div>
        `;
    }

    async function submitContributorDecision(requestId, decision, button) {
        if (!requestId || !decision) return;
        if (!window.confirm(`Are you sure you want to ${decision.toLowerCase()} this contributor application?`)) {
            return;
        }
        const commentInput = document.getElementById(`contributorComment-${requestId}`);
        button.disabled = true;
        try {
            await window.AdminModule.jsonRequest(`${ADMIN_CONTRIBUTOR_API}/${requestId}/decision`, {
                method: "POST",
                body: JSON.stringify({
                    decision,
                    reviewComment: commentInput ? commentInput.value.trim() : ""
                })
            });
            window.AdminModule.showToast(`Contributor application ${decision.toLowerCase()} successfully.`);
            await loadContributorApprovalData();
            await loadContributorDetail(requestId);
        } catch (error) {
            window.AdminModule.showToast(window.AdminModule.getErrorMessage(error, "Unable to review contributor application."));
        } finally {
            button.disabled = false;
        }
    }

    async function revokeContributor(userId, button) {
        if (!userId) return;
        if (!window.confirm("Revoke this user's contributor role? Their account and history will be kept.")) {
            return;
        }
        button.disabled = true;
        try {
            await window.AdminModule.jsonRequest(`${ADMIN_CONTRIBUTOR_API}/contributors/${userId}/revoke`, {
                method: "POST"
            });
            window.AdminModule.showToast("Contributor role revoked.");
            await loadContributorApprovalData();
        } catch (error) {
            window.AdminModule.showToast(window.AdminModule.getErrorMessage(error, "Unable to revoke contributor role."));
        } finally {
            button.disabled = false;
        }
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

    function capitalize(value) {
        if (!value) return "";
        return value.charAt(0).toUpperCase() + value.slice(1);
    }

    window.Module2Approval = {
        loadContributorApprovalData
    };
})();
