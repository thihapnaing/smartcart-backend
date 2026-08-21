package nus.iss.smartcart.backend.tools.service;

// Author: Htet Nandar (Grace)

import nus.iss.smartcart.backend.dto.CartItemsResponse;
import nus.iss.smartcart.backend.dto.ProductSearchResult;
import nus.iss.smartcart.backend.model.Gender;
import nus.iss.smartcart.backend.model.Order;
import nus.iss.smartcart.backend.model.OrderItem;
import nus.iss.smartcart.backend.repository.OrderRepository;
import nus.iss.smartcart.backend.service.CartService;
import nus.iss.smartcart.backend.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Backs the /internal/tools/** endpoints that smartcart-ai-service's MCP tools call into. This
 * is the *real* data layer for the two agent tools (get_order_history, search_products) - the
 * LLM never talks to this class directly, only through the Python MCP server acting as a proxy.
 *
 * Returns plain Map/List structures (not DTOs) since the shape needs to match exactly what's
 * documented in the MCP tool schemas in smartcart-ai-service/smartcart_mcp_server.py.
 */
@Service
public class ToolDataService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CartService cartService;

    public ToolDataService(OrderRepository orderRepository, ProductService productService, CartService cartService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.cartService = cartService;
    }

    @Transactional
    public Map<String, Object> getOrderHistory(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);

        // Walk orders in most-recent-first order and collect distinct category names in
        // first-seen order, so the resulting list reflects genuine purchase recency.
        List<String> purchasedCategories = new ArrayList<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                String categoryName = item.getProductVariant().getProduct().getCategory().getName();
                if (categoryName != null && !purchasedCategories.contains(categoryName)) {
                    purchasedCategories.add(categoryName);
                }
            }
        }
        String topCategory = purchasedCategories.isEmpty() ? null : purchasedCategories.get(0);

        BigDecimal totalSpent = orders.stream()
            .map(Order::getTotalAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> recentOrders = orders.stream()
            .limit(5)
            .map(o -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("orderId", o.getId());
                m.put("orderNumber", orderNumber(o));
                m.put("totalAmount", o.getTotalAmount());
                m.put("status", o.getStatus() != null ? o.getStatus().name() : null);
                m.put("orderDate", o.getOrderDate() != null ? o.getOrderDate().toString() : null);
                m.put("items", orderItemSummaries(o));
                return m;
            })
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topCategory", topCategory);
        result.put("purchasedCategories", purchasedCategories);
        result.put("totalSpent", totalSpent);
        result.put("orderCount", orders.size());
        result.put("recentOrders", recentOrders);
        return result;
    }

    /** Falls back to a zero-padded "SC-######" code when there's no real trackingNo yet
     * (e.g. an order that hasn't shipped) - the order card always needs something to show. */
    private String orderNumber(Order o) {
        String trackingNo = o.getTrackingNo();
        if (trackingNo != null && !trackingNo.isBlank()) {
            return trackingNo;
        }
        return o.getId() != null ? String.format("SC-%06d", o.getId()) : null;
    }

    private List<Map<String, Object>> orderItemSummaries(Order o) {
        return o.getItems().stream()
            .map(item -> {
                Map<String, Object> m = new LinkedHashMap<>();
                var variant = item.getProductVariant();
                var product = variant != null ? variant.getProduct() : null;
                m.put("name", product != null ? product.getName() : null);
                m.put("price", item.getUnitPrice());
                m.put("imageUrl", product != null ? product.getImageUrl() : null);
                m.put("quantity", item.getQuantity());
                m.put("productVariantId", variant != null ? variant.getId() : null);
                return m;
            })
            .toList();
    }

    /**
     * @param newestFirst when true, sorts by createdAt descending before applying the limit -
     *                    backs the "New arrivals" suggestion chip. Delegates to ProductService's
     *                    real Category/Gender-aware search (see ProductRepository.search).
     */
    public Map<String, Object> searchProducts(String category, BigDecimal maxPrice, String query, int limit, boolean newestFirst) {
        List<ProductSearchResult> results = productService.search(query, category, (Gender) null, newestFirst, Math.max(limit * 3, limit));

        List<Map<String, Object>> products = results.stream()
            .filter(p -> maxPrice == null || (p.getPrice() != null && p.getPrice().compareTo(maxPrice) <= 0))
            .limit(Math.max(1, limit))
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("productId", p.getId());
                m.put("name", p.getName());
                m.put("price", p.getPrice());
                m.put("imageUrl", p.getImageUrl());
                m.put("category", p.getCategoryName());
                m.put("defaultVariantId", p.getDefaultVariantId());
                return m;
            })
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("products", products);
        return result;
    }

    public Map<String, Object> getCart(Long userId) {
        CartItemsResponse cart = cartService.getCart(userId);
        List<Map<String, Object>> items = cart.getCartItemDetails().stream()
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productName", i.getProductName());
                    m.put("size", i.getSize());
                    m.put("quantity", i.getQuantity());
                    m.put("unitPrice", i.getUnitPrice());
                    m.put("subtotal", i.getSubtotal());
                    return m;
                })
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("cartTotal", cart.getCartTotal());
        result.put("itemCount", items.size());
        return result;
    }
}
