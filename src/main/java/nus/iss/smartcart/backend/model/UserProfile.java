package nus.iss.smartcart.backend.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "smartcart_user_profile")
public class UserProfile {

    // Required by JPA - Hibernate instantiates entities via reflection when loading from the DB.
    public UserProfile() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String firstName;

    private String lastName;

    private String address;

    @Column(name = "postal_code")
    private String postalCode;

    private String phoneNumber;

    private String avatarUrl;

    private String shopName;
    
    
    // For Recommender AI
    @Column(name = "budget", precision = 10, scale = 2) // User budget limit
    private BigDecimal budget;

    @Column(name = "interests")
    private String interests; // Stored as "Linen,Casual,Summer"

    @Column(name = "preferred_categories")
    private String preferredCategories; // Stored as "Tops,Bottoms"
    
    
    
 // --- Getters & Setters For Recommender AI ---

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getPreferredCategories() {
        return preferredCategories;
    }

    public void setPreferredCategories(String preferredCategories) {
        this.preferredCategories = preferredCategories;
    }
 
    
    //Getters and Setters

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
    
    
 // --- Helper Methods for FastAPI DTO Conversion For Recommender AI ---
    public List<String> getInterestsList() {
        if (interests == null || interests.isBlank()) return List.of();
        return Arrays.stream(interests.split(","))
                     .map(String::trim)
                     .toList();
    }

    public List<String> getPreferredCategoriesList() {
        if (preferredCategories == null || preferredCategories.isBlank()) return List.of();
        return Arrays.stream(preferredCategories.split(","))
                     .map(String::trim)
                     .toList();
    }
    
}
