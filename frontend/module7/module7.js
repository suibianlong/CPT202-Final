(() => {
    const AUTH_API = "/api/auth";
    const {
        bindLogoutButtons,
        escapeHtml,
        formatDateTime,
        requestJson,
        showToast
    } = window.SharedApp;

    function bindAdminBasics() {
        bindLogoutButtons({
            logoutUrl: `${AUTH_API}/logout`,
            redirectTo: `./index.html?message=${encodeURIComponent("You have logged out successfully.")}`
        });
    }

    async function requireAdmin() {
        try {
            const currentUser = await requestJson(`${AUTH_API}/me`, { method: "GET" });
            if (!currentUser || currentUser.role !== "ADMINISTRATOR") {
                showAccessMessage("Administrator permission is required.");
                return null;
            }
            return currentUser;
        } catch (error) {
            if (error.status === 401) {
                const next = `${window.location.pathname}${window.location.search}`;
                window.location.href = `./login.html?next=${encodeURIComponent(next)}`;
                return null;
            }
            showAccessMessage(error.message || "Unable to verify administrator access.");
            return null;
        }
    }

    function showAccessMessage(message) {
        const main = document.querySelector("[data-admin-main]");
        if (!main) return;
        main.innerHTML = `
            <section class="admin-panel">
                <div class="admin-error">${escapeHtml(message)}</div>
                <div class="admin-page-actions admin-access-actions">
                    <a class="admin-btn primary" href="./login.html">Go to Login</a>
                    <a class="admin-btn" href="./index.html">Back to Home</a>
                </div>
            </section>
        `;
    }

    function statusClass(value) {
        const normalized = normalizeStatus(value);
        if (normalized === "active") return "status-active";
        if (normalized === "inactive") return "status-inactive";
        if (normalized === "approved") return "status-approved";
        if (normalized === "rejected") return "status-rejected";
        if (normalized === "pending" || normalized === "pending review") return "status-pending-review";
        if (normalized === "archived") return "status-archived";
        return "";
    }

    function statusBadge(value) {
        return `<span class="status-badge ${statusClass(value)}">${escapeHtml(formatLabel(value))}</span>`;
    }

    function formatLabel(value) {
        if (value == null || value === "") return "-";
        return String(value)
            .replaceAll("_", " ")
            .replaceAll("-", " ")
            .toLowerCase()
            .replace(/\b\w/g, char => char.toUpperCase());
    }

    function normalizeStatus(value) {
        return String(value || "")
            .replaceAll("_", " ")
            .replaceAll("-", " ")
            .trim()
            .toLowerCase();
    }

    function emptyRow(colspan, text) {
        return `<tr><td colspan="${colspan}" class="admin-muted">${escapeHtml(text)}</td></tr>`;
    }

    function setState(elementId, message, type = "loading") {
        const element = document.getElementById(elementId);
        if (!element) return;
        element.innerHTML = `<div class="admin-${type}">${escapeHtml(message)}</div>`;
    }

    function getErrorMessage(error, fallback) {
        return error && error.message ? error.message : fallback;
    }

    async function jsonRequest(url, options = {}) {
        const headers = options.body
            ? { "Content-Type": "application/json", ...(options.headers || {}) }
            : options.headers;
        return requestJson(url, { ...options, headers });
    }

    window.AdminModule = {
        bindAdminBasics,
        emptyRow,
        escapeHtml,
        formatDateTime,
        formatLabel,
        getErrorMessage,
        jsonRequest,
        requestJson,
        requireAdmin,
        setState,
        showToast,
        statusBadge
    };
})();
