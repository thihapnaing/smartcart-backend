package nus.iss.smartcart.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecommendationRequestDTO {

    @JsonProperty("top_k")
    private Integer topK;

    @JsonProperty("mode")
    private String mode;

    @JsonProperty("customer_profile")
    private CustomerProfileDTO customerProfile;

    public RecommendationRequestDTO() {}

    public RecommendationRequestDTO(Integer topK, String mode, CustomerProfileDTO customerProfile) {
        this.topK = topK;
        this.mode = mode;
        this.customerProfile = customerProfile;
    }

    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public CustomerProfileDTO getCustomerProfile() { return customerProfile; }
    public void setCustomerProfile(CustomerProfileDTO customerProfile) { this.customerProfile = customerProfile; }
}