package nus.iss.smartcart.backend.dto;

// Sent back after an order's status has been changed, confirming the new state.
public class UpdateOrderStatusResponse {

    private final Long orderId;
    private final String status;

    public UpdateOrderStatusResponse(Long orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }
}