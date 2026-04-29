(() => {
    function getToastElement() {
        return document.getElementById("toast");
    }

    function getQueryMessage(param = "message") {
        const params = new URLSearchParams(window.location.search);
        return params.get(param);
    }

    async function requestJson(url, options = {}) {
        let response;

        try {
            response = await fetch(url, options);
        } catch (error) {
            const networkError = new Error("Unable to connect to the server.");
            networkError.isNetworkError = true;
            throw networkError;
        }

        if (!response.ok) {
            let message = "Request failed.";
            let details = [];

            try {
                const data = await response.json();
                message = data.message || message;
                details = Array.isArray(data.details) ? data.details : [];
            } catch (error) {
                try {
                    message = await response.text();
                } catch (ignored) {
                }
            }

            const requestError = new Error(details.length ? `${message} ${details.join(" ")}` : message);
            requestError.status = response.status;
            requestError.details = details;
            requestError.isNetworkError = false;
            throw requestError;
        }

        const contentType = response.headers.get("content-type") || "";
        if (!contentType.includes("application/json")) {
            return null;
        }

        return response.json();
    }

    function showToast(message, { duration = 2800 } = {}) {
        const toast = getToastElement();
        if (!toast) return;

        toast.textContent = message;
        toast.classList.add("show");

        clearTimeout(showToast._timer);
        showToast._timer = window.setTimeout(() => {
            toast.classList.remove("show");
        }, duration);
    }

    function showMessageFromQuery(options = {}) {
        const message = getQueryMessage(options.param);
        if (message) {
            showToast(message, options);
        }
    }

    function bindLogoutButtons({ logoutUrl, redirectTo } = {}) {
        if (!logoutUrl || !redirectTo) return;
        if (document.body?.dataset.sharedLogoutBound === "true") return;

        document.body.dataset.sharedLogoutBound = "true";

        document.addEventListener("click", async (event) => {
            const button = event.target.closest("[data-logout-btn]");
            if (!button) return;

            event.preventDefault();

            try {
                await requestJson(logoutUrl, { method: "POST" });
            } catch (error) {
                // Redirect even if the session has already expired.
            }

            window.location.href = redirectTo;
        });
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function formatDateTime(value, { emptyText = "-" } = {}) {
        if (!value) return emptyText;
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return String(value);
        return date.toLocaleString();
    }

    function setText(id, value) {
        const element = document.getElementById(id);
        if (!element) return;
        element.textContent = value ?? "";
    }

    function setValue(id, value) {
        const element = document.getElementById(id);
        if (!element) return;
        element.value = value ?? "";
    }

    window.SharedApp = {
        bindLogoutButtons,
        escapeHtml,
        formatDateTime,
        getQueryMessage,
        requestJson,
        setText,
        setValue,
        showMessageFromQuery,
        showToast
    };
})();
