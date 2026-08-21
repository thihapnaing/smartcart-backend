package nus.iss.smartcart.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

//Author: Junior

@Getter
@Setter
public class ImageSearchProductResponse {

    private Long id;

    private String name;

    private BigDecimal price;

    private String imageUrl;

    private String shopName;

    private Double similarity;

    public ImageSearchProductResponse() {
    }

    public ImageSearchProductResponse(
            Long id,
            String name,
            BigDecimal price,
            String imageUrl,
            String shopName,
            Double similarity
    ) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.shopName = shopName;
        this.similarity = similarity;
    }
}