package nus.iss.smartcart.backend.dto;

import nus.iss.smartcart.backend.model.OrderStatus;

// Describes the data a merchant's browser sends when changing an order's status.
// Example: { "status": "PACKED" }
public class UpdateOrderStatusRequest {

    private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}