package com.cpt202.HerLink.staticpage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Module6ScriptRegressionTest {

    @Test
    void viewerDetailScript_shouldPreferBackendCategoryNameAndAllowDetailFallback() throws IOException {
        String script = Files.readString(Path.of("src/main/resources/static/module6/module6.js"));

        assertTrue(script.contains("resource.categoryName || resolveCategoryName(resource.categoryId)"));
        assertTrue(script.contains("detail.categoryName || resolveCategoryName(detail.categoryId)"));
        assertTrue(script.contains("console.warn(\"Viewer category options failed to load.\", error);"));
        assertTrue(script.contains("if (categoryId == null || categoryId === \"\")"));
    }

    @Test
    void viewerDetailAssets_shouldIncludeCommentAndFeedbackFlow() throws IOException {
        String html = Files.readString(Path.of("src/main/resources/static/module6/viewer-detail.html"));
        String script = Files.readString(Path.of("src/main/resources/static/module6/module6.js"));
        String css = Files.readString(Path.of("src/main/resources/static/module6/module6.css"));

        assertTrue(html.contains("id=\"commentForm\""));
        assertTrue(html.contains("id=\"commentList\""));
        assertTrue(html.contains("id=\"feedbackForm\""));
        assertTrue(html.contains("id=\"feedbackList\""));
        assertTrue(script.contains("await loadViewerComments();"));
        assertTrue(script.contains("await loadViewerFeedbackHistory();"));
        assertTrue(script.contains("`${VIEWER_API_BASE}/${resourceId}/comments`"));
        assertTrue(script.contains("`${VIEWER_FEEDBACK_API_BASE}/mine`"));
        assertTrue(css.contains(".viewer-comment-list"));
        assertTrue(css.contains(".viewer-feedback-list"));
    }
}
