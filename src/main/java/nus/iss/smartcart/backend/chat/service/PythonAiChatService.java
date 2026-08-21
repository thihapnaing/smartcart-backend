package nus.iss.smartcart.backend.chat.service;

// Author: Htet Nandar (Grace)

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nus.iss.smartcart.backend.chat.dto.ChatResponse;
import nus.iss.smartcart.backend.chat.dto.OrderItemSummaryDto;
import nus.iss.smartcart.backend.chat.dto.OrderSummaryDto;
import nus.iss.smartcart.backend.chat.dto.ProductSummaryDto;
import nus.iss.smartcart.backend.config.PythonAiConfig;
import nus.iss.smartcart.backend.chat.model.ChatMessage;
import nus.iss.smartcart.backend.chat.model.ChatSession;
import nus.iss.smartcart.backend.chat.repository.ChatSessionRepository;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LIVE implementation. Calls out to smartcart-ai-service (a standalone Python/FastAPI
 * microservice nested at /smartcart-ai-service) instead of running the LLM/MCP agent loop
 * inside this backend.
 *
 * Conversation history is kept in memory per sessionId and sent with every request (the
 * Python service is stateless per-call) for speed, and mirrored into the chat_sessions /
 * chat_messages tables (ChatSession/ChatMessage) so conversations survive a restart and can be
 * queried later.
 */
@Service
public class PythonAiChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(PythonAiChatService.class);

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
        "Outfit under $50", "Best picks for me", "Best shoes for me", "New arrivals",
        "Where's my order?", "Show my spending"
    );

    private static final String GREETING = "Hi! I'm your SmartCart AI assistant. I can help you find outfits, "
        + "get recommendations, and check your order history. What can I do for you today?";

    // Author: Htet Nandar (Grace)
    // Repeated Map.of("role", ..., "content", ...) literals, pulled into constants.
    private static final String KEY_ROLE = "role";
    private static final String KEY_CONTENT = "content";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String KEY_IMAGE_URL = "imageUrl";

    private final PythonAiConfig aiConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;

    private record SessionState(Long userId, List<Map<String, Object>> history) {}

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    public PythonAiChatService(PythonAiConfig aiConfig, ObjectMapper objectMapper,
                                ChatSessionRepository chatSessionRepository, UserRepository userRepository) {
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.chatSessionRepository = chatSessionRepository;
        this.userRepository = userRepository;
        // Raw JDK HttpClient forced to HTTP/1.1 - uvicorn doesn't support h2c (cleartext
        // HTTP/2), and letting the JDK client negotiate on its own causes unreliable bodies.
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(aiConfig.getConnectTimeoutSeconds()))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    }

    @Override
    @Transactional
    public ChatResponse startSession(Long userId) {
        String sessionId = UUID.randomUUID().toString();

        List<Map<String, Object>> history = new ArrayList<>();
        history.add(Map.of(KEY_ROLE, ROLE_ASSISTANT, KEY_CONTENT, GREETING));
        sessions.put(sessionId, new SessionState(userId, history));

        try {
            User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
            ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .user(user)
                .build();
            session.addMessage(ChatMessage.builder().role(ROLE_ASSISTANT).content(GREETING).build());
            chatSessionRepository.save(session);
        } catch (Exception e) {
            // Persistence is best-effort - don't let a DB hiccup break the chat UX.
            log.error("Failed to persist new chat session {}", sessionId, e);
        }

        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setReply(GREETING);
        response.setSuggestions(DEFAULT_SUGGESTIONS);
        return response;
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public ChatResponse handleMessage(String sessionId, String message) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setSuggestions(DEFAULT_SUGGESTIONS);

        SessionState state = sessions.computeIfAbsent(sessionId, id -> {
            log.warn("No session state found for {} (backend likely restarted) - starting a fresh one.", id);
            return new SessionState(null, new ArrayList<>());
        });

        try {
            JsonNode aiResponse = callPythonChat(sessionId, message, state);

            String reply = aiResponse.has("reply") ? aiResponse.get("reply").asText() : "";
            response.setReply(reply);

            state.history().add(Map.of(KEY_ROLE, ROLE_USER, KEY_CONTENT, message));
            state.history().add(Map.of(KEY_ROLE, ROLE_ASSISTANT, KEY_CONTENT, reply));
            persistExchange(sessionId, state.userId(), message, reply);

            JsonNode productsNode = aiResponse.get("products");
            if (productsNode != null && productsNode.isArray() && !productsNode.isEmpty()) {
                List<Map<String, Object>> productsRaw =
                    objectMapper.convertValue(productsNode, List.class);
                response.setProducts(toProductDtos(productsRaw));
            }

            JsonNode ordersNode = aiResponse.get("orders");
            if (ordersNode != null && ordersNode.isArray() && !ordersNode.isEmpty()) {
                List<Map<String, Object>> ordersRaw =
                    objectMapper.convertValue(ordersNode, List.class);
                response.setOrders(toOrderDtos(ordersRaw));
            }
        } catch (InterruptedException e) {
            // Restore the interrupt flag instead of swallowing it, per Sonar S2142 - the thread
            // was told to stop, so callers up the stack need to see that signal too.
            Thread.currentThread().interrupt();
            log.error("Call to smartcart-ai-service was interrupted for session {}", sessionId, e);
            response.setReply("Sorry, I couldn't reach the AI assistant just now - make sure "
                + "smartcart-ai-service is running at " + aiConfig.getBaseUrl() + ". Please try again in a moment.");
        } catch (Exception e) {
            log.error("Call to smartcart-ai-service failed for session {}", sessionId, e);
            response.setReply("Sorry, I couldn't reach the AI assistant just now - make sure "
                + "smartcart-ai-service is running at " + aiConfig.getBaseUrl() + ". Please try again in a moment.");
        }

        return response;
    }

    /** Appends the user message + assistant reply to the persisted ChatSession, creating one on the fly
     * if it's missing (e.g. the backend restarted after the session started). Best-effort - a failure
     * here must never break the actual chat response. */
    private void persistExchange(String sessionId, Long userId, String userMessage, String assistantReply) {
        try {
            ChatSession session = chatSessionRepository.findBySessionId(sessionId).orElseGet(() -> {
                User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
                return chatSessionRepository.save(
                    ChatSession.builder().sessionId(sessionId).user(user).build());
            });
            session.addMessage(ChatMessage.builder().role(ROLE_USER).content(userMessage).build());
            session.addMessage(ChatMessage.builder().role(ROLE_ASSISTANT).content(assistantReply).build());
            chatSessionRepository.save(session);
        } catch (Exception e) {
            log.error("Failed to persist chat exchange for session {}", sessionId, e);
        }
    }

    // Author: Htet Nandar (Grace)
    // Declares the specific checked exceptions HttpClient.send() actually throws (IOException,
    // InterruptedException), not a blanket "throws Exception" that hides what can really fail.
    private JsonNode callPythonChat(String sessionId, String message, SessionState state) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("session_id", sessionId);
        body.put("message", message);
        body.set("history", objectMapper.valueToTree(state.history()));
        if (state.userId() != null) {
            body.put("user_id", state.userId());
        } else {
            body.putNull("user_id");
        }

        String requestBody = objectMapper.writeValueAsString(body);
        log.debug("Sending to Python AI: {}", requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(aiConfig.getBaseUrl() + "/api/chat"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(aiConfig.getReadTimeoutSeconds()))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            throw new AiServiceException(
                "AI service error: HTTP " + httpResponse.statusCode() + " - " + httpResponse.body());
        }

        return objectMapper.readTree(httpResponse.body());
    }

    private List<ProductSummaryDto> toProductDtos(List<Map<String, Object>> raw) {
        List<ProductSummaryDto> result = new ArrayList<>();
        for (Map<String, Object> p : raw) {
            ProductSummaryDto dto = new ProductSummaryDto();
            Object productId = p.get("productId");
            dto.setProductId(productId != null ? Long.valueOf(productId.toString()) : null);
            dto.setName(p.get("name") != null ? String.valueOf(p.get("name")) : null);
            Object price = p.get("price");
            dto.setPrice(price != null ? new BigDecimal(price.toString()) : null);
            Object imageUrl = p.get(KEY_IMAGE_URL);
            dto.setImageUrl(imageUrl != null ? String.valueOf(imageUrl) : null);
            dto.setCategory(p.get("category") != null ? String.valueOf(p.get("category")) : null);
            Object defaultVariantId = p.get("defaultVariantId");
            dto.setDefaultVariantId(defaultVariantId != null ? Long.valueOf(defaultVariantId.toString()) : null);
            result.add(dto);
        }
        return result;
    }

    private List<OrderSummaryDto> toOrderDtos(List<Map<String, Object>> raw) {
        List<OrderSummaryDto> result = new ArrayList<>();
        for (Map<String, Object> o : raw) {
            OrderSummaryDto dto = new OrderSummaryDto();
            Object orderId = o.get("orderId");
            dto.setOrderId(orderId != null ? Long.valueOf(orderId.toString()) : null);
            dto.setOrderNumber(o.get("orderNumber") != null ? String.valueOf(o.get("orderNumber")) : null);
            Object totalAmount = o.get("totalAmount");
            dto.setTotalAmount(totalAmount != null ? new BigDecimal(totalAmount.toString()) : null);
            dto.setStatus(o.get("status") != null ? String.valueOf(o.get("status")) : null);
            dto.setOrderDate(o.get("orderDate") != null ? String.valueOf(o.get("orderDate")) : null);
            dto.setItems(toOrderItemDtos(o.get("items")));
            result.add(dto);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<OrderItemSummaryDto> toOrderItemDtos(Object rawItems) {
        if (!(rawItems instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<OrderItemSummaryDto> result = new ArrayList<>();
        for (Object rawItem : list) {
            if (!(rawItem instanceof Map<?, ?> mapUntyped)) {
                continue;
            }
            Map<String, Object> item = (Map<String, Object>) mapUntyped;
            OrderItemSummaryDto dto = new OrderItemSummaryDto();
            dto.setName(item.get("name") != null ? String.valueOf(item.get("name")) : null);
            Object price = item.get("price");
            dto.setPrice(price != null ? new BigDecimal(price.toString()) : null);
            Object imageUrl = item.get(KEY_IMAGE_URL);
            dto.setImageUrl(imageUrl != null ? String.valueOf(imageUrl) : null);
            Object quantity = item.get("quantity");
            dto.setQuantity(quantity != null ? Integer.valueOf(quantity.toString()) : null);
            Object productVariantId = item.get("productVariantId");
            dto.setProductVariantId(productVariantId != null ? Long.valueOf(productVariantId.toString()) : null);
            result.add(dto);
        }
        return result;
    }
}
