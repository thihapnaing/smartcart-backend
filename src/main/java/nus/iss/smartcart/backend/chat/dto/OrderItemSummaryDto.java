package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One line item inside an OrderSummaryDto's order card - name/price/imageUrl for display,
 * quantity for the "3 items" count, and productVariantId so the card's "Buy again" action can
 * call POST /api/cart/items directly without a second lookup. price is the unitPrice actually
 * paid on that order (OrderItem.unitPrice), not the product's current live price - those can
 * drift apart after the fact and the order card should reflect what was actually charged.
 */
@Setter
@Getter
@NoArgsConstructor
public class OrderItemSummaryDto {

    private String name;
    private BigDecimal price;
    private String imageUrl;
    private Integer quantity;
    private Long productVariantId;
}
