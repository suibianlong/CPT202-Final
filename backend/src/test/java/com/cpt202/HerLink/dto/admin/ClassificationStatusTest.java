package com.cpt202.HerLink.dto.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class ClassificationStatusTest {

    // 正常情况：传入 ACTIVE → 正确返回
    @Test
    void fromDatabaseValue_ValidActive_ShouldReturnActive() {
        ClassificationStatus status = ClassificationStatus.fromDatabaseValue("ACTIVE");
        assertEquals(ClassificationStatus.ACTIVE, status);
    }

    // 正常情况：传入小写 inactive → 正确返回（忽略大小写）
    @Test
    void fromDatabaseValue_LowercaseInactive_ShouldReturnInactive() {
        ClassificationStatus status = ClassificationStatus.fromDatabaseValue("inactive");
        assertEquals(ClassificationStatus.INACTIVE, status);
    }

    // 边界/异常情况：传入 null → 抛异常
    @Test
    void fromDatabaseValue_NullValue_ShouldThrowIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> {
            ClassificationStatus.fromDatabaseValue(null);
        });
    }

    // 异常情况：传入无效值 → 抛异常
    @Test
    void fromDatabaseValue_InvalidValue_ShouldThrowIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> {
            ClassificationStatus.fromDatabaseValue("INVALID");
        });
    }
}