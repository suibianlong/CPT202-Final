package com.cpt202.HerLink.staticpage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceVisibilityRegressionTest {

    @Test
    void viewerResourceMapper_shouldOnlyExposeApprovedResourcesPublicly() throws IOException {
        String mapperXml = Files.readString(Path.of("src/main/resources/mapper/ResourceMapper.xml"));

        String approvedListQuery = section(mapperXml, "<select id=\"selectApprovedResources", "</select>");
        String approvedDetailQuery = section(mapperXml, "<select id=\"selectApprovedById", "</select>");

        assertTrue(approvedListQuery.contains("WHERE r.status = 'Approved'"));
        assertTrue(approvedDetailQuery.contains("AND r.status = 'Approved'"));
        assertFalse(approvedListQuery.contains("'Archived'"));
        assertFalse(approvedDetailQuery.contains("'Archived'"));
    }

    @Test
    void contributorResourceMapper_shouldNotHideArchivedResourcesFromOwnerList() throws IOException {
        String mapperXml = Files.readString(Path.of("src/main/resources/mapper/ResourceMapper.xml"));
        String myResourcesQuery = section(mapperXml, "<select id=\"selectMyResources", "</select>");

        assertTrue(myResourcesQuery.contains("WHERE r.contributorId = #{contributorId}"));
        assertFalse(myResourcesQuery.contains("r.status != 'Archived'"));
        assertFalse(myResourcesQuery.contains("r.status &lt;&gt; 'Archived'"));
    }

    private String section(String text, String startToken, String endToken) {
        int start = text.indexOf(startToken);
        assertTrue(start >= 0, "Missing section: " + startToken);
        int end = text.indexOf(endToken, start);
        assertTrue(end >= 0, "Missing end token after: " + startToken);
        return text.substring(start, end);
    }
}
