package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response shape for both POST /api/chat/start and POST /api/chat/{sessionId}. `products` is
 * populated whenever the assistant's reply should show recommendation cards inline.
 */
@Setter
@Getter
public class ChatResponse {

    private String sessionId;
    private String reply;
    private List<ProductSummaryDto> products;
    private List<OrderSummaryDto> orders;
    private List<String> suggestions;

    public ChatResponse() {
        // Required by Jackson to serialize/deserialize this DTO - fields are set via reflection.
    }

}
