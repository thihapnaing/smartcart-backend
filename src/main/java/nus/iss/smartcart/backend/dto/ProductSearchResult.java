package nus.iss.smartcart.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
public class ProductSearchResult {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String shopName;
    private String categoryName;
    private String gender;
    private String color; //Author: Junior

    /** First variant's id - lets a quick "+ Add" action skip size selection for a single default variant. */
    private Long defaultVariantId;     // Author: Htet Nandar (Grace)
    private String status;
    private List<ProductVariantSearchResult> variants;
}
