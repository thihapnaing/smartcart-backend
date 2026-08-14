package nus.iss.smartcart.backend.dto;

public class DeviceTokenRequest {

    private Long deliveryPersonId;
    private String fcmToken;

    public Long getDeliveryPersonId() {
        return deliveryPersonId;
    }

    public void setDeliveryPersonId(
            Long deliveryPersonId
    ) {
        this.deliveryPersonId = deliveryPersonId;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}