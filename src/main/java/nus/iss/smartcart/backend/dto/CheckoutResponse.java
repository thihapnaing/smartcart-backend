package nus.iss.smartcart.backend.dto;

import nus.iss.smartcart.backend.model.OrderStatus;
import nus.iss.smartcart.backend.model.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Getter
public class CheckoutResponse {
    private Long orderId;
    private List<CartItemDetail> cartItemDetails;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private DeliveryDetails deliveryDetails;
    private PaymentMethod paymentMethod;
}
