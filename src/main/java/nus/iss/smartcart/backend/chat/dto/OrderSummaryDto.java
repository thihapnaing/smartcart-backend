package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trimmed-down order shape used inside chat replies (order cards) for "track my order"
 * style questions - this is also the exact JSON shape smartcart-ai-service's Python routers
 * (chat.py) speak, so field names must stay in sync with their Order pydantic model.
 *
 * orderNumber/items back the detailed order card (order number, item thumbnails, "Buy again") -
 * see ToolDataService.getOrderHistory for how they're populated server-side.
 */
@Setter
@Getter
@NoArgsConstructor
public class OrderSummaryDto {

    private Long orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String status;
    private String orderDate;
    private List<OrderItemSummaryDto> items;

}
