package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Trimmed-down product shape used inside chat replies and recommendation results (mini
 * product cards) - this is also the exact JSON shape smartcart-ai-service's Python
 * routers (chat.py) speak, so field names must stay in sync with their
 * Product pydantic models.
 */
@Setter
@Getter
public class ProductSummaryDto {

    private Long productId;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    /** First variant's id - lets the "+ Add" button skip size selection for a single default variant. */
    private Long defaultVariantId;

    public ProductSummaryDto() {
        // Required by Jackson to deserialize the JSON smartcart-ai-service sends - fields are set via reflection.
    }

}
