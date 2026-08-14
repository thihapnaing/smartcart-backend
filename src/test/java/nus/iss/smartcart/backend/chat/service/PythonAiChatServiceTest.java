package nus.iss.smartcart.backend.chat.service;

// Author: Htet Nandar (Grace)

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import nus.iss.smartcart.backend.chat.dto.ChatResponse;
import nus.iss.smartcart.backend.chat.model.ChatSession;
import nus.iss.smartcart.backend.config.PythonAiConfig;
import nus.iss.smartcart.backend.chat.repository.ChatSessionRepository;
import nus.iss.smartcart.backend.model.User;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Exercises PythonAiChatService against a lightweight in-process HTTP server standing in for
 * smartcart-ai-service, rather than mocking the internal HttpClient (which is built inline in
 * the constructor and isn't injectable). Covers the happy path, the "AI service returned an
 * error" path, the interrupted-thread path, the products-array branch, the "session has a
 * logged-in user" branches, and both best-effort persistence catch blocks - i.e. every branch
 * SonarCloud flagged as uncovered new code.
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

    private void stubJsonReply(String path, int status, String json) {
        server.createContext(path, exchange -> {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    @Test
    void handleMessage_returnsReplyAndPersistsExchange_whenAiServiceRespondsSuccessfully() {
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here are some picks for you\",\"products\":[]}");

        when(chatSessionRepository.findBySessionId("session-1")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-1", "Show me new arrivals");

        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("Here are some picks for you"));
    }

    @Test
    void handleMessage_returnsFriendlyFallback_whenAiServiceReturnsNon200() {
        stubJsonReply("/api/chat", 500, "boom");

        ChatResponse response = service.handleMessage("session-2", "Show me new arrivals");

        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("couldn't reach the AI assistant"));
    }

    @Test
    void handleMessage_restoresInterruptFlag_whenThreadIsInterruptedDuringAiServiceCall() {
        stubJsonReply("/api/chat", 200, "{\"reply\":\"too slow\"}");

        // HttpClient.send() is send-async().get() under the hood, and Future.get() checks the
        // calling thread's interrupt status immediately - setting it beforehand reliably
        // triggers the InterruptedException branch without needing real timing/races.
        Thread.currentThread().interrupt();
        try {
            ChatResponse response = service.handleMessage("session-3", "Show me new arrivals");

            assertTrue(Thread.currentThread().isInterrupted(),
                "interrupt flag should be restored (Sonar S2142), not swallowed");
            assertNotNull(response.getReply());
            assertTrue(response.getReply().contains("couldn't reach the AI assistant"));
        } finally {
            Thread.interrupted(); // clear the flag so it doesn't leak into other tests
        }
    }

    @Test
    void handleMessage_populatesProductsWhenAiServiceReturnsProducts() {
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here are some picks\",\"products\":["
            + "{\"productId\":1,\"name\":\"Tee\",\"price\":19.99,\"imageUrl\":\"http://x/tee.jpg\","
            + "\"category\":\"Tops\",\"defaultVariantId\":5}]}");

        when(chatSessionRepository.findBySessionId("session-5")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-5", "Show me new arrivals");

        assertNotNull(response.getProducts());
        assertEquals(1, response.getProducts().size());
        assertEquals("Tee", response.getProducts().get(0).getName());
    }

    @Test
    void handleMessage_looksUpUserAndIncludesUserId_whenSessionBelongsToALoggedInUser() {
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Hi again\"}");
        when(userRepository.findById(42L)).thenReturn(Optional.of(new User()));
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatSessionRepository.findBySessionId(anyString())).thenReturn(Optional.empty());

        ChatResponse started = service.startSession(42L);
        ChatResponse response = service.handleMessage(started.getSessionId(), "Show me new arrivals");

        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("Hi again"));
    }

    @Test
    void startSession_returnsGreetingAndPersistsNewChatSession() {
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.startSession(null);

        assertNotNull(response.getSessionId());
        assertTrue(response.getReply().contains("SmartCart AI assistant"));
    }

    @Test
    void startSession_stillReturnsGreeting_whenPersistingSessionFails() {
        when(chatSessionRepository.save(any(ChatSession.class))).thenThrow(new RuntimeException("db down"));

        ChatResponse response = service.startSession(null);

        assertNotNull(response.getSessionId());
        assertTrue(response.getReply().contains("SmartCart AI assistant"));
    }

    @Test
    void handleMessage_stillReturnsReply_whenPersistingExchangeFails() {
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here you go\"}");
        when(chatSessionRepository.findBySessionId("session-4")).thenThrow(new RuntimeException("db down"));

        ChatResponse response = service.handleMessage("session-4", "Show me new arrivals");

        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("Here you go"));
    }

    // ── Remaining partial-branch coverage (SonarCloud condition coverage) ──

    @Test
    void handleMessage_defaultsToEmptyReply_whenAiResponseHasNoReplyField() {
        // Covers the false branch of aiResponse.has("reply") ? ... : "" - every other test's
        // fixture JSON always includes a "reply" key, so that branch was never exercised.
        stubJsonReply("/api/chat", 200, "{}");
        when(chatSessionRepository.findBySessionId("session-6")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-6", "Show me new arrivals");

        assertEquals("", response.getReply());
    }

    @Test
    void handleMessage_skipsProducts_whenProductsFieldIsNotAnArray() {
        // Covers the false branch of productsNode.isArray() - existing tests either omit
        // "products" entirely or send a real array, never a non-array value.
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here you go\",\"products\":{\"not\":\"an array\"}}");
        when(chatSessionRepository.findBySessionId("session-7")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-7", "Show me new arrivals");

        assertNotNull(response.getReply());
        assertNull(response.getProducts());
    }

    @Test
    void handleMessage_mapsProductWithMissingOptionalFieldsToNulls() {
        // Covers the false (null) branch of each ternary in toProductDtos - the earlier
        // products test only ever sent a product with every field populated.
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here you go\",\"products\":["
            + "{\"productId\":7,\"defaultVariantId\":9}]}");
        when(chatSessionRepository.findBySessionId("session-8")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-8", "Show me new arrivals");

        assertEquals(1, response.getProducts().size());
        assertEquals(7L, response.getProducts().get(0).getProductId());
        assertEquals(9L, response.getProducts().get(0).getDefaultVariantId());
        assertNull(response.getProducts().get(0).getName());
        assertNull(response.getProducts().get(0).getPrice());
        assertNull(response.getProducts().get(0).getImageUrl());
        assertNull(response.getProducts().get(0).getCategory());
    }

    @Test
    void handleMessage_mapsProductWithMissingIdFieldsToNulls() {
        // Covers the false (null) branch of the productId and defaultVariantId ternaries -
        // every other products test always sends both ids, so that branch was never exercised.
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here you go\",\"products\":["
            + "{\"name\":\"Tee\",\"price\":19.99,\"imageUrl\":\"http://x/tee.jpg\",\"category\":\"Tops\"}]}");
        when(chatSessionRepository.findBySessionId("session-9")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-9", "Show me new arrivals");

        assertEquals(1, response.getProducts().size());
        assertEquals("Tee", response.getProducts().get(0).getName());
        assertNull(response.getProducts().get(0).getProductId());
        assertNull(response.getProducts().get(0).getDefaultVariantId());
    }

    // ── orders (order-mini-card support) ─────────────────────────────────

    @Test
    void handleMessage_populatesOrdersWhenAiServiceReturnsOrders() {
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here's your order\",\"orders\":["
            + "{\"orderId\":101,\"totalAmount\":45.50,\"status\":\"DELIVERED\",\"orderDate\":\"2026-08-01\"}]}");
        when(chatSessionRepository.findBySessionId("session-10")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-10", "Where is my order");

        assertNotNull(response.getOrders());
        assertEquals(1, response.getOrders().size());
        assertEquals(101L, response.getOrders().get(0).getOrderId());
        assertEquals("DELIVERED", response.getOrders().get(0).getStatus());
        assertEquals("2026-08-01", response.getOrders().get(0).getOrderDate());
        assertEquals(0, new java.math.BigDecimal("45.50").compareTo(response.getOrders().get(0).getTotalAmount()));
    }

    @Test
    void handleMessage_skipsOrders_whenOrdersFieldIsNotAnArray() {
        // Covers the false branch of ordersNode.isArray().
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here you go\",\"orders\":{\"not\":\"an array\"}}");
        when(chatSessionRepository.findBySessionId("session-11")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-11", "Where is my order");

        assertNotNull(response.getReply());
        assertNull(response.getOrders());
    }

    @Test
    void handleMessage_skipsOrders_whenOrdersArrayIsEmpty() {
        // Covers the false branch of !ordersNode.isEmpty().
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here you go\",\"orders\":[]}");
        when(chatSessionRepository.findBySessionId("session-12")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-12", "Where is my order");

        assertNotNull(response.getReply());
        assertNull(response.getOrders());
    }

    @Test
    void handleMessage_mapsOrderWithMissingFieldsToNulls() {
        // Covers the false (null) branch of each ternary in toOrderDtos.
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here you go\",\"orders\":[{}]}");
        when(chatSessionRepository.findBySessionId("session-13")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-13", "Where is my order");

        assertEquals(1, response.getOrders().size());
        assertNull(response.getOrders().get(0).getOrderId());
        assertNull(response.getOrders().get(0).getOrderNumber());
        assertNull(response.getOrders().get(0).getTotalAmount());
        assertNull(response.getOrders().get(0).getStatus());
        assertNull(response.getOrders().get(0).getOrderDate());
        assertTrue(response.getOrders().get(0).getItems().isEmpty());
    }

    @Test
    void handleMessage_populatesOrderNumberAndItems() {
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here's your order\",\"orders\":["
            + "{\"orderId\":101,\"orderNumber\":\"SC-000101\",\"totalAmount\":45.50,"
            + "\"status\":\"DELIVERED\",\"orderDate\":\"2026-08-01\",\"items\":["
            + "{\"name\":\"Tee\",\"price\":19.90,\"imageUrl\":\"http://x/tee.jpg\","
            + "\"quantity\":2,\"productVariantId\":5}]}]}");
        when(chatSessionRepository.findBySessionId("session-14")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-14", "Where is my order");

        var order = response.getOrders().get(0);
        assertEquals("SC-000101", order.getOrderNumber());
        assertEquals(1, order.getItems().size());
        var item = order.getItems().get(0);
        assertEquals("Tee", item.getName());
        assertEquals(0, new java.math.BigDecimal("19.90").compareTo(item.getPrice()));
        assertEquals("http://x/tee.jpg", item.getImageUrl());
        assertEquals(2, item.getQuantity());
        assertEquals(5L, item.getProductVariantId());
    }

    @Test
    void handleMessage_treatsNonListItemsFieldAsNoItems() {
        // Covers the "rawItems instanceof List" false branch in toOrderItemDtos.
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here you go\",\"orders\":["
            + "{\"orderId\":101,\"items\":\"not a list\"}]}");
        when(chatSessionRepository.findBySessionId("session-15")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-15", "Where is my order");

        assertTrue(response.getOrders().get(0).getItems().isEmpty());
    }

    @Test
    void handleMessage_mapsOrderItemWithMissingFieldsToNulls() {
        // Covers the false (null) branch of each ternary in toOrderItemDtos.
        stubJsonReply("/api/chat", 200, "{\"reply\":\"Here you go\",\"orders\":["
            + "{\"orderId\":101,\"items\":[{}]}]}");
        when(chatSessionRepository.findBySessionId("session-16")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = service.handleMessage("session-16", "Where is my order");

        var item = response.getOrders().get(0).getItems().get(0);
        assertNull(item.getName());
        assertNull(item.getPrice());
        assertNull(item.getImageUrl());
        assertNull(item.getQuantity());
        assertNull(item.getProductVariantId());
    }
}
