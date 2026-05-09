package com.cpt202.HerLink.unit.util;

import com.cpt202.HerLink.util.*;
import com.cpt202.HerLink.util.TagIdNormalizer;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TagIdNormalizerTest {

    @Test
    void distinctNonNull_shouldRemoveDuplicatesAndNulls() {
        List<Long> input = new ArrayList<>(Arrays.asList(1L, 2L, 2L, null, 3L, 1L));

        List<Long> result = TagIdNormalizer.distinctNonNull(input);

        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    void distinctNonNull_shouldReturnEmptyListWhenInputIsNull() {
        List<Long> input = null;

        List<Long> result = TagIdNormalizer.distinctNonNull(input);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void distinctNonNull_shouldReturnEmptyListWhenInputIsEmpty() {
        List<Long> input = List.of();

        List<Long> result = TagIdNormalizer.distinctNonNull(input);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void distinctNonNull_shouldReturnEmptyListWhenInputContainsOnlyNulls() {
        List<Long> input = new ArrayList<>(Arrays.asList(null, null, null));

        List<Long> result = TagIdNormalizer.distinctNonNull(input);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(List.of(), result);
    }

    @Test
    void distinctNonNull_shouldKeepOriginalOrderOfFirstAppearance() {
        List<Long> input = new ArrayList<>(Arrays.asList(5L, 3L, 5L, 2L, 3L));

        List<Long> result = TagIdNormalizer.distinctNonNull(input);

        assertEquals(List.of(5L, 3L, 2L), result);
    }
}
