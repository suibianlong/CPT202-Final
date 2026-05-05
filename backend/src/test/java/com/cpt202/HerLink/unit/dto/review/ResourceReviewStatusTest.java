package com.cpt202.HerLink.unit.dto.review;

import com.cpt202.HerLink.dto.review.ResourceReviewStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cpt202.HerLink.enums.ResourceStatusEnum;

@DisplayName("ResourceReviewStatus Enum Unit Test")
class ResourceReviewStatusTest {

    @Test
    @DisplayName("Database values should match the corresponding ResourceStatusEnum values")
    void enumDatabaseValues_ShouldMatchResourceStatusEnumValues() {
        assertEquals(ResourceStatusEnum.DRAFT.getValue(), ResourceReviewStatus.DRAFT.toDatabaseValue());
        assertEquals(ResourceStatusEnum.PENDING_REVIEW.getValue(), ResourceReviewStatus.PENDING_REVIEW.toDatabaseValue());
        assertEquals(ResourceStatusEnum.APPROVED.getValue(), ResourceReviewStatus.APPROVED.toDatabaseValue());
        assertEquals(ResourceStatusEnum.REJECTED.getValue(), ResourceReviewStatus.REJECTED.toDatabaseValue());
        assertEquals(ResourceStatusEnum.ARCHIVED.getValue(), ResourceReviewStatus.ARCHIVED.toDatabaseValue());
    }

    @Test
    @DisplayName("toDatabaseValue method should return non-null and valid string")
    void toDatabaseValue_ShouldReturnNonNullAndValidString() {
        for (ResourceReviewStatus status : ResourceReviewStatus.values()) {
            String value = status.toDatabaseValue();
            assertNotNull(value);
            assertNotEquals("", value.trim());
        }
    }

    @Test
    @DisplayName("Valid database values should be converted to correct enum constants")
    void fromDatabaseValue_WithValidValue_ShouldReturnCorrectEnum() {
        assertEquals(ResourceReviewStatus.DRAFT, ResourceReviewStatus.fromDatabaseValue(ResourceStatusEnum.DRAFT.getValue()));
        assertEquals(ResourceReviewStatus.PENDING_REVIEW, ResourceReviewStatus.fromDatabaseValue(ResourceStatusEnum.PENDING_REVIEW.getValue()));
        assertEquals(ResourceReviewStatus.APPROVED, ResourceReviewStatus.fromDatabaseValue(ResourceStatusEnum.APPROVED.getValue()));
        assertEquals(ResourceReviewStatus.REJECTED, ResourceReviewStatus.fromDatabaseValue(ResourceStatusEnum.REJECTED.getValue()));
        assertEquals(ResourceReviewStatus.ARCHIVED, ResourceReviewStatus.fromDatabaseValue(ResourceStatusEnum.ARCHIVED.getValue()));
    }

    @Test
    @DisplayName("Enum should contain the correct number of constants")
    void enumConstants_ShouldHaveCorrectCount() {
        int expectedCount = 5;
        ResourceReviewStatus[] values = ResourceReviewStatus.values();
        assertEquals(expectedCount, values.length);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "UNKNOWN", "APPROVE", "REJECT", "DRAFTS"})
    @DisplayName("Invalid string values should throw IllegalArgumentException")
    void fromDatabaseValue_WithInvalidString_ShouldThrowException(String invalidValue) {
        assertThrows(IllegalArgumentException.class, () -> ResourceReviewStatus.fromDatabaseValue(invalidValue));
    }

    @Test
    @DisplayName("Null value should throw IllegalArgumentException")
    void fromDatabaseValue_WithNullValue_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ResourceReviewStatus.fromDatabaseValue(null));
        assertNotNull(exception.getMessage());
    }
}
