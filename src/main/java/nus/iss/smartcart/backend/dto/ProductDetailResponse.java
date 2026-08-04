package nus.iss.smartcart.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ProductDetailResponse {
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String gender;
    private String categoryName;
    private String shopName;
    private List<ProductVariantDetail> variants;
}
