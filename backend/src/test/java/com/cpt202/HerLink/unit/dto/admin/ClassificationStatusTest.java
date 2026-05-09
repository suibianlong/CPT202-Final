package com.cpt202.HerLink.unit.dto.admin;

import com.cpt202.HerLink.dto.admin.ClassificationStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class ClassificationStatusTest {

    @Test
    void fromDatabaseValue_ValidActive_ShouldReturnActive() {
        ClassificationStatus status = ClassificationStatus.fromDatabaseValue("ACTIVE");
        assertEquals(ClassificationStatus.ACTIVE, status);
    }

    @Test
    void fromDatabaseValue_LowercaseInactive_ShouldReturnInactive() {
        ClassificationStatus status = ClassificationStatus.fromDatabaseValue("inactive");
        assertEquals(ClassificationStatus.INACTIVE, status);
    }

    @Test
    void fromDatabaseValue_MixedCaseActive_ShouldReturnActive() {
        ClassificationStatus status = ClassificationStatus.fromDatabaseValue("AcTiVe");
        assertEquals(ClassificationStatus.ACTIVE, status);
    }

    @Test
    void fromDatabaseValue_NullValue_ShouldThrowIllegalArgument() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ClassificationStatus.fromDatabaseValue(null);
        });
        assertEquals("Classification status cannot be null.", exception.getMessage());
    }

    @Test
    void fromDatabaseValue_EmptyValue_ShouldThrowIllegalArgument() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ClassificationStatus.fromDatabaseValue("");
        });
        assertEquals("Unsupported classification status: ", exception.getMessage());
    }

    @Test
    void fromDatabaseValue_WhitespaceWrappedValue_ShouldThrowIllegalArgument() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ClassificationStatus.fromDatabaseValue(" ACTIVE ");
        });
        assertEquals("Unsupported classification status:  ACTIVE ", exception.getMessage());
    }

    @Test
    void fromDatabaseValue_InvalidValue_ShouldThrowIllegalArgument() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ClassificationStatus.fromDatabaseValue("INVALID");
        });
        assertEquals("Unsupported classification status: INVALID", exception.getMessage());
    }
}
