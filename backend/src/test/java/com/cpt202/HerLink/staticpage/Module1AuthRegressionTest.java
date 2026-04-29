package com.cpt202.HerLink.staticpage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Module1AuthRegressionTest {

    @Test
    void authPages_shouldPreserveSafeNextFlowAndRemoveRememberMe() throws IOException {
        String loginHtml = Files.readString(Path.of("../frontend/login.html"));
        String registerHtml = Files.readString(Path.of("../frontend/register.html"));
        String script = Files.readString(Path.of("../frontend/module1/module1.js"));

        assertFalse(loginHtml.contains("Remember me"));
        assertTrue(loginHtml.contains("id=\"loginCreateAccountLink\""));
        assertTrue(registerHtml.contains("id=\"registerLoginLink\""));
        assertTrue(script.contains("buildAuthPageUrl("));
        assertTrue(script.contains("sanitizeNextPath(next)"));
        assertTrue(script.contains("isAllowedNextPath(resolvedUrl.pathname)"));
        assertTrue(script.contains("window.location.href = buildAuthPageUrl("));
        assertTrue(script.contains("window.location.href = resolvePostLoginRedirect(currentUser);"));
    }

    @Test
    void authScript_shouldRejectExternalNextRedirects() throws IOException {
        String script = Files.readString(Path.of("../frontend/module1/module1.js"));

        assertTrue(script.contains("normalized.startsWith(\"http://\")"));
        assertTrue(script.contains("normalized.startsWith(\"https://\")"));
        assertTrue(script.contains("normalized.startsWith(\"//\")"));
        assertTrue(script.contains("normalized.startsWith(\"javascript:\")"));
        assertTrue(script.contains("normalized.startsWith(\"data:\")"));
        assertTrue(script.contains("normalized.startsWith(\"vbscript:\")"));
        assertTrue(script.contains("\"/module6/viewer-detail.html\""));
    }
}
