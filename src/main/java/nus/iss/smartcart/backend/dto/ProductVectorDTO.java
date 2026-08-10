package nus.iss.smartcart.backend.dto;


public interface ProductVectorDTO {
    Long getProductId();
    String getProductName();
    String getDescription();
    String getGender();
    Double getPrice();
    String getImageUrl();
    String getAvailableSizes();
    Long getTotalStock();
    String getCategory();
}