package com.cpt202.HerLink.unit.util;

import com.cpt202.HerLink.util.*;
import com.cpt202.HerLink.util.PasswordHashService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PasswordHashServiceTest {

    private final PasswordHashService passwordHashService = new PasswordHashService();

    @Nested
    @DisplayName("hash() method tests")
    class HashMethodTests {

        @Test
        @DisplayName("Hash valid password returns correctly formatted string")
        void hash_WithValidPassword_ReturnsCorrectFormattedHash() {
            String rawPassword = "HerLink@123456";
            String hashResult = passwordHashService.hash(rawPassword);

            assertNotNull(hashResult);
            assertFalse(hashResult.isBlank());

            String[] parts = hashResult.split(":");
            assertEquals(3, parts.length);

            assertDoesNotThrow(() -> Integer.parseInt(parts[0]));
            assertEquals(120000, Integer.parseInt(parts[0]));

            assertEquals(32, parts[1].length());
            assertEquals(64, parts[2].length());
        }

        @Test
        @DisplayName("Same password generates different hashes due to random salt")
        void hash_SamePasswordMultipleTimes_ReturnsDifferentHashes() {
            String password = "TestPass@123";
            String hash1 = passwordHashService.hash(password);
            String hash2 = passwordHashService.hash(password);

            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("Hash empty password generates valid hash without exception")
        void hash_WithEmptyPassword_GeneratesValidHash() {
            String emptyPassword = "";
            String hashResult = passwordHashService.hash(emptyPassword);

            assertNotNull(hashResult);
            assertEquals(3, hashResult.split(":").length);
        }

        @Test
        @DisplayName("Hash extremely long password generates valid hash without exception")
        void hash_WithExtraLongPassword_GeneratesValidHash() {
            String longPassword = "a".repeat(1000);
            assertDoesNotThrow(() -> {
                String hash = passwordHashService.hash(longPassword);
                assertNotNull(hash);
            });
        }
    }

    @Nested
    @DisplayName("matches() method tests")
    class MatchMethodTests {

        private static final String VALID_PASSWORD = "CorrectPass@789";
        private static final String INVALID_PASSWORD = "WrongPass@123";

        @Test
        @DisplayName("Correct password matches stored hash returns true")
        void matches_ValidPasswordAndHash_ReturnsTrue() {
            String storedHash = passwordHashService.hash(VALID_PASSWORD);
            boolean result = passwordHashService.matches(VALID_PASSWORD, storedHash);
            assertTrue(result);
        }

        @Test
        @DisplayName("Incorrect password against valid hash returns false")
        void matches_WrongPassword_ReturnsFalse() {
            String storedHash = passwordHashService.hash(VALID_PASSWORD);
            boolean result = passwordHashService.matches(INVALID_PASSWORD, storedHash);
            assertFalse(result);
        }

        @Test
        @DisplayName("Null stored hash returns false")
        void matches_StoredHashIsNull_ReturnsFalse() {
            boolean result = passwordHashService.matches(VALID_PASSWORD, null);
            assertFalse(result);
        }

        @Test
        @DisplayName("Blank stored hash returns false")
        void matches_StoredHashIsBlank_ReturnsFalse() {
            boolean result1 = passwordHashService.matches(VALID_PASSWORD, "");
            boolean result2 = passwordHashService.matches(VALID_PASSWORD, "   ");
            assertFalse(result1);
            assertFalse(result2);
        }

        @Test
        @DisplayName("Invalid hash format returns false")
        void matches_InvalidHashFormat_ReturnsFalse() {
            String invalidHash1 = "123456:abcdef";
            String invalidHash2 = "invalidhash";

            boolean result1 = passwordHashService.matches(VALID_PASSWORD, invalidHash1);
            boolean result2 = passwordHashService.matches(VALID_PASSWORD, invalidHash2);

            assertFalse(result1);
            assertFalse(result2);
        }

        @Test
        @DisplayName("Non-numeric iterations in hash returns false")
        void matches_HashWithNonNumericIterations_ReturnsFalse() {
            String invalidIterationHash = "not-a-number:abcdef123456:deadbeef";
            boolean result = passwordHashService.matches(VALID_PASSWORD, invalidIterationHash);
            assertFalse(result);
        }

        @Test
        @DisplayName("Invalid hex string in hash returns false")
        void matches_HashWithInvalidHexString_ReturnsFalse() {
            String invalidSaltHash = "120000:gggggggggggggggggggggggggggggggg:deadbeef";
            boolean result = passwordHashService.matches(VALID_PASSWORD, invalidSaltHash);
            assertFalse(result);
        }

        @Test
        @DisplayName("Mismatched digest length returns false")
        void matches_DigestLengthMismatch_ReturnsFalse() {
            String wrongLengthHash = "120000:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa:bbbb";
            boolean result = passwordHashService.matches(VALID_PASSWORD, wrongLengthHash);
            assertFalse(result);
        }

        @Test
        @DisplayName("Empty password matches correct hash returns true")
        void matches_EmptyPasswordWithCorrectHash_ReturnsTrue() {
            String emptyPwd = "";
            String hash = passwordHashService.hash(emptyPwd);
            boolean result = passwordHashService.matches(emptyPwd, hash);
            assertTrue(result);
        }
    }
}
