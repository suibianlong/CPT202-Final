package com.cpt202.HerLink.dto.review;

import java.util.List;

public record ReviewHistorySectionResponse(
        String label,
        ReviewHistoryContextType contextType,
        List<ReviewHistoryItemResponse> items
) {
}
