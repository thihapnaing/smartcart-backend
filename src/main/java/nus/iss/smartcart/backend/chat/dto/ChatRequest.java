package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

import lombok.Getter;
import lombok.Setter;

/** Body of POST /api/chat/{sessionId}. */
@Setter
@Getter
public class ChatRequest {

    private String message;

    public ChatRequest() {
        // Required by Jackson to deserialize the JSON request body - fields are set via reflection.
    }

}
