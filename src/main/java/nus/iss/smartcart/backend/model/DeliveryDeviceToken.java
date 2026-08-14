package nus.iss.smartcart.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_device_tokens")
public class DeliveryDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "delivery_person_id",
            nullable = false,
            unique = true
    )
    private Long deliveryPersonId;

    @Column(
            name = "fcm_token",
            nullable = false,
            length = 1000
    )
    private String fcmToken;

    public DeliveryDeviceToken() {
    }

    public Long getId() {
        return id;
    }

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