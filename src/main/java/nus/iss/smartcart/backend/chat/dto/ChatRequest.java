package nus.iss.smartcart.backend.chat.dto;

// Author: Htet Nandar (Grace)

/** Body of POST /api/chat/{sessionId}. */
public class ChatRequest {

    private String message;

    // Required by Jackson to deserialize the JSON request body - fields are set via reflection.
    public ChatRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
