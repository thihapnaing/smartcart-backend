package nus.iss.smartcart.backend.dto;

import java.math.BigDecimal;

public class RecommendedProductResponseDTO {

    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private String imageUrl;
    private String reason;
    private Double score;

    public RecommendedProductResponseDTO() {}

    public RecommendedProductResponseDTO(Long id, String name, String category, 
                                        BigDecimal price, String imageUrl, 
                                        String reason, Double score) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageUrl = imageUrl;
        this.reason = reason;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}