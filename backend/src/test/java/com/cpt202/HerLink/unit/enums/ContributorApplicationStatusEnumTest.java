package com.cpt202.HerLink.unit.enums;

import com.cpt202.HerLink.enums.ContributorApplicationStatusEnum;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("ContributorApplicationStatusEnum Unit Test")
class ContributorApplicationStatusEnumTest {

    @Nested
    @DisplayName("Enum constants and values")
    class EnumConstantsAndValuesTests {

        @Test
        @DisplayName("Should expose all supported statuses in declaration order")
        void values_ShouldExposeAllSupportedStatusesInDeclarationOrder() {
            assertArrayEquals(
                    new ContributorApplicationStatusEnum[]{
                            ContributorApplicationStatusEnum.PENDING,
                            ContributorApplicationStatusEnum.APPROVED,
                            ContributorApplicationStatusEnum.REJECTED,
                            ContributorApplicationStatusEnum.ARCHIVED
                    },
                    ContributorApplicationStatusEnum.values()
            );
        }

        @Test
        @DisplayName("Should return the expected persisted value for each status")
        void getValue_ShouldReturnExpectedPersistedValueForEachStatus() {
            assertAll(
                    () -> assertEquals("PENDING", ContributorApplicationStatusEnum.PENDING.getValue()),
                    () -> assertEquals("APPROVED", ContributorApplicationStatusEnum.APPROVED.getValue()),
                    () -> assertEquals("REJECTED", ContributorApplicationStatusEnum.REJECTED.getValue()),
                    () -> assertEquals("ARCHIVED", ContributorApplicationStatusEnum.ARCHIVED.getValue())
            );
        }
    }

    @Nested
    @DisplayName("fromValue method normal cases")
    class FromValueNormalCasesTests {

        @Test
        @DisplayName("Should convert every persisted value back to the matching enum")
        void fromValue_ShouldConvertEveryPersistedValueBackToTheMatchingEnum() {
            assertAll(
                    () -> assertSame(
                            ContributorApplicationStatusEnum.PENDING,
                            ContributorApplicationStatusEnum.fromValue(ContributorApplicationStatusEnum.PENDING.getValue())
                    ),
                    () -> assertSame(
                            ContributorApplicationStatusEnum.APPROVED,
                            ContributorApplicationStatusEnum.fromValue(ContributorApplicationStatusEnum.APPROVED.getValue())
                    ),
                    () -> assertSame(
                            ContributorApplicationStatusEnum.REJECTED,
                            ContributorApplicationStatusEnum.fromValue(ContributorApplicationStatusEnum.REJECTED.getValue())
                    ),
                    () -> assertSame(
                            ContributorApplicationStatusEnum.ARCHIVED,
                            ContributorApplicationStatusEnum.fromValue(ContributorApplicationStatusEnum.ARCHIVED.getValue())
                    )
            );
        }

        @ParameterizedTest(name = "Input \"{0}\" should map to {1}")
        @CsvSource({
                "pending, PENDING",
                "PeNdInG, PENDING",
                "approved, APPROVED",
                "ApPrOvEd, APPROVED",
                "rejected, REJECTED",
                "ReJeCtEd, REJECTED",
                "archived, ARCHIVED",
                "ArChIvEd, ARCHIVED"
        })
        @DisplayName("Should match supported values regardless of letter case")
        void fromValue_ShouldMatchSupportedValuesRegardlessOfLetterCase(String input,
                                                                        ContributorApplicationStatusEnum expectedStatus) {
            assertSame(expectedStatus, ContributorApplicationStatusEnum.fromValue(input));
        }
    }

    @Nested
    @DisplayName("fromValue method boundary and exception cases")
    class FromValueBoundaryAndExceptionCasesTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when input is null")
        void fromValue_ShouldThrowIllegalArgumentExceptionWhenInputIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ContributorApplicationStatusEnum.fromValue(null)
            );

            assertEquals("Contributor application status cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when input is an empty string")
        void fromValue_ShouldThrowIllegalArgumentExceptionWhenInputIsAnEmptyString() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ContributorApplicationStatusEnum.fromValue("")
            );

            assertEquals("Unknown contributor application status: ", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when input contains only whitespace")
        void fromValue_ShouldThrowIllegalArgumentExceptionWhenInputContainsOnlyWhitespace() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ContributorApplicationStatusEnum.fromValue("   ")
            );

            assertEquals("Unknown contributor application status:    ", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject values with leading or trailing whitespace because the input is not trimmed")
        void fromValue_ShouldRejectValuesWithLeadingOrTrailingWhitespaceBecauseTheInputIsNotTrimmed() {
            IllegalArgumentException leadingWhitespaceException = assertThrows(
                    IllegalArgumentException.class,
                    () -> ContributorApplicationStatusEnum.fromValue(" PENDING")
            );
            IllegalArgumentException trailingWhitespaceException = assertThrows(
                    IllegalArgumentException.class,
                    () -> ContributorApplicationStatusEnum.fromValue("APPROVED ")
            );
            IllegalArgumentException surroundingWhitespaceException = assertThrows(
                    IllegalArgumentException.class,
                    () -> ContributorApplicationStatusEnum.fromValue(" rejected ")
            );

            assertAll(
                    () -> assertEquals(
                            "Unknown contributor application status:  PENDING",
                            leadingWhitespaceException.getMessage()
                    ),
                    () -> assertEquals(
                            "Unknown contributor application status: APPROVED ",
                            trailingWhitespaceException.getMessage()
                    ),
                    () -> assertEquals(
                            "Unknown contributor application status:  rejected ",
                            surroundingWhitespaceException.getMessage()
                    )
            );
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when input is an unsupported status")
        void fromValue_ShouldThrowIllegalArgumentExceptionWhenInputIsAnUnsupportedStatus() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> ContributorApplicationStatusEnum.fromValue("UNDER_REVIEW")
            );

            assertEquals("Unknown contributor application status: UNDER_REVIEW", exception.getMessage());
        }
    }
}
