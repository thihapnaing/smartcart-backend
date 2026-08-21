package nus.iss.smartcart.backend.chat.controller;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.chat.dto.ChatRequest;
import nus.iss.smartcart.backend.chat.dto.ChatResponse;
import nus.iss.smartcart.backend.chat.service.ChatService;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
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
    private final CurrentUserProvider currentUserProvider;

    public ChatController(ChatService chatService, CurrentUserProvider currentUserProvider) {
        this.chatService = chatService;
        this.currentUserProvider = currentUserProvider;
    }

    /** Starts a new chat session and returns the greeting + starter suggestions. */
    @PostMapping("/start")
    public ChatResponse startSession() {
        Long userId = currentUserProvider.getCurrentCustomer().getId();
        return chatService.startSession(userId);
    }

    /** Sends one user message, returns the assistant's reply (+ optional products). */
    @PostMapping("/{sessionId}")
    public ChatResponse sendMessage(@PathVariable String sessionId, @RequestBody ChatRequest request) {
        return chatService.handleMessage(sessionId, request.getMessage());
    }
}
