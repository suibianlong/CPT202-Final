package com.cpt202.HerLink.unit.enums;

import com.cpt202.HerLink.enums.UserRoleEnum;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UserRoleEnum Unit Test")
class UserRoleEnumTest {

    @Nested
    @DisplayName("getValue")
    class GetValueTests {

        @Test
        @DisplayName("Should return the public viewer value")
        void shouldReturnRegisteredViewerValue() {
            assertEquals("user", UserRoleEnum.REGISTERED_VIEWER.getValue());
        }

        @Test
        @DisplayName("Should return the public administrator value")
        void shouldReturnAdministratorValue() {
            assertEquals("reviewer", UserRoleEnum.ADMINISTRATOR.getValue());
        }
    }

    @Nested
    @DisplayName("getApiValue")
    class GetApiValueTests {

        @Test
        @DisplayName("Should return the API viewer role value")
        void shouldReturnRegisteredViewerApiValue() {
            assertEquals("REGISTERED_VIEWER", UserRoleEnum.REGISTERED_VIEWER.getApiValue());
        }

        @Test
        @DisplayName("Should return the API administrator role value")
        void shouldReturnAdministratorApiValue() {
            assertEquals("ADMINISTRATOR", UserRoleEnum.ADMINISTRATOR.getApiValue());
        }
    }

    @Nested
    @DisplayName("matches")
    class MatchesTests {

        @Test
        @DisplayName("Should match viewer by public value, API value, and alias")
        void shouldMatchViewerByAllSupportedInputs() {
            assertTrue(UserRoleEnum.REGISTERED_VIEWER.matches("user"));
            assertTrue(UserRoleEnum.REGISTERED_VIEWER.matches("REGISTERED_VIEWER"));
            assertTrue(UserRoleEnum.REGISTERED_VIEWER.matches("USER"));
        }

        @Test
        @DisplayName("Should match administrator by public value, API value, and alias")
        void shouldMatchAdministratorByAllSupportedInputs() {
            assertTrue(UserRoleEnum.ADMINISTRATOR.matches("reviewer"));
            assertTrue(UserRoleEnum.ADMINISTRATOR.matches("ADMINISTRATOR"));
            assertTrue(UserRoleEnum.ADMINISTRATOR.matches("REVIEWER"));
        }

        @Test
        @DisplayName("Should match values regardless of letter case")
        void shouldMatchValuesIgnoringCase() {
            assertTrue(UserRoleEnum.REGISTERED_VIEWER.matches("UsEr"));
            assertTrue(UserRoleEnum.ADMINISTRATOR.matches("ReViEwEr"));
        }

        @Test
        @DisplayName("Should return false for null values")
        void shouldReturnFalseForNull() {
            assertFalse(UserRoleEnum.REGISTERED_VIEWER.matches(null));
            assertFalse(UserRoleEnum.ADMINISTRATOR.matches(null));
        }

        @Test
        @DisplayName("Should return false for unknown values")
        void shouldReturnFalseForUnknownValue() {
            assertFalse(UserRoleEnum.REGISTERED_VIEWER.matches("guest"));
            assertFalse(UserRoleEnum.ADMINISTRATOR.matches("moderator"));
        }

        @Test
        @DisplayName("Should return false for values with leading or trailing whitespace")
        void shouldReturnFalseForWhitespaceWrappedValue() {
            assertFalse(UserRoleEnum.REGISTERED_VIEWER.matches(" user "));
            assertFalse(UserRoleEnum.ADMINISTRATOR.matches(" reviewer "));
        }
    }

    @Nested
    @DisplayName("fromValue")
    class FromValueTests {

        @Test
        @DisplayName("Should return viewer enum for public value")
        void shouldReturnViewerForPublicValue() {
            assertEquals(UserRoleEnum.REGISTERED_VIEWER, UserRoleEnum.fromValue("user"));
        }

        @Test
        @DisplayName("Should return viewer enum for API value and alias")
        void shouldReturnViewerForApiValueAndAlias() {
            assertEquals(UserRoleEnum.REGISTERED_VIEWER, UserRoleEnum.fromValue("REGISTERED_VIEWER"));
            assertEquals(UserRoleEnum.REGISTERED_VIEWER, UserRoleEnum.fromValue("USER"));
        }

        @Test
        @DisplayName("Should return administrator enum for public value")
        void shouldReturnAdministratorForPublicValue() {
            assertEquals(UserRoleEnum.ADMINISTRATOR, UserRoleEnum.fromValue("reviewer"));
        }

        @Test
        @DisplayName("Should return administrator enum for API value and alias")
        void shouldReturnAdministratorForApiValueAndAlias() {
            assertEquals(UserRoleEnum.ADMINISTRATOR, UserRoleEnum.fromValue("ADMINISTRATOR"));
            assertEquals(UserRoleEnum.ADMINISTRATOR, UserRoleEnum.fromValue("REVIEWER"));
        }

        @Test
        @DisplayName("Should throw when value is null")
        void shouldThrowWhenValueIsNull() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> UserRoleEnum.fromValue(null));
            assertEquals("User role cannot be null.", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw when value is unknown")
        void shouldThrowWhenValueIsUnknown() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> UserRoleEnum.fromValue("guest"));
            assertTrue(exception.getMessage().contains("Unknown user role"));
            assertTrue(exception.getMessage().contains("guest"));
        }

        @Test
        @DisplayName("Should throw when value has leading or trailing whitespace")
        void shouldThrowWhenValueHasLeadingOrTrailingWhitespace() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> UserRoleEnum.fromValue(" reviewer "));
            assertEquals("Unknown user role:  reviewer ", exception.getMessage());
        }

        @Test
        @DisplayName("Should expose both enum constants")
        void shouldExposeBothEnumConstants() {
            assertNotNull(UserRoleEnum.valueOf("REGISTERED_VIEWER"));
            assertNotNull(UserRoleEnum.valueOf("ADMINISTRATOR"));
            assertEquals(2, UserRoleEnum.values().length);
        }
    }
}
