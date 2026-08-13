package nus.iss.smartcart.backend.dto;

public class OrderRequest {

    private String trackingNo;
    private Long deliveryPersonId;

    public OrderRequest() {
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
}