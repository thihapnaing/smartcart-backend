package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import java.util.List;

/**
 * Response shape for both POST /api/chat/start and POST /api/chat/{sessionId}. `products` is
 * populated whenever the assistant's reply should show recommendation cards inline.
 */
public class ChatResponse {

    private String sessionId;
    private String reply;
    private List<ProductSummaryDto> products;
    private List<String> suggestions;

    public ChatResponse() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public List<ProductSummaryDto> getProducts() { return products; }
    public void setProducts(List<ProductSummaryDto> products) { this.products = products; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
}
