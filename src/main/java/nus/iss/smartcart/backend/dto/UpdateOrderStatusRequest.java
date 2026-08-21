package nus.iss.smartcart.backend.dto;

import nus.iss.smartcart.backend.model.OrderStatus;

public class UpdateOrderStatusRequest {

    private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}