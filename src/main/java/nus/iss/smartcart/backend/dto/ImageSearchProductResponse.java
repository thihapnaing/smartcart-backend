package nus.iss.smartcart.backend.dto;

import java.math.BigDecimal;

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

    // getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }
}