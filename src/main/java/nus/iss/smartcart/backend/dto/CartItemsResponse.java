package nus.iss.smartcart.backend.dto;

import nus.iss.smartcart.backend.model.Cart;

import java.math.BigDecimal;
import java.util.List;

public class CartItemsResponse {

    public CartItemsResponse(List<CartItemDetail> cartItemDetails, BigDecimal cartTotal) {
        this.cartItemDetails = cartItemDetails;
        this.cartTotal = cartTotal;
    }
    private List<CartItemDetail> cartItemDetails;
    private BigDecimal cartTotal;

    public List<CartItemDetail> getCartItemDetails() {
        return cartItemDetails;
    }

    public BigDecimal getCartTotal() {
        return cartTotal;
    }
}
