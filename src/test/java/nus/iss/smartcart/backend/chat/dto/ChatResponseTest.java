package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatResponseTest {

    @Test
    void gettersAndSetters_roundTripAllFields() {
        ChatResponse response = new ChatResponse();
        ProductSummaryDto product = new ProductSummaryDto();
        product.setName("Tee");

        response.setSessionId("session-1");
        response.setReply("Here you go");
        response.setProducts(List.of(product));
        response.setSuggestions(List.of("New arrivals", "Best picks for me"));

        assertEquals("session-1", response.getSessionId());
        assertEquals("Here you go", response.getReply());
        assertEquals(1, response.getProducts().size());
        assertEquals("Tee", response.getProducts().get(0).getName());
        assertEquals(List.of("New arrivals", "Best picks for me"), response.getSuggestions());
    }
}
