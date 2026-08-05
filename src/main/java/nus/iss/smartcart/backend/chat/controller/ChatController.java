package nus.iss.smartcart.backend.chat.controller;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.chat.dto.ChatRequest;
import nus.iss.smartcart.backend.chat.dto.ChatResponse;
import nus.iss.smartcart.backend.chat.service.ChatService;
import org.springframework.web.bind.annotation.*;

/**
 * AI chat and agentic recommend-goods both go through this one endpoint - the assistant
 * decides internally whether to just reply with text or also attach product recommendations.
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** Starts a new chat session and returns the greeting + starter suggestions. */
    @PostMapping("/start")
    public ChatResponse startSession(@RequestParam(required = false) Long userId) {
        return chatService.startSession(userId);
    }

    /** Sends one user message, returns the assistant's reply (+ optional products). */
    @PostMapping("/{sessionId}")
    public ChatResponse sendMessage(@PathVariable String sessionId, @RequestBody ChatRequest request) {
        return chatService.handleMessage(sessionId, request.getMessage());
    }
}
