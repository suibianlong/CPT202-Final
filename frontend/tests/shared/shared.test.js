const path = require("path");

const SHARED_SCRIPT_PATH = path.resolve(__dirname, "../../shared/shared.js");

function loadSharedApp() {
    jest.resetModules();
    delete window.SharedApp;
    require(SHARED_SCRIPT_PATH);
    return window.SharedApp;
}

function createMockResponse({
    ok = true,
    status = 200,
    contentType = "application/json",
    jsonData = null,
    textData = "",
    jsonError = null,
    textError = null
} = {}) {
    return {
        ok,
        status,
        headers: {
            get: jest.fn().mockReturnValue(contentType)
        },
        json: jsonError
            ? jest.fn().mockRejectedValue(jsonError)
            : jest.fn().mockResolvedValue(jsonData),
        text: textError
            ? jest.fn().mockRejectedValue(textError)
            : jest.fn().mockResolvedValue(textData)
    };
}

describe("SharedApp", () => {
    let sharedApp;

    beforeEach(() => {
        document.body.innerHTML = "";
        document.body.removeAttribute("data-shared-logout-bound");
        window.history.replaceState({}, "", "/");
        jest.useRealTimers();
        global.fetch = jest.fn();
        window.fetch = global.fetch;
        sharedApp = loadSharedApp();
    });

    afterEach(() => {
        jest.clearAllTimers();
    });

    describe("getQueryMessage", () => {
        test("returns the default message query parameter in a normal case", () => {
            window.history.replaceState({}, "", "/?message=Login%20successful");

            expect(sharedApp.getQueryMessage()).toBe("Login successful");
        });

        test("returns null when the requested query parameter does not exist", () => {
            window.history.replaceState({}, "", "/?status=ok");

            expect(sharedApp.getQueryMessage()).toBeNull();
        });

        test("returns the value of a custom query parameter", () => {
            window.history.replaceState({}, "", "/?notice=Please%20refresh");

            expect(sharedApp.getQueryMessage("notice")).toBe("Please refresh");
        });
    });

    describe("requestJson", () => {
        test("returns parsed JSON for a successful JSON response", async () => {
            const payload = { id: 7, message: "ok" };
            fetch.mockResolvedValue(createMockResponse({
                ok: true,
                status: 200,
                contentType: "application/json; charset=utf-8",
                jsonData: payload
            }));

            await expect(sharedApp.requestJson("/api/example", { method: "GET" })).resolves.toEqual(payload);
            expect(fetch).toHaveBeenCalledWith("/api/example", { method: "GET" });
        });

        test("returns null for a successful response whose content type is not JSON", async () => {
            const response = createMockResponse({
                ok: true,
                status: 204,
                contentType: "text/plain",
                textData: ""
            });
            fetch.mockResolvedValue(response);

            await expect(sharedApp.requestJson("/api/example", { method: "DELETE" })).resolves.toBeNull();
            expect(response.json).not.toHaveBeenCalled();
        });

        test("returns null when a successful response does not provide a content type header", async () => {
            fetch.mockResolvedValue(createMockResponse({
                ok: true,
                status: 200,
                contentType: null,
                jsonData: { ignored: true }
            }));

            await expect(sharedApp.requestJson("/api/example")).resolves.toBeNull();
        });

        test("throws a network error when fetch rejects", async () => {
            fetch.mockRejectedValue(new Error("socket hang up"));

            await expect(sharedApp.requestJson("/api/example")).rejects.toMatchObject({
                message: "Unable to connect to the server.",
                isNetworkError: true
            });
        });

        test("throws an application error with status and joined details when the server returns JSON details", async () => {
            fetch.mockResolvedValue(createMockResponse({
                ok: false,
                status: 400,
                jsonData: {
                    message: "Validation failed.",
                    details: ["Email is required.", "Password is required."]
                }
            }));

            await expect(sharedApp.requestJson("/api/example")).rejects.toMatchObject({
                message: "Validation failed. Email is required. Password is required.",
                status: 400,
                details: ["Email is required.", "Password is required."],
                isNetworkError: false
            });
        });

        test("throws an application error with the text body when JSON parsing fails", async () => {
            fetch.mockResolvedValue(createMockResponse({
                ok: false,
                status: 404,
                jsonError: new Error("invalid json"),
                textData: "Resource not found."
            }));

            await expect(sharedApp.requestJson("/api/example")).rejects.toMatchObject({
                message: "Resource not found.",
                status: 404,
                details: [],
                isNetworkError: false
            });
        });

        test("falls back to the default error message when both JSON and text parsing fail", async () => {
            fetch.mockResolvedValue(createMockResponse({
                ok: false,
                status: 500,
                jsonError: new Error("invalid json"),
                textError: new Error("stream closed")
            }));

            await expect(sharedApp.requestJson("/api/example")).rejects.toMatchObject({
                message: "Request failed.",
                status: 500,
                details: [],
                isNetworkError: false
            });
        });
    });

    describe("showToast", () => {
        test("renders a toast message and hides it after the configured duration", () => {
            jest.useFakeTimers();
            document.body.innerHTML = '<div id="toast"></div>';

            sharedApp.showToast("Saved successfully.", { duration: 1200 });

            const toast = document.getElementById("toast");
            expect(toast.textContent).toBe("Saved successfully.");
            expect(toast.classList.contains("show")).toBe(true);

            jest.advanceTimersByTime(1200);

            expect(toast.classList.contains("show")).toBe(false);
        });

        test("does nothing when the toast element does not exist", () => {
            expect(() => sharedApp.showToast("No toast target")).not.toThrow();
        });

        test("clears the previous timer when showToast is called repeatedly", () => {
            jest.useFakeTimers();
            document.body.innerHTML = '<div id="toast"></div>';
            const toast = document.getElementById("toast");

            sharedApp.showToast("First", { duration: 1000 });
            jest.advanceTimersByTime(900);
            sharedApp.showToast("Second", { duration: 1000 });
            jest.advanceTimersByTime(200);

            expect(toast.textContent).toBe("Second");
            expect(toast.classList.contains("show")).toBe(true);

            jest.advanceTimersByTime(800);
            expect(toast.classList.contains("show")).toBe(false);
        });
    });

    describe("showMessageFromQuery", () => {
        test("shows the message from the default query parameter", () => {
            jest.useFakeTimers();
            document.body.innerHTML = '<div id="toast"></div>';
            window.history.replaceState({}, "", "/?message=Welcome%20back");

            sharedApp.showMessageFromQuery();

            expect(document.getElementById("toast").textContent).toBe("Welcome back");
        });

        test("does nothing when the requested query parameter is missing", () => {
            document.body.innerHTML = '<div id="toast"></div>';
            window.history.replaceState({}, "", "/?notice=NoDefaultMessage");

            sharedApp.showMessageFromQuery();

            expect(document.getElementById("toast").textContent).toBe("");
        });
    });

    describe("bindLogoutButtons", () => {
        test("binds a logout click handler once and redirects after a successful logout request", async () => {
            document.body.innerHTML = '<button data-logout-btn><span id="icon">Logout</span></button>';
            const addEventListenerSpy = jest.spyOn(document, "addEventListener");
            fetch.mockResolvedValue(createMockResponse({
                ok: true,
                status: 204,
                contentType: "text/plain"
            }));

            sharedApp.bindLogoutButtons({
                logoutUrl: "/api/auth/logout",
                redirectTo: "#/logged-out"
            });
            sharedApp.bindLogoutButtons({
                logoutUrl: "/api/auth/logout",
                redirectTo: "#/logged-out"
            });

            expect(document.body.dataset.sharedLogoutBound).toBe("true");
            expect(addEventListenerSpy).toHaveBeenCalledTimes(1);

            const clickHandler = addEventListenerSpy.mock.calls[0][1];
            const preventDefault = jest.fn();

            await clickHandler({
                target: document.getElementById("icon"),
                preventDefault
            });

            expect(preventDefault).toHaveBeenCalledTimes(1);
            expect(fetch).toHaveBeenCalledWith("/api/auth/logout", { method: "POST" });
            expect(window.location.hash).toBe("#/logged-out");
        });

        test("redirects even when the logout request fails", async () => {
            document.body.innerHTML = '<button data-logout-btn>Logout</button>';
            const addEventListenerSpy = jest.spyOn(document, "addEventListener");
            fetch.mockRejectedValue(new Error("network down"));

            sharedApp.bindLogoutButtons({
                logoutUrl: "/api/auth/logout",
                redirectTo: "#/logged-out"
            });

            const clickHandler = addEventListenerSpy.mock.calls[0][1];
            const preventDefault = jest.fn();

            await clickHandler({
                target: document.querySelector("[data-logout-btn]"),
                preventDefault
            });

            expect(preventDefault).toHaveBeenCalledTimes(1);
            expect(window.location.hash).toBe("#/logged-out");
        });

        test("does not bind anything when required configuration is missing", () => {
            const addEventListenerSpy = jest.spyOn(document, "addEventListener");

            sharedApp.bindLogoutButtons({ logoutUrl: "", redirectTo: "#/logged-out" });
            sharedApp.bindLogoutButtons({ logoutUrl: "/api/auth/logout", redirectTo: "" });

            expect(addEventListenerSpy).not.toHaveBeenCalled();
            expect(document.body.dataset.sharedLogoutBound).toBeUndefined();
        });

        test("ignores clicks outside a logout button", async () => {
            document.body.innerHTML = '<div id="outside"></div>';
            const addEventListenerSpy = jest.spyOn(document, "addEventListener");

            sharedApp.bindLogoutButtons({
                logoutUrl: "/api/auth/logout",
                redirectTo: "#/logged-out"
            });

            const clickHandler = addEventListenerSpy.mock.calls[0][1];
            const preventDefault = jest.fn();

            await clickHandler({
                target: document.getElementById("outside"),
                preventDefault
            });

            expect(preventDefault).not.toHaveBeenCalled();
            expect(fetch).not.toHaveBeenCalled();
            expect(window.location.hash).toBe("");
        });
    });

    describe("escapeHtml", () => {
        test("escapes all HTML-sensitive characters in a normal case", () => {
            expect(sharedApp.escapeHtml(`<div class="x">'&"</div>`))
                .toBe("&lt;div class=&quot;x&quot;&gt;&#39;&amp;&quot;&lt;/div&gt;");
        });

        test("returns an empty string for nullish values", () => {
            expect(sharedApp.escapeHtml(null)).toBe("");
            expect(sharedApp.escapeHtml(undefined)).toBe("");
        });

        test("returns unchanged text when no escaping is required", () => {
            expect(sharedApp.escapeHtml("plain text 123")).toBe("plain text 123");
        });
    });

    describe("formatDateTime", () => {
        test("formats a valid date value using the current locale", () => {
            const value = "2026-05-05T10:15:00.000Z";

            expect(sharedApp.formatDateTime(value)).toBe(new Date(value).toLocaleString());
        });

        test("returns the configured empty text when the value is empty", () => {
            expect(sharedApp.formatDateTime("", { emptyText: "N/A" })).toBe("N/A");
            expect(sharedApp.formatDateTime(null)).toBe("-");
        });

        test("returns the original value when the date cannot be parsed", () => {
            expect(sharedApp.formatDateTime("not-a-date")).toBe("not-a-date");
        });
    });

    describe("setText", () => {
        test("sets textContent when the target element exists", () => {
            document.body.innerHTML = '<span id="target"></span>';

            sharedApp.setText("target", "Updated");

            expect(document.getElementById("target").textContent).toBe("Updated");
        });

        test("sets an empty string when the value is null", () => {
            document.body.innerHTML = '<span id="target">Existing</span>';

            sharedApp.setText("target", null);

            expect(document.getElementById("target").textContent).toBe("");
        });

        test("does nothing when the target element is missing", () => {
            expect(() => sharedApp.setText("missing", "Ignored")).not.toThrow();
        });
    });

    describe("setValue", () => {
        test("sets value when the target form control exists", () => {
            document.body.innerHTML = '<input id="target" value="">';

            sharedApp.setValue("target", "new@example.com");

            expect(document.getElementById("target").value).toBe("new@example.com");
        });

        test("sets an empty string when the value is null", () => {
            document.body.innerHTML = '<input id="target" value="existing">';

            sharedApp.setValue("target", null);

            expect(document.getElementById("target").value).toBe("");
        });

        test("does nothing when the target form control is missing", () => {
            expect(() => sharedApp.setValue("missing", "Ignored")).not.toThrow();
        });
    });
});
