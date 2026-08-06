package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import java.math.BigDecimal;

/**
 * Trimmed-down product shape used inside chat replies and recommendation results (mini
 * product cards) - this is also the exact JSON shape smartcart-ai-service's Python
 * routers (chat.py) speak, so field names must stay in sync with their
 * Product pydantic models.
 */
public class ProductSummaryDto {

    private Long productId;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    /** First variant's id - lets the "+ Add" button skip size selection for a single default variant. */
    private Long defaultVariantId;

    // Required by Jackson to deserialize the JSON smartcart-ai-service sends - fields are set via reflection.
    public ProductSummaryDto() {}

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getDefaultVariantId() { return defaultVariantId; }
    public void setDefaultVariantId(Long defaultVariantId) { this.defaultVariantId = defaultVariantId; }
}
