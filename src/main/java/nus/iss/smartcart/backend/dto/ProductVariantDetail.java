package nus.iss.smartcart.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductVariantDetail {
    private Long productVariantId;
    private String size;
    private Integer stock;
}
