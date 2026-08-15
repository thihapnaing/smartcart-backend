package nus.iss.smartcart.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
public class ProductDetailResponse {
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String gender;
    private String categoryName;
    private String shopName;
    private String status;
    private String color;
    private List<ProductVariantDetail> variants;
}
