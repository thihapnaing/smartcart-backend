package nus.iss.smartcart.backend.dto;

import java.util.List;

public class CustomerProfileDTO {

    private String customerId;
    private List<String> interests;
    private List<String> cart;
    private List<String> recentlyViewed;
    private List<String> purchaseHistory;
    private List<String> preferredCategories;
    private Double budget;

    // Default Constructor
    public CustomerProfileDTO() {}

    // 7-Parameter Constructor
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

    // Getters and Setters
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