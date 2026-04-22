const ACCESS_AUTH_API = "/api/auth";
const ACCESS_CONTRIBUTOR_API = "/api/contributor-requests";
const ACCESS_ADMIN_API = "/api/admin/contributor-requests";
const ACCESS_EMAIL_PATTERN = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const {
    bindLogoutButtons,
    escapeHtml,
    formatDateTime,
    requestJson,
    setText,
    setValue,
    showMessageFromQuery,
    showToast
} = window.SharedApp;
const accessRequestJson = requestJson;
const showAccessToast = showToast;
const escapeAccessHtml = escapeHtml;

document.addEventListener("DOMContentLoaded", () => {
    const page = document.body.dataset.page;
    bindLogoutButtons({
        logoutUrl: `${ACCESS_AUTH_API}/logout`,
        redirectTo: `./index.html?message=${encodeURIComponent("You have logged out successfully.")}`
    });
    showMessageFromQuery();

    if (page === "home") {
        initHomePage();
    }

    if (page === "login") {
        initLoginPage();
    }

    if (page === "register") {
        initRegisterPage();
    }

    if (page === "account") {
        initAccountPage();
    }

    if (page === "admin-approval") {
        initAdminApprovalPage();
    }
});

async function initHomePage() {
    try {
        const currentUser = await accessRequestJson(`${ACCESS_AUTH_API}/me`, { method: "GET" });
        renderHomeSession(currentUser);
    } catch (error) {
        renderHomeSession(null);
    }
}

async function initLoginPage() {
    const loginForm = document.getElementById("loginForm");
    if (!loginForm) return;
    setAuthLinkHref("loginCreateAccountLink", "./register.html");

    try {
        const currentUser = await accessRequestJson(`${ACCESS_AUTH_API}/me`, { method: "GET" });
        window.location.href = resolvePostLoginRedirect(currentUser);
        return;
    } catch (error) {
        // Continue showing the login form when no valid session exists.
    }

    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const payload = {
            email: document.getElementById("loginEmail").value.trim(),
            password: document.getElementById("loginPassword").value
        };

        try {
            const currentUser = await accessRequestJson(`${ACCESS_AUTH_API}/login`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            showAccessToast("Login successful.");
            window.location.href = resolvePostLoginRedirect(currentUser);
        } catch (error) {
            showAccessToast(error.message || "Unable to log in.");
        }
    });
}

async function initRegisterPage() {
    const registerForm = document.getElementById("registerForm");
    if (!registerForm) return;
    setAuthLinkHref("registerLoginLink", "./login.html");
    const sendCodeButton = document.getElementById("sendRegisterCodeBtn");
    const emailInput = document.getElementById("registerEmail");
    const verificationCodeInput = document.getElementById("registerVerificationCode");
    const passwordInput = document.getElementById("registerPassword");
    const confirmPasswordInput = document.getElementById("registerConfirmPassword");

    try {
        const currentUser = await accessRequestJson(`${ACCESS_AUTH_API}/me`, { method: "GET" });
        window.location.href = resolvePostLoginRedirect(currentUser);
        return;
    } catch (error) {
        // Continue showing the registration form when no valid session exists.
    }

    if (sendCodeButton && emailInput) {
        sendCodeButton.addEventListener("click", async () => {
            const email = emailInput.value.trim();
            if (!email) {
                showAccessToast("Please enter your email address first.");
                emailInput.focus();
                return;
            }

            if (!ACCESS_EMAIL_PATTERN.test(email)) {
                showAccessToast("Please enter a valid email address before requesting a verification code.");
                emailInput.focus();
                return;
            }

            sendCodeButton.disabled = true;

            try {
                await accessRequestJson(`${ACCESS_AUTH_API}/register-verification-code`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email })
                });

                showAccessToast("Verification code sent. Please check your email.");
                startRegisterCodeCooldown(sendCodeButton, 60);
                if (verificationCodeInput) {
                    verificationCodeInput.focus();
                }
            } catch (error) {
                sendCodeButton.disabled = false;
                showAccessToast(error.message || "Unable to send the verification code.");
            }
        });
    }

    registerForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const password = passwordInput ? passwordInput.value : "";
        const confirmPassword = confirmPasswordInput ? confirmPasswordInput.value : "";
        const verificationCode = verificationCodeInput ? verificationCodeInput.value.trim() : "";

        if (password !== confirmPassword) {
            showAccessToast("The password confirmation does not match.");
            if (confirmPasswordInput) {
                confirmPasswordInput.focus();
            }
            return;
        }

        const payload = {
            name: document.getElementById("registerName").value.trim(),
            email: document.getElementById("registerEmail").value.trim(),
            password,
            confirmPassword,
            verificationCode
        };

        try {
            await accessRequestJson(`${ACCESS_AUTH_API}/register`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            showAccessToast("Registration successful. Please log in.");
            window.location.href = buildAuthPageUrl(
                "./login.html",
                { message: "Account created successfully. Please log in." }
            );
        } catch (error) {
            showAccessToast(error.message || "Unable to register.");
        }
    });
}

async function initAccountPage() {
    const currentUser = await requireAuthenticatedUser();
    await renderAccountPage(currentUser);

    const accountForm = document.getElementById("accountForm");
    if (accountForm) {
        accountForm.addEventListener("submit", async (event) => {
            event.preventDefault();

            const payload = {
                name: document.getElementById("accountName").value.trim(),
                email: document.getElementById("accountEmail").value.trim(),
                bio: document.getElementById("accountBio").value.trim()
            };

            try {
                await accessRequestJson(`${ACCESS_AUTH_API}/account`, {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });

                const refreshedUser = await accessRequestJson(`${ACCESS_AUTH_API}/me`, { method: "GET" });
                await renderAccountPage(refreshedUser);
                showAccessToast("Account details updated.");
            } catch (error) {
                showAccessToast(error.message || "Unable to update account.");
            }
        });
    }

    const requestButton = document.getElementById("submitContributorRequestBtn");
    if (requestButton) {
        requestButton.addEventListener("click", async () => {
            const reasonInput = document.getElementById("contributorApplicationReason");
            const applicationReason = reasonInput ? reasonInput.value.trim() : "";
            if (!applicationReason) {
                showAccessToast("Please enter an application reason before submitting.");
                if (reasonInput) {
                    reasonInput.focus();
                }
                return;
            }

            requestButton.disabled = true;

            try {
                await accessRequestJson(`${ACCESS_CONTRIBUTOR_API}`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ applicationReason })
                });
                const refreshedUser = await accessRequestJson(`${ACCESS_AUTH_API}/me`, { method: "GET" });
                await renderAccountPage(refreshedUser);
                showAccessToast("Contributor request submitted.");
            } catch (error) {
                showAccessToast(error.message || "Unable to submit contributor request.");
            } finally {
                requestButton.disabled = false;
            }
        });
    }
}

function startRegisterCodeCooldown(button, seconds) {
    if (!button) return;

    const originalLabel = button.dataset.originalLabel || button.textContent.trim() || "Send Code";
    button.dataset.originalLabel = originalLabel;

    let remainingSeconds = seconds;
    button.disabled = true;
    button.textContent = `${originalLabel} (${remainingSeconds}s)`;

    window.clearInterval(startRegisterCodeCooldown._timer);
    startRegisterCodeCooldown._timer = window.setInterval(() => {
        remainingSeconds -= 1;

        if (remainingSeconds <= 0) {
            window.clearInterval(startRegisterCodeCooldown._timer);
            button.disabled = false;
            button.textContent = originalLabel;
            return;
        }

        button.textContent = `${originalLabel} (${remainingSeconds}s)`;
    }, 1000);
}

async function initAdminApprovalPage() {
    const currentUser = await requireAuthenticatedUser();
    if (currentUser.role !== "ADMINISTRATOR") {
        window.location.href = `./account.html?message=${encodeURIComponent("Administrator permission is required.")}`;
        return;
    }

    const refreshButton = document.getElementById("refreshPendingRequestsBtn");
    if (refreshButton) {
        refreshButton.addEventListener("click", loadPendingRequestList);
    }

    const listContainer = document.getElementById("pendingRequestList");
    if (listContainer) {
        listContainer.addEventListener("click", async (event) => {
            const button = event.target.closest("[data-decision]");
            if (!button) return;

            const requestId = button.dataset.requestId;
            const decision = button.dataset.decision;
            const commentInput = document.getElementById(`reviewComment-${requestId}`);

            button.disabled = true;

            try {
                await accessRequestJson(`${ACCESS_ADMIN_API}/${requestId}/decision`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        decision,
                        reviewComment: commentInput ? commentInput.value.trim() : ""
                    })
                });

                showAccessToast(`Request ${decision.toLowerCase()} successfully.`);
                await loadPendingRequestList();
            } catch (error) {
                showAccessToast(error.message || "Unable to review request.");
            } finally {
                button.disabled = false;
            }
        });
    }

    await loadPendingRequestList();
}

async function renderAccountPage(currentUser) {
    populateAccountHeader(currentUser);
    populateAccountForm(currentUser);
    bindAccountBackButton(currentUser);

    const latestRequest = await accessRequestJson(`${ACCESS_CONTRIBUTOR_API}/me`, { method: "GET" });
    populateContributorPanel(currentUser, latestRequest);
    populateLatestRequestCard(latestRequest);
}

function populateAccountHeader(currentUser) {
    const statusPresentation = getAccountStatusPresentation(currentUser);

    setText("accountHeroTitle", `${currentUser.name}'s Account`);
    setText(
        "accountHeroSubtitle",
        "Manage your personal account details and view your current account status."
    );
    setText("accountStatusHeading", statusPresentation.title);
    setText("accountRoleChip", currentUser.role === "ADMINISTRATOR" ? "Administrator" : "Registered User");
    setText("accountContributorChip", getContributorRecordLabel(currentUser));
    setText("accountStatusText", buildAccountStatusCopy(currentUser));

    const statusImage = document.getElementById("accountStatusImage");
    if (statusImage) {
        statusImage.src = statusPresentation.imagePath;
        statusImage.alt = statusPresentation.imageAlt;
    }
}

function populateAccountForm(currentUser) {
    setValue("accountName", currentUser.name);
    setValue("accountEmail", currentUser.email);
    setValue("accountBio", currentUser.bio);
}

function populateContributorPanel(currentUser, latestRequest) {
    const actionText = document.getElementById("contributorActionText");
    const actionButton = document.getElementById("submitContributorRequestBtn");
    const workspaceLink = document.getElementById("accountStatusWorkspaceLink");
    const reasonField = document.getElementById("contributorReasonField");

    if (reasonField) {
        reasonField.classList.remove("hidden");
    }

    if (workspaceLink) {
        workspaceLink.classList.add("hidden");

        if (currentUser.role === "ADMINISTRATOR") {
            workspaceLink.href = "./admin-approval.html";
            workspaceLink.textContent = "Open Approval Board";
            workspaceLink.classList.remove("hidden");
        } else if (isApprovedContributor(currentUser)) {
            workspaceLink.href = "./resource-edit.html";
            workspaceLink.textContent = "Open Contributor Workspace";
            workspaceLink.classList.remove("hidden");
        }
    }

    if (!actionText || !actionButton) return;

    if (currentUser.role === "ADMINISTRATOR") {
        actionText.textContent = "Administrators review contributor requests and do not submit them.";
        actionButton.classList.add("hidden");
        if (reasonField) {
            reasonField.classList.add("hidden");
        }
        return;
    }

    actionButton.classList.remove("hidden");

    if (isApprovedContributor(currentUser)) {
        actionText.textContent = "Your contributor request has been approved. Contributor tools are now available to you.";
        actionButton.classList.add("hidden");
        if (reasonField) {
            reasonField.classList.add("hidden");
        }
        return;
    }

    if (latestRequest && latestRequest.status === "PENDING") {
        actionText.textContent = "Your contributor request is currently pending review.";
        actionButton.classList.add("hidden");
        if (reasonField) {
            reasonField.classList.add("hidden");
        }
        return;
    }

    if (latestRequest && latestRequest.status === "REJECTED") {
        actionText.textContent = "Your last contributor request was rejected. You can submit a new request.";
        actionButton.classList.remove("hidden");
        return;
    }

    actionText.textContent = "You are currently a Registered User. Submit a contributor request when you are ready.";
    actionButton.classList.remove("hidden");
}

function populateLatestRequestCard(latestRequest) {
    const container = document.getElementById("latestContributorRequest");
    if (!container) return;

    if (!latestRequest) {
        container.dataset.state = "empty";
        container.innerHTML = `
            <div class="account-request-empty">
                No contributor request has been submitted yet.
            </div>
        `;
        return;
    }

    const requestStatusLabel = escapeAccessHtml(formatEnumLabel(latestRequest.status));
    const requestStatusTone = getAccountRequestStatusTone(latestRequest.status);

    container.dataset.state = "filled";
    container.innerHTML = `
        <div class="account-request-grid">
            <div class="account-request-column">
                ${buildAccountRequestRow("Applicant", escapeAccessHtml(latestRequest.userName || "-"))}
                ${buildAccountRequestRow("Email", escapeAccessHtml(latestRequest.userEmail || "-"))}
                ${buildAccountRequestRow("Application Reason", escapeAccessHtml(latestRequest.applicationReason || "-"))}
                ${buildAccountRequestRow(
                    "Status",
                    `<span class="account-request-stamp account-request-stamp-${requestStatusTone}">${requestStatusLabel}</span>`
                )}
            </div>
            <div class="account-request-column">
                ${buildAccountRequestRow("Requested At", escapeAccessHtml(formatDateTime(latestRequest.requestedAt)))}
                ${buildAccountRequestRow("Reviewed At", escapeAccessHtml(formatDateTime(latestRequest.reviewedAt)))}
                ${buildAccountRequestRow("Review Comment", escapeAccessHtml(latestRequest.reviewComment || "-"))}
            </div>
        </div>
    `;
}

function buildAccountRequestRow(label, valueMarkup) {
    return `
        <div class="account-request-row">
            <div class="account-request-label">${escapeAccessHtml(label)}</div>
            <div class="account-request-value">${valueMarkup}</div>
        </div>
    `;
}

function getAccountRequestStatusTone(status) {
    if (status === "APPROVED") {
        return "approved";
    }

    if (status === "REJECTED") {
        return "rejected";
    }

    if (status === "PENDING") {
        return "pending";
    }

    return "default";
}

async function loadPendingRequestList() {
    const container = document.getElementById("pendingRequestList");
    if (!container) return;

    container.innerHTML = `<div class="empty-state">Loading pending requests...</div>`;

    try {
        const requests = await accessRequestJson(`${ACCESS_ADMIN_API}/pending`, { method: "GET" });

        if (!requests || requests.length === 0) {
            container.innerHTML = `<div class="empty-state">There are no pending contributor requests right now.</div>`;
            return;
        }

        container.innerHTML = requests.map(request => `
            <article class="approval-item">
                <div class="approval-item-head">
                    <div>
                        <h3>${escapeAccessHtml(request.userName || "Unknown Applicant")}</h3>
                        <p>${escapeAccessHtml(request.userEmail || "-")}</p>
                    </div>
                    <span class="status-pill status-pending_review">${escapeAccessHtml(formatEnumLabel(request.status))}</span>
                </div>

                <div class="approval-item-grid">
                    <div class="detail-item">
                        <span class="detail-label">Request ID</span>
                        <strong>${request.requestId ?? "-"}</strong>
                    </div>
                    <div class="detail-item">
                        <span class="detail-label">Requested At</span>
                        <strong>${escapeAccessHtml(formatDateTime(request.requestedAt))}</strong>
                    </div>
                </div>

                <div class="detail-item">
                    <span class="detail-label">Application Reason</span>
                    <strong>${escapeAccessHtml(request.applicationReason || "-")}</strong>
                </div>

                <div class="field">
                    <label for="reviewComment-${request.requestId}">Review Comment</label>
                    <textarea id="reviewComment-${request.requestId}" rows="3" placeholder="Optional comment for the applicant"></textarea>
                </div>

                <div class="portal-action-row">
                    <button type="button" class="btn btn-primary" data-request-id="${request.requestId}" data-decision="APPROVED">Approve</button>
                    <button type="button" class="btn btn-secondary" data-request-id="${request.requestId}" data-decision="REJECTED">Reject</button>
                </div>
            </article>
        `).join("");
    } catch (error) {
        container.innerHTML = `<div class="empty-state">${escapeAccessHtml(error.message || "Unable to load pending requests.")}</div>`;
    }
}

function renderHomeSession(currentUser) {
    const card = document.getElementById("homeSessionCard");
    if (!card) return;

    if (!currentUser) {
        card.className = "status-panel muted-panel";
        card.innerHTML = `
            <div class="status-panel-title">Current Session</div>
            <p>Not logged in yet. Open the login page or create a new account.</p>
        `;
        return;
    }

    card.className = "status-panel";
    card.innerHTML = `
        <div class="status-panel-title">${escapeAccessHtml(currentUser.name)}</div>
        <p>
            Logged in as ${escapeAccessHtml(formatEnumLabel(currentUser.role))}.
            Contributor status: ${escapeAccessHtml(formatEnumLabel(currentUser.contributorStatus))}.
        </p>
        <div class="portal-action-row">
            <a class="btn btn-secondary" href="${escapeAccessHtml(resolvePostLoginRedirect(currentUser))}">Continue</a>
            <button type="button" class="btn btn-ghost" data-logout-btn>Log Out</button>
        </div>
    `;
}

function buildAccountStatusCopy(currentUser) {
    if (currentUser.role === "ADMINISTRATOR") {
        return "You can review contributor applications, preserve approval records, and maintain your own account details from this page.";
    }

    if (isApprovedContributor(currentUser)) {
        return "Your contributor request has been approved, so the contributor workspace is unlocked for cataloguing and submission work.";
    }

    if (currentUser.contributorStatus === "PENDING") {
        return "Your contributor request is currently pending review. Contributor-only submission tools remain locked until approval is granted.";
    }

    if (currentUser.contributorStatus === "REJECTED") {
        return "Your last contributor request was rejected. You may revise your account details here and submit a fresh request when ready.";
    }

    return "You are currently a Registered User. Basic account maintenance is available, while contributor-only pages remain locked.";
}

function getAccountStatusPresentation(currentUser) {
    if (currentUser.role === "ADMINISTRATOR") {
        return {
            title: "Administrator",
            imagePath: "./module1/assets/account/status-administrator.png",
            imageAlt: "Administrator account status illustration"
        };
    }

    if (isApprovedContributor(currentUser)) {
        return {
            title: "Approved Contributor",
            imagePath: "./module1/assets/account/status-approved-contributor.png",
            imageAlt: "Approved contributor account status illustration"
        };
    }

    return {
        title: "Registered User",
        imagePath: "./module1/assets/account/status-default-user.png",
        imageAlt: "Registered user account status illustration"
    };
}

function getContributorRecordLabel(currentUser) {
    if (currentUser.role === "ADMINISTRATOR") {
        return "Not Required";
    }

    if (isApprovedContributor(currentUser)) {
        return "Approved";
    }

    if (!currentUser.contributorStatus || currentUser.contributorStatus === "NONE") {
        return "Not Submitted";
    }

    return formatEnumLabel(currentUser.contributorStatus);
}

function isApprovedContributor(currentUser) {
    return Boolean(currentUser && (currentUser.contributor === true || currentUser.contributorStatus === "APPROVED"));
}

function bindAccountBackButton(currentUser) {
    const backButton = document.getElementById("accountBackBtn");
    if (!backButton) return;

    backButton.onclick = (event) => {
        event.preventDefault();

        if (canUseAccountHistoryBack()) {
            window.history.back();
            return;
        }

        window.location.href = resolveAccountBackFallback(currentUser);
    };
}

function canUseAccountHistoryBack() {
    if (window.history.length <= 1 || !document.referrer) {
        return false;
    }

    try {
        const referrerUrl = new URL(document.referrer, window.location.origin);
        return referrerUrl.origin === window.location.origin;
    } catch (error) {
        return false;
    }
}

function resolveAccountBackFallback(currentUser) {
    if (currentUser.role === "ADMINISTRATOR") {
        return "./admin-approval.html";
    }

    if (isApprovedContributor(currentUser)) {
        return "./resource-edit.html";
    }

    return "./module6/heritage-viewer.html";
}

async function requireAuthenticatedUser() {
    try {
        return await accessRequestJson(`${ACCESS_AUTH_API}/me`, { method: "GET" });
    } catch (error) {
        if (error.status === 401) {
            const next = `${window.location.pathname}${window.location.search}`;
            window.location.href = `./login.html?next=${encodeURIComponent(next)}`;
        } else {
            showAccessToast(error.message || "Authentication is required.");
        }
        throw error;
    }
}

function resolvePostLoginRedirect(currentUser) {
    const next = getSafeNextPath();
    if (next) {
        return next;
    }

    if (currentUser.role === "ADMINISTRATOR") {
        return "./admin-approval.html";
    }

    if (currentUser.contributor) {
        return "./resource-edit.html";
    }

    return "./module6/heritage-viewer.html";
}

function setAuthLinkHref(elementId, targetPath, extraParams = {}) {
    const element = document.getElementById(elementId);
    if (!element) return;

    element.href = buildAuthPageUrl(targetPath, extraParams);
}

function buildAuthPageUrl(targetPath, extraParams = {}) {
    const url = new URL(targetPath, window.location.href);
    const next = getSafeNextPath();
    if (next) {
        url.searchParams.set("next", next);
    }

    Object.entries(extraParams).forEach(([key, value]) => {
        if (value == null || value === "") {
            return;
        }
        url.searchParams.set(key, value);
    });

    return `${url.pathname}${url.search}${url.hash}`;
}

function getSafeNextPath() {
    const params = new URLSearchParams(window.location.search);
    return sanitizeNextPath(params.get("next"));
}

function sanitizeNextPath(next) {
    if (!next) {
        return null;
    }

    const trimmed = next.trim();
    if (!trimmed) {
        return null;
    }

    const normalized = trimmed.toLowerCase();
    if (normalized.startsWith("http://")
            || normalized.startsWith("https://")
            || normalized.startsWith("//")
            || normalized.startsWith("javascript:")
            || normalized.startsWith("data:")
            || normalized.startsWith("vbscript:")) {
        return null;
    }

    if (!trimmed.startsWith("/") && !trimmed.startsWith("./") && !trimmed.startsWith("../")) {
        return null;
    }

    try {
        const resolvedUrl = new URL(trimmed, window.location.href);
        if (resolvedUrl.origin !== window.location.origin) {
            return null;
        }
        if (!isAllowedNextPath(resolvedUrl.pathname)) {
            return null;
        }

        return `${resolvedUrl.pathname}${resolvedUrl.search}${resolvedUrl.hash}`;
    } catch (error) {
        return null;
    }
}

function isAllowedNextPath(pathname) {
    const allowedPaths = new Set([
        "/",
        "/index.html",
        "/account.html",
        "/admin-approval.html",
        "/resource-edit.html",
        "/my-resources.html",
        "/module6/heritage-viewer.html",
        "/module6/viewer-detail.html"
    ]);

    return allowedPaths.has(pathname);
}

function formatEnumLabel(value) {
    if (!value) return "None";
    return value
        .toString()
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, char => char.toUpperCase());
}
