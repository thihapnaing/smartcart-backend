package nus.iss.smartcart.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Getter
public class MerchantOrderItemResponse {
    private Long orderId;
    private String productName;
    private String size;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String orderStatus;
    private LocalDateTime deliveredAt;
    private LocalDateTime orderDate;
    private String buyerFirstName;
    private String buyerLastName;
}
