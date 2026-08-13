package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Trimmed-down order shape used inside chat replies (mini order cards) for "track my order"
 * style questions - this is also the exact JSON shape smartcart-ai-service's Python routers
 * (chat.py) speak, so field names must stay in sync with their Order pydantic model.
 */
@Setter
@Getter
public class OrderSummaryDto {

    private Long orderId;
    private BigDecimal totalAmount;
    private String status;
    private String orderDate;

    public OrderSummaryDto() {
        // Required by Jackson to deserialize the JSON smartcart-ai-service sends - fields are set via reflection.
    }

}
