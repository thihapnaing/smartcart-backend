package nus.iss.smartcart.backend.dto;

// Author: Htet Nandar (Grace)

public class UpdateCartItemRequest {

    public UpdateCartItemRequest() {}

    private Integer quantity;

    public UpdateCartItemRequest(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
