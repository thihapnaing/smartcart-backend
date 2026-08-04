package nus.iss.smartcart.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class CartItemDetail {

    private Long cartItemId;
    private String productName;
    private String imageUrl;
    private String size;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
