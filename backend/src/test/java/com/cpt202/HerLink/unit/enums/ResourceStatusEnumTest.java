package com.cpt202.HerLink.unit.enums;

import com.cpt202.HerLink.enums.ResourceStatusEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Resource Status Enum - Unit Test")
class ResourceStatusEnumTest {

    @Test
    @DisplayName("Should return correct enum when given standard display value")
    void fromValue_WithStandardDisplayValue_ShouldReturnCorrectEnum() {
        assertEquals(ResourceStatusEnum.DRAFT, ResourceStatusEnum.fromValue("Draft"));
        assertEquals(ResourceStatusEnum.PENDING_REVIEW, ResourceStatusEnum.fromValue("Pending Review"));
        assertEquals(ResourceStatusEnum.APPROVED, ResourceStatusEnum.fromValue("Approved"));
        assertEquals(ResourceStatusEnum.REJECTED, ResourceStatusEnum.fromValue("Rejected"));
        assertEquals(ResourceStatusEnum.ARCHIVED, ResourceStatusEnum.fromValue("Archived"));
    }

    @Test
    @DisplayName("Should return correct enum when given alias value")
    void fromValue_WithAlias_ShouldReturnCorrectEnum() {
        assertEquals(ResourceStatusEnum.DRAFT, ResourceStatusEnum.fromValue("DRAFT"));
        assertEquals(ResourceStatusEnum.PENDING_REVIEW, ResourceStatusEnum.fromValue("PENDING_REVIEW"));
        assertEquals(ResourceStatusEnum.APPROVED, ResourceStatusEnum.fromValue("APPROVED"));
        assertEquals(ResourceStatusEnum.REJECTED, ResourceStatusEnum.fromValue("REJECTED"));
        assertEquals(ResourceStatusEnum.ARCHIVED, ResourceStatusEnum.fromValue("ARCHIVED"));
    }

    @Test
    @DisplayName("Should match correctly regardless of case")
    void fromValue_WithAnyCase_ShouldMatchCorrectly() {
        assertEquals(ResourceStatusEnum.DRAFT, ResourceStatusEnum.fromValue("draft"));
        assertEquals(ResourceStatusEnum.DRAFT, ResourceStatusEnum.fromValue("DRAFT"));
        assertEquals(ResourceStatusEnum.DRAFT, ResourceStatusEnum.fromValue("Draft"));
        assertEquals(ResourceStatusEnum.DRAFT, ResourceStatusEnum.fromValue("DrAfT"));

        assertEquals(ResourceStatusEnum.PENDING_REVIEW, ResourceStatusEnum.fromValue("pending review"));
        assertEquals(ResourceStatusEnum.APPROVED, ResourceStatusEnum.fromValue("approved"));
    }

    @Test
    @DisplayName("Should normalize and match with hyphen or underscore")
    void fromValue_WithHyphenOrUnderscore_ShouldNormalizeAndMatch() {
        assertEquals(ResourceStatusEnum.PENDING_REVIEW, ResourceStatusEnum.fromValue("pending_review"));
        assertEquals(ResourceStatusEnum.PENDING_REVIEW, ResourceStatusEnum.fromValue("pending-review"));
        assertEquals(ResourceStatusEnum.PENDING_REVIEW, ResourceStatusEnum.fromValue("PENDING-REVIEW"));
    }

    @Test
    @DisplayName("Should trim and match with extra spaces")
    void fromValue_WithExtraSpaces_ShouldTrimAndMatch() {
        assertEquals(ResourceStatusEnum.DRAFT, ResourceStatusEnum.fromValue("  Draft  "));
        assertEquals(ResourceStatusEnum.PENDING_REVIEW, ResourceStatusEnum.fromValue("  Pending   Review  "));
        assertEquals(ResourceStatusEnum.APPROVED, ResourceStatusEnum.fromValue("   Approved   "));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when value is null")
    void fromValue_WithNull_ShouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ResourceStatusEnum.fromValue(null));

        assertEquals("Resource status cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw unknown status exception for empty or blank value")
    void fromValue_WithEmptyOrBlank_ShouldThrowUnknownStatus() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> ResourceStatusEnum.fromValue(""));
        assertEquals("Unknown resource status: ", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> ResourceStatusEnum.fromValue("   "));
        assertEquals("Unknown resource status:    ", ex2.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for unknown status")
    void fromValue_WithUnknownStatus_ShouldThrowIllegalArgumentException() {
        String invalidValue = "EXPIRED";
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ResourceStatusEnum.fromValue(invalidValue));

        assertEquals("Unknown resource status: " + invalidValue, exception.getMessage());
    }

    @Test
    @DisplayName("Should return correct display value from getValue()")
    void getValue_ShouldReturnCorrectDisplayValue() {
        assertEquals("Draft", ResourceStatusEnum.DRAFT.getValue());
        assertEquals("Pending Review", ResourceStatusEnum.PENDING_REVIEW.getValue());
        assertEquals("Approved", ResourceStatusEnum.APPROVED.getValue());
        assertEquals("Rejected", ResourceStatusEnum.REJECTED.getValue());
        assertEquals("Archived", ResourceStatusEnum.ARCHIVED.getValue());
    }
}
