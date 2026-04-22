package com.cpt202.HerLink.util;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TagIdNormalizerTest {

    @Test
    void distinctNonNull_shouldRemoveDuplicatesAndNulls() {
        // setup
        List<Long> input = new ArrayList<>(Arrays.asList(1L, 2L, 2L, null, 3L, 1L));

        // call
        List<Long> result = TagIdNormalizer.distinctNonNull(input);

        // assertion
        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    void distinctNonNull_shouldReturnEmptyListWhenInputIsNull() {
        // setup
        List<Long> input = null;

        // call
        List<Long> result = TagIdNormalizer.distinctNonNull(input);

        // assertion
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void distinctNonNull_shouldReturnEmptyListWhenInputIsEmpty() {
        // setup
        List<Long> input = List.of();

        // call
        List<Long> result = TagIdNormalizer.distinctNonNull(input);

        // assertion
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void distinctNonNull_shouldKeepOriginalOrderOfFirstAppearance() {
        // setup
        List<Long> input = new ArrayList<>(Arrays.asList(5L, 3L, 5L, 2L, 3L));

        // call
        List<Long> result = TagIdNormalizer.distinctNonNull(input);

        // assertion
        assertEquals(List.of(5L, 3L, 2L), result);
    }
}