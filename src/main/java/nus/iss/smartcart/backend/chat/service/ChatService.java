package nus.iss.smartcart.backend.chat.service;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.chat.dto.ChatResponse;

/**
 * Contract the controller depends on. PythonAiChatService implements this by calling out to
 * smartcart-ai-service (LLM + MCP tool-calling) - the controller and the Angular frontend
 * don't need to know that.
 */
public interface ChatService {
    ChatResponse startSession(Long userId);
    ChatResponse handleMessage(String sessionId, String message);
}
