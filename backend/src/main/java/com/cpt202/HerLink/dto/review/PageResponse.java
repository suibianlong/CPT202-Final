package com.cpt202.HerLink.dto.review;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        List<T> items,
        int page,
        int pageSize,
        long total,
        String emptyMessage
) {
    public PageResponse(List<T> items, int page, int pageSize, long total) {
        this(items, page, pageSize, total, null);
    }
}
