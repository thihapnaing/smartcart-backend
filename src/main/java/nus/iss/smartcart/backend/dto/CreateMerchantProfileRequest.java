package nus.iss.smartcart.backend.dto;

import org.springframework.web.multipart.MultipartFile;

public class CreateMerchantProfileRequest {

    private Long userId;

    private String businessName;

    private String uen;

    private String businessType;

    private String businessAddress;

    private String postalCode;

    private String contactNumber;

    private String productCategory;

    private String businessDescription;

    private Boolean pickupAvailable;

    private MultipartFile logo;

    private MultipartFile registrationDocument;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public CreateMerchantProfileRequest() {
    }


    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }


    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }


    public String getUen() {
        return uen;
    }

    public void setUen(String uen) {
        this.uen = uen;
    }


    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }


    public String getBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }


    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }


    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }


    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }


    public String getBusinessDescription() {
        return businessDescription;
    }

    public void setBusinessDescription(String businessDescription) {
        this.businessDescription = businessDescription;
    }


    public Boolean getPickupAvailable() {
        return pickupAvailable;
    }

    public void setPickupAvailable(Boolean pickupAvailable) {
        this.pickupAvailable = pickupAvailable;
    }


    public MultipartFile getLogo() {
        return logo;
    }

    public void setLogo(MultipartFile logo) {
        this.logo = logo;
    }


    public MultipartFile getRegistrationDocument() {
        return registrationDocument;
    }

    public void setRegistrationDocument(
            MultipartFile registrationDocument) {
        this.registrationDocument = registrationDocument;
    }
}