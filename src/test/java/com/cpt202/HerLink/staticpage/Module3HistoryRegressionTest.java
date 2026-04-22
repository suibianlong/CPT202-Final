package com.cpt202.HerLink.staticpage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HerLinkHistoryRegressionTest {

    @Test
    void myResourcesPage_shouldExposeHistoryEntryAndModal() throws IOException {
        String html = Files.readString(Path.of("src/main/resources/static/my-resources.html"));
        String script = Files.readString(Path.of("src/main/resources/static/module3/module3.js"));

        assertTrue(html.contains("id=\"historyModal\""));
        assertTrue(html.contains("id=\"submissionHistoryBody\""));
        assertTrue(html.contains("id=\"versionHistoryBody\""));
        assertTrue(script.contains("data-history-id"));
        assertTrue(script.contains("resource.currentVersionNo != null"));
        assertTrue(script.contains("Feedback available"));
        assertTrue(script.contains("/submissions"));
        assertTrue(script.contains("/versions/compare"));
        assertTrue(script.contains("rollbackToVersion"));
        assertTrue(script.contains("function updateMediaFileAccept"));
        assertTrue(script.contains("mediaInput.accept = \".pdf,.doc,.docx\""));
    }
}
