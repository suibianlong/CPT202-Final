package com.cpt202.HerLink.config;

import com.cpt202.HerLink.enums.ContributorApplicationStatusEnum;
import com.cpt202.HerLink.enums.ResourceStatusEnum;
import com.cpt202.HerLink.enums.UserRoleEnum;
import com.cpt202.HerLink.util.PasswordHashService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DemoDataInitializer {

    @Bean
    public ApplicationRunner seedDemoData(JdbcTemplate jdbcTemplate,
                                          PasswordHashService passwordHashService,
                                          @Value("${HerLink.demo-data-enabled:false}") boolean demoDataEnabled) {
        return arguments -> {
            if (!demoDataEnabled) {
                return;
            }

            if (hasExistingSeedData(jdbcTemplate)) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            Timestamp currentTimestamp = Timestamp.valueOf(now);
            Timestamp earlierTimestamp = Timestamp.valueOf(now.minusDays(12));
            Timestamp approvedTimestamp = Timestamp.valueOf(now.minusDays(8));
            Timestamp draftTimestamp = Timestamp.valueOf(now.minusDays(2));
            Timestamp pendingTimestamp = Timestamp.valueOf(now.minusHours(20));

            jdbcTemplate.update(
                    "INSERT INTO category (categoryId, categoryTopic, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    1L, "places", "ACTIVE", 1, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO category (categoryId, categoryTopic, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    2L, "traditions", "ACTIVE", 1, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO category (categoryId, categoryTopic, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    3L, "stories", "ACTIVE", 0, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO category (categoryId, categoryTopic, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    4L, "objects", "ACTIVE", 0, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO category (categoryId, categoryTopic, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    5L, "educational materials", "ACTIVE", 0, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO category (categoryId, categoryTopic, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    6L, "other", "ACTIVE", 0, earlierTimestamp, currentTimestamp
            );

            jdbcTemplate.update(
                    "INSERT INTO resourceType (resourceTypeId, typeName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    1L, "photo", "ACTIVE", 1, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO resourceType (resourceTypeId, typeName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    2L, "video", "ACTIVE", 1, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO resourceType (resourceTypeId, typeName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    3L, "audio", "ACTIVE", 0, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO resourceType (resourceTypeId, typeName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    4L, "document", "ACTIVE", 0, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO resourceType (resourceTypeId, typeName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    5L, "extra link", "ACTIVE", 0, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO resourceType (resourceTypeId, typeName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    6L, "other", "ACTIVE", 0, earlierTimestamp, currentTimestamp
            );

            jdbcTemplate.update(
                    "INSERT INTO tag (tagId, tagName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    101L, "Temple", "ACTIVE", 1, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO tag (tagId, tagName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    102L, "Local Memory", "ACTIVE", 1, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO tag (tagId, tagName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    103L, "Festival", "ACTIVE", 1, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO tag (tagId, tagName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    104L, "Village", "ACTIVE", 1, earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO tag (tagId, tagName, status, usageCount, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?)",
                    105L, "Oral History", "ACTIVE", 0, earlierTimestamp, currentTimestamp
            );

            jdbcTemplate.update(
                    "INSERT INTO `user` (userId, username, email, passwordHash, role, isContributor, bio, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    11L, "System Admin", "admin@heritage.local", passwordHashService.hash("Admin123!"),
                    UserRoleEnum.ADMINISTRATOR.getValue(), false, "Reviewer account for contributor approval.", earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO `user` (userId, username, email, passwordHash, role, isContributor, bio, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    12L, "Approved Contributor", "contributor@heritage.local", passwordHashService.hash("Contributor123!"),
                    UserRoleEnum.REGISTERED_VIEWER.getValue(), true, "Approved contributor demo account.", earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO `user` (userId, username, email, passwordHash, role, isContributor, bio, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    13L, "Registered Viewer", "viewer@heritage.local", passwordHashService.hash("Viewer123!"),
                    UserRoleEnum.REGISTERED_VIEWER.getValue(), false, "Regular user demo account.", earlierTimestamp, currentTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO `user` (userId, username, email, passwordHash, role, isContributor, bio, createdAt, lastUpdatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    14L, "Pending Applicant", "pending@heritage.local", passwordHashService.hash("Pending123!"),
                    UserRoleEnum.REGISTERED_VIEWER.getValue(), false, "Pending contributor request demo account.", earlierTimestamp, currentTimestamp
            );

            jdbcTemplate.update(
                    "INSERT INTO contributorApplication (applicationId, userId, applicationReason, approvalStatus, submittedAt, reviewerId, reviewedAt, reviewComment, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    201L, 12L, "The applicant has prior heritage documentation experience.",
                    ContributorApplicationStatusEnum.APPROVED.getValue(),
                    Timestamp.valueOf(now.minusDays(9)), 11L, approvedTimestamp,
                    "The applicant can now create and submit resources.", approvedTimestamp
            );
            jdbcTemplate.update(
                    "INSERT INTO contributorApplication (applicationId, userId, applicationReason, approvalStatus, submittedAt, reviewerId, reviewedAt, reviewComment, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    202L, 14L, "The applicant would like to contribute local stories.",
                    ContributorApplicationStatusEnum.PENDING.getValue(),
                    Timestamp.valueOf(now.minusDays(1)), null, null,
                    null, Timestamp.valueOf(now.minusDays(1))
            );

            jdbcTemplate.update(
                    "INSERT INTO resource (id, contributorId, title, description, copyright, categoryId, resourceTypeId, place, previewImage, mediaUrl, status, reviewedAt, createdAt, updatedAt, archivedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    1001L, 12L, "Traditional Temple Entrance",
                    "Stone gateway details collected from the old village temple.",
                    "Community archive reference", 1L, 1L, "Suzhou", "resource-1001/temple-cover.jpg", "resource-1001/temple.jpg",
                    ResourceStatusEnum.DRAFT.getValue(), null, earlierTimestamp, draftTimestamp, null
            );
            jdbcTemplate.update(
                    "INSERT INTO resource (id, contributorId, title, description, copyright, categoryId, resourceTypeId, place, previewImage, mediaUrl, status, reviewedAt, createdAt, updatedAt, archivedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    1002L, 12L, "Festival Parade Recording",
                    "Video footage of the annual community heritage parade.",
                    "Community media contribution", 2L, 2L, "Hangzhou", "resource-1002/festival-cover.jpg", "resource-1002/festival.mp4",
                    ResourceStatusEnum.PENDING_REVIEW.getValue(), null, earlierTimestamp, pendingTimestamp, null
            );

            jdbcTemplate.update(
                    "INSERT INTO resourceSubmission (submissionId, resourceId, versionNo, submittedBy, submittedAt, submissionNote, statusSnapshot, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    301L, 1002L, 1, 12L, pendingTimestamp,
                    "First review submission for the festival archive.", ResourceStatusEnum.PENDING_REVIEW.getValue(), pendingTimestamp
            );

            jdbcTemplate.update(
                    "INSERT INTO resourceTag (id, resourceId, tagId) VALUES (?, ?, ?)",
                    401L, 1001L, 101L
            );
            jdbcTemplate.update(
                    "INSERT INTO resourceTag (id, resourceId, tagId) VALUES (?, ?, ?)",
                    402L, 1001L, 102L
            );
            jdbcTemplate.update(
                    "INSERT INTO resourceTag (id, resourceId, tagId) VALUES (?, ?, ?)",
                    403L, 1002L, 103L
            );
            jdbcTemplate.update(
                    "INSERT INTO resourceTag (id, resourceId, tagId) VALUES (?, ?, ?)",
                    404L, 1002L, 104L
            );
        };
    }

    private boolean hasExistingSeedData(JdbcTemplate jdbcTemplate) {
        Integer categoryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM category", Integer.class);
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class);
        return (categoryCount != null && categoryCount > 0) || (userCount != null && userCount > 0);
    }
}
