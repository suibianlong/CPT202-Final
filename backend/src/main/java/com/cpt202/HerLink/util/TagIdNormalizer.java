package com.cpt202.HerLink.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// normalize tags‘ ID
public final class TagIdNormalizer {

    private TagIdNormalizer() {
    }

    public static List<Long> distinctNonNull(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }

        Set<Long> distinctTagIds = new LinkedHashSet<>();
        for (Long tagId : tagIds) {
            if (tagId != null) {
                distinctTagIds.add(tagId);
            }
        }

        return new ArrayList<>(distinctTagIds);
    }
}
