package nus.iss.smartcart.backend.chat.service;

// Author: Htet Nandar (Grace)

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import nus.iss.smartcart.backend.chat.dto.ChatResponse;
import nus.iss.smartcart.backend.chat.model.ChatSession;
import nus.iss.smartcart.backend.chat.repository.ChatSessionRepository;
import nus.iss.smartcart.backend.config.PythonAiConfig;
import nus.iss.smartcart.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Exercises PythonAiChatService against a lightweight in-process HTTP server standing in for
 * smartcart-ai-service, rather than mocking the internal HttpClient (which is built inline in
 * the constructor and isn't injectable). Covers both the happy path and the "AI service
 * returned an error" path, since both touch the recent SonarCloud cleanup (AiServiceException,
 * the KEY_ROLE/ROLE_* constants, and the narrowed callPythonChat throws-clause).
 */
@ExtendWith(MockitoExtension.class)
class PythonAiChatServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private UserRepository userRepository;

    private HttpServer server;
    private PythonAiChatService service;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();

        PythonAiConfig aiConfig = new PythonAiConfig();
        aiConfig.setBaseUrl("http://localhost:" + server.getAddress().getPort());

        service = new PythonAiChatService(aiConfig, new ObjectMapper(), chatSessionRepository, userRepository);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void handleMessage_returnsReplyAndPersistsExchange_whenAiServiceRespondsSuccessfully() {
        server.createContext("/api/chat", exchange -> {
            byte[] bytes = "{\"reply\":\"Here are some picks for you\",\"products\":[]}"
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-1", "Show me new arrivals");

        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("Here are some picks for you"));
    }

    @Test
    void handleMessage_returnsFriendlyFallback_whenAiServiceReturnsNon200() {
        server.createContext("/api/chat", exchange -> {
            byte[] bytes = "boom".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        ChatResponse response = service.handleMessage("session-2", "Show me new arrivals");

        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("couldn't reach the AI assistant"));
    }

    @Test
    void startSession_returnsGreetingAndPersistsNewChatSession() {
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.startSession(null);

        assertNotNull(response.getSessionId());
        assertTrue(response.getReply().contains("SmartCart AI assistant"));
    }
}
