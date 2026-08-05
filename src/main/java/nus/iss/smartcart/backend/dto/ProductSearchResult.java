package nus.iss.smartcart.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
@Builder
@Getter
public class ProductSearchResult {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String shopName;
    private String categoryName;
    private String gender;
    /** First variant's id - lets a quick "+ Add" action skip size selection for a single default variant. */
    private Long defaultVariantId;     // Author: Htet Nandar (Grace)

}
