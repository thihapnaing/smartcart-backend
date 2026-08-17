package nus.iss.smartcart.backend.dto;

import nus.iss.smartcart.backend.model.OrderStatus;
import java.time.LocalDateTime;

public class OrderResponse {

    private Long id;
    private String trackingNo;
    private Long deliveryPersonId;
    private OrderStatus status;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String shippingAddress;
    private String deliveryProofKey;
    private LocalDateTime deliveredAt;

    public OrderResponse() {
    }

    public OrderResponse(
            Long id,
            String trackingNo,
            Long deliveryPersonId,
            OrderStatus status,
            String firstName,
            String lastName,
            String phoneNumber,
            String shippingAddress,
            String deliveryProofKey,
            LocalDateTime deliveredAt
    ) {
        this.id = id;
        this.trackingNo = trackingNo;
        this.deliveryPersonId = deliveryPersonId;
        this.status = status;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.shippingAddress = shippingAddress;
        this.deliveryProofKey = deliveryProofKey;
        this.deliveredAt = deliveredAt;
    }

    public Long getId() {
        return id;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public Long getDeliveryPersonId() {
        return deliveryPersonId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getDeliveryProofKey() {
        return deliveryProofKey;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }
}