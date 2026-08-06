package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatRequestTest {

    @Test
    void getterAndSetter_roundTripTheMessageField() {
        ChatRequest request = new ChatRequest();

        request.setMessage("Show me new arrivals");

        assertEquals("Show me new arrivals", request.getMessage());
    }
}
