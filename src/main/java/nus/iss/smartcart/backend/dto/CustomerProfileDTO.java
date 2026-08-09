package nus.iss.smartcart.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CustomerProfileDTO {

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("interests")
    private List<String> interests;

    @JsonProperty("cart")
    private List<String> cart;

    @JsonProperty("recently_viewed")
    private List<String> recentlyViewed;

    @JsonProperty("purchase_history")
    private List<String> purchaseHistory;

    @JsonProperty("preferred_categories")
    private List<String> preferredCategories;

    @JsonProperty("budget")
    private Double budget;

    public CustomerProfileDTO() {}

    public CustomerProfileDTO(String customerId,
                              List<String> interests,
                              List<String> cart,
                              List<String> recentlyViewed,
                              List<String> purchaseHistory,
                              List<String> preferredCategories,
                              Double budget) {
        this.customerId = customerId;
        this.interests = interests;
        this.cart = cart;
        this.recentlyViewed = recentlyViewed;
        this.purchaseHistory = purchaseHistory;
        this.preferredCategories = preferredCategories;
        this.budget = budget;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public List<String> getCart() { return cart; }
    public void setCart(List<String> cart) { this.cart = cart; }

    public List<String> getRecentlyViewed() { return recentlyViewed; }
    public void setRecentlyViewed(List<String> recentlyViewed) { this.recentlyViewed = recentlyViewed; }

    public List<String> getPurchaseHistory() { return purchaseHistory; }
    public void setPurchaseHistory(List<String> purchaseHistory) { this.purchaseHistory = purchaseHistory; }

    public List<String> getPreferredCategories() { return preferredCategories; }
    public void setPreferredCategories(List<String> preferredCategories) { this.preferredCategories = preferredCategories; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }
}