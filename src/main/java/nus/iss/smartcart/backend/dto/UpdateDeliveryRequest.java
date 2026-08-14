package nus.iss.smartcart.backend.dto;

import nus.iss.smartcart.backend.model.OrderStatus;

public class UpdateDeliveryRequest {

    private String trackingNo;
    private Long deliveryPersonId;
    private OrderStatus status;

    public UpdateDeliveryRequest() {
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    public Long getDeliveryPersonId() {
        return deliveryPersonId;
    }

    public void setDeliveryPersonId(
            Long deliveryPersonId
    ) {
        this.deliveryPersonId = deliveryPersonId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
