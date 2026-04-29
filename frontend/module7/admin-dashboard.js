document.addEventListener("DOMContentLoaded", async () => {
    const admin = window.AdminModule;
    admin.bindAdminBasics();
    const currentUser = await admin.requireAdmin();
    if (!currentUser) return;

    const title = document.getElementById("adminWelcomeTitle");
    const text = document.getElementById("adminWelcomeText");
    if (title) {
        title.textContent = `Welcome, ${currentUser.name || "Administrator"}`;
    }
    if (text) {
        text.textContent = "Choose a management area for classification, tags, or approval workflow.";
    }
});
