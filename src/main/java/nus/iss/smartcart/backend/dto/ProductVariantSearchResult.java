package nus.iss.smartcart.backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductVariantSearchResult {

    private Long id;
    private String size;
    private Integer stock;
}