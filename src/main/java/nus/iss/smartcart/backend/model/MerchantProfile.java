package nus.iss.smartcart.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "smartcart_merchant")
public class MerchantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(name = "uen", nullable = false, unique = true, length = 50)
    private String uen;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "business_address", length = 255)
    private String businessAddress;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Column(name = "product_category", length = 100)
    private String productCategory;

    @Column(name = "business_description", columnDefinition = "TEXT")
    private String businessDescription;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "business_document_url", nullable = false)
    private String registrationDocumentUrl;

    @Column(name = "pickup_available", nullable = false)
    private Boolean pickupAvailable = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private MerchantVerificationStatus verificationStatus =
            MerchantVerificationStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MerchantProfile() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (pickupAvailable == null) {
            pickupAvailable = false;
        }

        if (verificationStatus == null) {
            verificationStatus = MerchantVerificationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}